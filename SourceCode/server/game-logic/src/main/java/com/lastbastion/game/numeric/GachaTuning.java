package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

/** 抽卡（gacha.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class GachaTuning {
    public Map<Rarity, Double> rates = new LinkedHashMap<>();
    public int pityLimit;
    public long singleCostToken;
    public long tenCostToken;
    public long singleCostChip;
    public long tenCostChip;
    public long shardsPerDuplicate;
}
