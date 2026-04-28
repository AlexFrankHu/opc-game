package com.lastbastion.game.player;

import java.util.Optional;

/**
 * 玩家聚合根的持久化接口。实现类可以是内存、本地磁盘、Redis、MySQL。
 *
 * <p>键使用 {@code externalId}（第三方登录 ID / 设备 ID），由 SessionRegistry 负责传入。</p>
 */
public interface PlayerStore {

    /** 按 externalId 加载，返回已恢复的 PlayerContext；若不存在则 {@link Optional#empty()}。 */
    Optional<PlayerContext> load(String externalId);

    /** 写入或覆盖指定 externalId 下的快照。 */
    void save(String externalId, PlayerContext ctx);

    /**
     * 当前已知 externalId 数量（便于运维探测）。实现可选；默认返回 -1 表示未知。
     */
    default int size() { return -1; }
}
