package com.lastbastion.combat;

import com.lastbastion.common.AttributeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Resolve an Ability's effects against the current battlefield.
 */
public final class AbilityExecutor {

    private final DamageCalculator calc;
    private final Random rng;

    public AbilityExecutor(Random rng) {
        this.rng = rng;
        this.calc = new DamageCalculator(rng);
    }

    public void execute(CombatUnit actor, Ability ability, List<CombatUnit> allies,
                        List<CombatUnit> enemies, CombatLog log, int round) {
        for (AbilityEffect eff : ability.effects()) {
            List<CombatUnit> targets = resolveTargets(actor, eff.target(), allies, enemies);
            for (CombatUnit t : targets) {
                applyEffect(actor, t, eff, log, round, ability.name());
                if (!actor.isAlive()) return;
            }
        }
    }

    private List<CombatUnit> resolveTargets(CombatUnit actor, AbilityEffect.TargetType tt,
                                            List<CombatUnit> allies, List<CombatUnit> enemies) {
        List<CombatUnit> alive = new ArrayList<>();
        switch (tt) {
            case SELF -> alive.add(actor);
            case SINGLE_ENEMY -> {
                List<CombatUnit> pool = enemies.stream().filter(CombatUnit::isAlive).toList();
                if (!pool.isEmpty()) alive.add(pool.get(rng.nextInt(pool.size())));
            }
            case ALL_ENEMIES -> enemies.stream().filter(CombatUnit::isAlive).forEach(alive::add);
            case SINGLE_ALLY -> {
                List<CombatUnit> pool = allies.stream().filter(CombatUnit::isAlive).toList();
                if (!pool.isEmpty()) alive.add(pool.get(rng.nextInt(pool.size())));
            }
            case ALL_ALLIES -> allies.stream().filter(CombatUnit::isAlive).forEach(alive::add);
            case LOWEST_HP_ALLY -> allies.stream().filter(CombatUnit::isAlive)
                    .min(Comparator.comparingDouble(u -> u.currentHp() / u.maxHp()))
                    .ifPresent(alive::add);
        }
        return alive;
    }

    private void applyEffect(CombatUnit actor, CombatUnit target, AbilityEffect eff,
                             CombatLog log, int round, String abilityName) {
        switch (eff.kind()) {
            case DAMAGE -> {
                DamageCalculator.DamageResult r = calc.compute(actor, target, eff.magnitude());
                if (r.evaded) {
                    log.add(round, CombatLog.EventType.DAMAGE, actor.id(), target.id(), 0, "evaded");
                    return;
                }
                double dealt = target.takeDamage(r.amount);
                log.add(round, CombatLog.EventType.DAMAGE, actor.id(), target.id(), dealt,
                        abilityName + (r.crit ? " crit" : ""));
                if (!target.isAlive()) {
                    log.add(round, CombatLog.EventType.UNIT_DEATH, null, target.id(), 0, null);
                }
            }
            case HEAL -> {
                double amount = actor.stats().get(AttributeType.ATK) * eff.magnitude();
                target.heal(amount);
                log.add(round, CombatLog.EventType.HEAL, actor.id(), target.id(), amount, abilityName);
            }
            case APPLY_STATUS -> {
                StatusEffect se = new StatusEffect(eff.statusType(), eff.statusDuration(),
                        eff.magnitude() * actor.stats().get(AttributeType.ATK), actor.id());
                if (eff.statusType() == StatusType.ATK_UP || eff.statusType() == StatusType.DEF_UP
                        || eff.statusType() == StatusType.ATK_DOWN || eff.statusType() == StatusType.DEF_DOWN) {
                    se.setMagnitude(eff.magnitude()); // stays as ratio
                }
                boolean applied = target.applyStatus(se);
                log.add(round, CombatLog.EventType.STATUS_APPLIED, actor.id(), target.id(),
                        eff.statusDuration(), applied ? eff.statusType().name() : (eff.statusType() + " immune"));
            }
            case DISPEL -> {
                target.dispelDebuffs();
                log.add(round, CombatLog.EventType.STATUS_EXPIRED, actor.id(), target.id(), 0, "dispel");
            }
        }
    }
}
