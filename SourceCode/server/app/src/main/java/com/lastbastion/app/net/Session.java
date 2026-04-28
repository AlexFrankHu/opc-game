package com.lastbastion.app.net;

import com.lastbastion.game.player.PlayerContext;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家会话：对应一条活跃的 WebSocket 连接 + 一个 {@link PlayerContext}。
 */
public final class Session {

    private static final AtomicLong SEQ = new AtomicLong(1000);

    private final long sessionId;
    private volatile PlayerContext player;

    public Session() {
        this.sessionId = SEQ.incrementAndGet();
    }

    public long sessionId() { return sessionId; }

    public PlayerContext player() { return player; }

    public void bindPlayer(PlayerContext player) { this.player = player; }

    public boolean isLoggedIn() { return player != null; }
}
