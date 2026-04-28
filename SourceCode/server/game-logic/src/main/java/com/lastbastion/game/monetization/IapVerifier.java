package com.lastbastion.game.monetization;

/**
 * TASK-009 §9.4 IAP 服务端校验抽象。
 *
 * 生产环境实现：调用 Apple verifyReceipt / Google Play Developer API。
 * MVP 骨架：记录订单 + 幂等。
 */
public interface IapVerifier {

    /** @return VerificationResult 包含订单是否可兑现。 */
    VerificationResult verify(VerificationRequest req);

    enum Platform { APPLE, GOOGLE, TEST }

    final class VerificationRequest {
        public long playerId;
        public Platform platform;
        public String productId;
        public String receipt;
        public String orderId;
        public long priceCents;
    }

    final class VerificationResult {
        public boolean valid;
        public String reason;
        public String canonicalOrderId;

        public static VerificationResult valid(String orderId) {
            VerificationResult r = new VerificationResult();
            r.valid = true;
            r.canonicalOrderId = orderId;
            return r;
        }

        public static VerificationResult invalid(String reason) {
            VerificationResult r = new VerificationResult();
            r.valid = false;
            r.reason = reason;
            return r;
        }
    }
}
