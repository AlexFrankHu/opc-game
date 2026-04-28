package com.lastbastion.app;

import com.lastbastion.app.iogame.IoGameNettyRuntime;
import com.lastbastion.app.net.SessionRegistry;
import com.lastbastion.game.player.FilePlayerStore;
import com.lastbastion.game.player.InMemoryPlayerStore;
import com.lastbastion.game.player.PlayerStore;
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
            // "redis" / "mysql" 实现预留：只需新增 Jedis / JDBC 客户端即可替换。
            default -> throw new IllegalArgumentException("Unknown store.kind=" + kind);
        };
    }
}
