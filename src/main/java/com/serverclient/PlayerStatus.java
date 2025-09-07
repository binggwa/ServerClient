package com.serverclient;

import java.io.Serializable;

public class PlayerStatus implements Serializable {
    private String nickname;
    private boolean ready;

    public PlayerStatus() {}
    public PlayerStatus(String nickname, boolean ready) {
        this.nickname = nickname;
        this.ready = ready;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isReady() {
        return ready;
    }
}
