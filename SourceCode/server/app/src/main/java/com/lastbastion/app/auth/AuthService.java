package com.lastbastion.app.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 登录鉴权 — 测试服 / 生产可用。
 *
 * <p><b>策略：</b></p>
 * <ul>
 *   <li>启动时若 {@code LOGIN_SHARED_SECRET}（或系统属性 {@code auth.secret}）有值，
 *       要求每次 {@code user.login} 携带 {@code deviceId / ts / sig}：</li>
 *   <li><pre>sig = lower(hex(HMAC_SHA256(secret, userId + "|" + deviceId + "|" + ts)))</pre></li>
 *   <li>{@code |now - ts| > maxSkewMs}（默认 5 分钟）拒绝，防 replay。</li>
 *   <li>未配置 secret 时退化为「开放模式」（仅用于本地开发 / web-demo 烟测），
 *       服务启动 banner 会打 WARN 提醒。</li>
 * </ul>
 *
 * <p>设计上故意保持简单 — 真正的账号系统（手机号 + SMS / OAuth / Apple ID）
 * 由后续阶段独立接入，这里只是给测试服一个最低限度的反垃圾流量门槛。</p>
 */
public final class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long DEFAULT_MAX_SKEW_MS = 5 * 60 * 1000L;
    private static final HexFormat HEX = HexFormat.of();

    private final byte[] secret;
    private final long maxSkewMs;

    /** 工厂方法，按系统属性 / 环境变量构造。 */
    public static AuthService fromEnv() {
        String secret = System.getProperty("auth.secret",
                System.getenv().getOrDefault("LOGIN_SHARED_SECRET", ""));
        long skew;
        try {
            skew = Long.parseLong(System.getProperty("auth.maxSkewMs",
                    String.valueOf(DEFAULT_MAX_SKEW_MS)));
        } catch (NumberFormatException e) {
            skew = DEFAULT_MAX_SKEW_MS;
        }
        if (secret == null || secret.isBlank()) {
            log.warn("AuthService: LOGIN_SHARED_SECRET not set — running in OPEN mode "
                    + "(any userId can log in). Do not deploy this to production!");
            return new AuthService(null, skew);
        }
        log.info("AuthService: HMAC mode enabled (skew={} ms)", skew);
        return new AuthService(secret.getBytes(StandardCharsets.UTF_8), skew);
    }

    public AuthService(byte[] secret, long maxSkewMs) {
        this.secret = secret;
        this.maxSkewMs = maxSkewMs;
    }

    public boolean isEnforced() { return secret != null; }

    /**
     * 校验登录请求。
     *
     * @return {@code Result.OK} 表示通过；其余为拒绝原因。
     */
    public Result verify(String userId, String deviceId, long ts, String sigHex) {
        if (secret == null) return Result.OK; // 开放模式
        if (userId == null || userId.isBlank()) return Result.MISSING_FIELDS;
        if (deviceId == null || deviceId.isBlank()) return Result.MISSING_FIELDS;
        if (sigHex == null || sigHex.isBlank()) return Result.MISSING_FIELDS;
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > maxSkewMs) return Result.STALE;
        String expected = sign(userId, deviceId, ts);
        return constantTimeEquals(expected, sigHex.toLowerCase(Locale.ROOT))
                ? Result.OK : Result.BAD_SIGNATURE;
    }

    /** 客户端用的同款签名工具，便于在工具脚本里复用。 */
    public String sign(String userId, String deviceId, long ts) {
        if (secret == null) {
            throw new IllegalStateException("AuthService is not in HMAC mode");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String payload = userId + "|" + deviceId + "|" + ts;
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 unavailable", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    public enum Result {
        OK,
        MISSING_FIELDS,
        STALE,
        BAD_SIGNATURE
    }
}
