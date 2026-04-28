package com.lastbastion.game.onboarding;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OnboardingState {

    private OnboardingStep current = OnboardingStep.INTRO_CINEMATIC;
    private final Set<OnboardingStep> completed = EnumSet.noneOf(OnboardingStep.class);
    private final Map<OnboardingStep, Long> completedTimestamps = new EnumMap<>(OnboardingStep.class);
    private boolean skipped;

    public OnboardingStep current() { return current; }
    public void setCurrent(OnboardingStep s) { this.current = s; }
    public Set<OnboardingStep> completed() { return completed; }
    public Map<OnboardingStep, Long> completedTimestamps() { return completedTimestamps; }
    public boolean skipped() { return skipped; }
    public void setSkipped(boolean v) { this.skipped = v; }
}
