package com.lastbastion.balance.sim;

import com.lastbastion.balance.SimContext;
import com.lastbastion.balance.SimReport;
import com.lastbastion.game.numeric.ZoneIdleTuning;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compute deterministic offline reward for 12h / 24h windows from
 * {@code zone_idle.json} fightsPerHour and a synthetic drop table.
 *
 * Doesn't need RNG — formula is closed-form. Validates schema sanity.
 */
public final class ZoneIdleSim {

    public static final String NAME = "zone_idle";

    public Map<String, Object> run(SimContext ctx, SimReport report) {
        ZoneIdleTuning t = ctx.cfg.zoneIdle();

        long fightsPerHour = t.fightsPerHour;
        long fights12h = fightsPerHour * 12L;
        long fights24h = fightsPerHour * 24L;

        Map<String, Object> bag = report.beginSim(NAME);
        bag.put("fightsPerHour", fightsPerHour);
        bag.put("idleCapHours", t.idleCapMs() / 3_600_000L);
        bag.put("idleCapPremiumHours", t.idleCapPremiumMs() / 3_600_000L);
        bag.put("fights12h", fights12h);
        bag.put("fights24h", fights24h);

        // example: assume each fight yields ~50 credits expected
        long creditsPerFight = 50;
        Map<String, Long> example = new LinkedHashMap<>();
        example.put("expectedCredits12h", creditsPerFight * fights12h);
        example.put("expectedCredits24h", creditsPerFight * fights24h);
        bag.put("exampleProjection", example);

        if (fightsPerHour <= 0) report.fail(NAME, "fightsPerHour must be > 0");
        if (t.idleCapPremiumMs() < t.idleCapMs()) {
            report.fail(NAME, "premium cap " + t.idleCapPremiumMs()
                    + " must be >= base cap " + t.idleCapMs());
        }
        return bag;
    }
}
