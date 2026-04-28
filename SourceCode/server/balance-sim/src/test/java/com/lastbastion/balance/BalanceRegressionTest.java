package com.lastbastion.balance;

import com.lastbastion.balance.sim.ArenaScoreSim;
import com.lastbastion.balance.sim.BattlePassSim;
import com.lastbastion.balance.sim.CombatBalanceSim;
import com.lastbastion.balance.sim.GachaDistributionSim;
import com.lastbastion.balance.sim.GearEnhanceSim;
import com.lastbastion.balance.sim.ZoneIdleSim;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.numeric.NumericConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs every simulator with the SMALL sample budget and asserts each
 * simulator's built-in checks pass. Designed to keep total time under ~5s
 * inside {@code mvn test}.
 *
 * If a real numeric value is changed, the sim assertions may flag it — that
 * is intentional. Updating values in {@code assets/numeric/*.json} should be
 * accompanied by a rerun of the CLI (LARGE sample) and a manual review of the
 * resulting {@code balance-report.json}.
 */
final class BalanceRegressionTest {

    @Test
    void allSimulatorsPassSmallBudget() {
        NumericConfig cfg = NumericConfigLoader.fromClasspath();
        SimContext ctx = new SimContext(cfg, 42L, SimContext.SampleSize.SMALL);
        SimReport report = new SimReport();

        new GachaDistributionSim().run(ctx, report);
        new GearEnhanceSim().run(ctx, report);
        new CombatBalanceSim().run(ctx, report);
        new ZoneIdleSim().run(ctx, report);
        new BattlePassSim().run(ctx, report);
        new ArenaScoreSim().run(ctx, report);

        assertTrue(report.allPassed(),
                "balance assertions failed: " + report.assertionFailures);
        assertEquals(6, report.sims.size(),
                "expected 6 simulators, got " + report.sims.keySet());
    }

    @Test
    void gachaRatesWithinTolerance() {
        NumericConfig cfg = NumericConfigLoader.fromClasspath();
        SimContext ctx = new SimContext(cfg, 7L, SimContext.SampleSize.SMALL);
        SimReport report = new SimReport();
        new GachaDistributionSim().run(ctx, report);
        assertNull(report.assertionFailures.get(GachaDistributionSim.NAME),
                "gacha sim must pass: " + report.assertionFailures);
    }

    @Test
    void combatEqualPowerWinRateInRange() {
        NumericConfig cfg = NumericConfigLoader.fromClasspath();
        SimContext ctx = new SimContext(cfg, 99L, SimContext.SampleSize.SMALL);
        SimReport report = new SimReport();
        var bag = new CombatBalanceSim().run(ctx, report);
        double rate = (double) bag.get("equalPowerWinRate");
        assertTrue(rate >= 0.30 && rate <= 0.85,
                "equalPowerWinRate " + rate + " not in [0.30, 0.85]");
    }
}
