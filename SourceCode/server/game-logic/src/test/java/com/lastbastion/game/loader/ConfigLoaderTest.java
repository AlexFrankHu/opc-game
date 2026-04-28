package com.lastbastion.game.loader;

import com.lastbastion.common.Rarity;
import com.lastbastion.game.survivor.SurvivorConfig;
import com.lastbastion.game.survivor.SurvivorConfigRepository;
import com.lastbastion.game.zone.ZoneConfig;
import com.lastbastion.game.zone.ZoneConfigRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    void survivorsJsonHas20Entries() {
        ConfigLoader loader = new ConfigLoader();
        SurvivorConfigRepository repo = loader.loadSurvivors();
        assertEquals(20, repo.size());
        long legendary = repo.all().stream().filter(s -> s.rarity == Rarity.LEGENDARY).count();
        long epic = repo.all().stream().filter(s -> s.rarity == Rarity.EPIC).count();
        long rare = repo.all().stream().filter(s -> s.rarity == Rarity.RARE).count();
        assertEquals(5, legendary);
        assertEquals(7, epic);
        assertEquals(8, rare);
    }

    @Test
    void zonesJsonHasThreeChapters() {
        ConfigLoader loader = new ConfigLoader();
        ZoneConfigRepository repo = loader.loadZones();
        assertNotNull(repo.byChapter(1));
        assertNotNull(repo.byChapter(2));
        assertNotNull(repo.byChapter(3));
        // Zone 1: 10 + 1 BOSS
        ZoneConfig c1 = repo.byChapter(1);
        assertEquals(11, c1.stages.size());
        assertTrue(c1.stages.get(10).bossStage);
        // Zone 2: 12 + 1 BOSS
        assertEquals(13, repo.byChapter(2).stages.size());
        // Zone 3: 15 + 1 BOSS
        assertEquals(16, repo.byChapter(3).stages.size());
    }

    @Test
    void survivorConfigBaseStatsPresent() {
        SurvivorConfig rex = new ConfigLoader().loadSurvivors().byId("L_COMMANDER_REX");
        assertNotNull(rex);
        assertEquals(1200, rex.baseHp);
        assertEquals("Commander Rex", rex.name);
    }
}
