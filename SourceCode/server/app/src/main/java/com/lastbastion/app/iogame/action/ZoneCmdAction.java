package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.Messages;
import com.lastbastion.app.iogame.msg.Messages.ZoneSettleIdleReq;
import com.lastbastion.app.iogame.msg.Messages.ZoneSettleIdleResp;
import com.lastbastion.app.iogame.msg.ZoneClearReq;
import com.lastbastion.app.iogame.msg.ZoneClearResp;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.zone.ZoneService;

@ActionController(ActionRegistry.CMD_ZONE)
public final class ZoneCmdAction {

    @ActionMethod(1) // subCmd 1 = zone.clear
    public ZoneClearResp clear(ZoneClearReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        ZoneService.AttemptResult r = ServiceRegistry.services().zone
                .clear(ctx, req.chapter, req.stage, req.allyWon);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        ZoneClearResp resp = new ZoneClearResp();
        resp.won = r.won;
        resp.chapter = r.chapterId;
        resp.stage = r.stageId;
        resp.rewards.putAll(r.rewards);
        return resp;
    }

    @ActionMethod(3) // subCmd 3 = zone.settleIdle
    public ZoneSettleIdleResp settleIdle(ZoneSettleIdleReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        ZoneService.IdleReward r = ServiceRegistry.services().zone.settleIdle(ctx, System.currentTimeMillis());
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        ZoneSettleIdleResp resp = new ZoneSettleIdleResp();
        resp.elapsedMs = r.elapsedMs;
        resp.effectiveMs = r.effectiveMs;
        resp.currency = Messages.stringKeys(r.currency);
        return resp;
    }
}
