package com.lastbastion.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 启动真实 WebSocket 服务端，用本地客户端连上后依次调用多个 Action，
 * 验证 JSON 帧格式 + 会话登录态 + 各服务端到端可用。
 */
class EndToEndWsTest {

    private static IoGameRuntime runtime;
    private static int port;
    private static final ObjectMapper M = new ObjectMapper();

    @BeforeAll
    static void bootServer() throws Exception {
        port = freePort();
        runtime = new IoGameRuntime(new GameBootstrap().boot());
        runtime.start(port);
        // 等待 server 监听就绪
        Thread.sleep(300);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (runtime != null) runtime.stop();
    }

    @Test
    void loginThenZoneClearAndGacha() throws Exception {
        TestClient c = new TestClient(new URI("ws://127.0.0.1:" + port));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));

        // 1. login
        JsonNode login = c.call("user.login", M.createObjectNode().put("userId", "e2e-u1"));
        assertTrue(login.path("ok").asBoolean(), login.toString());
        long playerId = login.path("data").path("playerId").asLong();
        assertTrue(playerId > 0);

        // 2. heartbeat (doesn't require login)
        JsonNode hb = c.call("user.heartbeat", M.createObjectNode());
        assertTrue(hb.path("ok").asBoolean());

        // 3. zone.clear 1-1 win => first clear, rewards granted
        JsonNode zone = c.call("zone.clear", M.createObjectNode()
                .put("chapter", 1).put("stage", 1).put("allyWon", true));
        assertTrue(zone.path("ok").asBoolean(), zone.toString());
        assertTrue(zone.path("data").path("won").asBoolean());
        assertEquals(1, zone.path("data").path("chapter").asInt());

        // 4. zone.clear 1-3 out-of-order => error
        JsonNode bad = c.call("zone.clear", M.createObjectNode()
                .put("chapter", 1).put("stage", 3).put("allyWon", true));
        assertFalse(bad.path("ok").asBoolean());
        assertEquals("ZONE_LOCKED", bad.path("code").asText());

        // 5. survivor.pullGacha: 1-1 first clear gave 1 RECRUIT_TOKEN so a single FREE pull works.
        JsonNode gachaOk = c.call("survivor.pullGacha", M.createObjectNode()
                .put("pool", "FREE").put("count", 1));
        assertTrue(gachaOk.path("ok").asBoolean(), gachaOk.toString());
        assertEquals(1, gachaOk.path("data").path("results").size());

        // 5b. PREMIUM pool: 10-pull (270 chips) exceeds the 30 granted by first-clear => fail.
        JsonNode premFail = c.call("survivor.pullGacha", M.createObjectNode()
                .put("pool", "PREMIUM").put("count", 10));
        assertFalse(premFail.path("ok").asBoolean());
        assertEquals("INSUFFICIENT_CURRENCY", premFail.path("code").asText());

        // 6. unknown action
        JsonNode unk = c.call("foo.bar", M.createObjectNode());
        assertFalse(unk.path("ok").asBoolean());
        assertEquals("UNKNOWN_ACTION", unk.path("code").asText());

        c.closeBlocking();
    }

    @Test
    void unauthenticatedActionRejected() throws Exception {
        TestClient c = new TestClient(new URI("ws://127.0.0.1:" + port));
        assertTrue(c.connectBlocking(5, TimeUnit.SECONDS));
        JsonNode r = c.call("zone.clear", M.createObjectNode().put("chapter", 1).put("stage", 1));
        assertFalse(r.path("ok").asBoolean());
        assertEquals("NOT_LOGGED_IN", r.path("code").asText());
        c.closeBlocking();
    }

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
    }

    private static final class TestClient extends WebSocketClient {
        private final AtomicInteger seq = new AtomicInteger();
        private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();

        TestClient(URI uri) { super(uri); }

        JsonNode call(String action, com.fasterxml.jackson.databind.node.ObjectNode payload) throws Exception {
            int id = seq.incrementAndGet();
            payload.put("__meta_id", id);
            com.fasterxml.jackson.databind.node.ObjectNode frame = M.createObjectNode();
            frame.put("id", id);
            frame.put("action", action);
            frame.set("payload", payload);
            CompletableFuture<JsonNode> f = new CompletableFuture<>();
            pending.put(id, f);
            send(M.writeValueAsString(frame));
            return f.get(5, TimeUnit.SECONDS);
        }

        @Override public void onOpen(ServerHandshake handshakedata) {}
        @Override public void onClose(int code, String reason, boolean remote) {}
        @Override public void onError(Exception ex) { ex.printStackTrace(); }
        @Override public void onMessage(String message) {
            try {
                JsonNode n = M.readTree(message);
                int id = n.path("id").asInt();
                CompletableFuture<JsonNode> f = pending.remove(id);
                if (f != null) f.complete(n);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
