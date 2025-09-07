package com.serverclient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private List<ChatHandler> list = new ArrayList<>();

    private static final int PORT = 9500;

    // 동시 클릭 순서 결정용 블로킹 큐
    // 1024개 크기 큐, true로 FIFO 순서로 스레드 접근
    private BlockingQueue<ClickTask> queue = new ArrayBlockingQueue<>(1024, true);

    static class ClickTask {
        String player;
        int x, y;
        int clientSeq;
        ClickTask(String player, int x, int y, int clientSeq) {
            this.player = player;
            this.x = x; this.y = y;
            this.clientSeq = clientSeq;
        }
    }

    // 이벤트마다 붙힐 서버 Seq 생성. 클라이언트가 서버seq를 보고 중복, 역순 수신 걸러내기 위함.
    private long serverSeq = 0L;

    // 게임 엔진 서버관리
    private GameEngine engine;
    private HashMap<String,Integer> scores = new HashMap<>();
    private boolean running = false;
    private long endsAtMillis = 0L;

    private Thread gameLoop;

    // 서버에서 정해진 시간 이후 작업을 실행하는 예약 실행기
    // seconds 이후에 한꺼번에 서버에서 게임 종료를 선언하기 위해 필요
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public ChatServer() {
        try {
            serverSocket = new ServerSocket (PORT);
            System.out.println("서버 준비 완료");
            while( true ) {
                Socket socket = serverSocket.accept();      // 새로운 접속 대기
                ChatHandler handler = new ChatHandler(socket, this, list);      // 접속하는 클라이언트마다 핸들러 생성
                list.add(handler);      // 핸들러를 서버의 리스트에 등록 후 시작
                handler.start();        // Thread 상속으로, 각 클라를 독립 스레드로 handler의 run() 메서드 실행
            }
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

    // 로비 방송용
    public void broadcastLobby() {
        InfoDTO dto = new InfoDTO();
        dto.setCommand( Info.LOBBY_UPDATE );

        List<PlayerStatus> playerStatus = new ArrayList<>();
        for( ChatHandler h : list ) {
            playerStatus.add(new PlayerStatus( h.getNickname(), h.isReady() ));
        }
        dto.setLobby(playerStatus);
        broadcast(dto);
    }

    // 전체 브로드캐스트 (ObjectOutputStream 사용)
    public void broadcast(InfoDTO dto) {
        for( ChatHandler h : list ) {
            h.send(dto);
        }
    }

    // 모두 ready인지 검사
    public boolean allReady() {
        if( list.isEmpty() ) {
            return false;
        }
        for( ChatHandler h : list ) {
            if( !h.isReady() ) {
                return false;
            }
        }
        return true;
    }

    // 특정 클라이언트에게 보내기
    public void sendTo(String nickname, InfoDTO dto) {
        for ( ChatHandler h : list ) {
            if ( nickname != null && nickname.equals( h.getNickname() ) ) {
                h.send(dto);
                break;
            }
        }
    }

    // synchronized를 통해 중복 불가능하게 게임 스타트
    public synchronized void startGame() {
        if (!allReady()) return;

        final int W = 25, H = 25;
        final int seconds = 60;
        final long seed = System.currentTimeMillis();   // 현재 시간을 기준으로 rnd 시드생성

        engine = new GameEngine(W, H, seed);
        scores.clear();
        // 각 클라이언트의 점수를 0으로 설정
        for ( ChatHandler h : list ) {
            scores.put(h.getNickname(), 0);
        }

        running = true;
        endsAtMillis = System.currentTimeMillis() + seconds * 1000L;    // 현재시각 밀리초 기준으로 종료시각 설정

        // START_GAME
        InfoDTO start = new InfoDTO();
        start.setCommand(Info.START_GAME);
        start.setSeed(seed);
        start.setDurationSec(seconds);
        start.setWidth(W);
        start.setHeight(H);
        broadcast(start);

        // BOARD_SNAPSHOT
        // 게임 시작시 모두 같은 보드로 시작하도록 스냅샷 방송
        InfoDTO snap = new InfoDTO();
        snap.setCommand(Info.BOARD_SNAPSHOT);
        snap.setServerSeq(++serverSeq);
        snap.setBoard(copyBoard(engine.getBoard()));
        broadcast(snap);

        // 타이머 싱크
        InfoDTO t = new InfoDTO();
        t.setCommand(Info.TIMER_SYNC);
        t.setEndsAtMillis(endsAtMillis);
        broadcast(t);

        // 클릭을 블로킹 큐에 넣고 하나씩 꺼내 처리하는 루프
        startGameLoop();

        // 종료 예약
        // schedule(Runnable command, long delay, TimeUnit unit);
        scheduler.schedule(this::endGame, seconds, TimeUnit.SECONDS);       // endGame 메서드 지정 seconds 이후에 호출하기
    }

    // 클릭을 블로킹 큐에 넣고 하나씩 꺼내 처리하는 루프
    private void startGameLoop() {
        if ( gameLoop != null && gameLoop.isAlive() ) return;       // 이미 실행중이면 새로 루프를 만들지 않음
        gameLoop = new Thread(() -> {
            while (true) {
                try {
                    ClickTask task = queue.take();      // 큐에 뭔가가 들어오면 클릭 이벤트 FIFO로 꺼냄
                    handleClick(task);                  // 클릭 이벤트 처리
                } catch (InterruptedException ie) {     // 스레드 중단 시 루프 종료
                    return;
                } catch (Exception ex) {                // 에러 로그 추적
                    ex.printStackTrace();
                }
            }
        }, "게임 루프");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    // 원본 보드는 서버에서 관리하고 각 클라이언트로 복사본 보내기용
    private CellColor[][] copyBoard(CellColor[][] src) {
        int H = src.length;
        int W = src[0].length;
        CellColor[][] copy = new CellColor[H][W];
        for ( int y = 0; y < H; y++ ) {
            for ( int x = 0; x < W; x++ ) {
                copy[y][x] = src[y][x];
            }
        }
        return copy;
    }

    // 핸들러에서 블로킹 큐로 투입
    public void enqueueClick(String player, int x, int y, int clientSeq) {
        ClickTask t = new ClickTask(player, x, y, clientSeq);
        // 큐에 원소 추가
        if ( !queue.offer(t) ) {
            // 원소 추가 실패시 동작
            InfoDTO rej = new InfoDTO();
            rej.setCommand(Info.CLICK_REJECT);
            rej.setClientSeq(clientSeq);
            rej.setReason("큐 포화");
            sendTo(player, rej);
        }
    }

    // 블로킹 큐 순서대로 처리
    private synchronized void handleClick(ClickTask t) {
        // 클릭을 서버에서 처리할 때 서버seq 증가
        long sSeq = ++serverSeq;

        ClickResult res = engine.tryClick(t.x, t.y);
        if ( res.success ) {
            // 요청 성공 시, 점수 누적
            int newScore = scores.getOrDefault(t.player, 0) + res.changed.size();
            scores.put(t.player, newScore);

            // 클릭이 성공했다고 seq넘버와 함께 클릭한 플레이어에게 전송
            // 스코어 획득 전송
            InfoDTO ok = new InfoDTO();
            ok.setCommand(Info.CLICK_OK);
            ok.setClientSeq(t.clientSeq);
            ok.setServerSeq(sSeq);
            ok.setDiff(res.changed);
            ok.setScore(res.changed.size());
            sendTo(t.player, ok);

            // 판이 바뀌었으므로, 모든 플레이어에게 보드 변경 방송
            InfoDTO diff = new InfoDTO();
            diff.setCommand(Info.BOARD_DIFF);
            diff.setServerSeq(sSeq);
            diff.setDiff(res.changed);
            broadcast(diff);

            // 순위표에서 표시할 플레이어별 점수 클라이언트에게 전송
            InfoDTO up = new InfoDTO();
            up.setCommand(Info.SCORE_UPDATE);
            up.setServerSeq(sSeq);
            up.setPlayer(t.player);
            up.setScore(newScore);
            broadcast(up);

            // 클릭 불가능한 상황엔, 보드 재생성 및 스냅샷 재전송
            if ( !engine.hasAnyClick() ) {
                engine.regenerateBoard();
                InfoDTO snap = new InfoDTO();
                snap.setCommand(Info.BOARD_SNAPSHOT);
                snap.setServerSeq(++serverSeq);
                snap.setBoard(copyBoard(engine.getBoard()));
                broadcast(snap);
            }
        } else {    // 클릭 요청 실패시
            InfoDTO rej = new InfoDTO();
            rej.setCommand(Info.CLICK_REJECT);
            rej.setClientSeq(t.clientSeq);
            rej.setServerSeq(sSeq);
            rej.setReason(res.reason);
            sendTo(t.player, rej);
        }
    }

    // 게임 종료 처리 추가
    private synchronized void endGame() {
        if ( !running ) return;
        running = false;

        // 결과 집계
        List<PlayerScore> results = new ArrayList<>();
        for (var e : scores.entrySet()) {   // entrySet으로 닉네임(key)과 점수(value) 빼오기
            results.add(new PlayerScore(e.getKey(), e.getValue()));
        }
        results.sort((a,b) -> Integer.compare(b.getScore(), a.getScore())); // 내림차순으로 정렬

        // GAME_OVER 방송
        InfoDTO over = new InfoDTO();
        over.setCommand(Info.GAME_OVER);
        over.setResults(results);       // 점수 내림차순으로 정렬된 이름과 스코어 전송
        over.setServerSeq(++serverSeq);
        broadcast(over);

        // 종료 후 초기화
        engine = null;
        endsAtMillis = 0L;
        queue.clear();
        serverSeq = 0L;      // 시퀀스 리셋
        scores.clear();      // 점수 초기화

        // 모두 Ready 해제
        for ( ChatHandler h : list ) {
            h.setReady(false);
        }
        
        broadcastLobby();
    }


    public static void main(String[] args) {
        new ChatServer();
    }
}
