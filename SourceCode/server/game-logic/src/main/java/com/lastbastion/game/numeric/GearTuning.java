package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.GearQuality;

import java.util.LinkedHashMap;
import java.util.Map;

/** 装备（gear.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class GearTuning {
    public int maxLevel;
    public int bagCapacityDefault;
    public long enhanceCostBase;
    public long enhanceCostPerLevel;
    /** key = target level (1..maxLevel) as string. */
    public Map<String, Double> successRates = new LinkedHashMap<>();
    public Map<GearQuality, Long> decomposeAlloyByQuality = new LinkedHashMap<>();
    public long decomposeAlloyPerLevel;

    public double successRate(int toLevel) {
        Double v = successRates.get(Integer.toString(toLevel));
        return v == null ? 0.0 : v;
    }

    public long decomposeAlloy(GearQuality quality, int level) {
        long base = decomposeAlloyByQuality.getOrDefault(quality, 0L);
        return base + decomposeAlloyPerLevel * level;
    }

    public long enhanceCost(int currentLevel) {
        return enhanceCostBase + enhanceCostPerLevel * currentLevel;
    }
}
