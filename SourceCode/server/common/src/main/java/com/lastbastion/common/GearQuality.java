package com.lastbastion.common;

/** Gear quality (White/Green/Blue/Purple/Orange, matching TASK-004). */
public enum GearQuality {
    WHITE(1), GREEN(2), BLUE(3), PURPLE(4), ORANGE(5);

    private final int tier;

    GearQuality(int tier) {
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }
}
