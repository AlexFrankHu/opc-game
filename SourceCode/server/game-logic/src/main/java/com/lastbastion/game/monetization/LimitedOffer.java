package com.lastbastion.game.monetization;

import com.lastbastion.common.CurrencyType;

import java.util.ArrayList;
import java.util.List;

/**
 * TASK-009 §9.3 限时礼包模板。
 */
public final class LimitedOffer {

    public String id;
    public String displayName;
    public long priceCents;
    public long startTimeMs;
    public long endTimeMs;
    /** per account purchase limit (0 = unlimited). */
    public int purchaseLimit;
    public List<Reward> rewards = new ArrayList<>();

    public static final class Reward {
        public CurrencyType currency;
        public String payload; // non-currency payload label
        public long amount;
    }

    public boolean active(long nowMs) {
        return nowMs >= startTimeMs && nowMs <= endTimeMs;
    }
}
