package com.serverclient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private ServerSocket serverSocket;
    private List<ChatHandler> list;

    private static final int PORT = 9500;

    public ChatServer() {
        try{
            serverSocket = new ServerSocket (PORT);
            System.out.println("서버 준비 완료");
            list = new ArrayList<ChatHandler>();
            while(true) {
                Socket socket = serverSocket.accept();
                ChatHandler handler = new ChatHandler(socket, list);
                handler.start();
                list.add(handler);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatServer();
    }
}
