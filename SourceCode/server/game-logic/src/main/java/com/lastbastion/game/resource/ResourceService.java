package com.lastbastion.game.resource;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.numeric.ResourceTuning;
import com.lastbastion.game.player.PlayerContext;

import java.util.EnumMap;
import java.util.Map;

/**
 * TASK-008 §8.1 货币增减 + §8.4 资源流水埋点。
 */
public final class ResourceService {

    /**
     * 货币上限（防溢出）。来源 {@link ResourceTuning} / assets/numeric/resources.json。
     * 公开 static 引用为旧测试保留兼容；新代码请走构造器注入的 {@link #cap(CurrencyType)}。
     */
    public static final EnumMap<CurrencyType, Long> CAPS =
            NumericConfig.defaults().resources().capsAsEnumMap();

    private final AnalyticsService analytics;
    private final ResourceTuning tuning;

    public ResourceService(AnalyticsService analytics) {
        this(analytics, NumericConfig.defaults().resources());
    }

    public ResourceService(AnalyticsService analytics, ResourceTuning tuning) {
        this.analytics = analytics;
        this.tuning = tuning;
    }

    public long cap(CurrencyType type) {
        return tuning.cap(type);
    }

    /** 增加货币，返回实际添加数量（受上限约束）。 */
    public synchronized long add(PlayerContext ctx, CurrencyType type, long amount, SourceTag source) {
        if (amount <= 0) throw new GameException(ErrorCode.ILLEGAL_ARG, "amount must be > 0");
        long current = ctx.currencies().getOrDefault(type, 0L);
        long cap = tuning.cap(type);
        long newVal = Math.min(cap, current + amount);
        long actual = newVal - current;
        ctx.currencies().put(type, newVal);
        emit(ctx, "currency_add", type, actual, source);
        return actual;
    }

    /** 扣除货币，原子；不足则抛异常。 */
    public synchronized void spend(PlayerContext ctx, CurrencyType type, long amount, SourceTag source) {
        if (amount <= 0) throw new GameException(ErrorCode.ILLEGAL_ARG, "amount must be > 0");
        long current = ctx.currencies().getOrDefault(type, 0L);
        if (current < amount) throw new GameException(ErrorCode.INSUFFICIENT_CURRENCY,
                type + " need " + amount + " have " + current);
        ctx.currencies().put(type, current - amount);
        emit(ctx, "currency_spend", type, amount, source);
    }

    public synchronized void spendBatch(PlayerContext ctx, Map<CurrencyType, Long> costs, SourceTag source) {
        // validate first (全部满足才扣)
        for (Map.Entry<CurrencyType, Long> e : costs.entrySet()) {
            long cur = ctx.currencies().getOrDefault(e.getKey(), 0L);
            if (cur < e.getValue()) throw new GameException(ErrorCode.INSUFFICIENT_CURRENCY,
                    e.getKey() + " need " + e.getValue() + " have " + cur);
        }
        for (Map.Entry<CurrencyType, Long> e : costs.entrySet()) {
            spend(ctx, e.getKey(), e.getValue(), source);
        }
    }

    public long balance(PlayerContext ctx, CurrencyType type) {
        return ctx.currencies().getOrDefault(type, 0L);
    }

    private void emit(PlayerContext ctx, String name, CurrencyType type, long amount, SourceTag source) {
        if (analytics == null) return;
        analytics.emit(AnalyticsEvent.of(name)
                .prop("player_id", ctx.playerId())
                .prop("currency", type.name())
                .prop("amount", amount)
                .prop("source", source == null ? "UNKNOWN" : source.name())
                .build());
    }
}
