package com.lastbastion.app.iogame;

import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.auth.AuthService;
import com.lastbastion.app.net.SessionRegistry;

/**
 * ioGame 的 Action 执行通过 BarSkeleton 的反射调用，方法签名由框架决定，
 * 因此无法直接注入 Service。我们用一个静态"全局容器"让 Action 类拿到需要的服务。
 *
 * <p>替代方案是自定义 {@code ActionFactoryBean}（相当于一个 Spring ApplicationContext），
 * 本项目为了降低阅读成本，采用最简的静态注册。</p>
 */
public final class ServiceRegistry {

    private static volatile GameBootstrap.Services SERVICES;
    private static volatile SessionRegistry SESSIONS;
    private static volatile AuthService AUTH;

    private ServiceRegistry() {}

    public static void init(GameBootstrap.Services services, SessionRegistry sessions) {
        init(services, sessions, AuthService.fromEnv());
    }

    public static void init(GameBootstrap.Services services, SessionRegistry sessions, AuthService auth) {
        SERVICES = services;
        SESSIONS = sessions;
        AUTH = auth;
    }

    public static GameBootstrap.Services services() {
        if (SERVICES == null) throw new IllegalStateException("ServiceRegistry not initialised");
        return SERVICES;
    }

    public static SessionRegistry sessions() {
        if (SESSIONS == null) throw new IllegalStateException("ServiceRegistry not initialised");
        return SESSIONS;
    }

    public static AuthService auth() {
        if (AUTH == null) throw new IllegalStateException("ServiceRegistry not initialised");
        return AUTH;
    }
}
