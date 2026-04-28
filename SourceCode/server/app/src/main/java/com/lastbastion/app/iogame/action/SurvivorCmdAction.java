package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.GachaPullReq;
import com.lastbastion.app.iogame.msg.GachaPullResp;
import com.lastbastion.app.iogame.msg.Messages.SurvivorLevelUpReq;
import com.lastbastion.app.iogame.msg.Messages.SurvivorLevelUpResp;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.survivor.GachaService;
import com.lastbastion.game.survivor.SurvivorInstance;

import java.util.List;

@ActionController(ActionRegistry.CMD_SURVIVOR)
public final class SurvivorCmdAction {

    @ActionMethod(1) // subCmd 1 = survivor.levelUp
    public SurvivorLevelUpResp levelUp(SurvivorLevelUpReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        int newLevel = ServiceRegistry.services().survivor.levelUp(
                ctx, req.instanceId, req.small, req.medium, req.large);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        SurvivorLevelUpResp resp = new SurvivorLevelUpResp();
        resp.instanceId = req.instanceId;
        resp.level = newLevel;
        SurvivorInstance inst = ctx.survivors().get(req.instanceId);
        resp.star = inst == null ? 1 : inst.star();
        return resp;
    }

    @ActionMethod(4) // subCmd 4 = survivor.pullGacha
    public GachaPullResp pullGacha(GachaPullReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        GachaService.Pool pool = GachaService.Pool.valueOf(
                req.pool == null ? "FREE" : req.pool.toUpperCase());
        int count = Math.max(1, req.count);
        List<GachaService.Result> rs = ServiceRegistry.services().gacha.pull(ctx, pool, count);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);

        GachaPullResp resp = new GachaPullResp();
        for (GachaService.Result r : rs) {
            GachaPullResp.Entry e = new GachaPullResp.Entry();
            e.configId = r.configId;
            e.rarity = r.rarity.name();
            e.duplicate = r.duplicate;
            e.shardsAdded = r.shardsAdded;
            resp.results.add(e);
        }
        return resp;
    }
}
