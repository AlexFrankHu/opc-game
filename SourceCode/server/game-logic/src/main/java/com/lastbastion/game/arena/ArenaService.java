package com.lastbastion.game.arena;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.numeric.ArenaTuning;
import com.lastbastion.game.numeric.NumericConfig;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TASK-007 Arena.
 *
 * 排行榜以内存 TreeMap 存储（key=rank, value=playerId）；
 * 可由调用方替换为 Redis ZSET。匹配 ±15% 战力 + 从排名高处抽样。
 */
public final class ArenaService {

    private final ResourceService resource;
    private final AnalyticsService analytics;
    private final Random rng;
    private final ArenaTuning tuning;

    /** playerId -> record (rank stays implicit via sorted map) */
    private final Map<Long, ArenaRoster> rosters = new ConcurrentHashMap<>();
    /** rank (1-based) -> playerId */
    private final TreeMap<Integer, Long> leaderboard = new TreeMap<>();

    public ArenaService(ResourceService resource, AnalyticsService analytics, Random rng) {
        this(resource, analytics, rng, NumericConfig.defaults().arena());
    }

    public ArenaService(ResourceService resource, AnalyticsService analytics, Random rng, ArenaTuning tuning) {
        this.resource = resource;
        this.analytics = analytics;
        this.rng = rng;
        this.tuning = tuning;
    }

    public ArenaTuning tuning() { return tuning; }
    public int dailyFreeChallenges() { return tuning.dailyFreeChallenges; }
    public int dailyBuyLimit() { return tuning.dailyBuyLimit; }
    public long buyCostChips() { return tuning.buyCostChips; }

    public synchronized void registerOrUpdate(PlayerContext ctx, int power, long[] defenseTeam) {
        ArenaRoster existing = rosters.get(ctx.playerId());
        ArenaRoster r = existing != null ? existing
                : new ArenaRoster(ctx.playerId(), ctx.nickname(), power, defenseTeam, nextFreshRank());
        r.nickname = ctx.nickname();
        r.power = power;
        r.defenseTeam = defenseTeam.clone();
        rosters.put(ctx.playerId(), r);
        if (existing == null) {
            leaderboard.put(r.rank, r.playerId);
            ctx.arenaState().setRank(r.rank);
        }
    }

    private int nextFreshRank() {
        return leaderboard.isEmpty() ? 1 : leaderboard.lastKey() + 1;
    }

    /** 返回 3 个候选对手。 */
    public synchronized List<ArenaRoster> match(PlayerContext ctx) {
        ArenaRoster me = rosters.get(ctx.playerId());
        if (me == null) throw new GameException(ErrorCode.NOT_FOUND, "not registered in arena");
        double low = me.power * tuning.matchPowerWindowLow;
        double high = me.power * tuning.matchPowerWindowHigh;
        List<ArenaRoster> pool = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : leaderboard.entrySet()) {
            if (e.getValue().equals(me.playerId)) continue;
            if (e.getKey() >= me.rank) continue; // only higher ranks
            ArenaRoster r = rosters.get(e.getValue());
            if (r == null) continue;
            if (r.power >= low && r.power <= high) pool.add(r);
        }
        pool.sort(Comparator.comparingInt(r -> r.rank));
        List<ArenaRoster> out = new ArrayList<>();
        if (pool.isEmpty()) return out;
        // sample 3 uniformly
        List<ArenaRoster> shuffle = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffle, rng);
        for (int i = 0; i < Math.min(tuning.matchPoolSize, shuffle.size()); i++) out.add(shuffle.get(i));
        return out;
    }

    public synchronized void rolloverDaily(PlayerContext ctx, long nowMs) {
        long today = nowMs / (24L * 3600 * 1000);
        if (ctx.arenaState().lastResetDay() != today) {
            ctx.arenaState().setDailyFreeLeft(tuning.dailyFreeChallenges);
            ctx.arenaState().setDailyBoughtToday(0);
            ctx.arenaState().setLastResetDay(today);
        }
    }

    /** 购买额外挑战次数。 */
    public synchronized void buyChallenge(PlayerContext ctx) {
        if (ctx.arenaState().dailyBoughtToday() >= tuning.dailyBuyLimit) {
            throw new GameException(ErrorCode.ARENA_BUY_LIMIT);
        }
        resource.spend(ctx, CurrencyType.PREMIUM_CHIPS, tuning.buyCostChips, SourceTag.ARENA_DAILY);
        ctx.arenaState().setDailyBoughtToday(ctx.arenaState().dailyBoughtToday() + 1);
        ctx.arenaState().setDailyFreeLeft(ctx.arenaState().dailyFreeLeft() + 1);
    }

    /**
     * 挑战：调用方传入战斗结果。胜负分 = +25 / -15（败者保护排名不掉）。
     */
    public synchronized ArenaRecord challenge(PlayerContext ctx, long opponentId, boolean allyWon) {
        if (ctx.arenaState().dailyFreeLeft() <= 0) throw new GameException(ErrorCode.ARENA_DAILY_LIMIT);
        ArenaRoster me = rosters.get(ctx.playerId());
        ArenaRoster opp = rosters.get(opponentId);
        if (me == null || opp == null) throw new GameException(ErrorCode.NOT_FOUND);
        ctx.arenaState().setDailyFreeLeft(ctx.arenaState().dailyFreeLeft() - 1);

        int rankBefore = me.rank;
        int scoreDelta;
        if (allyWon && me.rank > opp.rank) {
            // swap ranks (挑战者 rank 数字低优先)
            int myRank = me.rank;
            int oppRank = opp.rank;
            leaderboard.put(oppRank, me.playerId);
            leaderboard.put(myRank, opp.playerId);
            me.rank = oppRank;
            opp.rank = myRank;
            scoreDelta = tuning.scoreWinSwap;
            me.score += tuning.scoreWinSwap;
            opp.score = Math.max(0, opp.score + tuning.scoreLossOpponentOnSwap);
        } else if (allyWon) {
            scoreDelta = tuning.scoreWinNoSwap;
            me.score += tuning.scoreWinNoSwap;
        } else {
            scoreDelta = tuning.scoreLossSelf;
            me.score = Math.max(0, me.score + tuning.scoreLossSelf);
        }
        ctx.arenaState().setRank(me.rank);
        ctx.arenaState().setScore(me.score);

        ArenaRecord rec = new ArenaRecord(opp.playerId, opp.nickname, allyWon,
                rankBefore, me.rank, scoreDelta, System.currentTimeMillis());
        ctx.arenaState().addRecord(rec);

        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("arena_challenge")
                    .prop("player_id", ctx.playerId())
                    .prop("opponent_id", opponentId)
                    .prop("result", allyWon ? "win" : "lose")
                    .prop("rank_after", me.rank)
                    .build());
        }
        return rec;
    }

    /** 取 top N 排行榜。 */
    public synchronized List<ArenaRoster> topRanks(int n) {
        List<ArenaRoster> out = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : leaderboard.entrySet()) {
            if (out.size() >= n) break;
            ArenaRoster r = rosters.get(e.getValue());
            if (r != null) out.add(r);
        }
        return out;
    }

    /** 每日结算根据排名发放奖励。 */
    public void dailyRankReward(PlayerContext ctx) {
        ArenaRoster r = rosters.get(ctx.playerId());
        if (r == null) return;
        long credits;
        long techCores = 0;
        long tokens = 0;
        if (r.rank == 1) { credits = 20000; techCores = 100; tokens = 5; }
        else if (r.rank <= 10) { credits = 10000; techCores = 60; tokens = 3; }
        else if (r.rank <= 100) { credits = 5000; techCores = 30; tokens = 2; }
        else if (r.rank <= 1000) { credits = 2000; techCores = 15; tokens = 1; }
        else { credits = 500; }
        resource.add(ctx, CurrencyType.CREDITS, credits, SourceTag.ARENA_REWARD);
        if (techCores > 0) resource.add(ctx, CurrencyType.TECH_CORES, techCores, SourceTag.ARENA_REWARD);
        if (tokens > 0) resource.add(ctx, CurrencyType.RECRUIT_TOKENS, tokens, SourceTag.ARENA_REWARD);
    }

    public static final class ArenaRoster {
        public final long playerId;
        public String nickname;
        public int power;
        public long[] defenseTeam;
        public int rank;
        public int score = 1000;

        public ArenaRoster(long playerId, String nickname, int power, long[] defenseTeam, int rank) {
            this.playerId = playerId;
            this.nickname = nickname;
            this.power = power;
            this.defenseTeam = defenseTeam;
            this.rank = rank;
        }
    }

    public Map<Long, ArenaRoster> rosters() {
        return rosters;
    }
}
