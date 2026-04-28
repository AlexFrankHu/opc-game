package com.lastbastion.combat;

import com.lastbastion.common.AttributeType;

import java.util.Random;

/**
 * TASK-002 §2.2 伤害计算。常量来自 {@link CombatParams}（assets/numeric/combat.json）。
 */
public final class DamageCalculator {

    private final Random rng;
    private final CombatParams params;

    public DamageCalculator(Random rng) {
        this(rng, CombatParams.defaults());
    }

    public DamageCalculator(Random rng, CombatParams params) {
        this.rng = rng;
        this.params = params;
    }

    /**
     * @return damage dealt (0 if fully evaded)
     */
    public DamageResult compute(CombatUnit attacker, CombatUnit target, double atkRatio) {
        double acc = attacker.stats().get(AttributeType.ACC);
        double res = target.stats().get(AttributeType.RES);
        double hitChance = 1.0 + (acc - res);
        if (rng.nextDouble() > hitChance) {
            return new DamageResult(0, false, true);
        }
        double atk = attacker.stats().get(AttributeType.ATK);
        double def = target.stats().get(AttributeType.DEF);
        double base = Math.max(params.minDamage, atk * atkRatio - def);
        double varianceWidth = params.damageVarianceMax - params.damageVarianceMin;
        double variance = params.damageVarianceMin + rng.nextDouble() * varianceWidth;
        base *= variance;
        boolean crit = rng.nextDouble() < attacker.stats().get(AttributeType.CRIT_RATE);
        if (crit) {
            double mult = params.critBaseMultiplier + attacker.stats().get(AttributeType.CRIT_DMG);
            base *= mult;
        }
        for (StatusEffect s : target.statuses()) {
            if (s.type() == StatusType.BURN) base *= (1.0 + params.burnTakenPct);
            if (s.type() == StatusType.FREEZE) base *= (1.0 + params.freezeTakenPct);
            if (s.type() == StatusType.DEF_DOWN) base *= 1 + s.magnitude();
        }
        for (StatusEffect s : attacker.statuses()) {
            if (s.type() == StatusType.ATK_UP) base *= 1 + s.magnitude();
            if (s.type() == StatusType.ATK_DOWN) base *= Math.max(0.1, 1 - s.magnitude());
        }
        return new DamageResult(Math.max(params.minDamage, base), crit, false);
    }

    public static final class DamageResult {
        public final double amount;
        public final boolean crit;
        public final boolean evaded;

        public DamageResult(double amount, boolean crit, boolean evaded) {
            this.amount = amount;
            this.crit = crit;
            this.evaded = evaded;
        }
    }
}
