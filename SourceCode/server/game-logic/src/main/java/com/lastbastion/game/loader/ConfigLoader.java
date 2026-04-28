package com.lastbastion.game.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastbastion.game.monetization.BattlePassConfig;
import com.lastbastion.game.survivor.SurvivorConfig;
import com.lastbastion.game.survivor.SurvivorConfigRepository;
import com.lastbastion.game.zone.ZoneConfig;
import com.lastbastion.game.zone.ZoneConfigRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一的配置加载器：从 classpath 下 /config/*.json 读取策划数据。
 */
public final class ConfigLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public SurvivorConfigRepository loadSurvivors() {
        SurvivorConfigRepository repo = new SurvivorConfigRepository();
        List<SurvivorConfig> list = readList("/config/survivors.json", SurvivorConfig.class);
        for (SurvivorConfig cfg : list) repo.register(cfg);
        return repo;
    }

    public ZoneConfigRepository loadZones() {
        ZoneConfigRepository repo = new ZoneConfigRepository();
        List<ZoneConfig> list = readList("/config/zones.json", ZoneConfig.class);
        for (ZoneConfig cfg : list) repo.register(cfg);
        return repo;
    }

    public BattlePassConfig loadBattlePass() {
        return BattlePassConfig.defaultSeason();
    }

    private <T> List<T> readList(String resource, Class<T> type) {
        try (InputStream is = ConfigLoader.class.getResourceAsStream(resource)) {
            if (is == null) return new ArrayList<>();
            return mapper.readValue(is,
                    mapper.getTypeFactory().constructCollectionType(ArrayList.class, type));
        } catch (Exception e) {
            throw new RuntimeException("failed to load " + resource, e);
        }
    }
}
