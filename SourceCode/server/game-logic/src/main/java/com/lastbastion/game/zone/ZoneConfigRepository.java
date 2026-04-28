package com.lastbastion.game.zone;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ZoneConfigRepository {

    private final Map<Integer, ZoneConfig> byChapter = new LinkedHashMap<>();

    public void register(ZoneConfig cfg) {
        byChapter.put(cfg.chapterId, cfg);
    }

    public ZoneConfig byChapter(int chapterId) {
        return byChapter.get(chapterId);
    }

    public Collection<ZoneConfig> all() {
        return Collections.unmodifiableCollection(byChapter.values());
    }
}
