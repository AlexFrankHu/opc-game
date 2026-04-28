package com.lastbastion.game.zone;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
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
 * TASK-006 Zone 推图 + 离线挂机结算。
 */
public final class ZoneService {

    /** offline reward cap in ms */
    public static final long IDLE_CAP_MS = 12L * 3600 * 1000;
    public static final long IDLE_CAP_PREMIUM_MS = 24L * 3600 * 1000;

    private final ZoneConfigRepository repo;
    private final ResourceService resource;
    private final AnalyticsService analytics;
    private final Random rng;

    public ZoneService(ZoneConfigRepository repo, ResourceService resource,
                       AnalyticsService analytics, Random rng) {
        this.repo = repo;
        this.resource = resource;
        this.analytics = analytics;
        this.rng = rng;
    }

    /** 试图通关指定关卡。需先通过 CombatSimulator 跑出结果。 */
    public AttemptResult clear(PlayerContext ctx, int chapterId, int stageId, boolean allyWon) {
        ZoneConfig chapter = repo.byChapter(chapterId);
        if (chapter == null) throw new GameException(ErrorCode.NOT_FOUND, "chapter " + chapterId);
        // 线性解锁
        int curChap = ctx.zoneProgressChapter();
        int curStage = ctx.zoneProgressStage();
        boolean isNextLinear =
                (chapterId == curChap && stageId == curStage + 1)
                || (chapterId == curChap && stageId <= curStage) // replay
                || (chapterId == curChap + 1 && stageId == 1 && curStage >= chapter.stages.size());
        if (!isNextLinear) throw new GameException(ErrorCode.ZONE_LOCKED);

        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("zone_attempt")
                    .prop("player_id", ctx.playerId())
                    .prop("chapter", chapterId)
                    .prop("stage", stageId)
                    .build());
        }

        AttemptResult result = new AttemptResult();
        result.chapterId = chapterId;
        result.stageId = stageId;
        result.won = allyWon;
        if (!allyWon) {
            if (analytics != null) {
                analytics.emit(AnalyticsEvent.of("zone_fail")
                        .prop("player_id", ctx.playerId())
                        .prop("chapter", chapterId).prop("stage", stageId).build());
            }
            return result;
        }

        // 奖励
        boolean firstClear = (chapterId > curChap)
                || (chapterId == curChap && stageId > curStage);
        rollAndGrantDrops(ctx, chapter.drops, result);
        if (firstClear) {
            rollAndGrantDrops(ctx, chapter.firstClearRewards, result);
            ctx.setZoneProgress(chapterId, stageId);
            if (stageId >= chapter.stages.size() && repo.byChapter(chapterId + 1) != null) {
                ctx.setZoneProgress(chapterId + 1, 0);
            }
        }

        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("zone_complete")
                    .prop("player_id", ctx.playerId())
                    .prop("chapter", chapterId)
                    .prop("stage", stageId)
                    .prop("first_clear", firstClear)
                    .build());
        }
        return result;
    }

    private void rollAndGrantDrops(PlayerContext ctx, List<ZoneConfig.DropEntry> drops,
                                   AttemptResult result) {
        for (ZoneConfig.DropEntry d : drops) {
            if (rng.nextDouble() > d.probability) continue;
            if (d.itemType.startsWith("CURRENCY_")) {
                CurrencyType c = CurrencyType.valueOf(d.itemType.substring("CURRENCY_".length()));
                resource.add(ctx, c, d.amount, SourceTag.ZONE_DROP);
                result.rewards.merge(d.itemType, d.amount, Long::sum);
            } else if (d.itemType.equals("SURVIVOR_SHARD")) {
                ctx.survivorShards().merge(d.payload, d.amount, Long::sum);
                result.rewards.merge(d.itemType + ":" + d.payload, d.amount, Long::sum);
            } else {
                // item bag would be used in a complete implementation
                result.rewards.merge(d.itemType, d.amount, Long::sum);
            }
        }
    }

    /**
     * 结算离线挂机收益。按当前关卡掉落率乘以（时长 / 战斗时长）估算。
     */
    public IdleReward settleIdle(PlayerContext ctx, long nowMs) {
        long last = ctx.lastLogoutTimestamp();
        if (last == 0) last = nowMs;
        long elapsed = Math.max(0, nowMs - last);
        long cap = ctx.battlePassActive() ? IDLE_CAP_PREMIUM_MS : IDLE_CAP_MS;
        long effective = Math.min(cap, elapsed);

        // 假定：每 30 秒视为一次推图。
        long fightsPerHour = 120;
        double hours = effective / 3_600_000.0;
        long fights = Math.round(hours * fightsPerHour);

        ZoneConfig chapter = repo.byChapter(ctx.zoneProgressChapter());
        IdleReward r = new IdleReward();
        r.elapsedMs = elapsed;
        r.effectiveMs = effective;
        if (chapter == null || fights == 0) return r;

        // expected rewards per fight from drops table
        Map<CurrencyType, Long> grant = new HashMap<>();
        for (ZoneConfig.DropEntry d : chapter.drops) {
            if (!d.itemType.startsWith("CURRENCY_")) continue;
            CurrencyType c = CurrencyType.valueOf(d.itemType.substring("CURRENCY_".length()));
            long expected = (long) (d.probability * d.amount * fights);
            if (expected <= 0) continue;
            grant.merge(c, expected, Long::sum);
        }
        for (Map.Entry<CurrencyType, Long> e : grant.entrySet()) {
            resource.add(ctx, e.getKey(), e.getValue(), SourceTag.ZONE_DROP);
            r.currency.put(e.getKey(), e.getValue());
        }
        return r;
    }

    public static final class AttemptResult {
        public int chapterId;
        public int stageId;
        public boolean won;
        public Map<String, Long> rewards = new HashMap<>();
    }

    public static final class IdleReward {
        public long elapsedMs;
        public long effectiveMs;
        public Map<CurrencyType, Long> currency = new HashMap<>();
    }

    /**
     * 扫荡倍速（1 / 2 / 4）—— 供客户端跳过战斗动画；战斗逻辑已在服务端结算，倍速只影响UI。
     */
    public List<AttemptResult> sweep(PlayerContext ctx, int chapterId, int stageId, int times,
                                     boolean wonOutcome) {
        if (times < 1) throw new GameException(ErrorCode.ILLEGAL_ARG);
        List<AttemptResult> out = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            out.add(clear(ctx, chapterId, stageId, wonOutcome));
        }
        return out;
    }
}
