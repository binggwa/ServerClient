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

    public ChatHandler(Socket socket, List<ChatHandler> list) throws IOException {
        this.socket = socket;
        this.list = list;
        writer = new ObjectOutputStream(socket.getOutputStream());
        reader = new ObjectInputStream(socket.getInputStream());
    }
    // 실행 스레드: 클라이언트에서 메시지를 계속 읽고 처리
    public void run(){
        InfoDTO dto = null;
        String nickName;
        try {
            while(true) {
                dto = (InfoDTO)reader.readObject();     // 클라이언트 메시지 수신
                nickName = dto.getNickName();
                // 사용자가 접속을 끊었을 경우, 프로그램을 끝내서는 안되고 남은 사용자들에게 퇴장메시지를 보내줘야 한다.
                if(dto.getCommand() == Info.EXIT) {
                    InfoDTO sendDto = new InfoDTO();

                    // 나가려고 ext를 보낸 클라이언트에게 답변 보내기
                    sendDto.setCommand(Info.EXIT);
                    writer.writeObject(sendDto);
                    writer.flush();

                    // 연결 해제
                    reader.close();
                    writer.close();
                    socket.close();

                    // 남아있는 클라이언트에게 퇴장메시지 보내기
                    list.remove(this);

                    sendDto.setCommand(Info.SEND);
                    sendDto.setMessage(nickName + "님 퇴장하셨습니다.");
                    broadcast(sendDto);
                    break;
                } else if (dto.getCommand() == Info.JOIN) {
                    // 모든 사용자에게 메시지 보내기
                    InfoDTO sendDto = new InfoDTO();
                    sendDto.setCommand(Info.SEND);
                    sendDto.setMessage(nickName + "님 입장하셨습니다.");
                    broadcast(sendDto);
                } else if (dto.getCommand() == Info.SEND) {
                    InfoDTO sendDto = new InfoDTO();
                    sendDto.setCommand(Info.SEND);
                    sendDto.setMessage("[" + nickName + "] " + dto.getMessage());
                    broadcast(sendDto);
                }
            } // while
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // broadcast 메서드 : 다른 클라이언트들에게 전체 메시지 보내주기
    public void broadcast(InfoDTO sendDto) throws IOException {
        for(ChatHandler handler : list) {
            handler.writer.writeObject(sendDto);    // 핸들러 안의 writer에 값을 보내기
            handler.writer.flush();                 // 핸들러 안의 writer 값 비워주기
        }
    }
}
