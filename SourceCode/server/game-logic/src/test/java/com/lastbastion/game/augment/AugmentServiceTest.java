package com.lastbastion.game.augment;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import com.lastbastion.game.survivor.SurvivorInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AugmentServiceTest {

    @Test
    void fusionProducesHigherStar() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        AugmentService svc = new AugmentService(a, res);
        PlayerContext ctx = new PlayerContext(1, "t");
        AugmentInstance x = svc.add(ctx, AugmentType.ATK, 1);
        AugmentInstance y = svc.add(ctx, AugmentType.ATK, 1);
        AugmentInstance z = svc.add(ctx, AugmentType.ATK, 1);
        AugmentInstance out = svc.fuse(ctx, x.instanceId(), y.instanceId(), z.instanceId());
        assertEquals(2, out.star());
        assertEquals(AugmentType.ATK, out.type());
        assertFalse(ctx.augmentBag().containsKey(x.instanceId()));
    }

    @Test
    void fusionMismatchTypeThrows() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        AugmentService svc = new AugmentService(a, res);
        PlayerContext ctx = new PlayerContext(1, "t");
        AugmentInstance x = svc.add(ctx, AugmentType.ATK, 1);
        AugmentInstance y = svc.add(ctx, AugmentType.DEF, 1);
        AugmentInstance z = svc.add(ctx, AugmentType.ATK, 1);
        assertThrows(GameException.class,
                () -> svc.fuse(ctx, x.instanceId(), y.instanceId(), z.instanceId()));
    }

    @Test
    void insertAndRemoveFromSurvivor() {
        AnalyticsService a = new AnalyticsService();
        ResourceService res = new ResourceService(a);
        AugmentService svc = new AugmentService(a, res);
        PlayerContext ctx = new PlayerContext(1, "t");
        res.add(ctx, CurrencyType.CREDITS, 10_000, SourceTag.TEST);
        SurvivorInstance s = new SurvivorInstance(1001L, "L_COMMANDER_REX");
        ctx.survivors().put(s.instanceId(), s);
        AugmentInstance aug = svc.add(ctx, AugmentType.ATK, 3);
        svc.insert(ctx, s.instanceId(), 0, aug.instanceId());
        assertEquals(aug.instanceId(), s.augmentSlots()[0]);
        svc.remove(ctx, s.instanceId(), 0);
        assertEquals(0, s.augmentSlots()[0]);
    }
}
