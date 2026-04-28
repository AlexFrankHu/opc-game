package com.lastbastion.app.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.ActionHandler;
import com.lastbastion.app.net.Session;
import com.lastbastion.game.survivor.GachaService;

import java.util.List;
import java.util.Map;

/** Survivor/招募相关 Action。 */
public final class SurvivorActions {

    private SurvivorActions() {}

    public static ActionHandler pullGacha(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "survivor.pullGacha"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                String poolStr = payload.path("pool").asText("FREE");
                int count = Math.max(1, payload.path("count").asInt(1));
                GachaService.Pool pool = GachaService.Pool.valueOf(poolStr);
                List<GachaService.Result> results = svc.gacha.pull(session.player(), pool, count);
                return Map.of("results", results);
            }
        };
    }

    public static ActionHandler levelUp(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "survivor.levelUp"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                long instanceId = payload.path("id").asLong();
                int small = payload.path("small").asInt(0);
                int medium = payload.path("medium").asInt(0);
                int large = payload.path("large").asInt(0);
                int newLevel = svc.survivor.levelUp(session.player(), instanceId, small, medium, large);
                var inst = session.player().survivors().get(instanceId);
                return Map.of(
                        "instanceId", instanceId,
                        "level", newLevel,
                        "star", inst == null ? 1 : inst.star()
                );
            }
        };
    }
}
