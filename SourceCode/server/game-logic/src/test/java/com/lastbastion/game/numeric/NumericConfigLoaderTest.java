package com.lastbastion.game.numeric;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NumericConfigLoaderTest {

    @Test
    void classpathLoadsAllModules() {
        NumericConfig cfg = NumericConfigLoader.fromClasspath();
        assertNotNull(cfg.combat());
        assertNotNull(cfg.gacha());
        assertNotNull(cfg.gear());
        assertNotNull(cfg.augment());
        assertNotNull(cfg.arena());
        assertNotNull(cfg.battlePass());
        assertNotNull(cfg.resources());
        assertNotNull(cfg.zoneIdle());
        assertNotNull(cfg.starterPack());
        assertNotNull(cfg.limitedOffers());
        assertNotNull(cfg.onboarding());
        assertNotNull(cfg.dailyQuests());

        assertEquals(80, cfg.gacha().pityLimit);
        assertEquals(20, cfg.gear().maxLevel);
        assertEquals(50, cfg.battlePass().maxLevel);
        assertEquals(15, cfg.dailyQuests().quests.size());
    }

    @Test
    void schemaErrorThrowsFastWhenFileMissing(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        // 写入合法的 12 个文件中的 11 个，故意漏掉 combat.json
        copyFromClasspath(dir, "gacha.json");
        copyFromClasspath(dir, "gear.json");
        copyFromClasspath(dir, "augment.json");
        copyFromClasspath(dir, "arena.json");
        copyFromClasspath(dir, "battlepass.json");
        copyFromClasspath(dir, "resources.json");
        copyFromClasspath(dir, "zone_idle.json");
        copyFromClasspath(dir, "starter_pack.json");
        copyFromClasspath(dir, "limited_offers.json");
        copyFromClasspath(dir, "onboarding.json");
        copyFromClasspath(dir, "daily_quests.json");

        NumericConfigException ex = assertThrows(NumericConfigException.class,
                () -> NumericConfigLoader.fromDir(dir));
        assertTrue(ex.getMessage().contains("combat.json"), "error should mention missing file: " + ex.getMessage());
    }

    @Test
    void schemaErrorThrowsFastOnBadProbabilitySum(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        copyFromClasspath(dir, "combat.json");
        // gacha.json with rates summing to 1.5 (invalid)
        Files.writeString(dir.resolve("gacha.json"),
                "{\"rates\":{\"RARE\":1.0,\"EPIC\":0.3,\"LEGENDARY\":0.2}," +
                "\"pityLimit\":80,\"singleCostToken\":1,\"tenCostToken\":10," +
                "\"singleCostChip\":30,\"tenCostChip\":270,\"shardsPerDuplicate\":20}");
        copyFromClasspath(dir, "gear.json");
        copyFromClasspath(dir, "augment.json");
        copyFromClasspath(dir, "arena.json");
        copyFromClasspath(dir, "battlepass.json");
        copyFromClasspath(dir, "resources.json");
        copyFromClasspath(dir, "zone_idle.json");
        copyFromClasspath(dir, "starter_pack.json");
        copyFromClasspath(dir, "limited_offers.json");
        copyFromClasspath(dir, "onboarding.json");
        copyFromClasspath(dir, "daily_quests.json");

        NumericConfigException ex = assertThrows(NumericConfigException.class,
                () -> NumericConfigLoader.fromDir(dir));
        assertTrue(ex.getMessage().toLowerCase().contains("rate") || ex.getMessage().contains("1.0"),
                "error should mention invalid gacha rates: " + ex.getMessage());
    }

    @Test
    void dirSourceMatchesClasspathValues(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        for (String name : new String[]{
                "combat.json", "gacha.json", "gear.json", "augment.json", "arena.json",
                "battlepass.json", "resources.json", "zone_idle.json", "starter_pack.json",
                "limited_offers.json", "onboarding.json", "daily_quests.json"}) {
            copyFromClasspath(dir, name);
        }
        NumericConfig fromDir = NumericConfigLoader.fromDir(dir);
        NumericConfig fromCp = NumericConfigLoader.fromClasspath();
        assertEquals(fromCp.gacha().pityLimit, fromDir.gacha().pityLimit);
        assertEquals(fromCp.gear().maxLevel, fromDir.gear().maxLevel);
        assertEquals(fromCp.battlePass().xpCurve.size(), fromDir.battlePass().xpCurve.size());
    }

    private static void copyFromClasspath(Path dir, String fileName) throws Exception {
        try (var is = NumericConfigLoaderTest.class.getResourceAsStream("/numeric/" + fileName)) {
            assertNotNull(is, "classpath resource missing: /numeric/" + fileName);
            Files.copy(is, dir.resolve(fileName));
        }
    }
}
