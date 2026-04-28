package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.LoginMsg;
import com.lastbastion.app.iogame.msg.LoginResp;
import com.lastbastion.app.iogame.msg.Messages.HeartbeatResp;
import com.lastbastion.game.player.PlayerContext;

/**
 * ioGame 原生 Action：用户相关命令。
 * 主路由 {@link ActionRegistry#CMD_USER}；子路由按 {@link ActionRegistry} 约定。
 */
@ActionController(ActionRegistry.CMD_USER)
public final class UserCmdAction {

    @ActionMethod(1) // subCmd 1 = user.login
    public LoginResp login(LoginMsg msg) {
        String extId = (msg == null || msg.userId == null || msg.userId.isBlank())
                ? "anon-" + System.nanoTime() : msg.userId;
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate(extId);
        LoginResp r = new LoginResp();
        r.playerId = ctx.playerId();
        r.externalId = extId;
        r.registerTimestamp = ctx.registerTimestamp();
        return r;
    }

    @ActionMethod(2) // subCmd 2 = user.heartbeat
    public HeartbeatResp heartbeat() {
        HeartbeatResp r = new HeartbeatResp();
        r.t = System.currentTimeMillis();
        return r;
    }
}
