package com.lastbastion.app.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.auth.AuthService;
import com.lastbastion.app.net.ActionHandler;
import com.lastbastion.app.net.Session;
import com.lastbastion.app.net.SessionRegistry;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.game.player.PlayerContext;

import java.util.LinkedHashMap;
import java.util.Map;

/** 登录/心跳。 */
public final class UserActions {

    private UserActions() {}

    public static ActionHandler login(SessionRegistry reg, GameBootstrap.Services svc, AuthService auth) {
        return new ActionHandler() {
            @Override public String name() { return "user.login"; }
            @Override public boolean requiresLogin() { return false; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                String extId = payload.path("userId").asText("guest-" + session.sessionId());
                String deviceId = payload.path("deviceId").asText("");
                long ts = payload.path("ts").asLong(0L);
                String sig = payload.path("sig").asText("");
                AuthService.Result authResult = auth.verify(extId, deviceId, ts, sig);
                if (authResult != AuthService.Result.OK) {
                    throw new GameException(ErrorCode.UNAUTHENTICATED, authResult.name());
                }
                PlayerContext ctx = reg.loginOrCreate(extId);
                session.bindPlayer(ctx);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("playerId", ctx.playerId());
                r.put("externalId", extId);
                r.put("firstLogin", ctx.registerTimestamp());
                r.put("currencies", ctx.currencies());
                r.put("authStatus", auth.isEnforced() ? "OK" : "OPEN_MODE");
                return r;
            }
        };
    }

    public static ActionHandler heartbeat() {
        return new ActionHandler() {
            @Override public String name() { return "user.heartbeat"; }
            @Override public boolean requiresLogin() { return false; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                return Map.of("t", System.currentTimeMillis());
            }
        };
    }
}
