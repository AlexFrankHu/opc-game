package com.lastbastion.combat;

import com.lastbastion.common.AttributeType;

import java.util.Random;

/**
 * TASK-002 §2.2 伤害计算。
 */
public final class DamageCalculator {

    private final Random rng;

    public DamageCalculator(Random rng) {
        this.rng = rng;
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
        double base = Math.max(1.0, atk * atkRatio - def);
        // ±10% random variance
        double variance = 0.9 + rng.nextDouble() * 0.2;
        base *= variance;
        // crit
        boolean crit = rng.nextDouble() < attacker.stats().get(AttributeType.CRIT_RATE);
        if (crit) {
            double mult = 1.5 + attacker.stats().get(AttributeType.CRIT_DMG);
            base *= mult;
        }
        // Burn increases damage taken by 10%
        for (StatusEffect s : target.statuses()) {
            if (s.type() == StatusType.BURN) base *= 1.10;
            if (s.type() == StatusType.FREEZE) base *= 1.15;
            if (s.type() == StatusType.DEF_DOWN) base *= 1 + s.magnitude();
        }
        for (StatusEffect s : attacker.statuses()) {
            if (s.type() == StatusType.ATK_UP) base *= 1 + s.magnitude();
            if (s.type() == StatusType.ATK_DOWN) base *= Math.max(0.1, 1 - s.magnitude());
        }
        return new DamageResult(Math.max(1.0, base), crit, false);
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
