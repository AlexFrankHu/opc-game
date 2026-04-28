package com.lastbastion.balance.sim;

import com.lastbastion.balance.SimContext;
import com.lastbastion.balance.SimReport;
import com.lastbastion.game.numeric.BattlePassTuning;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Project a free-tier player's BP progression assuming a fixed daily XP income
 * (drawn from zone + arena + quests). Reports level reached on day 30 / 60.
 */
public final class BattlePassSim {

    public static final String NAME = "battlepass";
    /** Roughly: 5 zone clears (50xp) + 5 arena (40xp) + 3 quests (100xp) = 590 xp/day on day-1 baseline. */
    public static final long DAILY_FREE_XP = 600;

    public Map<String, Object> run(SimContext ctx, SimReport report) {
        BattlePassTuning t = ctx.cfg.battlePass();

        long xpDay30 = DAILY_FREE_XP * 30;
        long xpDay60 = DAILY_FREE_XP * 60;
        int lvDay30 = levelFor(t, xpDay30);
        int lvDay60 = levelFor(t, xpDay60);

        Map<String, Object> bag = report.beginSim(NAME);
        bag.put("dailyFreeXp", DAILY_FREE_XP);
        bag.put("totalXpDay30", xpDay30);
        bag.put("totalXpDay60", xpDay60);
        bag.put("levelAtDay30", lvDay30);
        bag.put("levelAtDay60", lvDay60);
        bag.put("xpToLv50", t.xpCurve.get(t.maxLevel));

        if (lvDay30 < 25) {
            report.fail(NAME, "free-tier day-30 level " + lvDay30 + " < 25 (under-rewarding)");
        }
        if (lvDay30 >= t.maxLevel) {
            report.fail(NAME, "free-tier day-30 already maxed " + lvDay30 + " (over-rewarding)");
        }
        if (lvDay60 < 40) {
            report.fail(NAME, "free-tier day-60 level " + lvDay60 + " < 40 (season too long)");
        }
        return bag;
    }

    private static int levelFor(BattlePassTuning t, long totalXp) {
        int lv = 0;
        for (int i = 0; i <= t.maxLevel; i++) {
            if (t.xpCurve.get(i) <= totalXp) lv = i;
            else break;
        }
        return lv;
    }
}
