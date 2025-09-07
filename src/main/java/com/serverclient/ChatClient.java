package com.serverclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatClient extends Application implements Runnable {
    private TextArea output;
    private TextField input;
    private Button sendBtn;

    private Socket socket;
    private ObjectInputStream reader = null;
    private ObjectOutputStream writer = null;
    private String nickName;

    private ListView<String> lobbyView;     // 닉네임 (ready) 형식의 목록 추가
    private Button readyBtn;

    private static final int PORT = 9500;

    private GameController gameController;
    private long lastServerSeq = 0L;
    private int clientSeq = 0;

    @Override
    public void start(Stage primaryStage) {
        // UI 구성
        output = new TextArea();
        output.setEditable(false);

        input = new TextField();
        sendBtn = new Button("보내기");

        // 하단
        HBox bottom = new HBox(10, input, sendBtn);
        bottom.setPadding(new Insets(8));
        HBox.setHgrow(input, Priority.ALWAYS);

        // 우측 로비 상태
        lobbyView = new ListView<>();
        readyBtn = new Button("준비!");
        VBox right = new VBox(10, new Label("대기실"), lobbyView, readyBtn);
        right.setPadding(new Insets(8));
        VBox.setVgrow(lobbyView, Priority.ALWAYS);

        // 메인
        BorderPane root = new BorderPane();
        root.setCenter(output);
        root.setBottom(bottom);
        root.setRight(right);

        Scene scene = new Scene(root, 700, 400);
        primaryStage.setTitle("게임 시작 대기실");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 서버 접속
        service();

        // 이벤트 등록
        sendBtn.setOnAction(e -> sendMessage());
        input.setOnAction(e -> sendMessage());

        readyBtn.setOnAction(e -> sendReadyToggle());   // 준비!버튼 추가

        // 종료 이벤트 처리
        primaryStage.setOnCloseRequest(e -> {
            try {
                InfoDTO dto = new InfoDTO();
                dto.setNickName(nickName);
                dto.setCommand(Info.EXIT);
                writer.writeObject(dto);
                writer.flush();
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException io) {
                io.printStackTrace();
            }
        });
    }

    // 준비버튼 눌렀을 때 ready 상태 토글 메서드
    private void sendReadyToggle() {
            InfoDTO dto = new InfoDTO();
            dto.setCommand(Info.READY);
            dto.setNickName(nickName);
            writer.writeObject(dto);
            writer.flush();
    }

    // 클라이언트 안에서 서버와의 연결을 초기화하는 메서드
    private void service() {
        // 서버 IP 입력
        TextInputDialog ipDialog = new TextInputDialog("127.0.0.1");    // 로컬호스트로 기본 설정
        ipDialog.setHeaderText("서버 IP를 입력하세요.");
        ipDialog.setContentText("IP : ");
        String serverIP = ipDialog.showAndWait().orElse("127.0.0.1");

        // 닉네임 입력
        TextInputDialog nickDialog = new TextInputDialog("guest");
        nickDialog.setHeaderText("닉네임을 입력하세요.");
        nickDialog.setContentText("닉네임 :");
        nickName = nickDialog.showAndWait().orElse("guest");

        try {
            socket = new Socket(serverIP, PORT);
            writer = new ObjectOutputStream(socket.getOutputStream());
            reader = new ObjectInputStream(socket.getInputStream());

            // JOIN 패킷 보내기
            InfoDTO dto = new InfoDTO();
            dto.setCommand(Info.JOIN);
            dto.setNickName(nickName);
            writer.writeObject(dto);
            writer.flush();

            // 스레드 시작
            Thread t = new Thread(this);
            t.setDaemon(true); // UI 종료되면 스레드도 종료
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    // 보내기 버튼, 엔터 눌렀을 때 발동하는 메서드
    private void sendMessage() {
        try {
            String msg = input.getText();
            InfoDTO dto = new InfoDTO();
            dto.setCommand(Info.SEND);
            dto.setMessage(msg);
            dto.setNickName(nickName);
            writer.writeObject(dto);
            writer.flush();
            input.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CLICK_REQ
    private void sendClickReq(int x, int y) {
        try {
            InfoDTO dto = new InfoDTO();
            dto.setCommand(Info.CLICK_REQ);
            dto.setX(x);
            dto.setY(y);
            dto.setClientSeq(++clientSeq);
            writer.writeObject(dto);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        InfoDTO dto;
        try {
            while ( (dto = (InfoDTO) reader.readObject()) != null ) {
                if (dto.getCommand() == Info.EXIT) {
                    Platform.runLater(() -> {
                        output.appendText("서버에서 연결이 종료되었습니다.\n");
                    });
                    break;
                } else if (dto.getCommand() == Info.SEND) {
                    String msg = dto.getMessage();
                    Platform.runLater(() -> {
                        output.appendText(msg + "\n");
                    });
                } else if (dto.getCommand() == Info.LOBBY_UPDATE) {
                    List<PlayerStatus> lobby = dto.getLobby();
                    Platform.runLater(() -> {
                        List<String> items = new ArrayList<>();
                        for ( PlayerStatus p : lobby ) {
                            String text = p.getNickname();
                            // 만약 플레이어가 준비된 상태라면, 닉네임 옆에 준비완료! 띄우기
                            if ( p.isReady() ) {
                                text += "  ***준비완료!***";
                            }
                            items.add(text);
                        }
                        // 로비의 플레이어 상태가 변경될 때마다, 가져와서 리스트뷰 업데이트
                        lobbyView.getItems().setAll(items);
                    });
                } else if (dto.getCommand() == Info.START_GAME) {
                    int W = dto.getWidth();
                    int H = dto.getHeight();
                    int seconds = dto.getDurationSec();
                    Platform.runLater(() -> {
                        openGameWindow(W, H, seconds);
                    });
                    lastServerSeq = 0L;     // 새 게임 시작할때 서버seq 초기화
                } else if (dto.getCommand() == Info.BOARD_SNAPSHOT) {
                    CellColor[][] board = dto.getBoard();
                    Platform.runLater(() -> {
                        gameController.applySnapshot(board);
                    });
                } else if (dto.getCommand() == Info.BOARD_DIFF) {
                    List<Coord> diff = dto.getDiff();
                    Platform.runLater(() -> {
                        gameController.applyDiff(diff);
                    });
                } else if (dto.getCommand() == Info.SCORE_UPDATE) {
                    String player = dto.getPlayer();
                    Integer score = dto.getScore();
                    if ( player.equals(nickName) ) {
                        Platform.runLater(() -> {
                            gameController.updateMyScore(score);
                        });
                    }
                } else if (dto.getCommand() == Info.CLICK_OK || dto.getCommand() == Info.CLICK_REJECT) {
                    seqDetect(dto);
                } else if (dto.getCommand() == Info.TIMER_SYNC) {
                    Long endTime = dto.getEndsAtMillis();
                    if ( endTime != null ) {
                        Platform.runLater(() -> {
                            gameController.syncEndsAt(endTime);
                        });
                    }
                } else if (dto.getCommand() == Info.GAME_OVER) {
                    List<PlayerScore> results = dto.getResults();
                    Platform.runLater(() -> {
                        gameController.closeWindow();
                        StringBuilder sb = new StringBuilder();
                        if ( results != null ) {
                            int rank = 1;
                            for ( PlayerScore player : results ) {
                                sb.append(rank++)
                                        .append(" 위  :  ")
                                        .append(player.getNickname())
                                        .append("  - ")
                                        .append(player.getScore())
                                        .append(" 점\n");
                            }
                        }
                        // 알람창
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("게임 종료");
                        alert.setHeaderText("게임 순위");

                        // TextArea로 내용 표시
                        TextArea area = new TextArea(sb.toString());
                        area.setEditable(false);
                        area.setWrapText(true);

                        // 내용으로 교체
                        alert.getDialogPane().setContent(area);
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.setResizable(true);

                        alert.showAndWait();
                    });
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean seqDetect(InfoDTO dto) {
        Long seq = dto.getServerSeq();
        if (seq == null) return true;
        if (seq <= lastServerSeq) return false;
        lastServerSeq = seq;
        return true;
    }

    private void openGameWindow(int W, int H , int seconds) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/serverclient/GameView.fxml"));
            Parent root = loader.load();

            gameController = loader.getController();

            Stage gameStage = new Stage();
            gameStage.setTitle("게임");
            gameStage.setScene(new Scene(root, 900, 680));

            gameController.setClickSender(this::sendClickReq);
            gameController.init(W, H, seconds, gameStage);

            gameStage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
