/**
 * 与服务端模型对齐的数据类型镜像（TASK-008 / TASK-003 / TASK-004 / TASK-005）。
 */

export enum CurrencyType {
    CREDITS = "CREDITS",
    ALLOY = "ALLOY",
    TECH_CORES = "TECH_CORES",
    RECRUIT_TOKENS = "RECRUIT_TOKENS",
    PREMIUM_CHIPS = "PREMIUM_CHIPS",
}

export enum Rarity {
    COMMON = "COMMON",
    RARE = "RARE",
    EPIC = "EPIC",
    LEGENDARY = "LEGENDARY",
}

export enum SurvivorClass {
    WARRIOR = "WARRIOR",
    SCOUT = "SCOUT",
    MEDIC = "MEDIC",
    ENGINEER = "ENGINEER",
}

export enum GearSlot {
    WEAPON = "WEAPON",
    ARMOR = "ARMOR",
    HELMET = "HELMET",
    BOOTS = "BOOTS",
    ACCESSORY_1 = "ACCESSORY_1",
    ACCESSORY_2 = "ACCESSORY_2",
}

export enum GearQuality {
    WHITE = "WHITE",
    GREEN = "GREEN",
    BLUE = "BLUE",
    PURPLE = "PURPLE",
    ORANGE = "ORANGE",
}

export interface SurvivorConfig {
    id: string;
    name: string;
    rarity: Rarity;
    cls: SurvivorClass;
    baseHp: number;
    baseAtk: number;
    baseDef: number;
    baseSpd: number;
    critRate: number;
    critDmg: number;
    acc: number;
    res: number;
    activeSkillId: string;
    passiveSkill1Id: string;
    passiveSkill2Id: string;
}

export interface SurvivorInstance {
    instanceId: number;
    configId: string;
    level: number;
    star: number;
    equipped: Partial<Record<GearSlot, number>>;
    augmentSlots: [number, number, number];
}

export interface GearInstance {
    instanceId: number;
    slot: GearSlot;
    quality: GearQuality;
    level: number;
    mainStat: string;
    mainStatBase: number;
    subStats: Array<{ type: string; value: number }>;
    locked: boolean;
    equippedSurvivorId: number;
}

export interface AugmentInstance {
    instanceId: number;
    type: string;
    star: number;
    equippedSurvivorId: number;
    equippedSlotIndex: number;
}

export interface ZoneStage {
    stageId: number;
    label: string;
    elite: boolean;
    bossStage: boolean;
    recommendedPower: number;
    waves: string[][];
}

export interface ZoneChapter {
    chapterId: number;
    chapterName: string;
    recommendedPower: number;
    stages: ZoneStage[];
}

export interface BattlePassStateView {
    seasonId: number;
    level: number;
    xp: number;
    premiumActive: boolean;
    premiumPlusActive: boolean;
    freeClaimed: number[];
    premiumClaimed: number[];
}

export interface ArenaRecordView {
    opponentId: number;
    opponentName: string;
    won: boolean;
    myRankBefore: number;
    myRankAfter: number;
    scoreDelta: number;
    timestamp: number;
}
