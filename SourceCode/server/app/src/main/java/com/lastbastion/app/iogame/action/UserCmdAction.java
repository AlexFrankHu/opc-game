package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.auth.AuthService;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.LoginMsg;
import com.lastbastion.app.iogame.msg.LoginResp;
import com.lastbastion.app.iogame.msg.Messages.HeartbeatResp;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
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
        AuthService auth = ServiceRegistry.auth();
        AuthService.Result result = auth.verify(
                extId,
                msg == null ? null : msg.deviceId,
                msg == null ? 0L : msg.ts,
                msg == null ? null : msg.sig);
        if (result != AuthService.Result.OK) {
            throw new GameException(ErrorCode.UNAUTHENTICATED, result.name());
        }
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate(extId);
        LoginResp r = new LoginResp();
        r.playerId = ctx.playerId();
        r.externalId = extId;
        r.registerTimestamp = ctx.registerTimestamp();
        r.authStatus = auth.isEnforced() ? "OK" : "OPEN_MODE";
        return r;
    }

    @ActionMethod(2) // subCmd 2 = user.heartbeat
    public HeartbeatResp heartbeat() {
        HeartbeatResp r = new HeartbeatResp();
        r.t = System.currentTimeMillis();
        return r;
    }
}
