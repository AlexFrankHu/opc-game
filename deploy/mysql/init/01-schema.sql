-- Last Bastion MySQL 启动初始化脚本
-- docker-compose.yml 启动时由 mysql 镜像自动执行（仅在数据卷为空时运行一次）
--
-- JdbcPlayerStore 启动时也会做 CREATE TABLE IF NOT EXISTS，
-- 这里写一份兼容的 schema 方便人工排查 / 直接在已有 MySQL 上创建。

CREATE DATABASE IF NOT EXISTS lastbastion
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lastbastion;

CREATE TABLE IF NOT EXISTS player_snapshot (
    external_id VARCHAR(128) NOT NULL,
    snapshot    LONGBLOB     NOT NULL,
    updated_at  BIGINT       NOT NULL,
    PRIMARY KEY (external_id),
    KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 后续策划/运营可能需要的额外表（未启用，先留 placeholder）
-- CREATE TABLE IF NOT EXISTS analytics_event (...);
-- CREATE TABLE IF NOT EXISTS iap_receipt (...);
