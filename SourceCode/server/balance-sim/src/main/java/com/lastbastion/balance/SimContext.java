package com.lastbastion.balance;

import com.lastbastion.game.numeric.NumericConfig;

/** Shared input across all simulators. */
public final class SimContext {

    public final NumericConfig cfg;
    public final long seed;
    public final SampleSize size;

    public SimContext(NumericConfig cfg, long seed, SampleSize size) {
        this.cfg = cfg;
        this.seed = seed;
        this.size = size;
    }

    /** Sample size profile — switched between the regression test (small) and CLI (large). */
    public enum SampleSize {
        SMALL,  // unit-test budget: ~1s per sim
        LARGE;  // CLI budget: ~30s total

        public int gachaPulls() { return this == SMALL ? 5_000 : 100_000; }
        public int gearTrials() { return this == SMALL ? 200 : 2_000; }
        public int combatBattlesPerStage() { return this == SMALL ? 50 : 200; }
        public int arenaPlayers() { return this == SMALL ? 30 : 100; }
        public int arenaDays() { return this == SMALL ? 14 : 30; }
        public int idleHours() { return 12; } // fixed by spec
    }
}
