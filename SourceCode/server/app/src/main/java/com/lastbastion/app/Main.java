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
        // If you want to run ioGame server, uncomment the runtime stub below:
        // new IoGameRuntime(svc).start(Integer.parseInt(System.getProperty("port", "10100")));
    }
}
