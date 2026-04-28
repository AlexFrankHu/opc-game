package com.lastbastion.combat;

/**
 * 战斗常量参数，由数值层（assets/numeric/combat.json → game-logic CombatTuning）注入。
 *
 * <p>combat-engine 不依赖 Jackson；上层把字段拷过来即可。
 *
 * <p>{@link #defaults()} 给出 MVP 默认值（与 v0.1.0 行为一致），仅供 unit test 使用。
 */
public final class CombatParams {

    public final double critBaseMultiplier;
    public final double minDamage;
    public final double damageVarianceMin;
    public final double damageVarianceMax;
    public final double burnTakenPct;
    public final double freezeTakenPct;
    public final double bossRageThresholdHpRatio;
    public final double rageAtkMult;
    public final double rageSpdMult;
    public final int dotTickInterval;
    public final int shieldDurationDefaultTurns;
    public final int maxRoundsHardCap;

    private CombatParams(Builder b) {
        this.critBaseMultiplier = b.critBaseMultiplier;
        this.minDamage = b.minDamage;
        this.damageVarianceMin = b.damageVarianceMin;
        this.damageVarianceMax = b.damageVarianceMax;
        this.burnTakenPct = b.burnTakenPct;
        this.freezeTakenPct = b.freezeTakenPct;
        this.bossRageThresholdHpRatio = b.bossRageThresholdHpRatio;
        this.rageAtkMult = b.rageAtkMult;
        this.rageSpdMult = b.rageSpdMult;
        this.dotTickInterval = b.dotTickInterval;
        this.shieldDurationDefaultTurns = b.shieldDurationDefaultTurns;
        this.maxRoundsHardCap = b.maxRoundsHardCap;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CombatParams defaults() {
        return builder().build();
    }

    public static final class Builder {
        public double critBaseMultiplier = 1.5;
        public double minDamage = 1.0;
        public double damageVarianceMin = 0.9;
        public double damageVarianceMax = 1.1;
        public double burnTakenPct = 0.10;
        public double freezeTakenPct = 0.15;
        public double bossRageThresholdHpRatio = 0.5;
        public double rageAtkMult = 1.3;
        public double rageSpdMult = 1.2;
        public int dotTickInterval = 1;
        public int shieldDurationDefaultTurns = 3;
        public int maxRoundsHardCap = 30;

        public Builder critBaseMultiplier(double v) { this.critBaseMultiplier = v; return this; }
        public Builder minDamage(double v) { this.minDamage = v; return this; }
        public Builder damageVariance(double min, double max) { this.damageVarianceMin = min; this.damageVarianceMax = max; return this; }
        public Builder burnTakenPct(double v) { this.burnTakenPct = v; return this; }
        public Builder freezeTakenPct(double v) { this.freezeTakenPct = v; return this; }
        public Builder bossRage(double thresholdHpRatio, double atkMult, double spdMult) {
            this.bossRageThresholdHpRatio = thresholdHpRatio;
            this.rageAtkMult = atkMult;
            this.rageSpdMult = spdMult;
            return this;
        }
        public Builder dotTickInterval(int v) { this.dotTickInterval = v; return this; }
        public Builder shieldDurationDefaultTurns(int v) { this.shieldDurationDefaultTurns = v; return this; }
        public Builder maxRoundsHardCap(int v) { this.maxRoundsHardCap = v; return this; }

        public CombatParams build() { return new CombatParams(this); }
    }
}
