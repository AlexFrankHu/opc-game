package com.lastbastion.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lastbastion.balance.sim.ArenaScoreSim;
import com.lastbastion.balance.sim.BattlePassSim;
import com.lastbastion.balance.sim.CombatBalanceSim;
import com.lastbastion.balance.sim.GachaDistributionSim;
import com.lastbastion.balance.sim.GearEnhanceSim;
import com.lastbastion.balance.sim.ZoneIdleSim;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.numeric.NumericConfigException;
import com.lastbastion.game.numeric.NumericConfigLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Offline CLI driver for TASK-013 simulators.
 *
 * <pre>
 * mvn -pl balance-sim -am exec:java \
 *   -Dexec.mainClass=com.lastbastion.balance.BalanceSimMain \
 *   -Dexec.args="--sim=all --out=balance-report.json --size=large"
 * </pre>
 */
public final class BalanceSimMain {

    public static void main(String[] args) {
        Args a = Args.parse(args);
        NumericConfig cfg;
        try {
            cfg = NumericConfigLoader.load();
        } catch (NumericConfigException e) {
            System.err.println("[balance-sim] failed to load NumericConfig: " + e.getMessage());
            System.exit(2);
            return;
        }

        SimContext.SampleSize size = a.size;
        SimContext ctx = new SimContext(cfg, a.seed, size);
        SimReport report = new SimReport();

        if (a.runs("gacha"))      new GachaDistributionSim().run(ctx, report);
        if (a.runs("gear"))       new GearEnhanceSim().run(ctx, report);
        if (a.runs("combat"))     new CombatBalanceSim().run(ctx, report);
        if (a.runs("zone_idle"))  new ZoneIdleSim().run(ctx, report);
        if (a.runs("battlepass")) new BattlePassSim().run(ctx, report);
        if (a.runs("arena"))      new ArenaScoreSim().run(ctx, report);

        try {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(report);
            if (a.outPath != null) {
                Files.writeString(a.outPath, json);
            }
            System.out.println(json);
        } catch (Exception e) {
            System.err.println("[balance-sim] failed to write report: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (!report.allPassed()) {
            System.err.println("[balance-sim] FAIL: " + report.assertionFailures);
            System.exit(1);
        }
        System.out.println("[balance-sim] OK — " + report.sims.size() + " simulators all green");
    }

    static final class Args {
        Set<String> sims = new LinkedHashSet<>();
        Path outPath;
        long seed = 12345L;
        SimContext.SampleSize size = SimContext.SampleSize.LARGE;

        boolean runs(String name) {
            return sims.contains("all") || sims.contains(name);
        }

        static Args parse(String[] argv) {
            Args a = new Args();
            for (String s : argv) {
                if (s.startsWith("--sim=")) {
                    for (String n : s.substring("--sim=".length()).split(",")) {
                        a.sims.add(n.trim());
                    }
                } else if (s.startsWith("--out=")) {
                    a.outPath = Path.of(s.substring("--out=".length()));
                } else if (s.startsWith("--seed=")) {
                    a.seed = Long.parseLong(s.substring("--seed=".length()));
                } else if (s.startsWith("--size=")) {
                    a.size = SimContext.SampleSize.valueOf(s.substring("--size=".length()).toUpperCase());
                }
            }
            if (a.sims.isEmpty()) a.sims.add("all");
            return a;
        }
    }

    private BalanceSimMain() {}
}
