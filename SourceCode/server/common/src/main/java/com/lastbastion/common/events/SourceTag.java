package com.lastbastion.common.events;

/** 资源/道具增减来源标签，用于埋点 (TASK-008 §8.4)。 */
public enum SourceTag {
    ZONE_DROP,
    ZONE_FIRST_CLEAR,
    DAILY_TASK,
    MAIN_QUEST,
    ARENA_REWARD,
    ARENA_DAILY,
    GACHA,
    DECOMPOSE,
    GEAR_ENHANCE,
    AUGMENT_INSERT,
    AUGMENT_REMOVE,
    AUGMENT_FUSION,
    BATTLE_PASS_CLAIM,
    BATTLE_PASS_BUY,
    STARTER_PACK,
    LIMITED_OFFER,
    IAP,
    GUIDE_REWARD,
    SYSTEM_GIFT,
    TEST
}
