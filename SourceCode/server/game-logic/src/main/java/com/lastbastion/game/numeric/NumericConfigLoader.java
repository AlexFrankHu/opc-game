package com.lastbastion.game.numeric;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 加载 {@code assets/numeric/} 下全部 JSON。
 *
 * <p>查找优先级：
 * <ol>
 *   <li>系统属性 {@code -Dnumeric.dir=...}</li>
 *   <li>进程工作目录 {@code ./assets/numeric/}</li>
 *   <li>仓库根 {@code ./../../../../assets/numeric/}（开发期 IDE 跑测试时）</li>
 *   <li>classpath {@code /numeric/}（jar 内置）</li>
 * </ol>
 *
 * <p>任意一个 JSON 缺失或字段不匹配，均直接抛 {@link NumericConfigException}（fail-fast）。
 */
public final class NumericConfigLoader {

    public static final String SYSTEM_PROPERTY = "numeric.dir";
    public static final String CWD_DIR = "assets/numeric";

    private final ObjectMapper mapper;

    public NumericConfigLoader() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
    }

    /** 默认入口：按优先级链加载。 */
    public static NumericConfig load() {
        return new NumericConfigLoader().loadInternal();
    }

    /** 仅供测试：直接从 classpath 加载。 */
    public static NumericConfig fromClasspath() {
        NumericConfigLoader l = new NumericConfigLoader();
        return l.buildFromSource(Source.classpath());
    }

    /** 测试便利：从指定目录加载（不含 classpath fallback）。 */
    public static NumericConfig fromDir(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new NumericConfigException("numeric dir not found: " + dir);
        }
        NumericConfigLoader l = new NumericConfigLoader();
        return l.buildFromSource(Source.dir(dir));
    }

    private NumericConfig loadInternal() {
        Source src = resolveSource();
        return buildFromSource(src);
    }

    private Source resolveSource() {
        String prop = System.getProperty(SYSTEM_PROPERTY);
        if (prop != null && !prop.isEmpty()) {
            Path p = Path.of(prop);
            if (!Files.isDirectory(p)) {
                throw new NumericConfigException("system property " + SYSTEM_PROPERTY + " points to missing dir: " + p);
            }
            return Source.dir(p);
        }
        Path cwd = Path.of(CWD_DIR);
        if (Files.isDirectory(cwd)) return Source.dir(cwd);

        // 开发期 IDE 把工作目录指到模块下时，回到仓库根 4 级。
        for (Path candidate : new Path[]{
                Path.of("..", CWD_DIR),
                Path.of("..", "..", CWD_DIR),
                Path.of("..", "..", "..", CWD_DIR),
                Path.of("..", "..", "..", "..", CWD_DIR)
        }) {
            if (Files.isDirectory(candidate)) return Source.dir(candidate);
        }
        return Source.classpath();
    }

    private NumericConfig buildFromSource(Source src) {
        CombatTuning combat = read(src, "combat.json", CombatTuning.class);
        GachaTuning gacha = read(src, "gacha.json", GachaTuning.class);
        GearTuning gear = read(src, "gear.json", GearTuning.class);
        AugmentTuning augment = read(src, "augment.json", AugmentTuning.class);
        ArenaTuning arena = read(src, "arena.json", ArenaTuning.class);
        BattlePassTuning battlePass = read(src, "battlepass.json", BattlePassTuning.class);
        ResourceTuning resources = read(src, "resources.json", ResourceTuning.class);
        ZoneIdleTuning zoneIdle = read(src, "zone_idle.json", ZoneIdleTuning.class);
        StarterPackTuning starterPack = read(src, "starter_pack.json", StarterPackTuning.class);
        LimitedOffersTuning limitedOffers = read(src, "limited_offers.json", LimitedOffersTuning.class);
        OnboardingTuning onboarding = read(src, "onboarding.json", OnboardingTuning.class);
        DailyQuestsTuning dailyQuests = read(src, "daily_quests.json", DailyQuestsTuning.class);
        validate(combat, gacha, gear, augment, arena, battlePass, resources, zoneIdle,
                starterPack, limitedOffers, onboarding, dailyQuests);
        return new NumericConfig(combat, gacha, gear, augment, arena, battlePass, resources,
                zoneIdle, starterPack, limitedOffers, onboarding, dailyQuests);
    }

    private void validate(CombatTuning combat, GachaTuning gacha, GearTuning gear,
                          AugmentTuning augment, ArenaTuning arena, BattlePassTuning battlePass,
                          ResourceTuning resources, ZoneIdleTuning zoneIdle,
                          StarterPackTuning starterPack, LimitedOffersTuning limitedOffers,
                          OnboardingTuning onboarding, DailyQuestsTuning dailyQuests) {
        if (gacha.rates.isEmpty()) throw new NumericConfigException("gacha.rates empty");
        double sum = 0; for (double v : gacha.rates.values()) sum += v;
        if (Math.abs(sum - 1.0) > 1e-6) {
            throw new NumericConfigException("gacha.rates must sum to 1.0, got " + sum);
        }
        if (gear.maxLevel <= 0) throw new NumericConfigException("gear.maxLevel must be > 0");
        for (int lv = 1; lv <= gear.maxLevel; lv++) {
            if (!gear.successRates.containsKey(Integer.toString(lv))) {
                throw new NumericConfigException("gear.successRates missing level " + lv);
            }
        }
        if (battlePass.xpCurve.size() != battlePass.maxLevel + 1) {
            throw new NumericConfigException("battlePass.xpCurve length must be maxLevel+1");
        }
        if (onboarding.steps.isEmpty()) throw new NumericConfigException("onboarding.steps empty");
        if (resources.currencyCaps.isEmpty()) throw new NumericConfigException("resources.currencyCaps empty");
    }

    private <T> T read(Source src, String fileName, Class<T> type) {
        try (InputStream is = src.open(fileName)) {
            if (is == null) {
                throw new NumericConfigException("missing config file: " + fileName + " (source=" + src.describe() + ")");
            }
            return mapper.readValue(is, type);
        } catch (IOException e) {
            throw new NumericConfigException("failed to parse " + fileName + " (source=" + src.describe() + "): " + e.getMessage(), e);
        }
    }

    /** 配置来源抽象。 */
    private static final class Source {
        private final Path dir;

        private Source(Path dir) { this.dir = dir; }

        static Source dir(Path d) { return new Source(d); }
        static Source classpath() { return new Source(null); }

        InputStream open(String name) throws IOException {
            if (dir != null) {
                Path p = dir.resolve(name);
                if (!Files.isRegularFile(p)) return null;
                return Files.newInputStream(p);
            }
            return NumericConfigLoader.class.getResourceAsStream("/numeric/" + name);
        }

        String describe() {
            return dir != null ? "dir:" + dir.toAbsolutePath() : "classpath:/numeric/";
        }
    }
}
