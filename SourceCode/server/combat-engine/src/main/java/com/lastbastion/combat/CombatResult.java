package com.lastbastion.combat;

public final class CombatResult {

    public enum Outcome {
        ALLY_WIN,
        ENEMY_WIN,
        DRAW
    }

    private final Outcome outcome;
    private final int totalRounds;
    private final CombatLog log;

    public CombatResult(Outcome outcome, int totalRounds, CombatLog log) {
        this.outcome = outcome;
        this.totalRounds = totalRounds;
        this.log = log;
    }

    public Outcome outcome() { return outcome; }
    public int totalRounds() { return totalRounds; }
    public CombatLog log() { return log; }
    public boolean allyWon() { return outcome == Outcome.ALLY_WIN; }
}
