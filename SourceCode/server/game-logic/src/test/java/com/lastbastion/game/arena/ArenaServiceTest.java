package com.lastbastion.game.arena;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ArenaServiceTest {

    private ArenaService newSvc() {
        AnalyticsService a = new AnalyticsService();
        ResourceService r = new ResourceService(a);
        return new ArenaService(r, a, new Random(1));
    }

    @Test
    void winningAgainstHigherSwapsRanks() {
        ArenaService svc = newSvc();
        PlayerContext a = new PlayerContext(1, "a");
        PlayerContext b = new PlayerContext(2, "b");
        svc.registerOrUpdate(a, 1000, new long[]{});
        svc.registerOrUpdate(b, 1100, new long[]{});
        // a is rank 1 (first registered), b is rank 2.
        // Now assume b wants to challenge a.
        b.arenaState().setDailyFreeLeft(1);
        ArenaRecord r = svc.challenge(b, 1L, true);
        assertTrue(r.won);
        // b should now be rank 1
        assertEquals(1, b.arenaState().rank());
    }

    @Test
    void dailyLimitEnforced() {
        ArenaService svc = newSvc();
        PlayerContext a = new PlayerContext(1, "a");
        svc.registerOrUpdate(a, 1000, new long[]{});
        a.arenaState().setDailyFreeLeft(0);
        assertThrows(GameException.class, () -> svc.challenge(a, 999L, true));
    }

    @Test
    void buyChallengeIncreasesFreeLeft() {
        ArenaService svc = newSvc();
        PlayerContext a = new PlayerContext(1, "a");
        svc.registerOrUpdate(a, 1000, new long[]{});
        // give chips
        ResourceService res = new ResourceService(new AnalyticsService());
        res.add(a, CurrencyType.PREMIUM_CHIPS, 1000, SourceTag.TEST);
        ArenaService withFunds = new ArenaService(res, new AnalyticsService(), new Random(1));
        withFunds.registerOrUpdate(a, 1000, new long[]{});
        a.arenaState().setDailyFreeLeft(0);
        withFunds.buyChallenge(a);
        assertEquals(1, a.arenaState().dailyFreeLeft());
    }

    @Test
    void topRankOrdered() {
        ArenaService svc = newSvc();
        PlayerContext a = new PlayerContext(1, "a");
        PlayerContext b = new PlayerContext(2, "b");
        svc.registerOrUpdate(a, 1000, new long[]{});
        svc.registerOrUpdate(b, 1100, new long[]{});
        List<ArenaService.ArenaRoster> top = svc.topRanks(10);
        assertEquals(1L, top.get(0).playerId);
        assertEquals(2L, top.get(1).playerId);
    }
}
