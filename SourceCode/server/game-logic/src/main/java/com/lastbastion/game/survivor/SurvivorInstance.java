package com.lastbastion.game.survivor;

import com.lastbastion.common.GearSlot;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 玩家拥有的 Survivor 实例（等级/星级/技能等级 + 装备槽）。
 */
public final class SurvivorInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long instanceId;
    private final String configId;
    private int level;
    private int star;
    /** abilityId -> skill level */
    private final Map<String, Integer> skillLevels = new HashMap<>();
    /** slot -> gearInstanceId (0 = empty) */
    private final EnumMap<GearSlot, Long> equipped = new EnumMap<>(GearSlot.class);
    /** augment slots [0..2] */
    private final long[] augmentSlots = new long[3];

    public SurvivorInstance(long instanceId, String configId) {
        this.instanceId = instanceId;
        this.configId = configId;
        this.level = 1;
        this.star = 1;
    }

    public long instanceId() { return instanceId; }
    public String configId() { return configId; }
    public int level() { return level; }
    public void setLevel(int l) { this.level = l; }
    public int star() { return star; }
    public void setStar(int s) { this.star = s; }

    public int skillLevel(String abilityId) {
        return skillLevels.getOrDefault(abilityId, 1);
    }
    public void setSkillLevel(String abilityId, int lv) { skillLevels.put(abilityId, lv); }

    public EnumMap<GearSlot, Long> equipped() { return equipped; }
    public long[] augmentSlots() { return augmentSlots; }
}
