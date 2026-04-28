package com.lastbastion.app;

import com.lastbastion.app.iogame.IoGameNettyRuntime;
import com.lastbastion.app.net.SessionRegistry;
import com.lastbastion.game.player.FilePlayerStore;
import com.lastbastion.game.player.InMemoryPlayerStore;
import com.lastbastion.game.player.JdbcPlayerStore;
import com.lastbastion.game.player.PlayerStore;
import com.lastbastion.game.player.RedisPlayerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Last Bastion 服务端入口。
 *
 * 同时启动两个网关，二者共享同一套业务 Service：
 * <ul>
 *   <li>{@link IoGameRuntime} —— JSON-over-WebSocket 开发网关（默认 :10100），
 *       给 Cocos TS 客户端调试用，明文协议，13 个 ActionHandler。</li>
 *   <li>{@link IoGameNettyRuntime} —— ioGame 原生 {@code NettyRunOne}
 *       （broker :10210 + external WS :10110），生产流量走向。</li>
 * </ul>
 * 两者可独立关闭，互不影响。
 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Last Bastion server booting ...");
        GameBootstrap boot = new GameBootstrap();
        GameBootstrap.Services svc = boot.boot();
        log.info("Boot complete: survivors={}, zones={}",
                svc.survivorRepo.size(),
                svc.zoneRepo.all().size());

        PlayerStore store = buildPlayerStore();
        SessionRegistry sessions = new SessionRegistry(store);
        log.info("PlayerStore: {} (size={})", store.getClass().getSimpleName(), store.size());

        int devPort = Integer.parseInt(System.getProperty("devPort", "10100"));
        IoGameRuntime dev = new IoGameRuntime(svc, sessions);
        dev.start(devPort);
        log.info("Dev JSON gateway listening on ws://0.0.0.0:{}/", devPort);

        if (Boolean.parseBoolean(System.getProperty("iogame.enable", "true"))) {
            int extPort = Integer.parseInt(System.getProperty("iogame.externalPort",
                    String.valueOf(IoGameNettyRuntime.DEFAULT_EXTERNAL_PORT)));
            int brokerPort = Integer.parseInt(System.getProperty("iogame.brokerPort",
                    String.valueOf(IoGameNettyRuntime.DEFAULT_BROKER_PORT)));
            IoGameNettyRuntime netty = new IoGameNettyRuntime(svc, sessions);
            netty.start(extPort, brokerPort);
        } else {
            log.info("ioGame NettyRunOne disabled via -Diogame.enable=false");
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {}
    }

    private static PlayerStore buildPlayerStore() {
        String kind = System.getProperty("store.kind", "file").toLowerCase();
        return switch (kind) {
            case "memory", "mem", "in-memory" -> new InMemoryPlayerStore();
            case "file" -> {
                Path root = Paths.get(System.getProperty("store.root", "./data/players"));
                yield new FilePlayerStore(root);
            }
            case "redis" -> {
                String uri = prop("store.redis.uri", "REDIS_URI", "redis://127.0.0.1:6379/0");
                yield new RedisPlayerStore(uri);
            }
            case "mysql", "jdbc" -> {
                String url = prop("store.jdbc.url", "MYSQL_URL",
                        "jdbc:mysql://127.0.0.1:3306/lastbastion?useSSL=false&serverTimezone=UTC");
                String user = prop("store.jdbc.user", "MYSQL_USER", "root");
                String pwd = prop("store.jdbc.password", "MYSQL_PASSWORD", "");
                yield new JdbcPlayerStore(url, user, pwd);
            }
            default -> throw new IllegalArgumentException("Unknown store.kind=" + kind);
        };
    }

    private static String prop(String sysKey, String envKey, String def) {
        String v = System.getProperty(sysKey);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;
        return def;
    }
}
