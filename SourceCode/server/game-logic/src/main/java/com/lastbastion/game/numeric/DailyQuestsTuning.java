package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lastbastion.common.CurrencyType;

import java.util.ArrayList;
import java.util.List;

/** 前 3 日任务（daily_quests.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class DailyQuestsTuning {
    public List<QuestEntry> quests = new ArrayList<>();

    public static final class QuestEntry {
        public int id;
        public int day;
        public String description;
        public String conditionKey;
        public long targetValue;
        public CurrencyType rewardCurrency;
        public long rewardAmount;
    }
}
