package com.lastbastion.game.arena;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

/**
 * 玩家的竞技场当日状态。
 */
public final class ArenaState {

    private int rank = Integer.MAX_VALUE;
    private int score = 1000;
    private int dailyFreeLeft = 5;
    private int dailyBoughtToday = 0;
    private long lastResetDay = 0;

    private final Deque<ArenaRecord> history = new ArrayDeque<>();

    public int rank() { return rank; }
    public void setRank(int r) { this.rank = r; }

    public int score() { return score; }
    public void setScore(int s) { this.score = s; }

    public int dailyFreeLeft() { return dailyFreeLeft; }
    public void setDailyFreeLeft(int v) { this.dailyFreeLeft = v; }

    public int dailyBoughtToday() { return dailyBoughtToday; }
    public void setDailyBoughtToday(int v) { this.dailyBoughtToday = v; }

    public long lastResetDay() { return lastResetDay; }
    public void setLastResetDay(long v) { this.lastResetDay = v; }

    public void addRecord(ArenaRecord r) {
        history.addFirst(r);
        while (history.size() > 20) history.removeLast();
    }

    public java.util.List<ArenaRecord> history() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(history));
    }
}
