package com.lastbastion.balance.sim;

import com.lastbastion.balance.SimContext;
import com.lastbastion.balance.SimReport;
import com.lastbastion.game.numeric.GearTuning;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * For N independent gears, attempt 0→maxLevel using configured success rates.
 * Tracks: average final level, fail counter, distribution of "stuck" levels.
 */
public final class GearEnhanceSim {

    public static final String NAME = "gear";
    /** Lower bound of empirical reach-rate at level 15 (config has 60% nominal at lv15). */
    public static final double MIN_REACH_RATE_LV15 = 0.05; // very loose; full curve is product of probabilities

    public Map<String, Object> run(SimContext ctx, SimReport report) {
        GearTuning t = ctx.cfg.gear();
        int trials = ctx.size.gearTrials();
        Random rng = new Random(ctx.seed ^ 0x6EAB1);

        long reachLv15 = 0;
        long reachLv20 = 0;
        long totalFails = 0;
        long totalAttempts = 0;
        int[] finalLevels = new int[trials];

        for (int i = 0; i < trials; i++) {
            int lv = 0;
            // Each trial: keep enhancing until first failure that drops back to floor or until maxLevel.
            // We cap attempts to avoid pathological loops — 1000 attempts per trial.
            int attempts = 0;
            while (lv < t.maxLevel && attempts < 1000) {
                attempts++;
                totalAttempts++;
                double p = t.successRate(lv + 1);
                if (rng.nextDouble() < p) {
                    lv++;
                } else {
                    totalFails++;
                    // Mirror GearService.enhance behaviour: failure costs alloy but doesn't drop level.
                    // For sim purposes, we let the trial give up after K consecutive failures so it terminates.
                    if (attempts > 200) break;
                }
            }
            finalLevels[i] = lv;
            if (lv >= 15) reachLv15++;
            if (lv >= 20) reachLv20++;
        }

        double avg = 0;
        for (int v : finalLevels) avg += v;
        avg /= trials;

        Map<String, Object> bag = report.beginSim(NAME);
        bag.put("trials", trials);
        bag.put("avgFinalLevel", round(avg, 3));
        bag.put("reachLv15Pct", round(reachLv15 / (double) trials, 4));
        bag.put("reachLv20Pct", round(reachLv20 / (double) trials, 4));
        bag.put("totalAttempts", totalAttempts);
        bag.put("failRate", round(totalFails / (double) totalAttempts, 4));

        if (reachLv15 / (double) trials < MIN_REACH_RATE_LV15) {
            report.fail(NAME, "reachLv15Pct " + (reachLv15 / (double) trials)
                    + " < " + MIN_REACH_RATE_LV15);
        }

        // Histogram of final level
        Map<String, Long> hist = new LinkedHashMap<>();
        for (int v : finalLevels) hist.merge(String.valueOf(v), 1L, Long::sum);
        bag.put("finalLevelHistogram", hist);

        return bag;
    }

    private static double round(double v, int decimals) {
        double s = Math.pow(10, decimals);
        return Math.round(v * s) / s;
    }
}
