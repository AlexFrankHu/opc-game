package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** 竞技场（arena.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class ArenaTuning {
    public int dailyFreeChallenges;
    public int dailyBuyLimit;
    public long buyCostChips;
    public int matchPoolSize;
    public double matchPowerWindowLow;
    public double matchPowerWindowHigh;
    public int scoreWinSwap;
    public int scoreWinNoSwap;
    public int scoreLossSelf;
    public int scoreLossOpponentOnSwap;
    public List<RankTier> ranks = new ArrayList<>();

    public static final class RankTier {
        public String id;
        public int minScore;
        public long seasonRewardChips;
        public long seasonRewardHonor;
    }

    public RankTier rankFor(int score) {
        RankTier match = ranks.isEmpty() ? null : ranks.get(0);
        for (RankTier t : ranks) {
            if (score >= t.minScore) match = t;
        }
        return match;
    }
}
