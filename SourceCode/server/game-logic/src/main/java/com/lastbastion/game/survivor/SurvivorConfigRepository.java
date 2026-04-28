package com.lastbastion.game.survivor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SurvivorConfigRepository {

    private final Map<String, SurvivorConfig> byId = new LinkedHashMap<>();

    public void register(SurvivorConfig cfg) {
        byId.put(cfg.id, cfg);
    }

    public SurvivorConfig byId(String id) {
        return byId.get(id);
    }

    public Collection<SurvivorConfig> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
