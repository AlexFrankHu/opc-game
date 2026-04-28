package com.lastbastion.app.net;

import com.lastbastion.game.player.PlayerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家存档 + 会话索引。真实项目中 {@link PlayerContext} 来自持久层（MySQL/Redis），
 * 本 demo 采用内存存储，进程重启即清空。
 */
public final class SessionRegistry {

    private final Map<String, PlayerContext> playersByExtId = new ConcurrentHashMap<>();
    private final AtomicLong nextPlayerId = new AtomicLong(1);

    /**
     * 首次登录创建存档，再次登录返回已有存档。
     */
    public PlayerContext loginOrCreate(String externalId) {
        return playersByExtId.computeIfAbsent(externalId,
                id -> new PlayerContext(nextPlayerId.getAndIncrement(), id));
    }

    public int playerCount() {
        return playersByExtId.size();
    }
}
