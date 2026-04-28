package com.lastbastion.game.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 Redis 的持久化实现：以 {@code lb:player:<externalId>} 为 key，
 * 值为 {@link PlayerContext} 的 Java 序列化字节。
 *
 * <p>本地保留一级缓存（ConcurrentMap），避免同一玩家连续 action 时反复
 * 往 Redis 取数据；write-through，写入由 {@link #save} 同步覆盖，无异步延迟。</p>
 *
 * <p>生产推荐与 {@link JdbcPlayerStore} 搭配做「热存 Redis + 冷存 MySQL」，但本实现
 * 本身即可独立工作（Redis 持久化 = RDB/AOF 由运维配置）。</p>
 */
public final class RedisPlayerStore implements PlayerStore, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RedisPlayerStore.class);
    private static final String KEY_PREFIX = "lb:player:";

    private final JedisPool pool;
    private final ConcurrentMap<String, PlayerContext> cache = new ConcurrentHashMap<>();

    /**
     * @param redisUri 形如 {@code redis://host:6379/0} 或 {@code redis://:pwd@host:6379}
     */
    public RedisPlayerStore(String redisUri) {
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(32);
        cfg.setMaxIdle(8);
        cfg.setMinIdle(2);
        this.pool = new JedisPool(cfg, URI.create(redisUri));
        // 连通性探测，失败立刻抛出，避免启动后第一次 action 才暴露问题。
        try (Jedis j = pool.getResource()) {
            j.ping();
        }
        log.info("RedisPlayerStore connected to {}", redisUri);
    }

    RedisPlayerStore(JedisPool externalPool) {
        this.pool = externalPool;
    }

    @Override
    public Optional<PlayerContext> load(String externalId) {
        PlayerContext cached = cache.get(externalId);
        if (cached != null) return Optional.of(cached);
        byte[] bytes;
        try (Jedis j = pool.getResource()) {
            bytes = j.get(keyOf(externalId).getBytes());
        }
        if (bytes == null) return Optional.empty();
        Optional<PlayerContext> decoded = SerializationCodec.decode(bytes);
        decoded.ifPresent(ctx -> cache.put(externalId, ctx));
        return decoded;
    }

    @Override
    public void save(String externalId, PlayerContext ctx) {
        cache.put(externalId, ctx);
        byte[] bytes = SerializationCodec.encode(ctx);
        try (Jedis j = pool.getResource()) {
            j.set(keyOf(externalId).getBytes(), bytes);
        } catch (RuntimeException e) {
            log.error("Redis save failed for {}", externalId, e);
            throw e;
        }
    }

    @Override
    public int size() { return cache.size(); }

    @Override
    public void close() {
        pool.close();
    }

    private static String keyOf(String externalId) {
        return KEY_PREFIX + externalId;
    }
}
