package com.lastbastion.game.monetization;

import java.util.BitSet;

/**
 * 玩家本赛季 Battle Pass 状态（TASK-009）。
 */
public final class BattlePassState {

    public static final int MAX_LEVEL = 50;

    private int seasonId = 1;
    private long seasonStartMs;
    private long seasonEndMs;
    private long xp;
    private int level = 0;
    private boolean premiumActive = false;
    private boolean premiumPlusActive = false;
    private final BitSet freeClaimed = new BitSet(MAX_LEVEL + 1);
    private final BitSet premiumClaimed = new BitSet(MAX_LEVEL + 1);

    public int seasonId() { return seasonId; }
    public void setSeasonId(int id) { this.seasonId = id; }

    public long seasonStartMs() { return seasonStartMs; }
    public long seasonEndMs() { return seasonEndMs; }
    public void setSeasonWindow(long start, long end) {
        this.seasonStartMs = start;
        this.seasonEndMs = end;
    }

    public long xp() { return xp; }
    public void setXp(long xp) { this.xp = xp; }

    public int level() { return level; }
    public void setLevel(int l) { this.level = l; }

    public boolean premiumActive() { return premiumActive; }
    public void setPremiumActive(boolean v) { this.premiumActive = v; }

    public boolean premiumPlusActive() { return premiumPlusActive; }
    public void setPremiumPlusActive(boolean v) { this.premiumPlusActive = v; }

    public BitSet freeClaimed() { return freeClaimed; }
    public BitSet premiumClaimed() { return premiumClaimed; }
}
