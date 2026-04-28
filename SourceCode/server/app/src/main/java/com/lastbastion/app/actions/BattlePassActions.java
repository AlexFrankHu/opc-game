package com.lastbastion.app.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.ActionHandler;
import com.lastbastion.app.net.Session;

import java.util.BitSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class BattlePassActions {

    private BattlePassActions() {}

    public static ActionHandler claim(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "bp.claim"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                int level = payload.path("level").asInt();
                boolean premium = payload.path("premium").asBoolean(false);
                var rewards = svc.battlePass.claim(session.player(), level, premium);
                var st = session.player().battlePassState();
                return Map.of(
                        "level", st.level(),
                        "xp", st.xp(),
                        "premiumActive", st.premiumActive(),
                        "freeClaimed", toList(st.freeClaimed()),
                        "premiumClaimed", toList(st.premiumClaimed()),
                        "rewards", rewards
                );
            }
        };
    }

    private static java.util.List<Integer> toList(BitSet bs) {
        return IntStream.range(0, bs.length()).filter(bs::get).boxed().collect(Collectors.toList());
    }

    public static ActionHandler buy(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "bp.buy"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                String orderId = payload.path("orderId").asText();
                svc.battlePass.buyPremium(session.player(), orderId);
                return Map.of("premiumActive", session.player().battlePassState().premiumActive());
            }
        };
    }
}
