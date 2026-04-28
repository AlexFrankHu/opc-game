package com.lastbastion.game.resource;

import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;

import java.util.Map;

/**
 * TASK-008 §8.2 & §8.3 — 道具背包与使用接口。
 */
public final class ItemService {

    public static final long STACK_CAP = 99999;
    public static final int BAG_DISTINCT_CAP = 999;

    private final AnalyticsService analytics;

    public ItemService(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    public synchronized void add(PlayerContext ctx, int itemId, long amount, SourceTag source) {
        if (amount <= 0) throw new GameException(ErrorCode.ILLEGAL_ARG);
        Map<Integer, Long> bag = ctx.items();
        long current = bag.getOrDefault(itemId, 0L);
        if (current == 0 && bag.size() >= BAG_DISTINCT_CAP) {
            throw new GameException(ErrorCode.BAG_FULL);
        }
        long newVal = Math.min(STACK_CAP, current + amount);
        bag.put(itemId, newVal);
        emit(ctx, "item_add", itemId, newVal - current, source);
    }

    public synchronized void consume(PlayerContext ctx, int itemId, long amount, SourceTag source) {
        if (amount <= 0) throw new GameException(ErrorCode.ILLEGAL_ARG);
        Map<Integer, Long> bag = ctx.items();
        long current = bag.getOrDefault(itemId, 0L);
        if (current < amount) throw new GameException(ErrorCode.INSUFFICIENT_ITEM,
                "item=" + itemId + " need " + amount);
        long remain = current - amount;
        if (remain == 0) bag.remove(itemId);
        else bag.put(itemId, remain);
        emit(ctx, "item_consume", itemId, amount, source);
    }

    public long count(PlayerContext ctx, int itemId) {
        return ctx.items().getOrDefault(itemId, 0L);
    }

    private void emit(PlayerContext ctx, String name, int itemId, long amount, SourceTag source) {
        if (analytics == null) return;
        analytics.emit(AnalyticsEvent.of(name)
                .prop("player_id", ctx.playerId())
                .prop("item_id", itemId)
                .prop("amount", amount)
                .prop("source", source == null ? "UNKNOWN" : source.name())
                .build());
    }
}
