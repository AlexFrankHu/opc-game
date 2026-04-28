package com.lastbastion.app;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ioGame 命令号（cmd × subCmd）与协议方法名的映射。
 *
 * 在生产中这张表由 ioGame 自动扫描 {@code ActionController} 注解生成；
 * 此处以常量形式列出，方便客户端与服务端一致。
 */
public final class ActionRegistry {

    public static final Map<String, Integer> ALL = new LinkedHashMap<>();

    // cmd 段划分
    public static final int CMD_USER = 1;
    public static final int CMD_SURVIVOR = 2;
    public static final int CMD_GEAR = 3;
    public static final int CMD_AUGMENT = 4;
    public static final int CMD_ZONE = 5;
    public static final int CMD_ARENA = 6;
    public static final int CMD_BATTLE_PASS = 7;
    public static final int CMD_STORE = 8;
    public static final int CMD_ONBOARDING = 9;
    public static final int CMD_ANALYTICS = 10;

    static {
        put("user.login", CMD_USER, 1);
        put("user.heartbeat", CMD_USER, 2);

        put("survivor.levelUp", CMD_SURVIVOR, 1);
        put("survivor.starUp", CMD_SURVIVOR, 2);
        put("survivor.skillUp", CMD_SURVIVOR, 3);
        put("survivor.pullGacha", CMD_SURVIVOR, 4);

        put("gear.equip", CMD_GEAR, 1);
        put("gear.unequip", CMD_GEAR, 2);
        put("gear.enhance", CMD_GEAR, 3);
        put("gear.decompose", CMD_GEAR, 4);
        put("gear.lock", CMD_GEAR, 5);

        put("augment.fuse", CMD_AUGMENT, 1);
        put("augment.insert", CMD_AUGMENT, 2);
        put("augment.remove", CMD_AUGMENT, 3);

        put("zone.clear", CMD_ZONE, 1);
        put("zone.sweep", CMD_ZONE, 2);
        put("zone.settleIdle", CMD_ZONE, 3);

        put("arena.match", CMD_ARENA, 1);
        put("arena.challenge", CMD_ARENA, 2);
        put("arena.leaderboard", CMD_ARENA, 3);
        put("arena.buyChallenge", CMD_ARENA, 4);

        put("bp.claim", CMD_BATTLE_PASS, 1);
        put("bp.buy", CMD_BATTLE_PASS, 2);

        put("store.iapVerify", CMD_STORE, 1);
        put("store.starterPackBuy", CMD_STORE, 2);
        put("store.limitedOfferBuy", CMD_STORE, 3);

        put("onboarding.completeStep", CMD_ONBOARDING, 1);
        put("onboarding.skip", CMD_ONBOARDING, 2);
        put("onboarding.claimQuest", CMD_ONBOARDING, 3);

        put("analytics.track", CMD_ANALYTICS, 1);
    }

    private static void put(String key, int cmd, int subCmd) {
        ALL.put(key, (cmd << 16) | subCmd);
    }

    private ActionRegistry() {}
}
