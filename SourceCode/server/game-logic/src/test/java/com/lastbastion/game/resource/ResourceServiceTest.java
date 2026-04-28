package com.lastbastion.game.resource;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceServiceTest {

    @Test
    void addAndSpendWork() {
        AnalyticsService a = new AnalyticsService();
        AnalyticsService.InMemorySink sink = new AnalyticsService.InMemorySink();
        a.addSink(sink);
        ResourceService svc = new ResourceService(a);
        PlayerContext ctx = new PlayerContext(1L, "t");
        svc.add(ctx, CurrencyType.CREDITS, 100, SourceTag.SYSTEM_GIFT);
        assertEquals(100, svc.balance(ctx, CurrencyType.CREDITS));
        svc.spend(ctx, CurrencyType.CREDITS, 30, SourceTag.SYSTEM_GIFT);
        assertEquals(70, svc.balance(ctx, CurrencyType.CREDITS));
        assertEquals(2, sink.events().size());
    }

    @Test
    void insufficientThrows() {
        ResourceService svc = new ResourceService(new AnalyticsService());
        PlayerContext ctx = new PlayerContext(1L, "t");
        svc.add(ctx, CurrencyType.ALLOY, 5, SourceTag.TEST);
        assertThrows(GameException.class, () -> svc.spend(ctx, CurrencyType.ALLOY, 10, SourceTag.TEST));
        assertEquals(5, svc.balance(ctx, CurrencyType.ALLOY));
    }

    @Test
    void spendBatchIsAtomic() {
        ResourceService svc = new ResourceService(new AnalyticsService());
        PlayerContext ctx = new PlayerContext(1L, "t");
        svc.add(ctx, CurrencyType.CREDITS, 100, SourceTag.TEST);
        svc.add(ctx, CurrencyType.ALLOY, 10, SourceTag.TEST);
        assertThrows(GameException.class, () -> svc.spendBatch(ctx,
                Map.of(CurrencyType.CREDITS, 50L, CurrencyType.ALLOY, 50L), SourceTag.TEST));
        // neither currency should be decremented
        assertEquals(100, svc.balance(ctx, CurrencyType.CREDITS));
        assertEquals(10, svc.balance(ctx, CurrencyType.ALLOY));
    }

    @Test
    void addRespectsCap() {
        ResourceService svc = new ResourceService(new AnalyticsService());
        PlayerContext ctx = new PlayerContext(1L, "t");
        ctx.currencies().put(CurrencyType.ALLOY, ResourceService.CAPS.get(CurrencyType.ALLOY) - 10);
        long actual = svc.add(ctx, CurrencyType.ALLOY, 100, SourceTag.TEST);
        assertEquals(10, actual);
        assertEquals(ResourceService.CAPS.get(CurrencyType.ALLOY), svc.balance(ctx, CurrencyType.ALLOY));
    }
}
