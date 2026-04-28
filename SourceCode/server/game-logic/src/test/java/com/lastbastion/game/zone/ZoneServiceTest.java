package com.lastbastion.game.zone;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.loader.ConfigLoader;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ZoneServiceTest {

    @Test
    void progressIsLinear() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        ZoneService svc = new ZoneService(new ConfigLoader().loadZones(), res, a, new Random(1));
        PlayerContext ctx = new PlayerContext(1L, "t");

        // first clear 1-1
        ZoneService.AttemptResult r = svc.clear(ctx, 1, 1, true);
        assertTrue(r.won);
        assertEquals(1, ctx.zoneProgressStage());

        // can't skip
        assertThrows(GameException.class, () -> svc.clear(ctx, 1, 3, true));
        // Fail doesn't advance
        ZoneService.AttemptResult loss = svc.clear(ctx, 1, 2, false);
        assertFalse(loss.won);
        assertEquals(1, ctx.zoneProgressStage());
        // Replaying cleared stages is OK
        assertDoesNotThrow(() -> svc.clear(ctx, 1, 1, true));
    }

    @Test
    void idleSettlementCaps() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        ZoneService svc = new ZoneService(new ConfigLoader().loadZones(), res, a, new Random(1));
        PlayerContext ctx = new PlayerContext(1L, "t");
        long now = System.currentTimeMillis();
        ctx.setLastLogoutTimestamp(now - 48L * 3600 * 1000); // 48h offline
        ZoneService.IdleReward r = svc.settleIdle(ctx, now);
        // capped at 12h (non-BP user)
        assertEquals(12L * 3600 * 1000, r.effectiveMs);
        assertTrue(r.currency.getOrDefault(CurrencyType.CREDITS, 0L) > 0);
    }

    @Test
    void linearUnlockRejectsOutOfOrderChapter() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        ZoneService svc = new ZoneService(new ConfigLoader().loadZones(), res, a, new Random(1));
        PlayerContext ctx = new PlayerContext(1L, "t");
        GameException ex = assertThrows(GameException.class, () -> svc.clear(ctx, 2, 1, true));
        assertEquals(ErrorCode.ZONE_LOCKED, ex.errorCode());
    }
}
