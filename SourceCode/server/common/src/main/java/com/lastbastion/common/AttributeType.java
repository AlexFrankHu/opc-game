package com.lastbastion.common;

/** Attribute identifier used by stat blocks, gear sub-stats, augments, and buffs. */
public enum AttributeType {
    HP,
    ATK,
    DEF,
    SPD,
    CRIT_RATE,
    CRIT_DMG,
    ACC,
    RES,
    /** Percent-based modifiers (stored as 0.10 for +10%). */
    HP_PCT,
    ATK_PCT,
    DEF_PCT
}
