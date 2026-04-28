package com.lastbastion.game.gear;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.GameException;
import com.lastbastion.common.GearQuality;
import com.lastbastion.common.GearSlot;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;
import com.lastbastion.game.survivor.SurvivorInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GearServiceTest {

    private PlayerContext newCtx(ResourceService svc) {
        PlayerContext ctx = new PlayerContext(1L, "t");
        svc.add(ctx, CurrencyType.ALLOY, 5_000_000, SourceTag.TEST);
        return ctx;
    }

    @Test
    void enhanceRateMatchesConfigIn10kSimulations() {
        ResourceService resource = new ResourceService(new AnalyticsService());
        GearService gear = new GearService(resource, new AnalyticsService(), new Random(42));
        PlayerContext ctx = newCtx(resource);
        GearFactory fac = new GearFactory(new Random(42));
        int successesAt15 = 0;
        int totalAt15 = 10000;
        for (int i = 0; i < totalAt15; i++) {
            GearInstance g = fac.roll(GearSlot.WEAPON, GearQuality.PURPLE);
            g.setLevel(14); // so enhance goes to 15
            ctx.gearBag().put(g.instanceId(), g);
            GearService.EnhanceResult r = gear.enhance(ctx, g.instanceId());
            if (r.success) successesAt15++;
        }
        double rate = successesAt15 / (double) totalAt15;
        // expected 60% with ±2% tolerance
        assertTrue(rate > 0.57 && rate < 0.63, "rate=" + rate);
    }

    @Test
    void lockedGearSkippedInDecompose() {
        ResourceService resource = new ResourceService(new AnalyticsService());
        GearService gear = new GearService(resource, new AnalyticsService(), new Random(1));
        PlayerContext ctx = newCtx(resource);
        GearFactory fac = new GearFactory(new Random(1));
        GearInstance locked = fac.roll(GearSlot.WEAPON, GearQuality.BLUE);
        GearInstance unlocked = fac.roll(GearSlot.ARMOR, GearQuality.BLUE);
        ctx.gearBag().put(locked.instanceId(), locked);
        ctx.gearBag().put(unlocked.instanceId(), unlocked);
        gear.setLock(ctx, locked.instanceId(), true);
        long alloy = gear.decompose(ctx, List.of(locked.instanceId(), unlocked.instanceId()));
        assertTrue(alloy > 0);
        assertTrue(ctx.gearBag().containsKey(locked.instanceId()));
        assertFalse(ctx.gearBag().containsKey(unlocked.instanceId()));
    }

    @Test
    void bagFullThrows() {
        ResourceService resource = new ResourceService(new AnalyticsService());
        GearService gear = new GearService(resource, new AnalyticsService(), new Random(1));
        gear.setBagCapacity(2);
        PlayerContext ctx = newCtx(resource);
        GearFactory fac = new GearFactory(new Random(1));
        gear.add(ctx, fac.roll(GearSlot.WEAPON, GearQuality.BLUE));
        gear.add(ctx, fac.roll(GearSlot.ARMOR, GearQuality.BLUE));
        assertThrows(GameException.class,
                () -> gear.add(ctx, fac.roll(GearSlot.HELMET, GearQuality.BLUE)));
    }

    @Test
    void equipAndUnequipSwapsOwnership() {
        ResourceService resource = new ResourceService(new AnalyticsService());
        GearService gear = new GearService(resource, new AnalyticsService(), new Random(1));
        PlayerContext ctx = newCtx(resource);
        GearFactory fac = new GearFactory(new Random(1));
        GearInstance weapon = fac.roll(GearSlot.WEAPON, GearQuality.BLUE);
        gear.add(ctx, weapon);
        SurvivorInstance s = new SurvivorInstance(1001L, "L_COMMANDER_REX");
        ctx.survivors().put(s.instanceId(), s);
        gear.equip(ctx, s.instanceId(), weapon.instanceId());
        assertEquals(weapon.instanceId(), s.equipped().get(GearSlot.WEAPON));
        assertEquals(s.instanceId(), weapon.equippedSurvivorId());
        gear.unequip(ctx, s.instanceId(), GearSlot.WEAPON);
        assertNull(s.equipped().get(GearSlot.WEAPON));
        assertEquals(0, weapon.equippedSurvivorId());
    }
}
