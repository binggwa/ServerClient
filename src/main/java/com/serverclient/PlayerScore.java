package com.serverclient;

import java.io.Serializable;

public class PlayerScore implements Serializable {
    private String nickname;
    private int score;

    public PlayerScore() {}

    public PlayerScore(String nickname, int score) {
        this.nickname = nickname;
        this.score = score;
    }

    public String getNickname() {
        return nickname;
    }
    public int getScore() {
        return score;
    }
}
