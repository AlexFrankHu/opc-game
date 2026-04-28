package com.lastbastion.game.onboarding;

import com.lastbastion.common.CurrencyType;

/**
 * TASK-010 §10.3 主线任务（前 3 天）。
 */
public final class DailyQuest {

    public int id;
    public int day;
    public String description;
    public String conditionKey;
    public long targetValue;
    public CurrencyType rewardCurrency;
    public long rewardAmount;

    public DailyQuest() {
    }

    public DailyQuest(int id, int day, String description, String conditionKey, long targetValue,
                      CurrencyType rewardCurrency, long rewardAmount) {
        this.id = id;
        this.day = day;
        this.description = description;
        this.conditionKey = conditionKey;
        this.targetValue = targetValue;
        this.rewardCurrency = rewardCurrency;
        this.rewardAmount = rewardAmount;
    }
}
