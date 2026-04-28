package com.lastbastion.combat;

import com.lastbastion.common.AttributeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * TASK-002 §2.1 战斗主循环。
 *
 * 5v5 自动战斗 —— 每回合所有存活单位按 SPD 从高到低行动；
 * 全灭 or 达到 maxRounds 结束。
 */
public final class CombatSimulator {

    private final List<CombatUnit> allies;
    private final List<CombatUnit> enemies;
    private final int maxRounds;
    private final Random rng;
    private final AbilityExecutor abilityExecutor;
    private final CombatLog log = new CombatLog();

    public CombatSimulator(List<CombatUnit> allies, List<CombatUnit> enemies, int maxRounds, Random rng) {
        this.allies = allies;
        this.enemies = enemies;
        this.maxRounds = maxRounds;
        this.rng = rng;
        this.abilityExecutor = new AbilityExecutor(rng);
    }

    public CombatResult run() {
        int round = 0;
        while (round < maxRounds) {
            round++;
            log.add(round, CombatLog.EventType.ROUND_START, null, null, 0, null);
            List<CombatUnit> order = turnOrder();

            for (CombatUnit unit : order) {
                if (!unit.isAlive()) continue;
                tickTurnStartStatuses(unit, round);
                if (!unit.isAlive()) continue;
                if (unit.hasControlEffect()) {
                    log.add(round, CombatLog.EventType.UNIT_ACTION, unit.id(), null, 0, "skipped (CC)");
                    expireStatuses(unit, round);
                    unit.tickCooldowns();
                    continue;
                }
                act(unit, round);
                expireStatuses(unit, round);
                unit.tickCooldowns();

                if (allDead(allies) || allDead(enemies)) break;
            }

            if (allDead(allies) || allDead(enemies)) break;
        }

        CombatResult.Outcome outcome;
        if (allDead(enemies) && !allDead(allies)) outcome = CombatResult.Outcome.ALLY_WIN;
        else if (allDead(allies) && !allDead(enemies)) outcome = CombatResult.Outcome.ENEMY_WIN;
        else outcome = CombatResult.Outcome.DRAW;
        log.add(round, CombatLog.EventType.BATTLE_END, null, null, 0, outcome.name());
        return new CombatResult(outcome, round, log);
    }

    private List<CombatUnit> turnOrder() {
        List<CombatUnit> all = new ArrayList<>();
        all.addAll(allies);
        all.addAll(enemies);
        all.sort(Comparator.comparingDouble((CombatUnit u) -> u.stats().get(AttributeType.SPD)).reversed());
        return all;
    }

    private void tickTurnStartStatuses(CombatUnit unit, int round) {
        Iterator<StatusEffect> it = unit.statuses().iterator();
        while (it.hasNext()) {
            StatusEffect s = it.next();
            if (s.type().isDot()) {
                double dmg = s.magnitude();
                unit.takeDamage(dmg);
                log.add(round, CombatLog.EventType.STATUS_TICK, s.sourceId(), unit.id(), dmg, s.type().name());
                if (!unit.isAlive()) {
                    log.add(round, CombatLog.EventType.UNIT_DEATH, null, unit.id(), 0, null);
                    return;
                }
            }
        }
    }

    private void expireStatuses(CombatUnit unit, int round) {
        for (StatusEffect s : unit.statuses()) {
            s.decrement();
            if (s.expired()) {
                log.add(round, CombatLog.EventType.STATUS_EXPIRED, null, unit.id(), 0, s.type().name());
            }
        }
        unit.removeExpired();
    }

    private void act(CombatUnit unit, int round) {
        List<CombatUnit> allyPool = unit.side() == Side.ALLY ? allies : enemies;
        List<CombatUnit> foePool = unit.side() == Side.ALLY ? enemies : allies;

        // pick a usable active ability
        Ability chosen = null;
        for (Ability a : unit.abilities()) {
            if (a.trigger() != Ability.Trigger.ACTIVE) continue;
            if (unit.cooldown(a.id()) > 0) continue;
            if (unit.energy() < a.energyCost()) continue;
            chosen = a;
            break;
        }
        if (chosen == null) {
            basicAttack(unit, foePool, round);
        } else {
            log.add(round, CombatLog.EventType.UNIT_ACTION, unit.id(), null, 0, chosen.name());
            abilityExecutor.execute(unit, chosen, allyPool, foePool, log, round);
            unit.spendEnergy(chosen.energyCost());
            unit.setCooldown(chosen.id(), chosen.maxCooldown());
        }
    }

    private void basicAttack(CombatUnit unit, List<CombatUnit> enemies, int round) {
        List<CombatUnit> alive = enemies.stream().filter(CombatUnit::isAlive).toList();
        if (alive.isEmpty()) return;
        CombatUnit target = alive.get(rng.nextInt(alive.size()));
        Ability basic = new Ability("basic_" + unit.id(), "Basic Attack",
                Ability.Trigger.ACTIVE, 0, 0, 1,
                List.of(AbilityEffect.damage(AbilityEffect.TargetType.SINGLE_ENEMY, 1.0)));
        log.add(round, CombatLog.EventType.UNIT_ACTION, unit.id(), target.id(), 0, "basic");
        DamageCalculator calc = new DamageCalculator(rng);
        DamageCalculator.DamageResult r = calc.compute(unit, target, 1.0);
        if (r.evaded) {
            log.add(round, CombatLog.EventType.DAMAGE, unit.id(), target.id(), 0, "evaded");
        } else {
            double dealt = target.takeDamage(r.amount);
            log.add(round, CombatLog.EventType.DAMAGE, unit.id(), target.id(), dealt,
                    r.crit ? "basic crit" : "basic");
            if (!target.isAlive()) {
                log.add(round, CombatLog.EventType.UNIT_DEATH, null, target.id(), 0, null);
            }
        }
    }

    private boolean allDead(List<CombatUnit> team) {
        return team.stream().noneMatch(CombatUnit::isAlive);
    }

    public CombatLog getLog() {
        return log;
    }
}
