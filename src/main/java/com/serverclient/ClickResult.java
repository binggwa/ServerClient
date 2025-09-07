package com.serverclient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClickResult implements Serializable {
    public boolean success;
    public List<Coord> changed;
    public int scoreGain;
    public String reason;

    private ClickResult(boolean success, List<Coord> changed, int scoreGain, String reason) {
        this.success = success;
        this.changed = changed;
        this.scoreGain = scoreGain;
        this.reason = reason;
    }

    public static ClickResult ok(List<Coord> changed) {
        return new ClickResult(true, changed, changed.size(), null);
    }

    public static ClickResult fail(String reason) {
        return new ClickResult(false, new ArrayList<>(), 0, reason);
    }
}