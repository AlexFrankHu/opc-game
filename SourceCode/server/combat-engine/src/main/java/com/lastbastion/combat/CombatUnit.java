package com.lastbastion.combat;

import com.lastbastion.common.AttributeType;
import com.lastbastion.common.Stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 运行时战斗单位。
 */
public final class CombatUnit {

    private final String id;
    private final String name;
    private final Side side;
    private final Stats stats;
    private final List<Ability> abilities;
    /** cooldownCountdown[abilityId] */
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private final List<StatusEffect> statuses = new ArrayList<>();
    private final boolean ccImmune;
    private final boolean isBoss;
    private double currentHp;
    private int energy;
    private int ragePhase; // 0 = no rage, 1 = 50%, 2 = 25%
    private boolean alive = true;

    public CombatUnit(String id, String name, Side side, Stats stats, List<Ability> abilities,
                      boolean ccImmune, boolean isBoss) {
        this.id = id;
        this.name = name;
        this.side = side;
        this.stats = new Stats(stats).applyPercentBonuses();
        this.abilities = abilities;
        this.ccImmune = ccImmune;
        this.isBoss = isBoss;
        this.currentHp = this.stats.get(AttributeType.HP);
        this.energy = 0;
        for (Ability a : abilities) {
            cooldowns.put(a.id(), 0);
        }
    }

    public String id() { return id; }
    public String name() { return name; }
    public Side side() { return side; }
    public Stats stats() { return stats; }
    public List<Ability> abilities() { return abilities; }
    public boolean ccImmune() { return ccImmune; }
    public boolean isBoss() { return isBoss; }
    public boolean isAlive() { return alive; }
    public double currentHp() { return currentHp; }
    public double maxHp() { return stats.get(AttributeType.HP); }
    public int energy() { return energy; }
    public int ragePhase() { return ragePhase; }
    public List<StatusEffect> statuses() { return statuses; }

    public void gainEnergy(int v) {
        energy = Math.min(100, energy + v);
    }

    public void spendEnergy(int v) {
        energy = Math.max(0, energy - v);
    }

    public int cooldown(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void setCooldown(String abilityId, int v) {
        cooldowns.put(abilityId, v);
    }

    public void tickCooldowns() {
        for (Map.Entry<String, Integer> e : cooldowns.entrySet()) {
            if (e.getValue() > 0) e.setValue(e.getValue() - 1);
        }
    }

    /** True if CC should be skipped. */
    public boolean hasControlEffect() {
        for (StatusEffect s : statuses) {
            if (s.type().isControl()) return true;
        }
        return false;
    }

    /** Add a status, respecting CC Immunity for control kinds. Returns true if applied. */
    public boolean applyStatus(StatusEffect effect) {
        if (ccImmune && effect.type().isControl()) return false;
        statuses.add(effect);
        return true;
    }

    /** Remove all expired statuses at turn end. */
    public void removeExpired() {
        statuses.removeIf(StatusEffect::expired);
    }

    public void dispelDebuffs() {
        Iterator<StatusEffect> it = statuses.iterator();
        while (it.hasNext()) {
            StatusEffect s = it.next();
            if (s.type().isControl() || s.type().isDot()
                    || s.type() == StatusType.ATK_DOWN
                    || s.type() == StatusType.DEF_DOWN
                    || s.type() == StatusType.SPD_DOWN) {
                it.remove();
            }
        }
    }

    /**
     * Receive raw damage after mitigation; shields are consumed first.
     */
    public double takeDamage(double rawDamage) {
        double remaining = rawDamage;
        Iterator<StatusEffect> it = statuses.iterator();
        while (it.hasNext() && remaining > 0) {
            StatusEffect s = it.next();
            if (s.type() == StatusType.SHIELD) {
                if (s.magnitude() >= remaining) {
                    s.setMagnitude(s.magnitude() - remaining);
                    remaining = 0;
                    if (s.magnitude() <= 0) it.remove();
                } else {
                    remaining -= s.magnitude();
                    it.remove();
                }
            }
        }
        if (remaining > 0) {
            currentHp = Math.max(0, currentHp - remaining);
            if (currentHp <= 0) alive = false;
        }
        gainEnergy(10);
        maybeTriggerBossRage();
        return rawDamage - remaining;
    }

    public void heal(double amount) {
        if (!alive) return;
        currentHp = Math.min(maxHp(), currentHp + amount);
    }

    public boolean maybeTriggerBossRage() {
        if (!isBoss) return false;
        double pct = currentHp / maxHp();
        if (ragePhase < 1 && pct <= 0.5) {
            ragePhase = 1;
            stats.set(AttributeType.ATK, stats.get(AttributeType.ATK) * 1.5);
            stats.set(AttributeType.SPD, stats.get(AttributeType.SPD) * 1.2);
            return true;
        }
        if (ragePhase < 2 && pct <= 0.25) {
            ragePhase = 2;
            stats.set(AttributeType.ATK, stats.get(AttributeType.ATK) * 1.5);
            stats.set(AttributeType.SPD, stats.get(AttributeType.SPD) * 1.2);
            return true;
        }
        return false;
    }
}
