package com.lastbastion.game.combat;

import com.lastbastion.combat.Ability;
import com.lastbastion.combat.AbilityEffect;
import com.lastbastion.combat.AbilityEffect.TargetType;
import com.lastbastion.combat.StatusType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 20 个 Survivor 各自独有的主动技能 + 挑选过的被动。
 *
 * 注：combat-engine 当前支持的 Trigger：
 *  - ACTIVE（受 cooldown / energy 控制）
 *  - PASSIVE_ON_TURN_START
 *  - PASSIVE_ON_ATTACK
 *  - PASSIVE_ON_TAKE_HIT
 */
public final class AbilityLibrary {

    private static final Map<String, List<Ability>> BY_CFG = new HashMap<>();

    static {
        // --- LEGENDARY ---
        register("L_COMMANDER_REX", List.of(
                active("rallying_cry", "Rallying Cry", 4, 60,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 2.0),
                        AbilityEffect.applyStatus(TargetType.ALL_ALLIES, StatusType.ATK_UP, 2, 0.30)),
                passive("rex_shield_start", "War Banner",
                        Ability.Trigger.PASSIVE_ON_TURN_START, 1, 0,
                        AbilityEffect.applyStatus(TargetType.ALL_ALLIES, StatusType.SHIELD, 2, 0.15))
        ));
        register("L_GHOST_WRAITH", List.of(
                active("phantom_strike", "Phantom Strike", 3, 55,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 2.2),
                        AbilityEffect.applyStatus(TargetType.SINGLE_ENEMY, StatusType.POISON, 3, 0.25)),
                passive("wraith_stun", "Fear Grip",
                        Ability.Trigger.PASSIVE_ON_ATTACK, 2, 0,
                        AbilityEffect.applyStatus(TargetType.SINGLE_ENEMY, StatusType.STUN, 1, 0.0))
        ));
        register("L_DOC_SERAPH", List.of(
                active("purify_light", "Purify Light", 3, 55,
                        AbilityEffect.heal(TargetType.ALL_ALLIES, 1.5),
                        AbilityEffect.dispel(TargetType.ALL_ALLIES)),
                passive("seraph_heal_start", "Field Triage",
                        Ability.Trigger.PASSIVE_ON_TURN_START, 1, 0,
                        AbilityEffect.heal(TargetType.LOWEST_HP_ALLY, 0.30))
        ));
        register("L_FORGE_TITAN", List.of(
                active("shield_wall", "Shield Wall", 4, 60,
                        AbilityEffect.applyStatus(TargetType.ALL_ALLIES, StatusType.SHIELD, 2, 2.0),
                        AbilityEffect.applyStatus(TargetType.ALL_ALLIES, StatusType.DEF_UP, 2, 0.25)),
                passive("titan_reflect", "Pain Feedback",
                        Ability.Trigger.PASSIVE_ON_TAKE_HIT, 2, 0,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 0.5))
        ));
        register("L_HAVOC_QUEEN", List.of(
                active("annihilate", "Annihilate", 4, 70,
                        AbilityEffect.damage(TargetType.ALL_ENEMIES, 1.6),
                        AbilityEffect.applyStatus(TargetType.ALL_ENEMIES, StatusType.BURN, 2, 0.15)),
                passive("havoc_rage", "Rising Fury",
                        Ability.Trigger.PASSIVE_ON_TURN_START, 1, 0,
                        AbilityEffect.applyStatus(TargetType.SELF, StatusType.ATK_UP, 1, 0.15))
        ));

        // --- EPIC ---
        register("E_IRONHIDE", List.of(
                active("iron_taunt", "Iron Taunt", 3, 50,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.0),
                        AbilityEffect.applyStatus(TargetType.SELF, StatusType.DEF_UP, 2, 0.30))
        ));
        register("E_SWIFT_BLADE", List.of(
                active("whirlwind", "Whirlwind", 3, 55,
                        AbilityEffect.damage(TargetType.ALL_ENEMIES, 1.4))
        ));
        register("E_MENDER", List.of(
                active("first_aid", "First Aid", 2, 40,
                        AbilityEffect.heal(TargetType.LOWEST_HP_ALLY, 2.0),
                        AbilityEffect.dispel(TargetType.LOWEST_HP_ALLY))
        ));
        register("E_SAPPER", List.of(
                active("sticky_bomb", "Sticky Bomb", 3, 50,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.5),
                        AbilityEffect.applyStatus(TargetType.SINGLE_ENEMY, StatusType.SPD_DOWN, 2, 0.30))
        ));
        register("E_RONIN", List.of(
                active("iaido_strike", "Iaido Strike", 3, 55,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 2.5))
        ));
        register("E_SILENT_STEP", List.of(
                active("backstab", "Backstab", 3, 50,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.8),
                        AbilityEffect.applyStatus(TargetType.SINGLE_ENEMY, StatusType.POISON, 3, 0.10))
        ));
        register("E_FIELD_MEDIC", List.of(
                active("stim_pack", "Stim Pack", 3, 50,
                        AbilityEffect.heal(TargetType.ALL_ALLIES, 0.80),
                        AbilityEffect.applyStatus(TargetType.ALL_ALLIES, StatusType.ATK_UP, 2, 0.15))
        ));

        // --- RARE ---
        register("R_GRUNT", List.of(
                active("gun_down", "Gun Down", 2, 40,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.2))
        ));
        register("R_SCOUT_ALPHA", List.of(
                active("recon_shot", "Recon Shot", 2, 40,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.3),
                        AbilityEffect.applyStatus(TargetType.SINGLE_ENEMY, StatusType.DEF_DOWN, 2, 0.15))
        ));
        register("R_CORPSMAN", List.of(
                active("patch_up", "Patch Up", 2, 40,
                        AbilityEffect.heal(TargetType.LOWEST_HP_ALLY, 1.2))
        ));
        register("R_WRENCHJACK", List.of(
                active("repair_drone", "Repair Drone", 3, 45,
                        AbilityEffect.applyStatus(TargetType.LOWEST_HP_ALLY, StatusType.SHIELD, 2, 0.80),
                        AbilityEffect.applyStatus(TargetType.LOWEST_HP_ALLY, StatusType.DEF_UP, 2, 0.20))
        ));
        register("R_BRAWLER", List.of(
                active("pummel", "Pummel", 2, 40,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.4))
        ));
        register("R_RANGER", List.of(
                active("suppressing_fire", "Suppressing Fire", 3, 50,
                        AbilityEffect.damage(TargetType.ALL_ENEMIES, 1.0),
                        AbilityEffect.applyStatus(TargetType.ALL_ENEMIES, StatusType.SPD_DOWN, 1, 0.20))
        ));
        register("R_NURSE", List.of(
                active("bandage", "Bandage", 2, 35,
                        AbilityEffect.heal(TargetType.LOWEST_HP_ALLY, 1.4))
        ));
        register("R_TECHIE", List.of(
                active("sabotage", "Sabotage", 3, 45,
                        AbilityEffect.damage(TargetType.SINGLE_ENEMY, 1.1),
                        AbilityEffect.applyStatus(TargetType.SINGLE_ENEMY, StatusType.ATK_DOWN, 2, 0.20))
        ));
    }

    private static void register(String cfgId, List<Ability> list) {
        BY_CFG.put(cfgId, Collections.unmodifiableList(list));
    }

    private static Ability active(String id, String name, int cd, int energy, AbilityEffect... eff) {
        return new Ability(id, name, Ability.Trigger.ACTIVE, cd, energy, 1, List.of(eff));
    }

    private static Ability passive(String id, String name, Ability.Trigger t, int cd, int energy,
                                   AbilityEffect... eff) {
        return new Ability(id, name, t, cd, energy, 1, List.of(eff));
    }

    public static List<Ability> abilitiesFor(String cfgId) {
        return BY_CFG.get(cfgId);
    }

    public static int size() { return BY_CFG.size(); }

    private AbilityLibrary() {}
}
