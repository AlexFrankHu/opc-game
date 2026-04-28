package com.lastbastion.app.net;

import com.lastbastion.game.player.InMemoryPlayerStore;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.player.PlayerStore;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家存档 + 会话索引。
 *
 * <p>存档由注入的 {@link PlayerStore} 持久化，默认是 {@link InMemoryPlayerStore}（进程重启即清空）。
 * 生产建议挂 {@code FilePlayerStore}（单机）或 Redis/MySQL 实现。</p>
 */
public final class SessionRegistry {

    private final PlayerStore store;
    private final AtomicLong nextPlayerId = new AtomicLong(1);

    public SessionRegistry() {
        this(new InMemoryPlayerStore());
    }

    public SessionRegistry(PlayerStore store) {
        this.store = store;
    }

    /**
     * 首次登录创建存档，再次登录返回已有存档（可能来自磁盘）。
     */
    public PlayerContext loginOrCreate(String externalId) {
        return store.load(externalId).orElseGet(() -> {
            PlayerContext fresh = new PlayerContext(nextPlayerId.getAndIncrement(), externalId);
            store.save(externalId, fresh);
            return fresh;
        });
    }

    public void save(String externalId, PlayerContext ctx) {
        store.save(externalId, ctx);
    }

    public int playerCount() { return store.size(); }

    public PlayerStore store() { return store; }
}
