package com.lastbastion.app.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.lastbastion.app.GameBootstrap;
import com.lastbastion.app.net.ActionHandler;
import com.lastbastion.app.net.Session;
import com.lastbastion.game.onboarding.OnboardingStep;

import java.util.Map;

public final class OnboardingActions {

    private OnboardingActions() {}

    public static ActionHandler completeStep(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "onboarding.completeStep"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                OnboardingStep step = OnboardingStep.valueOf(payload.path("step").asText());
                svc.onboarding.complete(session.player(), step);
                return Map.of("current", session.player().onboardingState().current().name());
            }
        };
    }

    public static ActionHandler skip(GameBootstrap.Services svc) {
        return new ActionHandler() {
            @Override public String name() { return "onboarding.skip"; }
            @Override
            public Object handle(Session session, JsonNode payload) {
                svc.onboarding.skip(session.player());
                return Map.of("current", session.player().onboardingState().current().name());
            }
        };
    }
}
