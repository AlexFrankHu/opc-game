package com.lastbastion.game.player;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 测试 / 默认实现：JVM 内存缓存。进程重启后数据丢失。
 */
public final class InMemoryPlayerStore implements PlayerStore {

    private final ConcurrentMap<String, PlayerContext> byExternalId = new ConcurrentHashMap<>();

    @Override
    public Optional<PlayerContext> load(String externalId) {
        return Optional.ofNullable(byExternalId.get(externalId));
    }

    @Override
    public void save(String externalId, PlayerContext ctx) {
        byExternalId.put(externalId, ctx);
    }

    @Override
    public int size() { return byExternalId.size(); }
}
