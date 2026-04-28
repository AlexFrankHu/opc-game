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
        // user
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_USER, 1), "user.login");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_USER, 2), "user.heartbeat");
        // survivor
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_SURVIVOR, 1), "survivor.levelUp");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_SURVIVOR, 4), "survivor.pullGacha");
        // zone
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ZONE, 1), "zone.clear");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ZONE, 3), "zone.settleIdle");
        // arena
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ARENA, 1), "arena.match");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ARENA, 2), "arena.challenge");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ARENA, 3), "arena.leaderboard");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ARENA, 4), "arena.buyChallenge");
        // battle pass
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_BATTLE_PASS, 1), "bp.claim");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_BATTLE_PASS, 2), "bp.buy");
        // onboarding
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ONBOARDING, 1), "onboarding.completeStep");
        assertNotNull(regions.getActionCommand(ActionRegistry.CMD_ONBOARDING, 2), "onboarding.skip");

        assertTrue(regions.listCmdMerge().size() >= 6, "at least 6 cmd groups should exist");
    }

    @Test
    void nativeActionsCoverAllLegacyGatewayActions() {
        GameLogicStartup startup = new GameLogicStartup();
        ActionCommandRegions regions = startup.createBarSkeleton().getActionCommandRegions();

        // 名称 -> 期望的 cmd,subCmd（必须出现在 ActionRegistry 中，且要在 BarSkeleton 里注册）
        String[][] expected = {
                {"user.login",              String.valueOf(ActionRegistry.CMD_USER), "1"},
                {"user.heartbeat",          String.valueOf(ActionRegistry.CMD_USER), "2"},
                {"survivor.levelUp",        String.valueOf(ActionRegistry.CMD_SURVIVOR), "1"},
                {"survivor.pullGacha",      String.valueOf(ActionRegistry.CMD_SURVIVOR), "4"},
                {"zone.clear",              String.valueOf(ActionRegistry.CMD_ZONE), "1"},
                {"zone.settleIdle",         String.valueOf(ActionRegistry.CMD_ZONE), "3"},
                {"arena.match",             String.valueOf(ActionRegistry.CMD_ARENA), "1"},
                {"arena.challenge",         String.valueOf(ActionRegistry.CMD_ARENA), "2"},
                {"arena.leaderboard",       String.valueOf(ActionRegistry.CMD_ARENA), "3"},
                {"arena.buyChallenge",      String.valueOf(ActionRegistry.CMD_ARENA), "4"},
                {"bp.claim",                String.valueOf(ActionRegistry.CMD_BATTLE_PASS), "1"},
                {"bp.buy",                  String.valueOf(ActionRegistry.CMD_BATTLE_PASS), "2"},
                {"onboarding.completeStep", String.valueOf(ActionRegistry.CMD_ONBOARDING), "1"},
                {"onboarding.skip",         String.valueOf(ActionRegistry.CMD_ONBOARDING), "2"},
        };
        for (String[] e : expected) {
            int cmd = Integer.parseInt(e[1]);
            int sub = Integer.parseInt(e[2]);
            assertNotNull(regions.getActionCommand(cmd, sub),
                    e[0] + " should be registered at cmd=" + cmd + " subCmd=" + sub);
            assertTrue(ActionRegistry.ALL.containsKey(e[0]),
                    e[0] + " should be mirrored in ActionRegistry.ALL");
        }
    }
}
