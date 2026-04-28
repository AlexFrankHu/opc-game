package com.lastbastion.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ioGame 框架集成占位。
 *
 * 在真实项目中，这里会：
 *   1) 构造 {@code BrokerBootstrap} / {@code ExternalServer};
 *   2) 注册 {@code ActionController} 集合；
 *   3) 使用 {@code NettyRunOne} 单进程模式或分布式模式启动。
 *
 * 未将 ioGame 启动代码直接写进来，是因为版本/网络约束在 CI 中可能无法下载对应依赖；
 * 生产部署按 <a href="https://iohao.github.io/game">iogame 文档</a>补全即可。
 */
public final class IoGameRuntime {

    private static final Logger log = LoggerFactory.getLogger(IoGameRuntime.class);

    private final GameBootstrap.Services services;

    public IoGameRuntime(GameBootstrap.Services services) {
        this.services = services;
    }

    public void start(int port) {
        log.info("[ioGame] (stub) start on port {} with {} action modules",
                port, ActionRegistry.ALL.size());
        log.info("[ioGame] available commands: {}",
                ActionRegistry.ALL.keySet());
        // Production: new com.iohao.game.external.NettyRunOne().startup(port, this::registerActions);
    }
}
