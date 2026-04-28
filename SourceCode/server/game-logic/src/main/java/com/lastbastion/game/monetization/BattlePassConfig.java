package com.lastbastion.game.monetization;

import com.lastbastion.common.CurrencyType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 50 级赛季奖励配置；每级有 free + premium 两列。
 * 这里硬编码为默认赛季；实际可迁入 JSON 表。
 */
public final class BattlePassConfig {

    public int seasonId = 1;
    public long seasonDurationMs = 30L * 24 * 3600 * 1000;
    public long[] xpCurve = new long[BattlePassState.MAX_LEVEL + 1];
    public final List<Reward> freeTrack = new ArrayList<>();
    public final List<Reward> premiumTrack = new ArrayList<>();

    public static final class Reward {
        public int level;
        public RewardKind kind;
        public CurrencyType currency;
        public String payload;
        public long amount;
    }

    public enum RewardKind {
        CURRENCY, ITEM, GEAR_BOX, SURVIVOR_SHARD, EXCLUSIVE_SURVIVOR, EXCLUSIVE_SKIN
    }

    public static BattlePassConfig defaultSeason() {
        BattlePassConfig c = new BattlePassConfig();
        // 经验曲线 — 每级需要经验 = 200 + 30 * level
        long cum = 0;
        c.xpCurve[0] = 0;
        for (int lv = 1; lv <= BattlePassState.MAX_LEVEL; lv++) {
            cum += 200 + 30L * lv;
            c.xpCurve[lv] = cum;
        }
        for (int lv = 1; lv <= BattlePassState.MAX_LEVEL; lv++) {
            c.freeTrack.add(reward(lv, RewardKind.CURRENCY, CurrencyType.CREDITS, null, 500L + 50L * lv));
            if (lv % 5 == 0) {
                c.freeTrack.add(reward(lv, RewardKind.CURRENCY, CurrencyType.RECRUIT_TOKENS, null, 1));
            }
            c.premiumTrack.add(reward(lv, RewardKind.CURRENCY, CurrencyType.PREMIUM_CHIPS, null, 30L + 2L * lv));
            if (lv % 10 == 0) {
                c.premiumTrack.add(reward(lv, RewardKind.GEAR_BOX, null, "EPIC_GEAR_BOX", 1));
                c.premiumTrack.add(reward(lv, RewardKind.CURRENCY, CurrencyType.TECH_CORES, null, 100));
            }
        }
        // Premium 50 级 独占 Legendary Survivor
        c.premiumTrack.add(reward(50, RewardKind.EXCLUSIVE_SURVIVOR, null, "S_LEG_SEASON1", 1));
        return c;
    }

    private static Reward reward(int level, RewardKind kind, CurrencyType c, String payload, long amount) {
        Reward r = new Reward();
        r.level = level;
        r.kind = kind;
        r.currency = c;
        r.payload = payload;
        r.amount = amount;
        return r;
    }

    /** reward 索引 for O(1) 查询。 */
    public Map<Integer, List<Reward>> freeByLevel() {
        return groupByLevel(freeTrack);
    }

    public Map<Integer, List<Reward>> premiumByLevel() {
        return groupByLevel(premiumTrack);
    }

    private Map<Integer, List<Reward>> groupByLevel(List<Reward> list) {
        Map<Integer, List<Reward>> m = new LinkedHashMap<>();
        for (Reward r : list) m.computeIfAbsent(r.level, k -> new ArrayList<>()).add(r);
        return m;
    }
}
