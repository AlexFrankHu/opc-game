package com.lastbastion.game.augment;

import com.lastbastion.common.Stats;

import java.io.Serializable;

/**
 * TASK-005 Augment Chip 实例。
 */
public final class AugmentInstance implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** @deprecated 数值已迁至 assets/numeric/augment.json。请走 AugmentService.collectStats(...)。 */
    @Deprecated
    public Stats toStats() {
        return com.lastbastion.game.numeric.NumericConfig.defaults().augment().statsFor(type, star);
    }

    /** @deprecated 数值已迁至 assets/numeric/augment.json。 */
    @Deprecated
    public static double starMultiplier(int star) {
        return com.lastbastion.game.numeric.NumericConfig.defaults().augment().starMultiplier(star);
    }
}
