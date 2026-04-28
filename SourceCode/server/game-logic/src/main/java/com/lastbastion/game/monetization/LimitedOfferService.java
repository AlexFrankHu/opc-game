package com.lastbastion.game.monetization;

import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TASK-009 §9.3 限时礼包服务。
 */
public final class LimitedOfferService {

    private final Map<String, LimitedOffer> offers = new LinkedHashMap<>();
    /** playerId -> offerId -> times purchased */
    private final Map<Long, Map<String, Integer>> purchaseCounts = new HashMap<>();
    private final ResourceService resource;
    private final AnalyticsService analytics;

    public LimitedOfferService(ResourceService resource, AnalyticsService analytics) {
        this.resource = resource;
        this.analytics = analytics;
    }

    public void register(LimitedOffer offer) {
        offers.put(offer.id, offer);
    }

    public List<LimitedOffer> listActive(long nowMs) {
        List<LimitedOffer> out = new ArrayList<>();
        for (LimitedOffer o : offers.values()) if (o.active(nowMs)) out.add(o);
        return out;
    }

    public synchronized void purchase(PlayerContext ctx, String offerId, String orderId, long nowMs) {
        LimitedOffer o = offers.get(offerId);
        if (o == null) throw new GameException(ErrorCode.NOT_FOUND);
        if (!o.active(nowMs)) throw new GameException(ErrorCode.OFFER_EXPIRED);

        Map<String, Integer> counts = purchaseCounts.computeIfAbsent(ctx.playerId(), k -> new HashMap<>());
        int had = counts.getOrDefault(offerId, 0);
        if (o.purchaseLimit > 0 && had >= o.purchaseLimit) {
            throw new GameException(ErrorCode.OFFER_EXPIRED, "purchase limit");
        }

        for (LimitedOffer.Reward r : o.rewards) {
            if (r.currency != null) {
                resource.add(ctx, r.currency, r.amount, SourceTag.LIMITED_OFFER);
            }
        }
        counts.put(offerId, had + 1);
        ctx.addSpentCents(o.priceCents);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("iap_success")
                    .prop("player_id", ctx.playerId())
                    .prop("product_id", offerId)
                    .prop("price_cents", o.priceCents)
                    .prop("order_id", orderId)
                    .build());
        }
    }

    public int purchaseCount(long playerId, String offerId) {
        return purchaseCounts.getOrDefault(playerId, new HashMap<>()).getOrDefault(offerId, 0);
    }
}
