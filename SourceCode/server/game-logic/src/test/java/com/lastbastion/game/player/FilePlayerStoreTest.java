package com.lastbastion.game.player;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.game.survivor.SurvivorInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilePlayerStoreTest {

    @Test
    void roundTripAcrossStoreInstances(@TempDir Path tmp) {
        FilePlayerStore first = new FilePlayerStore(tmp);
        assertTrue(first.load("ext-1").isEmpty(), "empty root should yield no player");

        PlayerContext ctx = new PlayerContext(42L, "ext-1", "Huzi");
        ctx.currencies().put(CurrencyType.CREDITS, 12345L);
        ctx.currencies().put(CurrencyType.PREMIUM_CHIPS, 400L);
        ctx.setZoneProgress(2, 7);
        ctx.setHighestPowerRating(9999);
        ctx.survivors().put(100L, new SurvivorInstance(100L, "R_SCOUT_ALPHA"));
        ctx.survivorShards().put("R_SCOUT_ALPHA", 7L);
        ctx.onboardingState().setSkipped(true);

        first.save("ext-1", ctx);

        // 新建一个 Store，模拟进程重启后从磁盘读取。
        FilePlayerStore second = new FilePlayerStore(tmp);
        Optional<PlayerContext> loaded = second.load("ext-1");
        assertTrue(loaded.isPresent(), "saved snapshot must be reloadable");

        PlayerContext r = loaded.get();
        assertEquals(42L, r.playerId());
        assertEquals("ext-1", r.externalId());
        assertEquals("Huzi", r.nickname());
        assertEquals(12345L, r.currencies().get(CurrencyType.CREDITS));
        assertEquals(400L, r.currencies().get(CurrencyType.PREMIUM_CHIPS));
        assertEquals(2, r.zoneProgressChapter());
        assertEquals(7, r.zoneProgressStage());
        assertEquals(9999, r.highestPowerRating());
        assertEquals(1, r.survivors().size());
        assertEquals("R_SCOUT_ALPHA", r.survivors().get(100L).configId());
        assertEquals(7L, r.survivorShards().get("R_SCOUT_ALPHA"));
        assertTrue(r.onboardingState().skipped());
    }

    @Test
    void saveOverwritesExisting(@TempDir Path tmp) {
        FilePlayerStore store = new FilePlayerStore(tmp);
        PlayerContext a = new PlayerContext(1L, "ext-2", "v1");
        a.currencies().put(CurrencyType.ALLOY, 10L);
        store.save("ext-2", a);

        PlayerContext b = new PlayerContext(1L, "ext-2", "v2");
        b.currencies().put(CurrencyType.ALLOY, 20L);
        store.save("ext-2", b);

        // 新建 Store，强制走磁盘路径。
        PlayerContext loaded = new FilePlayerStore(tmp).load("ext-2").orElseThrow();
        assertEquals("v2", loaded.nickname());
        assertEquals(20L, loaded.currencies().get(CurrencyType.ALLOY));
    }
}
