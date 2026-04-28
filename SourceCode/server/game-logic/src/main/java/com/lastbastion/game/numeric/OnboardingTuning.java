package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.game.onboarding.OnboardingStep;

import java.util.ArrayList;
import java.util.List;

/** 引导（onboarding.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class OnboardingTuning {
    public List<OnboardingStep> steps = new ArrayList<>();
    public OnboardingStep skipAllowedAfterStep;
}
