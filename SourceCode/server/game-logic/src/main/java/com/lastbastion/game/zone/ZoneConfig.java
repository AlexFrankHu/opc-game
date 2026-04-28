package com.lastbastion.game.zone;

import java.util.ArrayList;
import java.util.List;

/**
 * Zone chapter + stages (TASK-006).
 */
public final class ZoneConfig {

    public int chapterId;
    public String chapterName;
    public int recommendedPower;
    public List<StageConfig> stages = new ArrayList<>();
    public List<DropEntry> drops = new ArrayList<>();
    public List<DropEntry> firstClearRewards = new ArrayList<>();

    public static final class StageConfig {
        public int stageId;
        public String label;
        public boolean elite;
        public boolean bossStage;
        public int recommendedPower;
        /** enemy squads in wave order (config ids). */
        public List<List<String>> waves = new ArrayList<>();
    }

    public static final class DropEntry {
        public String itemType; // CURRENCY_CREDITS / ITEM / GEAR_BOX_BLUE / SURVIVOR_SHARD etc.
        public String payload;
        public double probability;
        public long amount;
    }
}
