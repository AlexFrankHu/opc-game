package com.lastbastion.game.augment;

import com.lastbastion.common.AttributeType;
import com.lastbastion.common.Stats;

/**
 * TASK-005 Augment Chip 实例。
 */
public final class AugmentInstance {

    private final long instanceId;
    private final AugmentType type;
    private int star;
    private long equippedSurvivorId;
    private int equippedSlotIndex = -1;

    public AugmentInstance(long instanceId, AugmentType type, int star) {
        this.instanceId = instanceId;
        this.type = type;
        this.star = star;
    }

    public long instanceId() { return instanceId; }
    public AugmentType type() { return type; }
    public int star() { return star; }
    public void setStar(int s) { this.star = s; }
    public long equippedSurvivorId() { return equippedSurvivorId; }
    public int equippedSlotIndex() { return equippedSlotIndex; }
    public void setEquipped(long survivorId, int slotIndex) {
        this.equippedSurvivorId = survivorId;
        this.equippedSlotIndex = slotIndex;
    }
    public void clearEquipped() {
        this.equippedSurvivorId = 0;
        this.equippedSlotIndex = -1;
    }

    public Stats toStats() {
        Stats s = new Stats();
        double mul = starMultiplier(star);
        switch (type) {
            case ATK -> s.add(AttributeType.ATK_PCT, 0.08 * mul);
            case DEF -> s.add(AttributeType.DEF_PCT, 0.10 * mul);
            case HP -> s.add(AttributeType.HP_PCT, 0.12 * mul);
            case SPD -> s.add(AttributeType.SPD, 5 * mul);
            case CRIT -> {
                s.add(AttributeType.CRIT_RATE, 0.04 * mul);
                s.add(AttributeType.CRIT_DMG, 0.08 * mul);
            }
        }
        return s;
    }

    public static double starMultiplier(int star) {
        return switch (star) {
            case 1 -> 1.0;
            case 2 -> 1.4;
            case 3 -> 2.0;
            case 4 -> 3.0;
            case 5 -> 4.5;
            case 6 -> 7.0;
            default -> 1.0;
        };
    }
}
