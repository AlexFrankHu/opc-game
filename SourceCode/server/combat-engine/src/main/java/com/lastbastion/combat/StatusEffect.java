package com.lastbastion.combat;

/**
 * Buff/Debuff instance attached to a CombatUnit.
 */
public final class StatusEffect {

    private final StatusType type;
    private int durationTurns;
    private double magnitude;
    private final String sourceId;

    public StatusEffect(StatusType type, int durationTurns, double magnitude, String sourceId) {
        this.type = type;
        this.durationTurns = durationTurns;
        this.magnitude = magnitude;
        this.sourceId = sourceId;
    }

    public StatusType type() {
        return type;
    }

    public int duration() {
        return durationTurns;
    }

    public double magnitude() {
        return magnitude;
    }

    public void setMagnitude(double v) {
        this.magnitude = v;
    }

    public String sourceId() {
        return sourceId;
    }

    public void decrement() {
        durationTurns--;
    }

    public boolean expired() {
        return durationTurns <= 0;
    }
}
