package com.lastbastion.game.gear;

import com.lastbastion.common.AttributeType;
import com.lastbastion.common.GearQuality;
import com.lastbastion.common.GearSlot;
import com.lastbastion.common.Stats;

import java.util.ArrayList;
import java.util.List;

/**
 * TASK-004 — 装备实例。
 */
public final class GearInstance {

    public static final class SubStat {
        public AttributeType type;
        public double value;

        public SubStat(AttributeType type, double value) {
            this.type = type;
            this.value = value;
        }
    }

    private final long instanceId;
    private final GearSlot slot;
    private final GearQuality quality;
    private final AttributeType mainStat;
    private final double mainStatBase;
    private final List<SubStat> subStats;
    private int level;
    private boolean locked;
    private long equippedSurvivorId;

    public GearInstance(long instanceId, GearSlot slot, GearQuality quality,
                        AttributeType mainStat, double mainStatBase, List<SubStat> subStats) {
        this.instanceId = instanceId;
        this.slot = slot;
        this.quality = quality;
        this.mainStat = mainStat;
        this.mainStatBase = mainStatBase;
        this.subStats = new ArrayList<>(subStats);
        this.level = 0;
        this.locked = false;
        this.equippedSurvivorId = 0;
    }

    public long instanceId() { return instanceId; }
    public GearSlot slot() { return slot; }
    public GearQuality quality() { return quality; }
    public int level() { return level; }
    public void setLevel(int v) { this.level = v; }
    public boolean locked() { return locked; }
    public void setLocked(boolean v) { this.locked = v; }
    public long equippedSurvivorId() { return equippedSurvivorId; }
    public void setEquippedSurvivorId(long v) { this.equippedSurvivorId = v; }
    public AttributeType mainStat() { return mainStat; }
    public double mainStatBase() { return mainStatBase; }
    public List<SubStat> subStats() { return subStats; }

    /** 汇总本件装备的总属性加成（主属性 + 副属性）。 */
    public Stats toStats() {
        Stats s = new Stats();
        double mainFinal = mainStatBase * (1 + level * 0.08); // +8% per level
        s.add(mainStat, mainFinal);
        for (SubStat sub : subStats) s.add(sub.type, sub.value);
        return s;
    }
}
