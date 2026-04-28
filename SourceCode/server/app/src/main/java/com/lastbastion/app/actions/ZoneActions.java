package com.lastbastion.app.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.ActionHandler;
import com.lastbastion.app.net.Session;
import com.lastbastion.game.zone.ZoneService;

import java.util.Map;

public final class ZoneActions {

    private ZoneActions() {}

    public static ActionHandler clear(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "zone.clear"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                int chapter = payload.path("chapter").asInt(1);
                int stage = payload.path("stage").asInt(1);
                boolean won = payload.path("allyWon").asBoolean(true);
                ZoneService.AttemptResult r = svc.zone.clear(session.player(), chapter, stage, won);
                return Map.of(
                        "won", r.won,
                        "chapter", r.chapterId,
                        "stage", r.stageId,
                        "rewards", r.rewards
                );
            }
        };
    }

    public static ActionHandler settleIdle(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "zone.settleIdle"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                long now = System.currentTimeMillis();
                ZoneService.IdleReward r = svc.zone.settleIdle(session.player(), now);
                return Map.of(
                        "elapsedMs", r.elapsedMs,
                        "effectiveMs", r.effectiveMs,
                        "currency", r.currency
                );
            }
        };
    }
}
