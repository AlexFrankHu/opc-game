package com.lastbastion.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Active or passive ability descriptor. Immutable; runtime state lives on CombatUnit.
 */
public final class Ability {

    public enum Trigger {
        ACTIVE,              // uses cooldown + energy
        PASSIVE_ON_TURN_START,
        PASSIVE_ON_ATTACK,
        PASSIVE_ON_TAKE_HIT
    }

    private final String id;
    private final String name;
    private final Trigger trigger;
    private final int maxCooldown;
    private final int energyCost;
    private final int level;
    private final List<AbilityEffect> effects;

    public Ability(String id, String name, Trigger trigger, int maxCooldown, int energyCost,
                   int level, List<AbilityEffect> effects) {
        this.id = id;
        this.name = name;
        this.trigger = trigger;
        this.maxCooldown = maxCooldown;
        this.energyCost = energyCost;
        this.level = level;
        this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
    }

    public String id() { return id; }
    public String name() { return name; }
    public Trigger trigger() { return trigger; }
    public int maxCooldown() { return maxCooldown; }
    public int energyCost() { return energyCost; }
    public int level() { return level; }
    public List<AbilityEffect> effects() { return effects; }
}
