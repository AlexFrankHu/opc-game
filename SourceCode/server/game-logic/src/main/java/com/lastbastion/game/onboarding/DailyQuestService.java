package com.lastbastion.game.onboarding;

import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.numeric.DailyQuestsTuning;
import com.lastbastion.game.numeric.NumericConfig;
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
        this(resource, NumericConfig.defaults().dailyQuests());
    }

    public DailyQuestService(ResourceService resource, DailyQuestsTuning tuning) {
        this.resource = resource;
        seedFromTuning(tuning);
    }

    private void seedFromTuning(DailyQuestsTuning tuning) {
        for (DailyQuestsTuning.QuestEntry e : tuning.quests) {
            register(new DailyQuest(e.id, e.day, e.description, e.conditionKey,
                    e.targetValue, e.rewardCurrency, e.rewardAmount));
        }
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

}
