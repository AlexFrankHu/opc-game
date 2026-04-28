package com.lastbastion.game.augment;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.IdGenerator;
import com.lastbastion.common.Stats;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.numeric.AugmentTuning;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import com.lastbastion.game.survivor.SurvivorInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * TASK-005 合成 (3→1) + 镶嵌。
 */
public final class AugmentService {

    private final AnalyticsService analytics;
    private final ResourceService resource;
    private final AugmentTuning tuning;

    public AugmentService(AnalyticsService analytics, ResourceService resource) {
        this(analytics, resource, NumericConfig.defaults().augment());
    }

    public AugmentService(AnalyticsService analytics, ResourceService resource, AugmentTuning tuning) {
        this.analytics = analytics;
        this.resource = resource;
        this.tuning = tuning;
    }

    public AugmentTuning tuning() { return tuning; }
    public int bagCapacity() { return tuning.bagCapacity; }
    public long removeCostCredits() { return tuning.removeCostCredits; }

    public AugmentInstance add(PlayerContext ctx, AugmentType type, int star) {
        if (ctx.augmentBag().size() >= tuning.bagCapacity) {
            throw new GameException(ErrorCode.BAG_FULL);
        }
        AugmentInstance inst = new AugmentInstance(IdGenerator.next(), type, star);
        ctx.augmentBag().put(inst.instanceId(), inst);
        return inst;
    }

    /** 合成：3 个同类型同星级 → 1 个高一星。 */
    public AugmentInstance fuse(PlayerContext ctx, long id1, long id2, long id3) {
        AugmentInstance a = require(ctx, id1);
        AugmentInstance b = require(ctx, id2);
        AugmentInstance c = require(ctx, id3);
        if (a.type() != b.type() || b.type() != c.type()) {
            throw new GameException(ErrorCode.FUSION_MISMATCH, "type mismatch");
        }
        if (a.star() != b.star() || b.star() != c.star()) {
            throw new GameException(ErrorCode.FUSION_MISMATCH, "star mismatch");
        }
        if (a.star() >= tuning.maxStar) throw new GameException(ErrorCode.ILLEGAL_ARG, "already max star");
        if (a.equippedSurvivorId() != 0 || b.equippedSurvivorId() != 0 || c.equippedSurvivorId() != 0) {
            throw new GameException(ErrorCode.ILLEGAL_ARG, "cannot fuse equipped augment");
        }
        ctx.augmentBag().remove(id1);
        ctx.augmentBag().remove(id2);
        ctx.augmentBag().remove(id3);
        AugmentInstance out = add(ctx, a.type(), a.star() + 1);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("augment_fusion")
                    .prop("player_id", ctx.playerId())
                    .prop("type", a.type().name())
                    .prop("to_star", out.star())
                    .build());
        }
        return out;
    }

    /**
     * 批量合成至 target star（自动挑选库存中同类型同星级的 3 个组合合成）。
     * 贪心实现。
     */
    public int batchFuse(PlayerContext ctx, AugmentType type, int targetStar) {
        int fused = 0;
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int s = 1; s < targetStar; s++) {
                List<Long> ids = new ArrayList<>();
                for (AugmentInstance inst : ctx.augmentBag().values()) {
                    if (inst.type() == type && inst.star() == s && inst.equippedSurvivorId() == 0) {
                        ids.add(inst.instanceId());
                        if (ids.size() == 3) break;
                    }
                }
                if (ids.size() == 3) {
                    fuse(ctx, ids.get(0), ids.get(1), ids.get(2));
                    fused++;
                    progress = true;
                    break;
                }
            }
        }
        return fused;
    }

    public void insert(PlayerContext ctx, long survivorId, int slotIndex, long augmentId) {
        if (slotIndex < 0 || slotIndex >= 3) throw new GameException(ErrorCode.ILLEGAL_ARG, "slot 0..2");
        SurvivorInstance s = ctx.survivors().get(survivorId);
        if (s == null) throw new GameException(ErrorCode.NOT_FOUND, "survivor " + survivorId);
        AugmentInstance aug = require(ctx, augmentId);
        if (aug.equippedSurvivorId() != 0) {
            throw new GameException(ErrorCode.GEAR_ALREADY_EQUIPPED, "augment in use");
        }
        long prev = s.augmentSlots()[slotIndex];
        if (prev != 0) {
            AugmentInstance p = ctx.augmentBag().get(prev);
            if (p != null) p.clearEquipped();
        }
        s.augmentSlots()[slotIndex] = augmentId;
        aug.setEquipped(survivorId, slotIndex);
    }

    public void remove(PlayerContext ctx, long survivorId, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 3) throw new GameException(ErrorCode.ILLEGAL_ARG);
        SurvivorInstance s = ctx.survivors().get(survivorId);
        if (s == null) throw new GameException(ErrorCode.NOT_FOUND);
        long id = s.augmentSlots()[slotIndex];
        if (id == 0) return;
        resource.spend(ctx, CurrencyType.CREDITS, tuning.removeCostCredits, SourceTag.AUGMENT_REMOVE);
        s.augmentSlots()[slotIndex] = 0;
        AugmentInstance a = ctx.augmentBag().get(id);
        if (a != null) a.clearEquipped();
    }

    /** 汇总一个 Survivor 所有镶嵌的 Augment 属性。 */
    public Stats collectStats(PlayerContext ctx, long survivorId) {
        SurvivorInstance s = ctx.survivors().get(survivorId);
        Stats out = new Stats();
        if (s == null) return out;
        for (long id : s.augmentSlots()) {
            if (id == 0) continue;
            AugmentInstance a = ctx.augmentBag().get(id);
            if (a != null) out.addAll(tuning.statsFor(a.type(), a.star()));
        }
        return out;
    }

    private AugmentInstance require(PlayerContext ctx, long id) {
        AugmentInstance a = ctx.augmentBag().get(id);
        if (a == null) throw new GameException(ErrorCode.NOT_FOUND, "augment " + id);
        return a;
    }
}
