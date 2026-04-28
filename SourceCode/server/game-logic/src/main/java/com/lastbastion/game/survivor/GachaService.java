package com.lastbastion.game.survivor;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.Rarity;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TASK-003 §3.5 — Recruit (Gacha) 系统，含 80 抽保底 Legendary。
 *
 * 免费池消耗 Recruit Tokens；付费池消耗 Premium Chips。
 */
public final class GachaService {

    public enum Pool { FREE, PREMIUM }

    /**
     * 概率表（参考主流放置 RPG）：
     *   Rare 0.85, Epic 0.13, Legendary 0.02
     *   （Common 稀有度不在招募池中，仅作为低品阶杂兵/掉落物存在。）
     */
    private static final double[] RATES = {0.85, 0.13, 0.02};
    private static final Rarity[] RARITIES = {Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY};

    public static final int PITY_LIMIT = 80;
    public static final long SINGLE_COST_TOKEN = 1;
    public static final long SINGLE_COST_CHIP = 30;
    public static final long TEN_COST_TOKEN = 10;
    public static final long TEN_COST_CHIP = 270; // 10% 折扣
    public static final long SHARDS_PER_DUPLICATE = 20;

    private final SurvivorConfigRepository repo;
    private final SurvivorService survivorService;
    private final ResourceService resourceService;
    private final AnalyticsService analytics;
    private final Random rng;

    private final Map<Long, Integer> pityCounter = new HashMap<>();

    public GachaService(SurvivorConfigRepository repo, SurvivorService survivorService,
                        ResourceService resourceService, AnalyticsService analytics, Random rng) {
        this.repo = repo;
        this.survivorService = survivorService;
        this.resourceService = resourceService;
        this.analytics = analytics;
        this.rng = rng;
    }

    public static final class Result {
        public final String configId;
        public final Rarity rarity;
        public final boolean duplicate;
        public final long shardsAdded;

        public Result(String configId, Rarity rarity, boolean duplicate, long shardsAdded) {
            this.configId = configId;
            this.rarity = rarity;
            this.duplicate = duplicate;
            this.shardsAdded = shardsAdded;
        }
    }

    public List<Result> pull(PlayerContext ctx, Pool pool, int count) {
        if (count != 1 && count != 10) throw new GameException(ErrorCode.ILLEGAL_ARG, "count must be 1 or 10");
        long cost = switch (pool) {
            case FREE -> count == 1 ? SINGLE_COST_TOKEN : TEN_COST_TOKEN;
            case PREMIUM -> count == 1 ? SINGLE_COST_CHIP : TEN_COST_CHIP;
        };
        CurrencyType currency = pool == Pool.FREE ? CurrencyType.RECRUIT_TOKENS : CurrencyType.PREMIUM_CHIPS;
        resourceService.spend(ctx, currency, cost, SourceTag.GACHA);

        List<Result> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(doPull(ctx));

        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("gacha_pull")
                    .prop("player_id", ctx.playerId())
                    .prop("pool", pool.name())
                    .prop("count", count)
                    .build());
        }
        return out;
    }

    private Result doPull(PlayerContext ctx) {
        int pity = pityCounter.getOrDefault(ctx.playerId(), 0);
        Rarity rolled;
        if (pity + 1 >= PITY_LIMIT) {
            rolled = Rarity.LEGENDARY;
        } else {
            rolled = rollRarity();
        }
        if (rolled == Rarity.LEGENDARY) {
            pityCounter.put(ctx.playerId(), 0);
        } else {
            pityCounter.put(ctx.playerId(), pity + 1);
        }
        // pick a random survivor of the rolled rarity
        List<SurvivorConfig> pool = new ArrayList<>();
        for (SurvivorConfig c : repo.all()) if (c.rarity == rolled) pool.add(c);
        if (pool.isEmpty()) throw new GameException(ErrorCode.GACHA_POOL_EMPTY, "no " + rolled);
        SurvivorConfig chosen = pool.get(rng.nextInt(pool.size()));

        // do we already own? turn to shards.
        boolean owned = ctx.survivors().values().stream()
                .anyMatch(s -> s.configId().equals(chosen.id));
        if (owned) {
            ctx.survivorShards().merge(chosen.id, SHARDS_PER_DUPLICATE, Long::sum);
            return new Result(chosen.id, rolled, true, SHARDS_PER_DUPLICATE);
        } else {
            survivorService.grant(ctx, chosen.id);
            return new Result(chosen.id, rolled, false, 0);
        }
    }

    private Rarity rollRarity() {
        double roll = rng.nextDouble();
        double acc = 0;
        for (int i = 0; i < RATES.length; i++) {
            acc += RATES[i];
            if (roll < acc) return RARITIES[i];
        }
        return RARITIES[RARITIES.length - 1];
    }

    public int pityFor(long playerId) {
        return pityCounter.getOrDefault(playerId, 0);
    }

    /** 将重复英雄转化为碎片（外部入口）。 */
    public long convertDuplicate(PlayerContext ctx, long survivorId) {
        SurvivorInstance inst = ctx.survivors().get(survivorId);
        if (inst == null) throw new GameException(ErrorCode.NOT_FOUND);
        boolean hasDuplicate = ctx.survivors().values().stream()
                .filter(s -> s.configId().equals(inst.configId()))
                .count() >= 2;
        if (!hasDuplicate) throw new GameException(ErrorCode.ILLEGAL_ARG, "must keep at least one");
        ctx.survivors().remove(survivorId);
        ctx.survivorShards().merge(inst.configId(), SHARDS_PER_DUPLICATE, Long::sum);
        return SHARDS_PER_DUPLICATE;
    }
}
