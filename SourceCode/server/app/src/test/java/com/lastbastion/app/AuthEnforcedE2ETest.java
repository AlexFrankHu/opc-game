package com.lastbastion.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lastbastion.app.auth.AuthService;
import com.lastbastion.app.net.SessionRegistry;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端验证：服务端启用 HMAC 鉴权后，少了 / 篡改 sig 的登录请求会被拒，
 * 提供合法签名的请求会被放行。
 */
class AuthEnforcedE2ETest {

    private static IoGameRuntime runtime;
    private static AuthService auth;
    private static int port;
    private static final ObjectMapper M = new ObjectMapper();

    @BeforeAll
    static void bootServer() throws Exception {
        port = freePort();
        auth = new AuthService("e2e-secret".getBytes(StandardCharsets.UTF_8), 60_000);
        runtime = new IoGameRuntime(
                new GameBootstrap().boot(),
                new SessionRegistry(),
                auth);
        runtime.start(port);
        Thread.sleep(300);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (runtime != null) runtime.stop();
    }

    @Test
    void rejectsLoginWithoutSignature() throws Exception {
        TestClient c = new TestClient(new URI("ws://127.0.0.1:" + port));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));
        JsonNode r = c.call("user.login", M.createObjectNode().put("userId", "u-1"));
        assertFalse(r.path("ok").asBoolean(), r.toString());
        assertEquals("UNAUTHENTICATED", r.path("code").asText());
        c.closeBlocking();
    }

    @Test
    void acceptsLoginWithValidSignature() throws Exception {
        TestClient c = new TestClient(new URI("ws://127.0.0.1:" + port));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));
        long ts = System.currentTimeMillis();
        String sig = auth.sign("u-2", "device-A", ts);
        ObjectNode payload = M.createObjectNode()
                .put("userId", "u-2")
                .put("deviceId", "device-A")
                .put("ts", ts)
                .put("sig", sig);
        JsonNode r = c.call("user.login", payload);
        assertTrue(r.path("ok").asBoolean(), r.toString());
        assertEquals("OK", r.path("data").path("authStatus").asText());
        c.closeBlocking();
    }

    @Test
    void rejectsTamperedSignature() throws Exception {
        TestClient c = new TestClient(new URI("ws://127.0.0.1:" + port));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));
        long ts = System.currentTimeMillis();
        String sig = auth.sign("u-3", "device-A", ts);
        // 把签名第一位改掉，必然不匹配
        char[] tampered = sig.toCharArray();
        tampered[0] = tampered[0] == 'a' ? 'b' : 'a';
        ObjectNode payload = M.createObjectNode()
                .put("userId", "u-3")
                .put("deviceId", "device-A")
                .put("ts", ts)
                .put("sig", new String(tampered));
        JsonNode r = c.call("user.login", payload);
        assertFalse(r.path("ok").asBoolean(), r.toString());
        assertEquals("UNAUTHENTICATED", r.path("code").asText());
        c.closeBlocking();
    }

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
    }

    private static final class TestClient extends WebSocketClient {
        private final AtomicInteger seq = new AtomicInteger();
        private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
        TestClient(URI uri) { super(uri); }
        JsonNode call(String action, ObjectNode payload) throws Exception {
            int id = seq.incrementAndGet();
            ObjectNode frame = M.createObjectNode();
            frame.put("id", id).put("action", action).set("payload", payload);
            CompletableFuture<JsonNode> f = new CompletableFuture<>();
            pending.put(id, f);
            send(M.writeValueAsString(frame));
            return f.get(5, TimeUnit.SECONDS);
        }
        @Override public void onOpen(ServerHandshake handshakedata) {}
        @Override public void onClose(int code, String reason, boolean remote) {}
        @Override public void onError(Exception ex) {}
        @Override public void onMessage(String message) {
            try {
                JsonNode n = M.readTree(message);
                CompletableFuture<JsonNode> f = pending.remove(n.path("id").asInt());
                if (f != null) f.complete(n);
            } catch (Exception ignore) {}
        }
    }
}
