package com.lastbastion.app.iogame;

import com.iohao.game.action.skeleton.core.ActionCommand;
import com.iohao.game.action.skeleton.core.ActionCommandRegions;
import com.iohao.game.action.skeleton.core.BarSkeleton;
import com.lastbastion.app.ActionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 ioGame 逻辑服的 BarSkeleton 已经正确注册所有 Action 路由。
 * 不启动 Broker/External，避免占用端口与网络。
 */
class GameLogicStartupTest {

    @Test
    void barSkeletonContainsRegisteredActions() {
        GameLogicStartup startup = new GameLogicStartup();
        BarSkeleton skeleton = startup.createBarSkeleton();
        assertNotNull(skeleton, "skeleton must be built");

        ActionCommandRegions regions = skeleton.getActionCommandRegions();
        // user.login
        ActionCommand login = regions.getActionCommand(ActionRegistry.CMD_USER, 1);
        assertNotNull(login, "user.login should be registered");
        // survivor.pullGacha
        ActionCommand gacha = regions.getActionCommand(ActionRegistry.CMD_SURVIVOR, 4);
        assertNotNull(gacha, "survivor.pullGacha should be registered");
        // zone.clear
        ActionCommand zone = regions.getActionCommand(ActionRegistry.CMD_ZONE, 1);
        assertNotNull(zone, "zone.clear should be registered");

        assertTrue(regions.listCmdMerge().size() >= 3,
                "at least 3 cmd groups should exist");
    }
}
