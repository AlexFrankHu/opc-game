package com.lastbastion.app.auth;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private static final byte[] SECRET = "test-secret".getBytes(StandardCharsets.UTF_8);

    @Test
    void openModeAcceptsAnyInput() {
        AuthService open = new AuthService(null, 60_000);
        assertFalse(open.isEnforced());
        assertEquals(AuthService.Result.OK, open.verify("u1", null, 0L, null));
        assertEquals(AuthService.Result.OK, open.verify("anything", "device", 1L, "ignored"));
    }

    @Test
    void hmacVerifiesValidSignature() {
        AuthService auth = new AuthService(SECRET, 60_000);
        long ts = System.currentTimeMillis();
        String sig = auth.sign("user-1", "device-A", ts);
        assertEquals(AuthService.Result.OK, auth.verify("user-1", "device-A", ts, sig));
    }

    @Test
    void hmacRejectsTamperedSignature() {
        AuthService auth = new AuthService(SECRET, 60_000);
        long ts = System.currentTimeMillis();
        String sig = auth.sign("user-1", "device-A", ts);
        // 修改 userId 后 sig 必然不匹配
        assertEquals(AuthService.Result.BAD_SIGNATURE,
                auth.verify("user-2", "device-A", ts, sig));
        // 单 hex 字符篡改
        char[] chars = sig.toCharArray();
        chars[0] = chars[0] == 'a' ? 'b' : 'a';
        assertEquals(AuthService.Result.BAD_SIGNATURE,
                auth.verify("user-1", "device-A", ts, new String(chars)));
    }

    @Test
    void hmacRejectsStaleTimestamp() {
        AuthService auth = new AuthService(SECRET, 1_000); // 1s skew
        long old = System.currentTimeMillis() - 60_000;
        String sig = auth.sign("user-1", "device-A", old);
        assertEquals(AuthService.Result.STALE,
                auth.verify("user-1", "device-A", old, sig));
    }

    @Test
    void hmacRejectsMissingFields() {
        AuthService auth = new AuthService(SECRET, 60_000);
        assertEquals(AuthService.Result.MISSING_FIELDS,
                auth.verify(null, "d", System.currentTimeMillis(), "sig"));
        assertEquals(AuthService.Result.MISSING_FIELDS,
                auth.verify("u", "", System.currentTimeMillis(), "sig"));
        assertEquals(AuthService.Result.MISSING_FIELDS,
                auth.verify("u", "d", System.currentTimeMillis(), null));
    }

    @Test
    void signRequiresSecret() {
        AuthService open = new AuthService(null, 60_000);
        assertThrows(IllegalStateException.class, () -> open.sign("u", "d", 1L));
    }
}
