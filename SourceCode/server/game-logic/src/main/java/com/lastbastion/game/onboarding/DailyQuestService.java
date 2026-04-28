package com.lastbastion.game.onboarding;

import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ResourceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TASK-010 §10.3 前 3 天主线任务驱动。
 */
public final class DailyQuestService {

    private final List<DailyQuest> registry = new ArrayList<>();
    private final ResourceService resource;
    /** playerId -> (questId -> progress) */
    private final Map<Long, Map<Integer, Long>> progress = new HashMap<>();
    /** playerId -> set of claimed quest ids */
    private final Map<Long, java.util.Set<Integer>> claimed = new HashMap<>();

    public DailyQuestService(ResourceService resource) {
        this.resource = resource;
        seedDefaultFirstThreeDays();
    }

    public void register(DailyQuest q) {
        registry.add(q);
    }

    public List<DailyQuest> questsByDay(int day) {
        List<DailyQuest> out = new ArrayList<>();
        for (DailyQuest q : registry) if (q.day == day) out.add(q);
        return out;
    }

    public synchronized void recordEvent(PlayerContext ctx, String conditionKey, long delta) {
        Map<Integer, Long> p = progress.computeIfAbsent(ctx.playerId(), k -> new LinkedHashMap<>());
        for (DailyQuest q : registry) {
            if (!q.conditionKey.equals(conditionKey)) continue;
            p.merge(q.id, delta, Long::sum);
        }
    }

    public synchronized void claim(PlayerContext ctx, int questId) {
        DailyQuest q = registry.stream().filter(x -> x.id == questId).findFirst()
                .orElseThrow(() -> new GameException(ErrorCode.NOT_FOUND));
        Map<Integer, Long> p = progress.computeIfAbsent(ctx.playerId(), k -> new LinkedHashMap<>());
        long cur = p.getOrDefault(questId, 0L);
        if (cur < q.targetValue) throw new GameException(ErrorCode.ILLEGAL_ARG, "not completed");
        java.util.Set<Integer> cs = claimed.computeIfAbsent(ctx.playerId(), k -> new java.util.HashSet<>());
        if (!cs.add(questId)) throw new GameException(ErrorCode.PASS_ALREADY_CLAIMED, "quest already claimed");
        if (q.rewardCurrency != null && q.rewardAmount > 0) {
            resource.add(ctx, q.rewardCurrency, q.rewardAmount, SourceTag.MAIN_QUEST);
        }
    }

    public boolean completed(long playerId, int questId) {
        DailyQuest q = registry.stream().filter(x -> x.id == questId).findFirst().orElse(null);
        if (q == null) return false;
        long cur = progress.getOrDefault(playerId, new HashMap<>()).getOrDefault(questId, 0L);
        return cur >= q.targetValue;
    }

    public boolean isClaimed(long playerId, int questId) {
        return claimed.getOrDefault(playerId, new java.util.HashSet<>()).contains(questId);
    }

    private void seedDefaultFirstThreeDays() {
        // Day 1 (每天 5 个任务)
        register(new DailyQuest(10101, 1, "Clear Zone 1-3", "zone_complete:1:3", 1, CurrencyType.CREDITS, 1000));
        register(new DailyQuest(10102, 1, "Enhance any gear to +3", "gear_enhance_to:3", 1, CurrencyType.ALLOY, 200));
        register(new DailyQuest(10103, 1, "Complete 3 arena battles", "arena_challenge", 3, CurrencyType.CREDITS, 1500));
        register(new DailyQuest(10104, 1, "Level up a Survivor to 10", "survivor_level_to:10", 1, CurrencyType.RECRUIT_TOKENS, 1));
        register(new DailyQuest(10105, 1, "Claim a Battle Pass reward", "battlepass_claim", 1, CurrencyType.PREMIUM_CHIPS, 30));

        // Day 2
        register(new DailyQuest(10201, 2, "Clear Zone 2-5", "zone_complete:2:5", 1, CurrencyType.CREDITS, 3000));
        register(new DailyQuest(10202, 2, "Enhance any gear to +6", "gear_enhance_to:6", 1, CurrencyType.ALLOY, 400));
        register(new DailyQuest(10203, 2, "Win 5 arena battles", "arena_win", 5, CurrencyType.TECH_CORES, 30));
        register(new DailyQuest(10204, 2, "Level up 2 Survivors to 20", "survivor_level_to:20", 2, CurrencyType.RECRUIT_TOKENS, 2));
        register(new DailyQuest(10205, 2, "Fuse 3 augments to 2-star", "augment_fuse_to:2", 1, CurrencyType.CREDITS, 2500));

        // Day 3
        register(new DailyQuest(10301, 3, "Clear Zone 3 BOSS", "zone_complete:3:15", 1, CurrencyType.CREDITS, 5000));
        register(new DailyQuest(10302, 3, "Enhance any gear to +9", "gear_enhance_to:9", 1, CurrencyType.ALLOY, 800));
        register(new DailyQuest(10303, 3, "Reach arena rank top 1000", "arena_rank_top:1000", 1, CurrencyType.TECH_CORES, 60));
        register(new DailyQuest(10304, 3, "Recruit 10 times", "gacha_pull", 10, CurrencyType.RECRUIT_TOKENS, 5));
        register(new DailyQuest(10305, 3, "Star-up a Survivor to 2-star", "survivor_star_to:2", 1, CurrencyType.PREMIUM_CHIPS, 50));
    }
}
