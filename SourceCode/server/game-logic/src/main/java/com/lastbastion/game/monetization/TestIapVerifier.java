package com.lastbastion.game.monetization;

/**
 * 单元测试/沙盒环境使用的假校验器 —— 默认一切通过。
 */
public final class TestIapVerifier implements IapVerifier {

    @Override
    public VerificationResult verify(VerificationRequest req) {
        if (req.orderId == null || req.orderId.isEmpty()) {
            return VerificationResult.invalid("empty orderId");
        }
        return VerificationResult.valid(req.orderId);
    }
}
