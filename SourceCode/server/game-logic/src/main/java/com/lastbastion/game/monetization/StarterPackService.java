package com.lastbastion.game.monetization;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.numeric.StarterPackTuning;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

/**
 * TASK-009 §9.2 Starter Pack ($0.99).
 */
public final class StarterPackService {

    private final ResourceService resource;
    private final AnalyticsService analytics;
    private final StarterPackTuning tuning;

    public StarterPackService(ResourceService resource, AnalyticsService analytics) {
        this(resource, analytics, NumericConfig.defaults().starterPack());
    }

    public StarterPackService(ResourceService resource, AnalyticsService analytics, StarterPackTuning tuning) {
        this.resource = resource;
        this.analytics = analytics;
        this.tuning = tuning;
    }

    public long priceCents() { return tuning.priceCents; }

    /** 关卡触发：玩家首次通关 Zone 1-3 时调用。 */
    public synchronized boolean maybeTrigger(PlayerContext ctx, int clearedChapter, int clearedStage) {
        StarterPackState st = ctx.starterPackState();
        if (st.purchased()) return false;
        if (st.eligible()) return false;
        if (clearedChapter == tuning.triggerChapter && clearedStage >= tuning.triggerStage) {
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
        resource.add(ctx, CurrencyType.PREMIUM_CHIPS, tuning.rewardPremiumChips, SourceTag.STARTER_PACK);
        resource.add(ctx, CurrencyType.RECRUIT_TOKENS, tuning.rewardRecruitTokens, SourceTag.STARTER_PACK);
        ctx.addSpentCents(tuning.priceCents);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("startpack_purchase")
                    .prop("player_id", ctx.playerId())
                    .prop("price_cents", tuning.priceCents)
                    .prop("order_id", orderId)
                    .build());
        }
    }
}
