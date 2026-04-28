package com.lastbastion.game.survivor;

import com.lastbastion.common.Rarity;
import com.lastbastion.common.SurvivorClass;

/**
 * Survivor 策划配置（来自 survivors.json）。
 */
public final class SurvivorConfig {

    public String id;
    public String name;
    public Rarity rarity;
    public SurvivorClass cls;
    /** 初始 1 级基础属性 */
    public double baseHp;
    public double baseAtk;
    public double baseDef;
    public double baseSpd;
    public double critRate;
    public double critDmg;
    public double acc;
    public double res;
    /** 职业系数（用于升级成长）*/
    public double growthHp;
    public double growthAtk;
    public double growthDef;
    /** 主动技能 id（指向 ability 配置） */
    public String activeSkillId;
    public String passiveSkill1Id;
    public String passiveSkill2Id;

    public SurvivorConfig() {}
}
