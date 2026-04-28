package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.AttributeType;
import com.lastbastion.common.Stats;
import com.lastbastion.game.augment.AugmentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 芯片（augment.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class AugmentTuning {
    public int bagCapacity;
    public long removeCostCredits;
    public int maxStar;
    /** star (1..maxStar) -> multiplier. */
    public Map<String, Double> starMultipliers = new LinkedHashMap<>();
    /** type -> list of base attribute contributions (1-star). */
    public Map<AugmentType, List<AttributeContribution>> baseStats = new LinkedHashMap<>();

    public static final class AttributeContribution {
        public AttributeType attribute;
        public double value;
        public AttributeContribution() {}
        public AttributeContribution(AttributeType a, double v) { this.attribute = a; this.value = v; }
    }

    public double starMultiplier(int star) {
        Double v = starMultipliers.get(Integer.toString(star));
        return v == null ? 1.0 : v;
    }

    public Stats statsFor(AugmentType type, int star) {
        Stats s = new Stats();
        List<AttributeContribution> base = baseStats.getOrDefault(type, new ArrayList<>());
        double mul = starMultiplier(star);
        for (AttributeContribution c : base) {
            s.add(c.attribute, c.value * mul);
        }
        return s;
    }
}
