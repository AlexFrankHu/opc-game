package com.lastbastion.game.gear;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.GearQuality;
import com.lastbastion.common.GearSlot;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.numeric.GearTuning;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import com.lastbastion.game.survivor.SurvivorInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * TASK-004 Gear 强化 / 分解 / 锁定 / 穿戴。
 */
public final class GearService {

    private final ResourceService resource;
    private final AnalyticsService analytics;
    private final Random rng;
    private final GearTuning tuning;
    private int bagCapacity;

    public GearService(ResourceService resource, AnalyticsService analytics, Random rng) {
        this(resource, analytics, rng, NumericConfig.defaults().gear());
    }

    public GearService(ResourceService resource, AnalyticsService analytics, Random rng, GearTuning tuning) {
        this.resource = resource;
        this.analytics = analytics;
        this.rng = rng;
        this.tuning = tuning;
        this.bagCapacity = tuning.bagCapacityDefault;
    }

    public int maxLevel() { return tuning.maxLevel; }

    public void setBagCapacity(int cap) {
        this.bagCapacity = cap;
    }

    /** 加入背包；满时抛 BAG_FULL。 */
    public synchronized void add(PlayerContext ctx, GearInstance gear) {
        if (ctx.gearBag().size() >= bagCapacity) {
            throw new GameException(ErrorCode.BAG_FULL);
        }
        ctx.gearBag().put(gear.instanceId(), gear);
    }

    public GearInstance require(PlayerContext ctx, long gearId) {
        GearInstance g = ctx.gearBag().get(gearId);
        if (g == null) throw new GameException(ErrorCode.NOT_FOUND, "gear " + gearId);
        return g;
    }

    /**
     * 强化 +1，成功率根据当前等级衰减；
     * +1..+12 100% / +13..+16 80..50% / +17..+20 30..15%。
     */
    public synchronized EnhanceResult enhance(PlayerContext ctx, long gearId) {
        GearInstance g = require(ctx, gearId);
        if (g.level() >= tuning.maxLevel) throw new GameException(ErrorCode.ENHANCE_MAX);
        long cost = tuning.enhanceCost(g.level());
        resource.spend(ctx, CurrencyType.ALLOY, cost, SourceTag.GEAR_ENHANCE);

        double p = successRate(g.level() + 1);
        boolean ok = rng.nextDouble() < p;
        if (ok) g.setLevel(g.level() + 1);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("gear_enhance")
                    .prop("player_id", ctx.playerId())
                    .prop("gear_id", gearId)
                    .prop("to_level", g.level())
                    .prop("success", ok)
                    .build());
        }
        return new EnhanceResult(ok, g.level(), cost);
    }

    public double successRate(int toLevel) {
        return tuning.successRate(toLevel);
    }

    /** 分解，产出 Alloy。锁定装备跳过。批量按品质筛选。 */
    public synchronized long decompose(PlayerContext ctx, List<Long> gearIds) {
        long totalAlloy = 0;
        List<Long> toRemove = new ArrayList<>();
        for (Long id : gearIds) {
            GearInstance g = ctx.gearBag().get(id);
            if (g == null) continue;
            if (g.locked()) continue; // §4.4
            if (g.equippedSurvivorId() != 0) continue;
            long payout = tuning.decomposeAlloy(g.quality(), g.level());
            totalAlloy += payout;
            toRemove.add(id);
        }
        for (Long id : toRemove) ctx.gearBag().remove(id);
        if (totalAlloy > 0) resource.add(ctx, CurrencyType.ALLOY, totalAlloy, SourceTag.DECOMPOSE);
        return totalAlloy;
    }

    public synchronized void setLock(PlayerContext ctx, long gearId, boolean lock) {
        GearInstance g = require(ctx, gearId);
        g.setLocked(lock);
    }

    public synchronized void equip(PlayerContext ctx, long survivorId, long gearId) {
        SurvivorInstance survivor = ctx.survivors().get(survivorId);
        if (survivor == null) throw new GameException(ErrorCode.NOT_FOUND, "survivor " + survivorId);
        GearInstance g = require(ctx, gearId);
        if (g.equippedSurvivorId() != 0 && g.equippedSurvivorId() != survivorId) {
            throw new GameException(ErrorCode.GEAR_ALREADY_EQUIPPED);
        }
        // unequip existing in this slot
        Long existing = survivor.equipped().get(g.slot());
        if (existing != null && existing != 0) {
            GearInstance prev = ctx.gearBag().get(existing);
            if (prev != null) prev.setEquippedSurvivorId(0);
        }
        survivor.equipped().put(g.slot(), g.instanceId());
        g.setEquippedSurvivorId(survivorId);
    }

    public synchronized void unequip(PlayerContext ctx, long survivorId, GearSlot slot) {
        SurvivorInstance survivor = ctx.survivors().get(survivorId);
        if (survivor == null) throw new GameException(ErrorCode.NOT_FOUND, "survivor " + survivorId);
        Long id = survivor.equipped().remove(slot);
        if (id == null || id == 0) return;
        GearInstance g = ctx.gearBag().get(id);
        if (g != null) g.setEquippedSurvivorId(0);
    }

    /** 聚合某 Survivor 所有装备位的属性加成。 */
    public Map<String, com.lastbastion.common.Stats> collectStats(PlayerContext ctx, long survivorId) {
        SurvivorInstance survivor = ctx.survivors().get(survivorId);
        java.util.LinkedHashMap<String, com.lastbastion.common.Stats> out = new java.util.LinkedHashMap<>();
        if (survivor == null) return out;
        for (Map.Entry<GearSlot, Long> e : survivor.equipped().entrySet()) {
            if (e.getValue() == null || e.getValue() == 0) continue;
            GearInstance g = ctx.gearBag().get(e.getValue());
            if (g != null) out.put(e.getKey().name(), g.toStats());
        }
        return out;
    }

    public static final class EnhanceResult {
        public final boolean success;
        public final int level;
        public final long alloySpent;

        public EnhanceResult(boolean success, int level, long alloySpent) {
            this.success = success;
            this.level = level;
            this.alloySpent = alloySpent;
        }
    }
}
