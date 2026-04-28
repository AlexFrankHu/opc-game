package com.lastbastion.app.net;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Java-WebSocket 的简易运行时，承担"ioGame 对外网关"角色。
 *
 * <p>生产环境会替换为 ioGame 的 {@code NettyRunOne}（分布式 + 二进制协议 + 反压），
 * 本实现足以在开发/测试中跑通前后端对接 + 写集成用例。</p>
 */
public final class GameWebSocketServer extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketServer.class);

    private final ActionDispatcher dispatcher;
    private final Map<WebSocket, Session> sessions = new ConcurrentHashMap<>();

    public GameWebSocketServer(int port, ActionDispatcher dispatcher) {
        super(new InetSocketAddress(port));
        this.dispatcher = dispatcher;
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Session s = new Session();
        sessions.put(conn, s);
        log.info("[WS] open session={} from {}", s.sessionId(), conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Session s = sessions.remove(conn);
        log.info("[WS] close session={} reason={}", s == null ? -1 : s.sessionId(), reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Session s = sessions.get(conn);
        if (s == null) { conn.close(); return; }
        String resp = dispatcher.dispatchRaw(s, message);
        if (resp != null) conn.send(resp);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("[WS] error", ex);
    }

    @Override
    public void onStart() {
        log.info("[WS] started on port {} with {} handlers", getAddress().getPort(), dispatcher.size());
        setConnectionLostTimeout(60);
    }
}
