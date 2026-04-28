package com.lastbastion.game.onboarding;

import com.lastbastion.common.GameException;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingServiceTest {

    @Test
    void stepsProgressInOrder() {
        OnboardingService svc = new OnboardingService(new AnalyticsService());
        PlayerContext ctx = new PlayerContext(1, "t");
        svc.complete(ctx, OnboardingStep.INTRO_CINEMATIC);
        assertEquals(OnboardingStep.FIRST_SURVIVOR, ctx.onboardingState().current());
        // Wrong order throws
        assertThrows(GameException.class,
                () -> svc.complete(ctx, OnboardingStep.ZONE_1_1_FIGHT));
    }

    @Test
    void skipOnlyAllowedAfterStep5() {
        OnboardingService svc = new OnboardingService(new AnalyticsService());
        PlayerContext ctx = new PlayerContext(1, "t");
        assertThrows(GameException.class, () -> svc.skip(ctx));
        // advance through steps 1..5
        svc.complete(ctx, OnboardingStep.INTRO_CINEMATIC);
        svc.complete(ctx, OnboardingStep.FIRST_SURVIVOR);
        svc.complete(ctx, OnboardingStep.ZONE_1_1_FIGHT);
        svc.complete(ctx, OnboardingStep.EQUIP_GEAR);
        svc.complete(ctx, OnboardingStep.LEVELUP_SURVIVOR);
        svc.skip(ctx);
        assertEquals(OnboardingStep.COMPLETE, ctx.onboardingState().current());
    }
}
