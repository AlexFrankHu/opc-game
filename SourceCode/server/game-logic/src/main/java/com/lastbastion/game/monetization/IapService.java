package com.lastbastion.game.monetization;

import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;

import java.util.HashSet;
import java.util.Set;

/**
 * IAP 订单主流程：接收回调 → 校验 → 幂等 → 发货。
 */
public final class IapService {

    private final IapVerifier verifier;
    private final AnalyticsService analytics;
    private final Set<String> fulfilledOrderIds = new HashSet<>();

    public IapService(IapVerifier verifier, AnalyticsService analytics) {
        this.verifier = verifier;
        this.analytics = analytics;
    }

    /** @return 是否首次发货（幂等保护）。 */
    public synchronized boolean processReceipt(PlayerContext ctx, IapVerifier.VerificationRequest req,
                                               Runnable fulfillment) {
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("iap_attempt")
                    .prop("player_id", ctx.playerId())
                    .prop("product_id", req.productId)
                    .build());
        }
        IapVerifier.VerificationResult v = verifier.verify(req);
        if (!v.valid) {
            if (analytics != null) {
                analytics.emit(AnalyticsEvent.of("iap_fail")
                        .prop("player_id", ctx.playerId())
                        .prop("product_id", req.productId)
                        .prop("reason", v.reason).build());
            }
            throw new GameException(ErrorCode.ILLEGAL_ARG, "invalid receipt: " + v.reason);
        }
        if (fulfilledOrderIds.contains(v.canonicalOrderId)) {
            return false;
        }
        fulfilledOrderIds.add(v.canonicalOrderId);
        fulfillment.run();
        return true;
    }
}
