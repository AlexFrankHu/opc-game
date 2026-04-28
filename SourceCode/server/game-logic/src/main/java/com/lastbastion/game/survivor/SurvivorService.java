package com.lastbastion.game.survivor;

import com.lastbastion.common.AttributeType;
import com.lastbastion.common.CurrencyType;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import com.lastbastion.common.IdGenerator;
import com.lastbastion.common.Stats;
import com.lastbastion.common.events.SourceTag;
import com.lastbastion.game.analytics.AnalyticsEvent;
import com.lastbastion.game.analytics.AnalyticsService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.resource.ItemService;
import com.lastbastion.game.resource.ResourceService;

import java.util.Map;

/**
 * TASK-003 Survivor 系统核心服务。
 */
public final class SurvivorService {

    /** item id for XP books (small/medium/large). */
    public static final int ITEM_XP_BOOK_SMALL = 2001;
    public static final int ITEM_XP_BOOK_MEDIUM = 2002;
    public static final int ITEM_XP_BOOK_LARGE = 2003;
    public static final int ITEM_SKILL_BOOK = 2101;
    public static final long XP_PER_BOOK_SMALL = 100;
    public static final long XP_PER_BOOK_MEDIUM = 500;
    public static final long XP_PER_BOOK_LARGE = 2500;

    private final SurvivorConfigRepository repo;
    private final ResourceService resourceService;
    private final ItemService itemService;
    private final AnalyticsService analytics;

    public SurvivorService(SurvivorConfigRepository repo, ResourceService resourceService,
                           ItemService itemService, AnalyticsService analytics) {
        this.repo = repo;
        this.resourceService = resourceService;
        this.itemService = itemService;
        this.analytics = analytics;
    }

    /** 赠送一个 Survivor 实例到背包（首次招募 / 引导奖励）。 */
    public SurvivorInstance grant(PlayerContext ctx, String configId) {
        SurvivorConfig cfg = repo.byId(configId);
        if (cfg == null) throw new GameException(ErrorCode.NOT_FOUND, "survivor cfg " + configId);
        SurvivorInstance inst = new SurvivorInstance(IdGenerator.next(), configId);
        ctx.survivors().put(inst.instanceId(), inst);
        emit(ctx, "survivor_grant", configId, 0);
        return inst;
    }

    /** 消耗经验书/经验值升级。 */
    public int levelUp(PlayerContext ctx, long survivorId, int xpBookSmall, int xpBookMedium, int xpBookLarge) {
        SurvivorInstance inst = requireSurvivor(ctx, survivorId);
        if (inst.level() >= LevelCurve.MAX_LEVEL_PHASE1) return inst.level();
        // consume books (validate all first)
        if (xpBookSmall > 0) itemService.consume(ctx, ITEM_XP_BOOK_SMALL, xpBookSmall, SourceTag.SYSTEM_GIFT);
        if (xpBookMedium > 0) itemService.consume(ctx, ITEM_XP_BOOK_MEDIUM, xpBookMedium, SourceTag.SYSTEM_GIFT);
        if (xpBookLarge > 0) itemService.consume(ctx, ITEM_XP_BOOK_LARGE, xpBookLarge, SourceTag.SYSTEM_GIFT);

        long xpGained = xpBookSmall * XP_PER_BOOK_SMALL
                + xpBookMedium * XP_PER_BOOK_MEDIUM
                + xpBookLarge * XP_PER_BOOK_LARGE;
        long cumulative = LevelCurve.cumulativeXp(inst.level())
                + xpGained;
        // cost Credits = 50 per level * ceil(xpGained / xpPerBookSmall)
        long creditCost = xpGained / 10; // 10 credits per XP (rough)
        resourceService.spend(ctx, CurrencyType.CREDITS, creditCost, SourceTag.SYSTEM_GIFT);
        // find new level
        int newLevel = inst.level();
        while (newLevel < LevelCurve.MAX_LEVEL_PHASE1
                && cumulative >= LevelCurve.cumulativeXp(newLevel + 1)) {
            newLevel++;
        }
        if (newLevel > inst.level()) {
            inst.setLevel(newLevel);
            emit(ctx, "survivor_levelup", inst.configId(), newLevel);
        }
        return newLevel;
    }

    /** 星级进阶。每次进阶消耗对应数量的同名 Survivor Shard。 */
    public int starUp(PlayerContext ctx, long survivorId) {
        SurvivorInstance inst = requireSurvivor(ctx, survivorId);
        if (inst.star() >= 6) throw new GameException(ErrorCode.ILLEGAL_ARG, "max star reached");
        // shards required by target star: 2→30, 3→80, 4→150, 5→250, 6→400
        int[] req = {0, 30, 80, 150, 250, 400};
        int target = inst.star() + 1;
        int need = req[target - 1];
        long have = ctx.survivorShards().getOrDefault(inst.configId(), 0L);
        if (have < need) throw new GameException(ErrorCode.INSUFFICIENT_ITEM,
                "need " + need + " shards have " + have);
        ctx.survivorShards().put(inst.configId(), have - need);
        inst.setStar(target);
        emit(ctx, "survivor_starup", inst.configId(), target);
        return target;
    }

    /** 技能升级 (consume skill book + credits)。 */
    public int upgradeSkill(PlayerContext ctx, long survivorId, String abilityId) {
        SurvivorInstance inst = requireSurvivor(ctx, survivorId);
        int cur = inst.skillLevel(abilityId);
        if (cur >= 10) throw new GameException(ErrorCode.ILLEGAL_ARG, "max skill level reached");
        int bookCost = cur;
        long creditCost = (long) cur * 1000L;
        itemService.consume(ctx, ITEM_SKILL_BOOK, bookCost, SourceTag.SYSTEM_GIFT);
        resourceService.spend(ctx, CurrencyType.CREDITS, creditCost, SourceTag.SYSTEM_GIFT);
        inst.setSkillLevel(abilityId, cur + 1);
        emit(ctx, "skill_upgrade", inst.configId(), cur + 1);
        return cur + 1;
    }

    /**
     * 根据当前等级/星级/装备/芯片，合成最终战斗属性。
     */
    public Stats computeStats(PlayerContext ctx, SurvivorInstance inst,
                              Map<String, Stats> gearBonuses, Stats augmentBonuses) {
        SurvivorConfig cfg = repo.byId(inst.configId());
        if (cfg == null) throw new GameException(ErrorCode.NOT_FOUND, "survivor cfg " + inst.configId());
        Stats s = new Stats();
        s.set(AttributeType.HP, LevelCurve.scaleStat(cfg.baseHp, inst.level(), inst.star()));
        s.set(AttributeType.ATK, LevelCurve.scaleStat(cfg.baseAtk, inst.level(), inst.star()));
        s.set(AttributeType.DEF, LevelCurve.scaleStat(cfg.baseDef, inst.level(), inst.star()));
        s.set(AttributeType.SPD, cfg.baseSpd);
        s.set(AttributeType.CRIT_RATE, cfg.critRate);
        s.set(AttributeType.CRIT_DMG, cfg.critDmg);
        s.set(AttributeType.ACC, cfg.acc);
        s.set(AttributeType.RES, cfg.res);

        if (gearBonuses != null) {
            for (Stats g : gearBonuses.values()) s.addAll(g);
        }
        if (augmentBonuses != null) s.addAll(augmentBonuses);
        return s.applyPercentBonuses();
    }

    /** 战力公式 (用于排行榜)。 */
    public int powerRating(PlayerContext ctx, SurvivorInstance inst,
                           Map<String, Stats> gearBonuses, Stats augmentBonuses) {
        Stats s = computeStats(ctx, inst, gearBonuses, augmentBonuses);
        double power = s.get(AttributeType.HP) * 0.1
                + s.get(AttributeType.ATK) * 1.0
                + s.get(AttributeType.DEF) * 1.2
                + s.get(AttributeType.SPD) * 0.5
                + s.get(AttributeType.CRIT_RATE) * 1000
                + s.get(AttributeType.CRIT_DMG) * 500;
        return (int) Math.round(power);
    }

    private SurvivorInstance requireSurvivor(PlayerContext ctx, long id) {
        SurvivorInstance inst = ctx.survivors().get(id);
        if (inst == null) throw new GameException(ErrorCode.NOT_FOUND, "survivor " + id);
        return inst;
    }

    private void emit(PlayerContext ctx, String name, String cfgId, int value) {
        if (analytics == null) return;
        analytics.emit(AnalyticsEvent.of(name)
                .prop("player_id", ctx.playerId())
                .prop("cfg_id", cfgId)
                .prop("value", value)
                .build());
    }

    public SurvivorConfigRepository repo() {
        return repo;
    }
}
