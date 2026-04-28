package com.lastbastion.game.monetization;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattlePassServiceTest {

    @Test
    void xpGainLevelsUp() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        BattlePassConfig cfg = BattlePassConfig.defaultSeason();
        BattlePassService svc = new BattlePassService(cfg, res, a);
        PlayerContext ctx = new PlayerContext(1, "t");
        svc.startSeason(ctx, 0);
        svc.gainXp(ctx, 10_000, SourceTag.MAIN_QUEST);
        assertTrue(ctx.battlePassState().level() > 5);
    }

    @Test
    void claimOnlyOncePerLevel() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        BattlePassConfig cfg = BattlePassConfig.defaultSeason();
        BattlePassService svc = new BattlePassService(cfg, res, a);
        PlayerContext ctx = new PlayerContext(1, "t");
        svc.startSeason(ctx, 0);
        ctx.battlePassState().setLevel(3);
        svc.claim(ctx, 1, false);
        svc.claim(ctx, 2, false);
        svc.claim(ctx, 3, false);
        assertThrows(GameException.class, () -> svc.claim(ctx, 1, false));
    }

    @Test
    void premiumTrackRequiresPurchase() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        BattlePassConfig cfg = BattlePassConfig.defaultSeason();
        BattlePassService svc = new BattlePassService(cfg, res, a);
        PlayerContext ctx = new PlayerContext(1, "t");
        svc.startSeason(ctx, 0);
        ctx.battlePassState().setLevel(5);
        assertThrows(GameException.class, () -> svc.claim(ctx, 1, true));
        svc.buyPremium(ctx, "test-order-1");
        // now premium claims succeed
        svc.claim(ctx, 1, true);
    }

    @Test
    void catchupAdvancesLevel() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        BattlePassConfig cfg = BattlePassConfig.defaultSeason();
        BattlePassService svc = new BattlePassService(cfg, res, a);
        PlayerContext ctx = new PlayerContext(1, "t");
        svc.startSeason(ctx, 0);
        res.add(ctx, CurrencyType.PREMIUM_CHIPS, 10_000, SourceTag.TEST);
        svc.catchup(ctx, 3);
        assertEquals(3, ctx.battlePassState().level());
    }
}
