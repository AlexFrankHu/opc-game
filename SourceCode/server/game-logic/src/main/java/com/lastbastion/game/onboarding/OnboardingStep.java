package com.lastbastion.game.onboarding;

/**
 * TASK-010 引导步骤。
 */
public enum OnboardingStep {
    INTRO_CINEMATIC(1),
    FIRST_SURVIVOR(2),
    ZONE_1_1_FIGHT(3),
    EQUIP_GEAR(4),
    LEVELUP_SURVIVOR(5),
    ZONE_1_2_FIGHT(6),
    STARTER_PACK_POPUP(7),
    ARENA_ENTRY(8),
    BATTLE_PASS_HIGHLIGHT(9),
    COMPLETE(10);

    private final int order;

    OnboardingStep(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }

    /** 第 5 步后允许跳过剩余引导。 */
    public static final int SKIP_ALLOWED_AFTER = LEVELUP_SURVIVOR.order();
}
