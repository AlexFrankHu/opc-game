package com.lastbastion.game.monetization;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

/**
 * TASK-009 §9.2 Starter Pack ($0.99).
 */
public final class StarterPackService {

    public static final long PRICE_CENTS = 99;
    public static final long PREMIUM_CHIPS = 300;
    public static final long RECRUIT_TOKENS = 10;

    private final ResourceService resource;
    private final AnalyticsService analytics;

    public StarterPackService(ResourceService resource, AnalyticsService analytics) {
        this.resource = resource;
        this.analytics = analytics;
    }

    /** 关卡触发：玩家首次通关 Zone 1-3 时调用。 */
    public synchronized boolean maybeTrigger(PlayerContext ctx, int clearedChapter, int clearedStage) {
        StarterPackState st = ctx.starterPackState();
        if (st.purchased()) return false;
        if (st.eligible()) return false;
        if (clearedChapter == 1 && clearedStage >= 3) {
            st.setEligible(true);
            st.setFirstEligibleMs(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public synchronized void purchase(PlayerContext ctx, String orderId) {
        StarterPackState st = ctx.starterPackState();
        if (st.purchased()) throw new GameException(ErrorCode.STARTER_PACK_USED);
        st.setPurchased(true);
        resource.add(ctx, CurrencyType.PREMIUM_CHIPS, PREMIUM_CHIPS, SourceTag.STARTER_PACK);
        resource.add(ctx, CurrencyType.RECRUIT_TOKENS, RECRUIT_TOKENS, SourceTag.STARTER_PACK);
        ctx.addSpentCents(PRICE_CENTS);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("startpack_purchase")
                    .prop("player_id", ctx.playerId())
                    .prop("price_cents", PRICE_CENTS)
                    .prop("order_id", orderId)
                    .build());
        }
    }
}
