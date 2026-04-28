package com.lastbastion.game.gear;

import com.lastbastion.common.AttributeType;
import com.lastbastion.common.GearQuality;
import com.lastbastion.common.GearSlot;
import com.lastbastion.common.IdGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Roll a new GearInstance — for quest rewards, chests, zone drops.
 */
public final class GearFactory {

    private static final AttributeType[] SUB_STAT_POOL = {
            AttributeType.ATK_PCT, AttributeType.DEF_PCT, AttributeType.HP_PCT,
            AttributeType.SPD, AttributeType.CRIT_RATE, AttributeType.CRIT_DMG,
            AttributeType.ACC, AttributeType.RES
    };

    private final Random rng;

    public GearFactory(Random rng) {
        this.rng = rng;
    }

    public GearInstance roll(GearSlot slot, GearQuality quality) {
        AttributeType mainStat = mainStatFor(slot);
        double mainBase = mainBaseValue(mainStat, quality);
        int subStatCount = switch (quality) {
            case WHITE -> 0;
            case GREEN -> 1;
            case BLUE -> 2;
            case PURPLE -> 3;
            case ORANGE -> 4;
        };
        List<AttributeType> pool = new ArrayList<>(Arrays.asList(SUB_STAT_POOL));
        pool.remove(mainStat);
        Collections.shuffle(pool, rng);
        List<GearInstance.SubStat> subs = new ArrayList<>();
        for (int i = 0; i < subStatCount && i < pool.size(); i++) {
            AttributeType t = pool.get(i);
            subs.add(new GearInstance.SubStat(t, subValueFor(t, quality)));
        }
        return new GearInstance(IdGenerator.next(), slot, quality, mainStat, mainBase, subs);
    }

    private AttributeType mainStatFor(GearSlot slot) {
        return switch (slot) {
            case WEAPON -> AttributeType.ATK;
            case ARMOR -> AttributeType.DEF;
            case HELMET -> AttributeType.HP;
            case BOOTS -> AttributeType.SPD;
            case ACCESSORY_1, ACCESSORY_2 -> {
                AttributeType[] choices = {AttributeType.CRIT_RATE, AttributeType.CRIT_DMG,
                        AttributeType.ACC, AttributeType.RES, AttributeType.ATK_PCT, AttributeType.DEF_PCT};
                yield choices[rng.nextInt(choices.length)];
            }
        };
    }

    private double mainBaseValue(AttributeType t, GearQuality q) {
        double[] mul = {1.0, 1.5, 2.2, 3.0, 4.0};
        double m = mul[q.tier() - 1];
        return switch (t) {
            case HP -> 300 * m;
            case ATK -> 40 * m;
            case DEF -> 30 * m;
            case SPD -> 6 * m;
            case CRIT_RATE -> 0.05 * m;
            case CRIT_DMG -> 0.10 * m;
            case ACC, RES -> 0.08 * m;
            case ATK_PCT, DEF_PCT, HP_PCT -> 0.08 * m;
        };
    }

    private double subValueFor(AttributeType t, GearQuality q) {
        double base = switch (t) {
            case HP -> 100;
            case ATK -> 15;
            case DEF -> 12;
            case SPD -> 3;
            case CRIT_RATE -> 0.03;
            case CRIT_DMG -> 0.05;
            case ACC, RES -> 0.04;
            case HP_PCT, ATK_PCT, DEF_PCT -> 0.04;
        };
        // 0.8 .. 1.2 roll
        return base * (0.8 + rng.nextDouble() * 0.4) * (1 + (q.tier() - 1) * 0.15);
    }
}
