package com.lastbastion.game.player;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.game.survivor.SurvivorInstance;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用 H2（MySQL 兼容模式）跑 {@link JdbcPlayerStore}：
 * 验证建表 SQL / upsert / 二次 Store 实例从库里读取快照能完整还原玩家状态。
 *
 * <p>线上对接的是 MySQL 8，DDL 与 SQL 都是 MySQL 方言，但 H2 的 MODE=MySQL
 * 已经覆盖 LONGBLOB / ON UPDATE CURRENT_TIMESTAMP / INSERT ... ON DUPLICATE KEY
 * UPDATE 这几条本测试关心的语法。如果未来用到更复杂的 MySQL-only 特性，
 * 切到 testcontainers + 真实 MySQL 即可。</p>
 */
class JdbcPlayerStoreTest {

    private HikariDataSource ds;

    @BeforeEach
    void start() {
        HikariConfig cfg = new HikariConfig();
        // DB_CLOSE_DELAY=-1 让连接关闭后内存库不消失；MODE=MySQL 走 MySQL 方言。
        cfg.setJdbcUrl("jdbc:h2:mem:store-" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(4);
        ds = new HikariDataSource(cfg);
    }

    @AfterEach
    void stop() {
        if (ds != null) ds.close();
    }

    @Test
    void roundTripAcrossStoreInstances() {
        JdbcPlayerStore first = new JdbcPlayerStore(ds);
        assertTrue(first.load("ext-jdbc-1").isEmpty(),
                "empty schema should yield no player");

        PlayerContext ctx = new PlayerContext(7L, "ext-jdbc-1", "Huzi");
        ctx.currencies().put(CurrencyType.CREDITS, 99_999L);
        ctx.currencies().put(CurrencyType.PREMIUM_CHIPS, 1_234L);
        ctx.setZoneProgress(3, 5);
        ctx.setHighestPowerRating(54_321);
        ctx.survivors().put(900L, new SurvivorInstance(900L, "L_TANK_RHINO"));

        first.save("ext-jdbc-1", ctx);

        // 第二个 Store 共享同一个 DataSource — 模拟同进程内 reset cache，
        // 真实场景是另一台机器拿同一个 DB 重新加载。
        JdbcPlayerStore second = new JdbcPlayerStore(ds);
        Optional<PlayerContext> loaded = second.load("ext-jdbc-1");
        assertTrue(loaded.isPresent(),
                "snapshot must be reloadable from a fresh JdbcPlayerStore");

        PlayerContext r = loaded.get();
        assertEquals(7L, r.playerId());
        assertEquals("ext-jdbc-1", r.externalId());
        assertEquals("Huzi", r.nickname());
        assertEquals(99_999L, r.currencies().get(CurrencyType.CREDITS));
        assertEquals(1_234L, r.currencies().get(CurrencyType.PREMIUM_CHIPS));
        assertEquals(3, r.zoneProgressChapter());
        assertEquals(5, r.zoneProgressStage());
        assertEquals(54_321, r.highestPowerRating());
        assertEquals(1, r.survivors().size());
        assertEquals("L_TANK_RHINO", r.survivors().get(900L).configId());
    }

    @Test
    void upsertReplacesPreviousSnapshot() {
        JdbcPlayerStore store = new JdbcPlayerStore(ds);

        PlayerContext v1 = new PlayerContext(1L, "ext-jdbc-2", "Huzi");
        v1.currencies().put(CurrencyType.CREDITS, 100L);
        store.save("ext-jdbc-2", v1);

        PlayerContext v2 = new PlayerContext(1L, "ext-jdbc-2", "Huzi");
        v2.currencies().put(CurrencyType.CREDITS, 9_999L);
        store.save("ext-jdbc-2", v2);

        JdbcPlayerStore fresh = new JdbcPlayerStore(ds);
        Optional<PlayerContext> loaded = fresh.load("ext-jdbc-2");
        assertTrue(loaded.isPresent());
        assertEquals(9_999L, loaded.get().currencies().get(CurrencyType.CREDITS));
    }
}
