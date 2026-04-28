package com.lastbastion.game.player;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于关系型数据库（MySQL / 兼容 MySQL 协议）的持久化实现。
 *
 * <p>表结构自动建：</p>
 * <pre>
 * CREATE TABLE IF NOT EXISTS player_snapshot (
 *   external_id VARCHAR(128) NOT NULL PRIMARY KEY,
 *   snapshot    LONGBLOB     NOT NULL,
 *   updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
 *                            ON UPDATE CURRENT_TIMESTAMP
 * ) ENGINE=InnoDB;
 * </pre>
 *
 * <p>upsert 使用 MySQL 的 {@code INSERT ... ON DUPLICATE KEY UPDATE} 语义。</p>
 *
 * <p>存储形式：与 {@link FilePlayerStore} 一致的 Java 序列化字节。后续若要切
 * JSON / Protobuf，只需替换 {@link SerializationCodec} 并同步迁移任务。</p>
 */
public final class JdbcPlayerStore implements PlayerStore, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JdbcPlayerStore.class);

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS player_snapshot (
                external_id VARCHAR(128) NOT NULL PRIMARY KEY,
                snapshot    LONGBLOB     NOT NULL,
                updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP
            )
            """;

    private static final String SELECT_SQL =
            "SELECT snapshot FROM player_snapshot WHERE external_id = ?";

    private static final String UPSERT_SQL =
            "INSERT INTO player_snapshot (external_id, snapshot) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE snapshot = VALUES(snapshot)";

    private final HikariDataSource dataSource;
    private final boolean ownsDataSource;
    private final ConcurrentMap<String, PlayerContext> cache = new ConcurrentHashMap<>();

    /**
     * @param jdbcUrl  形如 {@code jdbc:mysql://host:3306/lastbastion?useSSL=false}
     * @param user     账号
     * @param password 密码
     */
    public JdbcPlayerStore(String jdbcUrl, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(16);
        cfg.setMinimumIdle(2);
        cfg.setPoolName("lastbastion-player-store");
        this.dataSource = new HikariDataSource(cfg);
        this.ownsDataSource = true;
        init();
        log.info("JdbcPlayerStore connected to {}", jdbcUrl);
    }

    /** 测试/内嵌使用：由外部注入 DataSource，close 时不关闭它。 */
    JdbcPlayerStore(DataSource external) {
        if (external instanceof HikariDataSource hds) {
            this.dataSource = hds;
        } else {
            HikariConfig cfg = new HikariConfig();
            cfg.setDataSource(external);
            this.dataSource = new HikariDataSource(cfg);
        }
        this.ownsDataSource = false;
        init();
    }

    private void init() {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate(DDL);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to init player_snapshot table", e);
        }
    }

    @Override
    public Optional<PlayerContext> load(String externalId) {
        PlayerContext cached = cache.get(externalId);
        if (cached != null) return Optional.of(cached);
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_SQL)) {
            ps.setString(1, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                byte[] bytes = rs.getBytes(1);
                Optional<PlayerContext> decoded = SerializationCodec.decode(bytes);
                decoded.ifPresent(ctx -> cache.put(externalId, ctx));
                return decoded;
            }
        } catch (SQLException e) {
            log.error("JDBC load failed for {}", externalId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(String externalId, PlayerContext ctx) {
        cache.put(externalId, ctx);
        byte[] bytes = SerializationCodec.encode(ctx);
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, externalId);
            ps.setBytes(2, bytes);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("JDBC upsert failed for {}", externalId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int size() { return cache.size(); }

    @Override
    public void close() {
        if (ownsDataSource && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
