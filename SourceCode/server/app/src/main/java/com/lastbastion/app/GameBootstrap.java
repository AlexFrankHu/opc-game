package com.lastbastion.app;

import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.arena.ArenaService;
import com.lastbastion.game.augment.AugmentService;
import com.lastbastion.game.combat.CombatAssembler;
import com.lastbastion.game.gear.GearService;
import com.lastbastion.game.loader.ConfigLoader;
import com.lastbastion.game.monetization.BattlePassConfig;
import com.lastbastion.game.monetization.BattlePassService;
import com.lastbastion.game.monetization.IapService;
import com.lastbastion.game.monetization.LimitedOfferService;
import com.lastbastion.game.monetization.StarterPackService;
import com.lastbastion.game.monetization.TestIapVerifier;
import com.lastbastion.game.onboarding.DailyQuestService;
import com.lastbastion.game.onboarding.OnboardingService;
import com.lastbastion.game.resource.ItemService;
import com.lastbastion.game.resource.ResourceService;
import com.lastbastion.game.survivor.GachaService;
import com.lastbastion.game.survivor.SurvivorConfigRepository;
import com.lastbastion.game.survivor.SurvivorService;
import com.lastbastion.game.zone.ZoneConfigRepository;
import com.lastbastion.game.zone.ZoneService;

import java.util.Random;

/**
 * 将所有玩法服务一次性装配。
 *
 * 真实部署时：
 *   1) 在 ioGame 的 {@code ExternalJoinEnterHandler} 启动时构造本对象；
 *   2) 在 {@code ActionController}/{@code BrokerClient} 中注入 {@link #services()}。
 */
public final class GameBootstrap {

    public static final class Services {
        public final AnalyticsService analytics;
        public final ResourceService resource;
        public final ItemService item;
        public final SurvivorConfigRepository survivorRepo;
        public final SurvivorService survivor;
        public final GachaService gacha;
        public final GearService gear;
        public final AugmentService augment;
        public final ZoneConfigRepository zoneRepo;
        public final ZoneService zone;
        public final ArenaService arena;
        public final BattlePassConfig battlePassConfig;
        public final BattlePassService battlePass;
        public final StarterPackService starterPack;
        public final LimitedOfferService limitedOffer;
        public final IapService iap;
        public final OnboardingService onboarding;
        public final DailyQuestService dailyQuest;
        public final CombatAssembler combatAssembler;

        public Services(AnalyticsService analytics, ResourceService resource, ItemService item,
                        SurvivorConfigRepository survivorRepo, SurvivorService survivor,
                        GachaService gacha, GearService gear, AugmentService augment,
                        ZoneConfigRepository zoneRepo, ZoneService zone, ArenaService arena,
                        BattlePassConfig battlePassConfig, BattlePassService battlePass,
                        StarterPackService starterPack, LimitedOfferService limitedOffer, IapService iap,
                        OnboardingService onboarding, DailyQuestService dailyQuest,
                        CombatAssembler combatAssembler) {
            this.analytics = analytics;
            this.resource = resource;
            this.item = item;
            this.survivorRepo = survivorRepo;
            this.survivor = survivor;
            this.gacha = gacha;
            this.gear = gear;
            this.augment = augment;
            this.zoneRepo = zoneRepo;
            this.zone = zone;
            this.arena = arena;
            this.battlePassConfig = battlePassConfig;
            this.battlePass = battlePass;
            this.starterPack = starterPack;
            this.limitedOffer = limitedOffer;
            this.iap = iap;
            this.onboarding = onboarding;
            this.dailyQuest = dailyQuest;
            this.combatAssembler = combatAssembler;
        }
    }

    private Services services;

    public Services services() {
        return services;
    }

    public Services boot() {
        Random rng = new Random();
        AnalyticsService analytics = new AnalyticsService();
        analytics.addSink(new AnalyticsService.LoggingSink());

        ResourceService resource = new ResourceService(analytics);
        ItemService item = new ItemService(analytics);

        ConfigLoader loader = new ConfigLoader();
        SurvivorConfigRepository survivorRepo = loader.loadSurvivors();
        SurvivorService survivor = new SurvivorService(survivorRepo, resource, item, analytics);
        GachaService gacha = new GachaService(survivorRepo, survivor, resource, analytics, rng);

        GearService gear = new GearService(resource, analytics, rng);
        AugmentService augment = new AugmentService(analytics, resource);

        ZoneConfigRepository zoneRepo = loader.loadZones();
        ZoneService zone = new ZoneService(zoneRepo, resource, analytics, rng);

        ArenaService arena = new ArenaService(resource, analytics, rng);

        BattlePassConfig bpCfg = loader.loadBattlePass();
        BattlePassService bp = new BattlePassService(bpCfg, resource, analytics);
        StarterPackService sp = new StarterPackService(resource, analytics);
        LimitedOfferService lo = new LimitedOfferService(resource, analytics);
        IapService iap = new IapService(new TestIapVerifier(), analytics);

        OnboardingService onboarding = new OnboardingService(analytics);
        DailyQuestService dq = new DailyQuestService(resource);

        CombatAssembler asm = new CombatAssembler(survivor, gear, augment);

        this.services = new Services(analytics, resource, item, survivorRepo, survivor, gacha,
                gear, augment, zoneRepo, zone, arena, bpCfg, bp, sp, lo, iap, onboarding, dq, asm);
        return services;
    }
}
