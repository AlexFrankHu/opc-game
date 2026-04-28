package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.CurrencyType;

import java.util.ArrayList;
import java.util.List;

/** 限时礼包（limited_offers.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class LimitedOffersTuning {
    public List<OfferEntry> offers = new ArrayList<>();

    public static final class OfferEntry {
        public String id;
        public String name;
        public long priceCents;
        public long startMs;
        public long endMs;
        public int purchaseLimit;
        public List<RewardEntry> rewards = new ArrayList<>();
    }

    public static final class RewardEntry {
        public CurrencyType currency;
        public long amount;
    }
}
