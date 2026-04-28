package com.lastbastion.game.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于文件快照的持久化：将 {@link PlayerContext} 用 Java 序列化存到
 * {@code <root>/<escapedExternalId>.ser}。
 *
 * <p>特点：</p>
 * <ul>
 *   <li>零外部依赖，单节点部署够用；</li>
 *   <li>通过进程内 ConcurrentMap 做一级缓存，读取走内存；</li>
 *   <li>写入是 write-through：先写 tmp 再原子重命名，避免半写文件；</li>
 *   <li>适合开发 / 测试服；正式集群推荐切到 Redis + MySQL 实现同一个 {@link PlayerStore} 接口。</li>
 * </ul>
 */
public final class FilePlayerStore implements PlayerStore {

    private static final Logger log = LoggerFactory.getLogger(FilePlayerStore.class);

    private final Path root;
    private final ConcurrentMap<String, PlayerContext> cache = new ConcurrentHashMap<>();

    public FilePlayerStore(Path root) {
        this.root = root;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create player store dir: " + root, e);
        }
    }

    @Override
    public Optional<PlayerContext> load(String externalId) {
        PlayerContext cached = cache.get(externalId);
        if (cached != null) return Optional.of(cached);

        Path file = fileOf(externalId);
        if (!Files.exists(file)) return Optional.empty();

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object read = in.readObject();
            if (read instanceof PlayerContext ctx) {
                cache.put(externalId, ctx);
                return Optional.of(ctx);
            }
            log.warn("Snapshot for {} has unexpected type {}", externalId, read.getClass());
            return Optional.empty();
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to load player snapshot for {} from {}", externalId, file, e);
            return Optional.empty();
        }
    }

    @Override
    public void save(String externalId, PlayerContext ctx) {
        cache.put(externalId, ctx);
        Path file = fileOf(externalId);
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            out.writeObject(ctx);
            out.flush();
        } catch (IOException e) {
            log.error("Failed to write snapshot for {} to {}", externalId, tmp, e);
            return;
        }
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to rename {} -> {}", tmp, file, e);
        }
    }

    @Override
    public int size() { return cache.size(); }

    public Path root() { return root; }

    private Path fileOf(String externalId) {
        String safe = externalId.replaceAll("[^A-Za-z0-9._-]", "_");
        return root.resolve(safe + ".ser");
    }
}
