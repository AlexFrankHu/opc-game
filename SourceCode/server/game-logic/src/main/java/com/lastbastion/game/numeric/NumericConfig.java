package com.lastbastion.game.numeric;

/**
 * 数值配置聚合 — 一次启动加载，运行期不可变。
 *
 * <p>各子项对应 {@code assets/numeric/*.json} 中的一个文件。
 *
 * @see NumericConfigLoader
 */
public final class NumericConfig {
    private final CombatTuning combat;
    private final GachaTuning gacha;
    private final GearTuning gear;
    private final AugmentTuning augment;
    private final ArenaTuning arena;
    private final BattlePassTuning battlePass;
    private final ResourceTuning resources;
    private final ZoneIdleTuning zoneIdle;
    private final StarterPackTuning starterPack;
    private final LimitedOffersTuning limitedOffers;
    private final OnboardingTuning onboarding;
    private final DailyQuestsTuning dailyQuests;

    public NumericConfig(CombatTuning combat, GachaTuning gacha, GearTuning gear,
                         AugmentTuning augment, ArenaTuning arena, BattlePassTuning battlePass,
                         ResourceTuning resources, ZoneIdleTuning zoneIdle,
                         StarterPackTuning starterPack, LimitedOffersTuning limitedOffers,
                         OnboardingTuning onboarding, DailyQuestsTuning dailyQuests) {
        this.combat = combat;
        this.gacha = gacha;
        this.gear = gear;
        this.augment = augment;
        this.arena = arena;
        this.battlePass = battlePass;
        this.resources = resources;
        this.zoneIdle = zoneIdle;
        this.starterPack = starterPack;
        this.limitedOffers = limitedOffers;
        this.onboarding = onboarding;
        this.dailyQuests = dailyQuests;
    }

    public CombatTuning combat() { return combat; }
    public GachaTuning gacha() { return gacha; }
    public GearTuning gear() { return gear; }
    public AugmentTuning augment() { return augment; }
    public ArenaTuning arena() { return arena; }
    public BattlePassTuning battlePass() { return battlePass; }
    public ResourceTuning resources() { return resources; }
    public ZoneIdleTuning zoneIdle() { return zoneIdle; }
    public StarterPackTuning starterPack() { return starterPack; }
    public LimitedOffersTuning limitedOffers() { return limitedOffers; }
    public OnboardingTuning onboarding() { return onboarding; }
    public DailyQuestsTuning dailyQuests() { return dailyQuests; }

    /** 测试 & 静态字段便利：从 classpath 加载默认值并缓存。生产由 {@link NumericConfigLoader#load()} 走优先级链。 */
    public static NumericConfig defaults() {
        return DefaultsHolder.INSTANCE;
    }

    private static final class DefaultsHolder {
        static final NumericConfig INSTANCE = NumericConfigLoader.fromClasspath();
    }
}
