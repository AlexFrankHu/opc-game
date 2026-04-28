package com.lastbastion.combat;

/**
 * One step inside an Ability. Effects are executed in order by AbilityExecutor.
 */
public final class AbilityEffect {

    public enum Kind {
        DAMAGE,
        HEAL,
        APPLY_STATUS,
        DISPEL
    }

    public enum TargetType {
        SELF,
        SINGLE_ENEMY,
        ALL_ENEMIES,
        SINGLE_ALLY,
        ALL_ALLIES,
        LOWEST_HP_ALLY
    }

    private final Kind kind;
    private final TargetType target;
    private final double magnitude;
    private final StatusType statusType;
    private final int statusDuration;

    private AbilityEffect(Kind kind, TargetType target, double magnitude,
                          StatusType statusType, int statusDuration) {
        this.kind = kind;
        this.target = target;
        this.magnitude = magnitude;
        this.statusType = statusType;
        this.statusDuration = statusDuration;
    }

    public static AbilityEffect damage(TargetType target, double atkRatio) {
        return new AbilityEffect(Kind.DAMAGE, target, atkRatio, null, 0);
    }

    public static AbilityEffect heal(TargetType target, double atkRatio) {
        return new AbilityEffect(Kind.HEAL, target, atkRatio, null, 0);
    }

    public static AbilityEffect applyStatus(TargetType target, StatusType status,
                                            int durationTurns, double magnitude) {
        return new AbilityEffect(Kind.APPLY_STATUS, target, magnitude, status, durationTurns);
    }

    public static AbilityEffect dispel(TargetType target) {
        return new AbilityEffect(Kind.DISPEL, target, 0, null, 0);
    }

    public Kind kind() { return kind; }
    public TargetType target() { return target; }
    public double magnitude() { return magnitude; }
    public StatusType statusType() { return statusType; }
    public int statusDuration() { return statusDuration; }
}
