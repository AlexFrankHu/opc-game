/**
 * 与 server/app/src/main/java/com/lastbastion/app/ActionRegistry.java 对齐。
 */

export const Cmd = {
    USER: 1,
    SURVIVOR: 2,
    GEAR: 3,
    AUGMENT: 4,
    ZONE: 5,
    ARENA: 6,
    BATTLE_PASS: 7,
    STORE: 8,
    ONBOARDING: 9,
    ANALYTICS: 10,
} as const;

export const Action = {
    "user.login": [Cmd.USER, 1],
    "user.heartbeat": [Cmd.USER, 2],

    "survivor.levelUp": [Cmd.SURVIVOR, 1],
    "survivor.starUp": [Cmd.SURVIVOR, 2],
    "survivor.skillUp": [Cmd.SURVIVOR, 3],
    "survivor.pullGacha": [Cmd.SURVIVOR, 4],

    "gear.equip": [Cmd.GEAR, 1],
    "gear.unequip": [Cmd.GEAR, 2],
    "gear.enhance": [Cmd.GEAR, 3],
    "gear.decompose": [Cmd.GEAR, 4],
    "gear.lock": [Cmd.GEAR, 5],

    "augment.fuse": [Cmd.AUGMENT, 1],
    "augment.insert": [Cmd.AUGMENT, 2],
    "augment.remove": [Cmd.AUGMENT, 3],

    "zone.clear": [Cmd.ZONE, 1],
    "zone.sweep": [Cmd.ZONE, 2],
    "zone.settleIdle": [Cmd.ZONE, 3],

    "arena.match": [Cmd.ARENA, 1],
    "arena.challenge": [Cmd.ARENA, 2],
    "arena.leaderboard": [Cmd.ARENA, 3],
    "arena.buyChallenge": [Cmd.ARENA, 4],

    "bp.claim": [Cmd.BATTLE_PASS, 1],
    "bp.buy": [Cmd.BATTLE_PASS, 2],

    "store.iapVerify": [Cmd.STORE, 1],
    "store.starterPackBuy": [Cmd.STORE, 2],
    "store.limitedOfferBuy": [Cmd.STORE, 3],

    "onboarding.completeStep": [Cmd.ONBOARDING, 1],
    "onboarding.skip": [Cmd.ONBOARDING, 2],
    "onboarding.claimQuest": [Cmd.ONBOARDING, 3],

    "analytics.track": [Cmd.ANALYTICS, 1],
} as const;

export type ActionKey = keyof typeof Action;
