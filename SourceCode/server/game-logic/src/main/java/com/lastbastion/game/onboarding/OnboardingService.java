package com.lastbastion.game.onboarding;

import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;

/**
 * TASK-010 §10.1/10.2 — 新手引导状态机。
 */
public final class OnboardingService {

    private final AnalyticsService analytics;

    public OnboardingService(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    public void complete(PlayerContext ctx, OnboardingStep step) {
        OnboardingState s = ctx.onboardingState();
        if (s.completed().contains(step)) return;
        if (step.order() != s.current().order()) {
            throw new GameException(ErrorCode.GUIDE_STEP_ORDER,
                    "expected " + s.current() + " got " + step);
        }
        s.completed().add(step);
        s.completedTimestamps().put(step, System.currentTimeMillis());
        OnboardingStep next = nextOf(step);
        s.setCurrent(next == null ? OnboardingStep.COMPLETE : next);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("tutorial_step_complete")
                    .prop("player_id", ctx.playerId())
                    .prop("step", step.name())
                    .build());
        }
    }

    public void skip(PlayerContext ctx) {
        OnboardingState s = ctx.onboardingState();
        if (s.current().order() <= OnboardingStep.SKIP_ALLOWED_AFTER) {
            throw new GameException(ErrorCode.GUIDE_STEP_ORDER,
                    "cannot skip before step " + OnboardingStep.SKIP_ALLOWED_AFTER);
        }
        for (OnboardingStep step : OnboardingStep.values()) {
            if (step.order() < OnboardingStep.COMPLETE.order()) s.completed().add(step);
        }
        s.setCurrent(OnboardingStep.COMPLETE);
        s.setSkipped(true);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("tutorial_skip")
                    .prop("player_id", ctx.playerId())
                    .prop("step", s.current().name())
                    .build());
        }
    }

    private static OnboardingStep nextOf(OnboardingStep s) {
        OnboardingStep[] all = OnboardingStep.values();
        int idx = s.ordinal();
        if (idx + 1 >= all.length) return null;
        return all[idx + 1];
    }
}
