package com.lastbastion.game.monetization;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TASK-009 §9.1 Battle Pass.
 */
public final class BattlePassService {

    public static final long BUY_PRICE_CENTS = 499;
    public static final long BUY_PLUS_PRICE_CENTS = 999;
    public static final long CATCHUP_COST_CHIPS = 80;

    private final BattlePassConfig cfg;
    private final ResourceService resource;
    private final AnalyticsService analytics;

    public BattlePassService(BattlePassConfig cfg, ResourceService resource, AnalyticsService analytics) {
        this.cfg = cfg;
        this.resource = resource;
        this.analytics = analytics;
    }

    public void startSeason(PlayerContext ctx, long nowMs) {
        BattlePassState s = ctx.battlePassState();
        s.setSeasonId(cfg.seasonId);
        s.setSeasonWindow(nowMs, nowMs + cfg.seasonDurationMs);
        s.setLevel(0);
        s.setXp(0);
        s.setPremiumActive(false);
        s.setPremiumPlusActive(false);
        s.freeClaimed().clear();
        s.premiumClaimed().clear();
    }

    /**
     * 加赛季经验。自动推进 level。 Premium+ 多获 20% XP。
     */
    public synchronized void gainXp(PlayerContext ctx, long xp, SourceTag source) {
        BattlePassState s = ctx.battlePassState();
        long scaled = xp;
        if (s.premiumPlusActive()) scaled = Math.round(xp * 1.2);
        s.setXp(s.xp() + scaled);
        while (s.level() < BattlePassState.MAX_LEVEL
                && s.xp() >= cfg.xpCurve[s.level() + 1]) {
            s.setLevel(s.level() + 1);
        }
    }

    /** 购买 Battle Pass (Premium Track)。 */
    public synchronized void buyPremium(PlayerContext ctx, String orderId) {
        BattlePassState s = ctx.battlePassState();
        s.setPremiumActive(true);
        ctx.setBattlePassActive(true);
        ctx.addSpentCents(BUY_PRICE_CENTS);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("battlepass_purchase")
                    .prop("player_id", ctx.playerId())
                    .prop("product_id", "BATTLE_PASS_PREMIUM")
                    .prop("price_cents", BUY_PRICE_CENTS)
                    .prop("order_id", orderId)
                    .build());
        }
    }

    public synchronized void buyPremiumPlus(PlayerContext ctx, String orderId) {
        BattlePassState s = ctx.battlePassState();
        s.setPremiumActive(true);
        s.setPremiumPlusActive(true);
        ctx.setBattlePassActive(true);
        ctx.addSpentCents(BUY_PLUS_PRICE_CENTS);
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("battlepass_purchase")
                    .prop("player_id", ctx.playerId())
                    .prop("product_id", "BATTLE_PASS_PREMIUM_PLUS")
                    .prop("price_cents", BUY_PLUS_PRICE_CENTS)
                    .prop("order_id", orderId)
                    .build());
        }
    }

    /**
     * 领取指定等级的奖励。逐级领取，不跳级。
     */
    public synchronized List<BattlePassConfig.Reward> claim(PlayerContext ctx, int level, boolean premiumSide) {
        BattlePassState s = ctx.battlePassState();
        if (level < 1 || level > BattlePassState.MAX_LEVEL) throw new GameException(ErrorCode.ILLEGAL_ARG);
        if (level > s.level()) throw new GameException(ErrorCode.ILLEGAL_ARG, "level not reached");
        java.util.BitSet claimed = premiumSide ? s.premiumClaimed() : s.freeClaimed();
        if (claimed.get(level)) throw new GameException(ErrorCode.PASS_ALREADY_CLAIMED);
        // enforce逐级：要求前面的都已领
        for (int lv = 1; lv < level; lv++) {
            if (!claimed.get(lv)) throw new GameException(ErrorCode.ILLEGAL_ARG,
                    "must claim level " + lv + " first");
        }
        if (premiumSide && !s.premiumActive()) throw new GameException(ErrorCode.PASS_NOT_ACTIVE);

        Map<Integer, List<BattlePassConfig.Reward>> byLevel =
                premiumSide ? cfg.premiumByLevel() : cfg.freeByLevel();
        List<BattlePassConfig.Reward> rewards = byLevel.getOrDefault(level, new ArrayList<>());
        claimed.set(level);

        for (BattlePassConfig.Reward r : rewards) {
            if (r.kind == BattlePassConfig.RewardKind.CURRENCY) {
                resource.add(ctx, r.currency, r.amount, SourceTag.BATTLE_PASS_CLAIM);
            }
            // ITEM/GEAR_BOX/SURVIVOR_SHARD handled by upstream reward expander
        }
        if (analytics != null) {
            analytics.emit(AnalyticsEvent.of("battlepass_claim")
                    .prop("player_id", ctx.playerId())
                    .prop("level", level)
                    .prop("track", premiumSide ? "premium" : "free")
                    .build());
        }
        return rewards;
    }

    /** 赛季结束时自动结算未领奖励（默认自动领 free track）。 */
    public synchronized void settleExpiredSeason(PlayerContext ctx, long nowMs) {
        BattlePassState s = ctx.battlePassState();
        if (nowMs < s.seasonEndMs()) return;
        for (int lv = 1; lv <= s.level(); lv++) {
            if (!s.freeClaimed().get(lv)) claim(ctx, lv, false);
            if (s.premiumActive() && !s.premiumClaimed().get(lv)) claim(ctx, lv, true);
        }
    }

    /** 补签（购买过去等级奖励）。消耗 Premium Chips。 */
    public synchronized void catchup(PlayerContext ctx, int toLevel) {
        BattlePassState s = ctx.battlePassState();
        if (toLevel <= s.level()) throw new GameException(ErrorCode.ILLEGAL_ARG);
        int diff = toLevel - s.level();
        long cost = CATCHUP_COST_CHIPS * diff;
        resource.spend(ctx, CurrencyType.PREMIUM_CHIPS, cost, SourceTag.BATTLE_PASS_BUY);
        s.setLevel(toLevel);
        if (s.xp() < cfg.xpCurve[toLevel]) s.setXp(cfg.xpCurve[toLevel]);
    }
}
