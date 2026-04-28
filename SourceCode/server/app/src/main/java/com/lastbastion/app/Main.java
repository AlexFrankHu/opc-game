package com.lastbastion.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last Bastion 服务端入口。
 *
 * 真实部署参考 ioGame 官方模板：
 *  1) {@code new NettyRunOne().startup(port, logic)}
 *  2) 将 action classes（继承 com.iohao.game.action.skeleton.core.ActionController）
 *     注册到 BrokerClient。
 *
 * 为方便本仓库单元测试与 CI，这里仅演示 bootstrap 装配并立即退出。
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
        int port = Integer.parseInt(System.getProperty("port", "10100"));
        IoGameRuntime rt = new IoGameRuntime(svc);
        rt.start(port);
        log.info("Server listening on ws://0.0.0.0:{}/", port);
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {}
    }
}
