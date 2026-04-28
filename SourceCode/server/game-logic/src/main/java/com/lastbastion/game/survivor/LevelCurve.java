package com.lastbastion.game.survivor;

/**
 * 经验曲线 & 属性成长 (TASK-003 §3.2)。
 *
 * 迁移自 opc-game 原体系：使用二次函数近似 — XP(L) = 100 * L^2 + 50 * L。
 * 属性成长：每级 +growthCoeff * baseStat，叠加进阶星级倍率。
 */
public final class LevelCurve {

    public static final int MAX_LEVEL_PHASE1 = 60;

    /** 升到下一级所需经验。 */
    public static long xpToNext(int currentLevel) {
        long L = currentLevel;
        return 100L * L * L + 50L * L;
    }

    /** 累计到指定等级的经验。 */
    public static long cumulativeXp(int level) {
        long total = 0;
        for (int i = 1; i < level; i++) total += xpToNext(i);
        return total;
    }

    /** 星级属性倍率：1→×1.00, 2→×1.15, 3→×1.25, 4→×1.40, 5→×1.60, 6→×2.00 */
    public static double starMultiplier(int star) {
        return switch (star) {
            case 1 -> 1.00;
            case 2 -> 1.15;
            case 3 -> 1.25;
            case 4 -> 1.40;
            case 5 -> 1.60;
            case 6 -> 2.00;
            default -> 1.00;
        };
    }

    /**
     * 基础属性线性成长：final = base * (1 + (level-1)*0.08) * starMultiplier
     */
    public static double scaleStat(double baseValue, int level, int star) {
        return baseValue * (1 + (level - 1) * 0.08) * starMultiplier(star);
    }
}
