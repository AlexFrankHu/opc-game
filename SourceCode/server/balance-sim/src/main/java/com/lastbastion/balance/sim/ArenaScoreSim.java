package com.lastbastion.balance.sim;

import com.lastbastion.balance.SimContext;
import com.lastbastion.balance.SimReport;
import com.lastbastion.game.numeric.ArenaTuning;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulate N players each fighting K days × 5 challenges/day. Skill differential
 * derived from each player's base power. Tracks score distribution and counts
 * how many reach each rank threshold.
 */
public final class ArenaScoreSim {

    public static final String NAME = "arena";

    public Map<String, Object> run(SimContext ctx, SimReport report) {
        ArenaTuning t = ctx.cfg.arena();
        int players = ctx.size.arenaPlayers();
        int days = ctx.size.arenaDays();
        int dailyChallenges = t.dailyFreeChallenges;
        Random rng = new Random(ctx.seed ^ 0x123ABC);

        // Each player has a "skill" rating ~ N(1500, 400)
        double[] skill = new double[players];
        int[] score = new int[players];
        for (int i = 0; i < players; i++) {
            skill[i] = 1000 + rng.nextGaussian() * 400;
            score[i] = 0;
        }

        for (int d = 0; d < days; d++) {
            for (int p = 0; p < players; p++) {
                for (int c = 0; c < dailyChallenges; c++) {
                    int oppIdx;
                    do { oppIdx = rng.nextInt(players); } while (oppIdx == p);
                    double diff = skill[p] - skill[oppIdx];
                    double winProb = 1.0 / (1.0 + Math.exp(-diff / 250.0));
                    boolean win = rng.nextDouble() < winProb;
                    boolean swap = win && score[oppIdx] > score[p];
                    int delta;
                    if (swap) {
                        delta = t.scoreWinSwap;
                        score[p] += t.scoreWinSwap;
                        score[oppIdx] = Math.max(0, score[oppIdx] + t.scoreLossOpponentOnSwap);
                    } else if (win) {
                        delta = t.scoreWinNoSwap;
                        score[p] += t.scoreWinNoSwap;
                    } else {
                        delta = t.scoreLossSelf;
                        score[p] = Math.max(0, score[p] + t.scoreLossSelf);
                    }
                    // suppress unused warning
                    if (delta == Integer.MIN_VALUE) throw new IllegalStateException();
                }
            }
        }

        int[] sorted = Arrays.copyOf(score, score.length);
        Arrays.sort(sorted);
        int max = sorted[sorted.length - 1];
        int min = sorted[0];
        double mean = Arrays.stream(sorted).average().orElse(0);

        Map<String, Long> rankCounts = new LinkedHashMap<>();
        for (ArenaTuning.RankTier rt : t.ranks) {
            long count = Arrays.stream(score).filter(s -> s >= rt.minScore).count();
            rankCounts.put(rt.id, count);
        }

        Map<String, Object> bag = report.beginSim(NAME);
        bag.put("players", players);
        bag.put("days", days);
        bag.put("scoreMin", min);
        bag.put("scoreMax", max);
        bag.put("scoreMean", round(mean, 2));
        bag.put("rankReached", rankCounts);

        long topTier = rankCounts.getOrDefault("KING", 0L)
                + rankCounts.getOrDefault("MASTER", 0L);
        double topPct = topTier / (double) players;
        bag.put("masterPlusPct", round(topPct, 4));
        if (topPct > 0.30) {
            report.fail(NAME, "MASTER+ tier reached by " + topPct + " > 0.30 (too inflated)");
        }
        if (max - min > 6000) {
            report.fail(NAME, "score spread " + (max - min) + " > 6000");
        }
        return bag;
    }

    private static double round(double v, int decimals) {
        double s = Math.pow(10, decimals);
        return Math.round(v * s) / s;
    }
}
