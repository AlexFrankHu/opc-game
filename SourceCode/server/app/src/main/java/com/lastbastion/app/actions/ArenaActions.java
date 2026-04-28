package com.lastbastion.app.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.ActionHandler;
import com.lastbastion.app.net.Session;

import java.util.Map;

public final class ArenaActions {

    private ArenaActions() {}

    public static ActionHandler match(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "arena.match"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                return Map.of("candidates", svc.arena.match(session.player()));
            }
        };
    }

    public static ActionHandler challenge(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "arena.challenge"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                long opponentId = payload.path("opponentId").asLong();
                boolean won = payload.path("won").asBoolean(true);
                return svc.arena.challenge(session.player(), opponentId, won);
            }
        };
    }

    public static ActionHandler leaderboard(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "arena.leaderboard"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                int n = Math.min(100, Math.max(1, payload.path("n").asInt(10)));
                return Map.of("top", svc.arena.topRanks(n));
            }
        };
    }
}
