package com.lastbastion.game.survivor;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.Rarity;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.numeric.GachaTuning;
import com.lastbastion.game.numeric.NumericConfig;
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
 * <p>免费池消耗 Recruit Tokens；付费池消耗 Premium Chips。
 * <p>所有数值来自 {@link GachaTuning}（assets/numeric/gacha.json）。
 */
public final class GachaService {

    public enum Pool { FREE, PREMIUM }

    private final SurvivorConfigRepository repo;
    private final SurvivorService survivorService;
    private final ResourceService resourceService;
    private final AnalyticsService analytics;
    private final Random rng;
    private final GachaTuning tuning;

    /** rarities order matches probabilities ascending; resolved at construction. */
    private final Rarity[] rarities;
    private final double[] cumulative;

    private final Map<Long, Integer> pityCounter = new HashMap<>();

    public GachaService(SurvivorConfigRepository repo, SurvivorService survivorService,
                        ResourceService resourceService, AnalyticsService analytics, Random rng) {
        this(repo, survivorService, resourceService, analytics, rng, NumericConfig.defaults().gacha());
    }

    public GachaService(SurvivorConfigRepository repo, SurvivorService survivorService,
                        ResourceService resourceService, AnalyticsService analytics, Random rng,
                        GachaTuning tuning) {
        this.repo = repo;
        this.survivorService = survivorService;
        this.resourceService = resourceService;
        this.analytics = analytics;
        this.rng = rng;
        this.tuning = tuning;
        this.rarities = tuning.rates.keySet().toArray(new Rarity[0]);
        this.cumulative = new double[rarities.length];
        double acc = 0;
        for (int i = 0; i < rarities.length; i++) {
            acc += tuning.rates.get(rarities[i]);
            cumulative[i] = acc;
        }
    }

    public int pityLimit() { return tuning.pityLimit; }
    public long shardsPerDuplicate() { return tuning.shardsPerDuplicate; }

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
            case FREE -> count == 1 ? tuning.singleCostToken : tuning.tenCostToken;
            case PREMIUM -> count == 1 ? tuning.singleCostChip : tuning.tenCostChip;
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
        if (pity + 1 >= tuning.pityLimit) {
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

        boolean owned = ctx.survivors().values().stream()
                .anyMatch(s -> s.configId().equals(chosen.id));
        if (owned) {
            ctx.survivorShards().merge(chosen.id, tuning.shardsPerDuplicate, Long::sum);
            return new Result(chosen.id, rolled, true, tuning.shardsPerDuplicate);
        } else {
            survivorService.grant(ctx, chosen.id);
            return new Result(chosen.id, rolled, false, 0);
        }
    }

    private Rarity rollRarity() {
        double roll = rng.nextDouble();
        for (int i = 0; i < cumulative.length; i++) {
            if (roll < cumulative[i]) return rarities[i];
        }
        return rarities[rarities.length - 1];
    }
}
