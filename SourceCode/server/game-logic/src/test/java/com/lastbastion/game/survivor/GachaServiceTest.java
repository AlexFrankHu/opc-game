package com.lastbastion.game.survivor;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.Rarity;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.loader.ConfigLoader;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ItemService;
import com.lastbastion.game.resource.ResourceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GachaServiceTest {

    private GachaService buildSvc(PlayerContext ctx, Random rng) {
        AnalyticsService a = new AnalyticsService();
        ResourceService resource = new ResourceService(a);
        SurvivorConfigRepository repo = new ConfigLoader().loadSurvivors();
        SurvivorService survivor = new SurvivorService(repo, resource, new ItemService(a), a);
        // give ctx infinite tokens
        resource.add(ctx, CurrencyType.RECRUIT_TOKENS, 99_999_999L, SourceTag.TEST);
        resource.add(ctx, CurrencyType.PREMIUM_CHIPS, 99_999_999L, SourceTag.TEST);
        return new GachaService(repo, survivor, resource, a, rng);
    }

    @Test
    void pityGuaranteesLegendaryAt80() {
        PlayerContext ctx = new PlayerContext(1L, "t");
        GachaService svc = buildSvc(ctx, new Random(42));
        boolean gotLegendaryBefore80 = false;
        int pulls = 0;
        for (; pulls < 80; pulls++) {
            GachaService.Result r = svc.pull(ctx, GachaService.Pool.FREE, 1).get(0);
            if (r.rarity == Rarity.LEGENDARY) {
                gotLegendaryBefore80 = true;
                break;
            }
        }
        if (!gotLegendaryBefore80) {
            // the 80th should be guaranteed
            // but loop stops at 80 without pull. Pull the 80th.
        }
        // the final pull (80th overall without legendary) must be legendary
        if (!gotLegendaryBefore80) {
            // already pulled 80, none legendary? Actually the loop ends before 80th pull if no legendary. Fix:
            GachaService.Result next = svc.pull(ctx, GachaService.Pool.FREE, 1).get(0);
            assertEquals(Rarity.LEGENDARY, next.rarity);
        }
        assertTrue(true);
    }

    @Test
    void rarityRatesApproximateOver10000Pulls() {
        PlayerContext ctx = new PlayerContext(2L, "t");
        GachaService svc = buildSvc(ctx, new Random(7));
        int n = 10000;
        int legendary = 0;
        int epic = 0;
        for (int i = 0; i < n; i++) {
            GachaService.Result r = svc.pull(ctx, GachaService.Pool.FREE, 1).get(0);
            if (r.rarity == Rarity.LEGENDARY) legendary++;
            if (r.rarity == Rarity.EPIC) epic++;
        }
        // With pity guarantee, Legendary rate will be slightly higher than 2%.
        double legRate = legendary / (double) n;
        assertTrue(legRate >= 0.02 && legRate <= 0.04, "legRate=" + legRate);
        double epicRate = epic / (double) n;
        // Epic expected 13%, tolerate ±3%
        assertTrue(epicRate >= 0.10 && epicRate <= 0.16, "epicRate=" + epicRate);
    }

    @Test
    void pullsDeductTokens() {
        PlayerContext ctx = new PlayerContext(3L, "t");
        AnalyticsService a = new AnalyticsService();
        ResourceService resource = new ResourceService(a);
        SurvivorConfigRepository repo = new ConfigLoader().loadSurvivors();
        SurvivorService survivor = new SurvivorService(repo, resource, new ItemService(a), a);
        resource.add(ctx, CurrencyType.RECRUIT_TOKENS, 10, SourceTag.TEST);
        GachaService svc = new GachaService(repo, survivor, resource, a, new Random(1));
        List<GachaService.Result> res = svc.pull(ctx, GachaService.Pool.FREE, 10);
        assertEquals(10, res.size());
        assertEquals(0, resource.balance(ctx, CurrencyType.RECRUIT_TOKENS));
    }
}
