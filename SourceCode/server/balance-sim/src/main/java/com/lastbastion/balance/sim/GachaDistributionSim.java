package com.lastbastion.balance.sim;

import com.lastbastion.balance.SimContext;
import com.lastbastion.balance.SimReport;
import com.lastbastion.common.Rarity;
import com.lastbastion.game.numeric.GachaTuning;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Pull N times against the configured rates; assert empirical hit rate matches
 * declared rates within tolerance. Also verify pity ceiling ≤ {@code pityLimit}
 * always grants a LEGENDARY.
 */
public final class GachaDistributionSim {

    public static final String NAME = "gacha";
    /**
     * Allowed deviation per rarity (absolute). Pity ceiling biases
     * LEGENDARY upward and RARE downward by ~1–2 pp; we accept ±2.5 pp.
     */
    public static final double TOLERANCE = 0.025;

    public Map<String, Object> run(SimContext ctx, SimReport report) {
        GachaTuning t = ctx.cfg.gacha();
        Random rng = new Random(ctx.seed ^ 0x9ACDA);
        int pulls = ctx.size.gachaPulls();

        Rarity[] rarities = t.rates.keySet().toArray(new Rarity[0]);
        double[] cumulative = new double[rarities.length];
        double acc = 0;
        for (int i = 0; i < rarities.length; i++) {
            acc += t.rates.get(rarities[i]);
            cumulative[i] = acc;
        }

        EnumMap<Rarity, Long> hits = new EnumMap<>(Rarity.class);
        for (Rarity r : rarities) hits.put(r, 0L);
        int pityCounter = 0;
        long pityHits = 0;
        long maxPityRun = 0;
        long pityRun = 0;

        for (int i = 0; i < pulls; i++) {
            Rarity rolled = rollOnce(rng, rarities, cumulative);
            pityCounter++;
            pityRun++;
            if (pityCounter + 1 >= t.pityLimit) {
                rolled = Rarity.LEGENDARY;
                pityHits++;
            }
            hits.merge(rolled, 1L, Long::sum);
            if (rolled == Rarity.LEGENDARY) {
                pityCounter = 0;
                if (pityRun > maxPityRun) maxPityRun = pityRun;
                pityRun = 0;
            }
        }

        Map<String, Object> bag = report.beginSim(NAME);
        bag.put("pulls", pulls);
        bag.put("pityLimit", t.pityLimit);
        bag.put("pityHits", pityHits);
        bag.put("maxPityRun", maxPityRun);

        Map<String, Map<String, Double>> byRarity = new java.util.LinkedHashMap<>();
        for (Rarity r : rarities) {
            double observed = hits.get(r) / (double) pulls;
            double expected = t.rates.get(r);
            Map<String, Double> v = new java.util.LinkedHashMap<>();
            v.put("expected", round(expected, 5));
            v.put("observed", round(observed, 5));
            v.put("delta", round(observed - expected, 5));
            byRarity.put(r.name(), v);
            if (Math.abs(observed - expected) > TOLERANCE) {
                report.fail(NAME, r + " rate deviation " + (observed - expected) + " > " + TOLERANCE
                        + " (observed=" + observed + ", expected=" + expected + ")");
            }
        }
        bag.put("rates", byRarity);

        if (maxPityRun > t.pityLimit) {
            report.fail(NAME, "max pity run " + maxPityRun + " exceeds pityLimit " + t.pityLimit);
        }
        return bag;
    }

    private static Rarity rollOnce(Random rng, Rarity[] rarities, double[] cumulative) {
        double r = rng.nextDouble();
        for (int i = 0; i < cumulative.length; i++) {
            if (r < cumulative[i]) return rarities[i];
        }
        return rarities[rarities.length - 1];
    }

    private static double round(double v, int decimals) {
        double s = Math.pow(10, decimals);
        return Math.round(v * s) / s;
    }
}
