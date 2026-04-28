package com.lastbastion.game.combat;

import com.lastbastion.combat.Ability;
import com.lastbastion.combat.AbilityEffect;
import com.lastbastion.combat.CombatUnit;
import com.lastbastion.combat.Side;
import com.lastbastion.combat.StatusType;
import com.lastbastion.common.Stats;
import com.lastbastion.common.SurvivorClass;
import com.lastbastion.game.augment.AugmentService;
import com.lastbastion.game.gear.GearService;
import com.lastbastion.game.player.PlayerContext;
import com.lastbastion.game.survivor.SurvivorConfig;
import com.lastbastion.game.survivor.SurvivorInstance;
import com.lastbastion.game.survivor.SurvivorService;

import java.util.ArrayList;
import java.util.List;

/**
 * 构造玩家上阵队伍的 CombatUnit。
 */
public final class CombatAssembler {

    private final SurvivorService survivorService;
    private final GearService gearService;
    private final AugmentService augmentService;

    public CombatAssembler(SurvivorService survivorService, GearService gearService,
                           AugmentService augmentService) {
        this.survivorService = survivorService;
        this.gearService = gearService;
        this.augmentService = augmentService;
    }

    public List<CombatUnit> buildTeam(PlayerContext ctx, long[] survivorIds, Side side) {
        List<CombatUnit> list = new ArrayList<>();
        for (long sid : survivorIds) {
            if (sid == 0) continue;
            SurvivorInstance inst = ctx.survivors().get(sid);
            if (inst == null) continue;
            CombatUnit unit = buildUnit(ctx, inst, side);
            list.add(unit);
        }
        return list;
    }

    public CombatUnit buildUnit(PlayerContext ctx, SurvivorInstance inst, Side side) {
        SurvivorConfig cfg = survivorService.repo().byId(inst.configId());
        Stats gearStats = null;
        if (gearService != null) {
            Stats agg = new Stats();
            gearService.collectStats(ctx, inst.instanceId()).values().forEach(agg::addAll);
            gearStats = agg;
        }
        Stats augmentStats = augmentService != null
                ? augmentService.collectStats(ctx, inst.instanceId())
                : new Stats();

        Stats total = new Stats();
        total.addAll(survivorService.computeStats(ctx,
                inst,
                java.util.Map.of("GEAR", gearStats == null ? new Stats() : gearStats),
                augmentStats));

        boolean ccImmune = cfg != null && cfg.cls == SurvivorClass.WARRIOR;
        List<Ability> abilities = new ArrayList<>();
        abilities.add(defaultAbility(cfg == null ? "generic" : cfg.id));
        return new CombatUnit(
                "S" + inst.instanceId(),
                cfg == null ? "Unknown" : cfg.name,
                side,
                total,
                abilities,
                ccImmune,
                false);
    }

    /** 默认主动技能：攻击最弱一个敌人造成 180% ATK 伤害并附加 2 回合 POISON (20% ATK/回合)。 */
    public static Ability defaultAbility(String cfgId) {
        List<AbilityEffect> eff = List.of(
                AbilityEffect.damage(AbilityEffect.TargetType.SINGLE_ENEMY, 1.8),
                AbilityEffect.applyStatus(AbilityEffect.TargetType.SINGLE_ENEMY, StatusType.POISON, 2, 0.2)
        );
        return new Ability("active_" + cfgId, "Precision Strike",
                Ability.Trigger.ACTIVE, 3, 50, 1, eff);
    }
}
