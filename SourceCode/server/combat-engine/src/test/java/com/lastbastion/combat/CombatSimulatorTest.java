package com.lastbastion.combat;

import com.lastbastion.common.AttributeType;
import com.lastbastion.common.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CombatSimulatorTest {

    private static Stats warrior(double hp, double atk, double def, double spd) {
        Stats s = new Stats();
        s.set(AttributeType.HP, hp).set(AttributeType.ATK, atk)
                .set(AttributeType.DEF, def).set(AttributeType.SPD, spd)
                .set(AttributeType.CRIT_RATE, 0.15).set(AttributeType.CRIT_DMG, 0.50)
                .set(AttributeType.ACC, 1.0).set(AttributeType.RES, 0.0);
        return s;
    }

    @Test
    void fiveVsFiveCompletesWithoutError() {
        Random rng = new Random(42);
        List<CombatUnit> allies = List.of(
                new CombatUnit("a1", "Ally1", Side.ALLY, warrior(500, 80, 30, 100), List.of(), false, false),
                new CombatUnit("a2", "Ally2", Side.ALLY, warrior(500, 80, 30, 95), List.of(), false, false),
                new CombatUnit("a3", "Ally3", Side.ALLY, warrior(500, 80, 30, 90), List.of(), false, false),
                new CombatUnit("a4", "Ally4", Side.ALLY, warrior(500, 80, 30, 85), List.of(), false, false),
                new CombatUnit("a5", "Ally5", Side.ALLY, warrior(500, 80, 30, 80), List.of(), false, false)
        );
        List<CombatUnit> enemies = List.of(
                new CombatUnit("e1", "Enemy1", Side.ENEMY, warrior(400, 70, 20, 100), List.of(), false, false),
                new CombatUnit("e2", "Enemy2", Side.ENEMY, warrior(400, 70, 20, 95), List.of(), false, false),
                new CombatUnit("e3", "Enemy3", Side.ENEMY, warrior(400, 70, 20, 90), List.of(), false, false),
                new CombatUnit("e4", "Enemy4", Side.ENEMY, warrior(400, 70, 20, 85), List.of(), false, false),
                new CombatUnit("e5", "Enemy5", Side.ENEMY, warrior(400, 70, 20, 80), List.of(), false, false)
        );
        CombatSimulator sim = new CombatSimulator(allies, enemies, 50, rng);
        CombatResult result = sim.run();
        assertNotNull(result);
        assertTrue(result.totalRounds() >= 1 && result.totalRounds() <= 50);
        assertEquals(CombatResult.Outcome.ALLY_WIN, result.outcome());
    }

    @Test
    void ccImmunitySkipsStun() {
        Random rng = new Random(1);
        CombatUnit boss = new CombatUnit("b", "Boss", Side.ENEMY, warrior(1000, 100, 40, 50),
                List.of(), true, true);
        assertFalse(boss.applyStatus(new StatusEffect(StatusType.STUN, 2, 0, "x")));
        assertFalse(boss.hasControlEffect());
        assertTrue(boss.applyStatus(new StatusEffect(StatusType.POISON, 2, 50, "x")));
    }

    @Test
    void bossRageTriggersAt50AndThen25() {
        Random rng = new Random(1);
        CombatUnit boss = new CombatUnit("b", "Boss", Side.ENEMY, warrior(1000, 100, 40, 50),
                List.of(), false, true);
        double initialAtk = boss.stats().get(AttributeType.ATK);
        boss.takeDamage(500); // hp=500, 50% threshold
        assertEquals(1, boss.ragePhase());
        assertEquals(initialAtk * 1.5, boss.stats().get(AttributeType.ATK), 0.01);
        boss.takeDamage(260); // hp=240 → 24%
        assertEquals(2, boss.ragePhase());
        assertEquals(initialAtk * 1.5 * 1.5, boss.stats().get(AttributeType.ATK), 0.01);
    }

    @Test
    void poisonDotDamagesAtTurnStart() {
        Random rng = new Random(1);
        CombatUnit u = new CombatUnit("u", "U", Side.ALLY, warrior(500, 80, 30, 100),
                List.of(), false, false);
        CombatUnit e = new CombatUnit("e", "E", Side.ENEMY, warrior(200, 1, 0, 1),
                List.of(), false, false);
        u.applyStatus(new StatusEffect(StatusType.POISON, 3, 50, "src"));
        CombatSimulator sim = new CombatSimulator(List.of(u), List.of(e), 1, rng);
        sim.run();
        // poison ticks once at round 1 start (50 damage); enemy might also chip 1 damage
        assertTrue(u.currentHp() <= 450 && u.currentHp() >= 445,
                "expected ~450 hp after single poison tick, got " + u.currentHp());
    }

    @Test
    void shieldAbsorbsDamageBeforeHp() {
        Random rng = new Random(1);
        CombatUnit u = new CombatUnit("u", "U", Side.ALLY, warrior(500, 10, 0, 1),
                List.of(), false, false);
        u.applyStatus(new StatusEffect(StatusType.SHIELD, 2, 100, "x"));
        u.takeDamage(60);
        assertEquals(500, u.currentHp(), 0.01);
        assertEquals(1, u.statuses().size());
        u.takeDamage(80); // consumes 40 shield, then 40 hp
        assertEquals(460, u.currentHp(), 0.01);
        assertEquals(0, u.statuses().size());
    }
}
