package com.lastbastion.combat;

/**
 * Status effect kinds. TASK-002 §2.4 列出的最小集合 + CC 相关类。
 */
public enum StatusType {
    POISON(false, true),        // per-turn damage
    BURN(false, true),          // per-turn damage + DEF down
    STUN(true, false),          // skip turn
    SILENCE(true, false),       // cannot use active
    FREEZE(true, false),        // skip turn, takes more damage
    SHIELD(false, false),       // absorb damage
    ATK_UP(false, false),
    DEF_UP(false, false),
    SPD_UP(false, false),
    ATK_DOWN(false, false),
    DEF_DOWN(false, false),
    SPD_DOWN(false, false);

    private final boolean controlEffect;
    private final boolean dotEffect;

    StatusType(boolean controlEffect, boolean dotEffect) {
        this.controlEffect = controlEffect;
        this.dotEffect = dotEffect;
    }

    /** True if this is a crowd-control effect (blocked by CC Immunity). */
    public boolean isControl() {
        return controlEffect;
    }

    /** True if this is a damage-over-time effect (ticks at turn start). */
    public boolean isDot() {
        return dotEffect;
    }
}
