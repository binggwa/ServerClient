package com.serverclient;

import java.io.Serializable;

public class Coord implements Serializable {
    public int x;
    public int y;
    public Coord(int x, int y){
        this.x = x;
        this.y = y;
    }
}
