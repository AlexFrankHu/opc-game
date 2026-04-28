package com.lastbastion.game.combat;

import com.lastbastion.combat.Ability;
import com.lastbastion.game.loader.ConfigLoader;
import com.lastbastion.game.survivor.SurvivorConfig;
import com.lastbastion.game.survivor.SurvivorConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilityLibraryTest {

    @Test
    void everyConfiguredSurvivorHasAtLeastOneActiveAbility() {
        SurvivorConfigRepository repo = new ConfigLoader().loadSurvivors();
        assertEquals(20, repo.size());
        for (SurvivorConfig cfg : repo.all()) {
            List<Ability> list = AbilityLibrary.abilitiesFor(cfg.id);
            assertNotNull(list, "survivor " + cfg.id + " missing ability entry");
            assertFalse(list.isEmpty(), "survivor " + cfg.id + " has empty ability list");
            boolean hasActive = list.stream().anyMatch(a -> a.trigger() == Ability.Trigger.ACTIVE);
            assertTrue(hasActive, "survivor " + cfg.id + " lacks an ACTIVE ability");
        }
    }

    @Test
    void librarySize20() {
        assertEquals(20, AbilityLibrary.size());
    }
}
