package com.lastbastion.app;

import com.lastbastion.app.net.SessionRegistry;
import com.lastbastion.common.CurrencyType;
import com.lastbastion.game.player.FilePlayerStore;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.zone.ZoneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证：登录 → 过 zone → 领奖励 → 写盘 → 进程重启（销毁 Session/Service）→ 再登录
 *       数据完全恢复。
 */
class PersistenceAcrossRestartTest {

    @Test
    void playerStateSurvivesRestart(@TempDir Path tmp) {
        // --- 第一轮进程 ---
        GameBootstrap.Services svc1 = new GameBootstrap().boot();
        SessionRegistry reg1 = new SessionRegistry(new FilePlayerStore(tmp));

        PlayerContext p1 = reg1.loginOrCreate("player-persist");
        long originalId = p1.playerId();

        // 完整通 1-1（会给他一些奖励 + 推进 zone 进度）
        ZoneService.AttemptResult r1 = svc1.zone.clear(p1, 1, 1, true);
        assertTrue(r1.won);
        reg1.save(p1.externalId(), p1);

        long creditsAfter = p1.currencies().getOrDefault(CurrencyType.CREDITS, 0L);
        assertTrue(creditsAfter > 0, "zone.clear should grant credits");

        // --- 第二轮进程：销毁旧 registry / service，建立新的 ---
        GameBootstrap.Services svc2 = new GameBootstrap().boot();
        SessionRegistry reg2 = new SessionRegistry(new FilePlayerStore(tmp));

        PlayerContext p2 = reg2.loginOrCreate("player-persist");
        assertEquals(originalId, p2.playerId(), "playerId must round-trip");
        assertEquals("player-persist", p2.externalId());
        assertEquals(1, p2.zoneProgressChapter());
        assertEquals(1, p2.zoneProgressStage());
        assertEquals(creditsAfter, p2.currencies().getOrDefault(CurrencyType.CREDITS, 0L),
                "credits should be restored byte-for-byte");

        // 进度继续推进，确保 repo 能处理恢复后的玩家状态
        ZoneService.AttemptResult r2 = svc2.zone.clear(p2, 1, 2, true);
        assertTrue(r2.won);
        assertEquals(2, p2.zoneProgressStage());
    }
}
