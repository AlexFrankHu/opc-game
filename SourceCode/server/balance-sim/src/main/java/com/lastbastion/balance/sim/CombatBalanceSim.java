package com.lastbastion.balance.sim;

import com.lastbastion.balance.SimContext;
import com.lastbastion.balance.SimReport;
import com.lastbastion.combat.Ability;
import com.lastbastion.combat.CombatResult;
import com.lastbastion.combat.CombatSimulator;
import com.lastbastion.combat.CombatUnit;
import com.lastbastion.combat.Side;
import com.lastbastion.common.AttributeType;
import com.lastbastion.common.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Spar a synthetic 5v5 ally team against a scaled enemy team. Vary the power
 * ratio (1.0× / 1.5×) and aggregate win-rate / round-count.
 *
 * No external survivor data needed — units are pure stat blocks.
 */
public final class CombatBalanceSim {

    public static final String NAME = "combat";

    public Map<String, Object> run(SimContext ctx, SimReport report) {
        Random rng = new Random(ctx.seed ^ 0x4F5BA);
        int battlesPer = ctx.size.combatBattlesPerStage();

        Map<String, Object> bag = report.beginSim(NAME);

        Snapshot equal = battle(ctx, rng, 1.0, battlesPer);
        Snapshot strong = battle(ctx, rng, 1.5, battlesPer);
        Snapshot weak = battle(ctx, rng, 0.7, battlesPer);

        bag.put("battlesPerProfile", battlesPer);
        bag.put("equalPowerWinRate", equal.winRate);
        bag.put("equalAvgRounds", equal.avgRounds);
        bag.put("strongPowerWinRate", strong.winRate);
        bag.put("strongAvgRounds", strong.avgRounds);
        bag.put("weakPowerWinRate", weak.winRate);
        bag.put("weakAvgRounds", weak.avgRounds);

        if (equal.winRate < 0.30 || equal.winRate > 0.85) {
            report.fail(NAME, "equalPowerWinRate " + equal.winRate + " outside [0.30, 0.85]");
        }
        if (strong.winRate < 0.65) {
            report.fail(NAME, "strongPowerWinRate " + strong.winRate + " < 0.65");
        }
        if (weak.winRate > 0.45) {
            report.fail(NAME, "weakPowerWinRate " + weak.winRate + " > 0.45");
        }
        if (equal.avgRounds < 2 || equal.avgRounds > ctx.cfg.combat().maxRoundsHardCap) {
            report.fail(NAME, "equalAvgRounds " + equal.avgRounds + " out of range");
        }
        return bag;
    }

    private Snapshot battle(SimContext ctx, Random rng, double allyPowerScale, int n) {
        int wins = 0;
        long totalRounds = 0;
        for (int i = 0; i < n; i++) {
            List<CombatUnit> ally = newTeam("A", Side.ALLY, allyPowerScale);
            List<CombatUnit> enemy = newTeam("E", Side.ENEMY, 1.0);
            CombatSimulator sim = new CombatSimulator(ally, enemy,
                    ctx.cfg.combat().maxRoundsHardCap, rng);
            CombatResult r = sim.run();
            if (r.allyWon()) wins++;
            totalRounds += r.totalRounds();
        }
        Snapshot s = new Snapshot();
        s.winRate = round(wins / (double) n, 4);
        s.avgRounds = round(totalRounds / (double) n, 3);
        return s;
    }

    private static List<CombatUnit> newTeam(String prefix, Side side, double scale) {
        List<CombatUnit> units = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Stats stats = new Stats();
            stats.set(AttributeType.HP, 1000.0 * scale);
            stats.set(AttributeType.ATK, 100.0 * scale);
            stats.set(AttributeType.DEF, 50.0 * scale);
            stats.set(AttributeType.SPD, 100.0 + i);
            stats.set(AttributeType.CRIT_RATE, 0.10);
            stats.set(AttributeType.CRIT_DMG, 0.50);
            units.add(new CombatUnit(prefix + i, prefix + "-unit-" + i, side,
                    stats, List.<Ability>of(), false, false));
        }
        return units;
    }

    private static double round(double v, int decimals) {
        double s = Math.pow(10, decimals);
        return Math.round(v * s) / s;
    }

    private static final class Snapshot {
        double winRate;
        double avgRounds;
    }
}
