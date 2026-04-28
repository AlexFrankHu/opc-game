package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.CurrencyType;
import com.lastbastion.game.monetization.BattlePassConfig;
import com.lastbastion.game.monetization.BattlePassConfig.RewardKind;

import java.util.ArrayList;
import java.util.List;

/** Battle Pass（battlepass.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class BattlePassTuning {
    public int seasonId;
    public int seasonDurationDays;
    public int maxLevel;
    public List<Long> xpCurve = new ArrayList<>();
    public List<RewardEntry> freeTrack = new ArrayList<>();
    public List<RewardEntry> premiumTrack = new ArrayList<>();

    public static final class RewardEntry {
        public int level;
        public RewardKind kind;
        public CurrencyType currency;
        public String payload;
        public long amount;
    }

    public BattlePassConfig toBattlePassConfig() {
        BattlePassConfig c = new BattlePassConfig();
        c.seasonId = seasonId;
        c.seasonDurationMs = seasonDurationDays * 24L * 3600L * 1000L;
        c.xpCurve = new long[xpCurve.size()];
        for (int i = 0; i < xpCurve.size(); i++) c.xpCurve[i] = xpCurve.get(i);
        for (RewardEntry r : freeTrack) c.freeTrack.add(toReward(r));
        for (RewardEntry r : premiumTrack) c.premiumTrack.add(toReward(r));
        return c;
    }

    private static BattlePassConfig.Reward toReward(RewardEntry e) {
        BattlePassConfig.Reward r = new BattlePassConfig.Reward();
        r.level = e.level;
        r.kind = e.kind;
        r.currency = e.currency;
        r.payload = e.payload;
        r.amount = e.amount;
        return r;
    }
}
