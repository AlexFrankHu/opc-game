package com.lastbastion.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameBootstrapTest {

    @Test
    void bootsAllServices() {
        GameBootstrap.Services svc = new GameBootstrap().boot();
        assertEquals(20, svc.survivorRepo.size());
        assertEquals(3, svc.zoneRepo.all().size());
        assertNotNull(svc.gacha);
        assertNotNull(svc.gear);
        assertNotNull(svc.augment);
        assertNotNull(svc.arena);
        assertNotNull(svc.battlePass);
        assertNotNull(svc.onboarding);
        assertFalse(ActionRegistry.ALL.isEmpty());
    }
}
