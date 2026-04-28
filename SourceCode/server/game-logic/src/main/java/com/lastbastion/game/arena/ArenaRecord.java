package com.lastbastion.game.arena;

public final class ArenaRecord {
    public final long opponentId;
    public final String opponentName;
    public final boolean won;
    public final int myRankBefore;
    public final int myRankAfter;
    public final int scoreDelta;
    public final long timestamp;

    public ArenaRecord(long opponentId, String opponentName, boolean won,
                       int myRankBefore, int myRankAfter, int scoreDelta, long timestamp) {
        this.opponentId = opponentId;
        this.opponentName = opponentName;
        this.won = won;
        this.myRankBefore = myRankBefore;
        this.myRankAfter = myRankAfter;
        this.scoreDelta = scoreDelta;
        this.timestamp = timestamp;
    }
}
