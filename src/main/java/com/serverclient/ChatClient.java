package com.serverclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ChatClient extends Application implements Runnable {
    private TextArea output;
    private TextField input;
    private Button sendBtn;

    private Socket socket;
    private ObjectInputStream reader = null;
    private ObjectOutputStream writer = null;
    private String nickName;

    private static final int PORT = 9500;

    @Override
    public void start(Stage primaryStage) {
        // UI 구성
        output = new TextArea();
        output.setEditable(false);

        input = new TextField();
        sendBtn = new Button("보내기");

        HBox bottom = new HBox(10, input, sendBtn);
        bottom.setPadding(new Insets(8));
        HBox.setHgrow(input, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setCenter(output);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("JavaFX 채팅 클라이언트");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 서버 접속
        service();

        // 이벤트 등록
        sendBtn.setOnAction(e -> sendMessage());
        input.setOnAction(e -> sendMessage());

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

    private void service() {
        // 서버 IP 입력
        TextInputDialog ipDialog = new TextInputDialog("127.0.0.1");
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

    private void sendMessage() {
        try {
            String msg = input.getText();
            InfoDTO dto = new InfoDTO();
            if (msg.equals("exit")) {
                dto.setCommand(Info.EXIT);
                dto.setNickName(nickName);
            } else {
                dto.setCommand(Info.SEND);
                dto.setMessage(msg);
                dto.setNickName(nickName);
            }
            writer.writeObject(dto);
            writer.flush();
            input.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        InfoDTO dto;
        try {
            while ((dto = (InfoDTO) reader.readObject()) != null) {
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
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
