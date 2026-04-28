package com.lastbastion.common;

import java.util.EnumMap;
import java.util.Map;

/**
 * 可变属性容器。支持 add/multiply，用于叠加装备词条、Augment、Buff。
 */
public final class Stats {

    private final EnumMap<AttributeType, Double> values = new EnumMap<>(AttributeType.class);

    public Stats() {
        for (AttributeType t : AttributeType.values()) {
            values.put(t, 0.0);
        }
    }

    public Stats(Stats other) {
        for (AttributeType t : AttributeType.values()) {
            values.put(t, other.get(t));
        }
    }

    public double get(AttributeType t) {
        return values.getOrDefault(t, 0.0);
    }

    public Stats set(AttributeType t, double v) {
        values.put(t, v);
        return this;
    }

    public Stats add(AttributeType t, double v) {
        values.merge(t, v, Double::sum);
        return this;
    }

    public Stats addAll(Stats other) {
        for (Map.Entry<AttributeType, Double> e : other.values.entrySet()) {
            values.merge(e.getKey(), e.getValue(), Double::sum);
        }
        return this;
    }

    /** 汇总最终属性：baseline + flat + percentBonus。适用于 (HP_PCT / ATK_PCT / DEF_PCT) 转换。 */
    public Stats applyPercentBonuses() {
        values.put(AttributeType.HP, get(AttributeType.HP) * (1 + get(AttributeType.HP_PCT)));
        values.put(AttributeType.ATK, get(AttributeType.ATK) * (1 + get(AttributeType.ATK_PCT)));
        values.put(AttributeType.DEF, get(AttributeType.DEF) * (1 + get(AttributeType.DEF_PCT)));
        values.put(AttributeType.HP_PCT, 0.0);
        values.put(AttributeType.ATK_PCT, 0.0);
        values.put(AttributeType.DEF_PCT, 0.0);
        return this;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
