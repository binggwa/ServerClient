package com.serverclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ChatHandler extends Thread {

    private ObjectInputStream reader;       // 클라이언트에서 객체(InfoDTO) 읽는 스트림
    private ObjectOutputStream writer;      // 클라이언트로 객체(InfoDTO) 보내는 스트림
    private Socket socket;
    private List<ChatHandler> list;
    private ChatServer server;              // 서버 참조용(로비 방송/시작검사용)

    private String nickname;

    private int lastClientSeq = 0;          // 플레이어별 클릭 요청 클라이언트 시퀀스

    public String getNickname() {
        return nickname;
    }

    private boolean ready = false;

    public boolean isReady() {
        return ready;
    }
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public ChatHandler(Socket socket, ChatServer server, List<ChatHandler> list) {
        this.socket = socket;
        this.server = server;
        this.list = list;
        writer = new ObjectOutputStream(socket.getOutputStream());
        reader = new ObjectInputStream(socket.getInputStream());
    }

    // 서버로 송신용
    public void send(InfoDTO dto) {
        writer.writeObject(dto);
        writer.flush();
    }

    // 실행 스레드, 클라이언트에서 메시지를 계속 읽고 처리
    public void run(){
        InfoDTO dto;
        String nickName;
        try {
            while( true ) {
                dto = (InfoDTO)reader.readObject();     // 클라이언트 메시지 수신
                nickName = dto.getNickName();
                // 사용자가 접속을 끊었을 경우, 프로그램을 끝내서는 안되고 남은 사용자들에게 퇴장메시지를 보내줘야 한다.
                if(dto.getCommand() == Info.EXIT) {
                    InfoDTO sendDto = new InfoDTO();

                    // 나가려고 ext를 보낸 클라이언트에게 답변 보내기
                    sendDto.setCommand(Info.EXIT);
                    send(sendDto);

                    // 연결 해제
                    reader.close();
                    writer.close();
                    socket.close();

                    // 남아있는 클라이언트에게 퇴장메시지 보내기
                    list.remove(this);
                    sendDto.setCommand(Info.SEND);
                    sendDto.setMessage(nickName + "님 퇴장하셨습니다.");
                    server.broadcast(sendDto);
                    server.broadcastLobby();
                    break;
                } else if (dto.getCommand() == Info.JOIN) {     // 참가 시 행동
                    // 모든 사용자에게 메시지 보내기
                    InfoDTO sendDto = new InfoDTO();
                    sendDto.setCommand(Info.SEND);
                    sendDto.setMessage(nickName + "님 입장하셨습니다.");
                    this.nickname = dto.getNickName();
                    this.ready = false; // 입장 시 ready 초기화
                    server.broadcast(sendDto);
                    server.broadcastLobby();
                } else if (dto.getCommand() == Info.SEND) {     // 메시지 송신 시 행동
                    InfoDTO sendDto = new InfoDTO();
                    sendDto.setCommand(Info.SEND);
                    sendDto.setMessage("[" + nickName + "] " + dto.getMessage());
                    server.broadcast(sendDto);
                } else if (dto.getCommand() == Info.READY) {        // 준비 버튼 눌렀을 때 행동
                    // 토글
                    this.ready = (dto.getReady() != null) ? dto.getReady() : !this.ready;
                    server.broadcastLobby();
                    server.startGame();
                } else if (dto.getCommand() == Info.CLICK_REQ) {    // 클릭했을 때 행동
                    int cs = dto.getClientSeq();                    // 클라이언트 seq 넘버를 받아와서, 마지막 시퀀스번호랑 비교
                    // 같은 요청 재전송/역순 방지
                    if (cs <= lastClientSeq) {                      // 마지막 시퀀스번호보다 이전게 왔으면, 무시
                        continue;
                    }
                    // 요청 성공 시, 클라이언트 seq 동기화
                    lastClientSeq = cs;

                    server.enqueueClick(this.nickname, dto.getX(), dto.getY(), cs);     // 요청 성공 시, 큐에 넣기
                }
            } // while
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {         // why?
            server.broadcastLobby();
        }
    }
}
