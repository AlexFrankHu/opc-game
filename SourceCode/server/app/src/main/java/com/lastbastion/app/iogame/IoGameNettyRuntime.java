package com.lastbastion.app.iogame;

import com.iohao.game.bolt.broker.server.BrokerServer;
import com.iohao.game.external.core.ExternalServer;
import com.iohao.game.external.core.config.ExternalJoinEnum;
import com.iohao.game.external.core.netty.kit.ExternalServerCreateKit;
import com.iohao.game.external.core.netty.simple.NettyRunOne;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 真正的 ioGame 多服单进程运行时（broker + external + logic）。
 *
 * <p>此 Runtime 与 {@link com.lastbastion.app.IoGameRuntime}（JSON dev gateway）并存：</p>
 * <ul>
 *   <li>{@link IoGameNettyRuntime} 用 ioGame 原生 bolt + Netty，是生产流量落地方案；</li>
 *   <li>{@code IoGameRuntime} 用 Java-WebSocket + JSON 帧，方便 Cocos TS SDK 调试。</li>
 * </ul>
 * 两者共享同一套业务 Service（通过 {@link ServiceRegistry}），状态一致。
 */
public final class IoGameNettyRuntime {

    private static final Logger log = LoggerFactory.getLogger(IoGameNettyRuntime.class);

    public static final int DEFAULT_EXTERNAL_PORT = 10110;
    public static final int DEFAULT_BROKER_PORT = 10210;

    private final GameBootstrap.Services services;
    private final SessionRegistry sessionRegistry;
    private final GameLogicStartup logicStartup = new GameLogicStartup();

    private BrokerServer brokerServer;
    private ExternalServer externalServer;
    private NettyRunOne runOne;

    public IoGameNettyRuntime(GameBootstrap.Services services, SessionRegistry sessionRegistry) {
        this.services = services;
        this.sessionRegistry = sessionRegistry;
    }

    public void start() {
        start(DEFAULT_EXTERNAL_PORT, DEFAULT_BROKER_PORT);
    }

    public void start(int externalPort, int brokerPort) {
        // 如果 Main 已经显式注入了 AuthService，这里不要覆盖；否则按 env 创建。
        try {
            ServiceRegistry.auth();
        } catch (IllegalStateException notInited) {
            ServiceRegistry.init(services, sessionRegistry);
        }

        externalServer = ExternalServerCreateKit.createExternalServer(
                externalPort, ExternalJoinEnum.WEBSOCKET);

        brokerServer = BrokerServer.newBuilder()
                .port(brokerPort)
                .build();

        runOne = new NettyRunOne()
                .setExternalServer(externalServer)
                .setBrokerServer(brokerServer)
                .setLogicServerList(List.of(logicStartup));

        runOne.startup();
        log.info("ioGame NettyRunOne up — external(WS)={}, broker={}", externalPort, brokerPort);
    }

    public GameLogicStartup logicStartup() { return logicStartup; }
    public BrokerServer brokerServer() { return brokerServer; }
    public ExternalServer externalServer() { return externalServer; }

    public void stop() {
        try { if (brokerServer != null) brokerServer.shutdown(); } catch (Exception e) { log.warn("broker stop", e); }
        // ExternalServer / LogicServer 会随 JVM 停止
    }
}
