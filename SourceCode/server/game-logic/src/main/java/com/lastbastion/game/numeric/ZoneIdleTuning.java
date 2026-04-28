package com.lastbastion.game.numeric;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 离线挂机（zone_idle.json）。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class ZoneIdleTuning {
    public int idleCapHours;
    public int idleCapPremiumHours;
    public long fightsPerHour;

    public long idleCapMs() { return idleCapHours * 3600L * 1000L; }
    public long idleCapPremiumMs() { return idleCapPremiumHours * 3600L * 1000L; }
}
