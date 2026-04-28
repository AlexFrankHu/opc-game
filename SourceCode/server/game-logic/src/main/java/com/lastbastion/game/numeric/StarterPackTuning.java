package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Starter Pack（starter_pack.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class StarterPackTuning {
    public long priceCents;
    public long rewardPremiumChips;
    public long rewardRecruitTokens;
    public int triggerChapter;
    public int triggerStage;
}
