package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.Messages.ArenaBuyChallengeReq;
import com.lastbastion.app.iogame.msg.Messages.ArenaBuyChallengeResp;
import com.lastbastion.app.iogame.msg.Messages.ArenaChallengeReq;
import com.lastbastion.app.iogame.msg.Messages.ArenaChallengeResp;
import com.lastbastion.app.iogame.msg.Messages.ArenaLeaderboardReq;
import com.lastbastion.app.iogame.msg.Messages.ArenaLeaderboardResp;
import com.lastbastion.app.iogame.msg.Messages.ArenaMatchReq;
import com.lastbastion.app.iogame.msg.Messages.ArenaMatchResp;
import com.lastbastion.app.iogame.msg.Messages.ArenaRoster;
import com.lastbastion.game.arena.ArenaRecord;
import com.lastbastion.game.arena.ArenaService;
import com.lastbastion.game.player.PlayerContext;

import java.util.List;

@ActionController(ActionRegistry.CMD_ARENA)
public final class ArenaCmdAction {

    @ActionMethod(1) // arena.match
    public ArenaMatchResp match(ArenaMatchReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        List<ArenaService.ArenaRoster> candidates = ServiceRegistry.services().arena.match(ctx);
        ArenaMatchResp resp = new ArenaMatchResp();
        for (ArenaService.ArenaRoster c : candidates) resp.candidates.add(toMsg(c));
        return resp;
    }

    @ActionMethod(2) // arena.challenge
    public ArenaChallengeResp challenge(ArenaChallengeReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        int rankBefore = ctx.arenaState().rank();
        ArenaRecord rec = ServiceRegistry.services().arena.challenge(ctx, req.opponentId, req.won);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        ArenaChallengeResp resp = new ArenaChallengeResp();
        resp.opponentId = rec.opponentId;
        resp.allyWon = rec.won;
        resp.selfRankBefore = rankBefore;
        resp.selfRankAfter = rec.myRankAfter;
        resp.scoreDelta = rec.scoreDelta;
        return resp;
    }

    @ActionMethod(3) // arena.leaderboard
    public ArenaLeaderboardResp leaderboard(ArenaLeaderboardReq req) {
        int n = Math.min(100, Math.max(1, req.n));
        List<ArenaService.ArenaRoster> top = ServiceRegistry.services().arena.topRanks(n);
        ArenaLeaderboardResp resp = new ArenaLeaderboardResp();
        for (ArenaService.ArenaRoster c : top) resp.top.add(toMsg(c));
        return resp;
    }

    @ActionMethod(4) // arena.buyChallenge
    public ArenaBuyChallengeResp buyChallenge(ArenaBuyChallengeReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        ServiceRegistry.services().arena.buyChallenge(ctx);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        ArenaBuyChallengeResp resp = new ArenaBuyChallengeResp();
        resp.remainingBuys = ServiceRegistry.services().arena.dailyBuyLimit() - ctx.arenaState().dailyBoughtToday();
        return resp;
    }

    private static ArenaRoster toMsg(ArenaService.ArenaRoster src) {
        ArenaRoster m = new ArenaRoster();
        m.playerId = src.playerId;
        m.nickname = src.nickname;
        m.power = src.power;
        m.rank = src.rank;
        m.score = src.score;
        return m;
    }
}
