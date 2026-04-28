package com.lastbastion.game.player;

import com.lastbastion.common.CurrencyType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个玩家的运行时聚合根。生产环境中会由数据库/缓存持久化。
 */
public final class PlayerContext {

    private final long playerId;
    private String nickname;
    private long registerTimestamp;
    private long lastLogoutTimestamp;
    private long lastLoginTimestamp;
    private int zoneProgressChapter = 1;
    private int zoneProgressStage = 0; // 0 means no stage cleared yet
    private int highestPowerRating = 0;
    private long totalSpentCents = 0;
    private boolean battlePassActive = false;

    private final EnumMap<CurrencyType, Long> currencies = new EnumMap<>(CurrencyType.class);
    private final Map<Integer, Long> items = new HashMap<>();
    /** inventory slot id -> gear instance */
    private final Map<Long, com.lastbastion.game.gear.GearInstance> gearBag = new LinkedHashMap<>();
    private final Map<Long, com.lastbastion.game.augment.AugmentInstance> augmentBag = new LinkedHashMap<>();
    private final Map<Long, com.lastbastion.game.survivor.SurvivorInstance> survivors = new LinkedHashMap<>();
    /** survivor shards by config id */
    private final Map<String, Long> survivorShards = new HashMap<>();

    private final com.lastbastion.game.arena.ArenaState arenaState = new com.lastbastion.game.arena.ArenaState();
    private final com.lastbastion.game.monetization.BattlePassState battlePassState =
            new com.lastbastion.game.monetization.BattlePassState();
    private final com.lastbastion.game.monetization.StarterPackState starterPackState =
            new com.lastbastion.game.monetization.StarterPackState();
    private final com.lastbastion.game.onboarding.OnboardingState onboardingState =
            new com.lastbastion.game.onboarding.OnboardingState();

    private final long[] activeTeamSurvivors = new long[5];
    private final long[] defenseTeamSurvivors = new long[5];

    public PlayerContext(long playerId, String nickname) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.registerTimestamp = System.currentTimeMillis();
        for (CurrencyType c : CurrencyType.values()) currencies.put(c, 0L);
    }

    public long playerId() { return playerId; }
    public String nickname() { return nickname; }
    public void setNickname(String n) { this.nickname = n; }

    public long registerTimestamp() { return registerTimestamp; }
    public long lastLogoutTimestamp() { return lastLogoutTimestamp; }
    public void setLastLogoutTimestamp(long ts) { this.lastLogoutTimestamp = ts; }
    public long lastLoginTimestamp() { return lastLoginTimestamp; }
    public void setLastLoginTimestamp(long ts) { this.lastLoginTimestamp = ts; }

    public int zoneProgressChapter() { return zoneProgressChapter; }
    public int zoneProgressStage() { return zoneProgressStage; }
    public void setZoneProgress(int chapter, int stage) {
        this.zoneProgressChapter = chapter;
        this.zoneProgressStage = stage;
    }

    public int highestPowerRating() { return highestPowerRating; }
    public void setHighestPowerRating(int p) { this.highestPowerRating = p; }

    public long totalSpentCents() { return totalSpentCents; }
    public void addSpentCents(long cents) { this.totalSpentCents += cents; }

    public boolean battlePassActive() { return battlePassActive; }
    public void setBattlePassActive(boolean v) { this.battlePassActive = v; }

    public EnumMap<CurrencyType, Long> currencies() { return currencies; }
    public Map<Integer, Long> items() { return items; }
    public Map<Long, com.lastbastion.game.gear.GearInstance> gearBag() { return gearBag; }
    public Map<Long, com.lastbastion.game.augment.AugmentInstance> augmentBag() { return augmentBag; }
    public Map<Long, com.lastbastion.game.survivor.SurvivorInstance> survivors() { return survivors; }
    public Map<String, Long> survivorShards() { return survivorShards; }

    public com.lastbastion.game.arena.ArenaState arenaState() { return arenaState; }
    public com.lastbastion.game.monetization.BattlePassState battlePassState() { return battlePassState; }
    public com.lastbastion.game.monetization.StarterPackState starterPackState() { return starterPackState; }
    public com.lastbastion.game.onboarding.OnboardingState onboardingState() { return onboardingState; }

    public long[] activeTeam() { return activeTeamSurvivors; }
    public long[] defenseTeam() { return defenseTeamSurvivors; }
}
