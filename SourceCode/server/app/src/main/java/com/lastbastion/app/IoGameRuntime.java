package com.lastbastion.app;

import com.lastbastion.app.actions.ArenaActions;
import com.lastbastion.app.auth.AuthService;
import com.lastbastion.app.actions.BattlePassActions;
import com.lastbastion.app.actions.OnboardingActions;
import com.lastbastion.app.actions.SurvivorActions;
import com.lastbastion.app.actions.UserActions;
import com.lastbastion.app.actions.ZoneActions;
import com.lastbastion.app.net.ActionDispatcher;
import com.lastbastion.app.net.GameWebSocketServer;
import com.lastbastion.app.net.SessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对外网关。默认实现使用 Java-WebSocket + JSON 帧；生产环境可替换为
 * ioGame 的 {@code NettyRunOne}（见 <a href="https://iohao.github.io/game">ioGame 文档</a>）。
 */
public final class IoGameRuntime {

    private static final Logger log = LoggerFactory.getLogger(IoGameRuntime.class);

    private final GameBootstrap.Services services;
    private final SessionRegistry sessionRegistry;
    private final AuthService auth;
    private final ActionDispatcher dispatcher = new ActionDispatcher();
    private GameWebSocketServer wsServer;

    public IoGameRuntime(GameBootstrap.Services services) {
        this(services, new SessionRegistry(), AuthService.fromEnv());
    }

    public IoGameRuntime(GameBootstrap.Services services, SessionRegistry sessionRegistry) {
        this(services, sessionRegistry, AuthService.fromEnv());
    }

    public IoGameRuntime(GameBootstrap.Services services, SessionRegistry sessionRegistry, AuthService auth) {
        this.services = services;
        this.sessionRegistry = sessionRegistry;
        this.auth = auth;
        this.dispatcher.setSessionRegistry(sessionRegistry);
        registerActions();
    }

    public ActionDispatcher dispatcher() { return dispatcher; }
    public SessionRegistry sessions() { return sessionRegistry; }

    private void registerActions() {
        dispatcher.register(UserActions.login(sessionRegistry, services, auth));
        dispatcher.register(UserActions.heartbeat());
        dispatcher.register(SurvivorActions.pullGacha(services));
        dispatcher.register(SurvivorActions.levelUp(services));
        dispatcher.register(ZoneActions.clear(services));
        dispatcher.register(ZoneActions.settleIdle(services));
        dispatcher.register(ArenaActions.match(services));
        dispatcher.register(ArenaActions.challenge(services));
        dispatcher.register(ArenaActions.leaderboard(services));
        dispatcher.register(BattlePassActions.claim(services));
        dispatcher.register(BattlePassActions.buy(services));
        dispatcher.register(OnboardingActions.completeStep(services));
        dispatcher.register(OnboardingActions.skip(services));
    }

    public void start(int port) {
        wsServer = new GameWebSocketServer(port, dispatcher);
        wsServer.start();
        log.info("IoGameRuntime started on :{} with {} actions", port, dispatcher.size());
    }

    public void stop() throws InterruptedException {
        if (wsServer != null) wsServer.stop(1000);
    }

    public int port() {
        return wsServer == null ? -1 : wsServer.getAddress().getPort();
    }
}
