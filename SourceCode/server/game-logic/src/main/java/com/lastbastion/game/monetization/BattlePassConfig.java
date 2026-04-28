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

    /** 默认赛季：来自 assets/numeric/battlepass.json（通过 NumericConfig 加载并缓存）。 */
    public static BattlePassConfig defaultSeason() {
        return com.lastbastion.game.numeric.NumericConfig.defaults().battlePass().toBattlePassConfig();
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
