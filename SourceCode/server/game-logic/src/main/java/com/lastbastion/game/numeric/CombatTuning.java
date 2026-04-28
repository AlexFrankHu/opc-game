package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.combat.CombatParams;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 战斗常量（combat.json）。
 * 字段映射 {@link com.lastbastion.combat.DamageCalculator} / {@link com.lastbastion.combat.CombatSimulator} 中的硬编码。
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class CombatTuning {
    public double critBaseMultiplier;
    public double armorK;
    public double minDamage;
    public double damageVarianceMin;
    public double damageVarianceMax;
    public double bossRageThresholdHpRatio;
    public double rageAtkMult;
    public double rageSpdMult;
    public int dotTickInterval;
    public int shieldDurationDefaultTurns;
    public Map<String, Double> statusDamageModifiers = new LinkedHashMap<>();
    public int maxRoundsHardCap;

    public double statusModifier(String key, double fallback) {
        Double v = statusDamageModifiers.get(key);
        return v == null ? fallback : v;
    }

    public CombatParams toParams() {
        return CombatParams.builder()
                .critBaseMultiplier(critBaseMultiplier)
                .minDamage(minDamage)
                .damageVariance(damageVarianceMin, damageVarianceMax)
                .burnTakenPct(statusModifier("BURN_TAKEN_PCT", 0.10))
                .freezeTakenPct(statusModifier("FREEZE_TAKEN_PCT", 0.15))
                .bossRage(bossRageThresholdHpRatio, rageAtkMult, rageSpdMult)
                .dotTickInterval(dotTickInterval)
                .shieldDurationDefaultTurns(shieldDurationDefaultTurns)
                .maxRoundsHardCap(maxRoundsHardCap)
                .build();
    }
}
