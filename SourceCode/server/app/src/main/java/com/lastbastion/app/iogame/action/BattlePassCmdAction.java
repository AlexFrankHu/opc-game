package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.Messages.BpBuyReq;
import com.lastbastion.app.iogame.msg.Messages.BpBuyResp;
import com.lastbastion.app.iogame.msg.Messages.BpClaimReq;
import com.lastbastion.app.iogame.msg.Messages.BpClaimResp;
import com.lastbastion.game.monetization.BattlePassConfig;
import com.lastbastion.game.monetization.BattlePassState;
import com.lastbastion.game.player.PlayerContext;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ActionController(ActionRegistry.CMD_BATTLE_PASS)
public final class BattlePassCmdAction {

    @ActionMethod(1) // bp.claim
    public BpClaimResp claim(BpClaimReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        List<BattlePassConfig.Reward> rewards = ServiceRegistry.services().battlePass
                .claim(ctx, req.level, req.premium);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);

        BattlePassState st = ctx.battlePassState();
        BpClaimResp resp = new BpClaimResp();
        resp.level = st.level();
        resp.xp = st.xp();
        resp.premiumActive = st.premiumActive();
        resp.freeClaimed = toLevelList(st.freeClaimed());
        resp.premiumClaimed = toLevelList(st.premiumClaimed());
        for (BattlePassConfig.Reward r : rewards) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", r.kind.name());
            m.put("currency", r.currency == null ? null : r.currency.name());
            m.put("amount", r.amount);
            m.put("payload", r.payload);
            resp.rewards.add(m);
        }
        return resp;
    }

    @ActionMethod(2) // bp.buy
    public BpBuyResp buy(BpBuyReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        ServiceRegistry.services().battlePass.buyPremium(ctx, req.orderId);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        BpBuyResp resp = new BpBuyResp();
        resp.premiumActive = ctx.battlePassState().premiumActive();
        return resp;
    }

    private static List<Integer> toLevelList(BitSet bs) {
        List<Integer> out = new java.util.ArrayList<>(bs.cardinality());
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) out.add(i);
        return out;
    }
}
