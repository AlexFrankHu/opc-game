# TASK-012 — 数值配置接入运行链路（Numeric Config Integration）

> 目标：把 `assets/numeric/` 下的全部数值配置接入项目运行链路，消灭服务端代码里散落的「硬编码 / placeholder / `defaultSeason()`」一类常量。

## 1. 背景

MVP 落地后，数值常量分散在多个 Service 类里：

- `combat-engine/DamageCalculator`：`CRIT_MULT`、`ARMOR_K`、状态修正系数
- `survivor/GachaService`：`RATES`、`PITY_LIMIT`、各币种抽卡成本、`SHARDS_PER_DUPLICATE`
- `gear/GearService`：`MAX_LEVEL`、`BAG_CAPACITY_DEFAULT`、`successRate(toLevel)` switch、`alloyByQuality()` switch、消耗公式
- `augment/AugmentService` + `AugmentInstance`：5 类 × 6 星属性表、`starMultiplier()`、`BAG_CAPACITY`、`REMOVE_COST_CREDITS`
- `arena/ArenaService`：`DAILY_FREE_CHALLENGES`、`DAILY_BUY_LIMIT`、`BUY_COST_CHIPS`、`MATCH_POOL_SIZE`、匹配区间 ±15%、胜负积分 +25/-15
- `monetization/BattlePassConfig.defaultSeason()`：50 级经验曲线 + 双轨奖励硬编码
- `monetization/StarterPackService`：`PRICE_CENTS`、`PREMIUM_CHIPS`、`RECRUIT_TOKENS`，触发条件硬编码 chap=1/stage=3
- `monetization/LimitedOfferService`：依赖外部注册，没有默认包
- `resource/ResourceService`：货币 `CAPS` 静态 EnumMap
- `zone/ZoneService`：`IDLE_CAP_MS=12h`、`IDLE_CAP_PREMIUM_MS=24h`、`fightsPerHour=120`
- `onboarding/OnboardingStep`（枚举 + `SKIP_ALLOWED_AFTER`）+ `DailyQuestService.seedDefaultFirstThreeDays()`

策划无法在不发版的情况下调整这些数值。需要把它们抽到独立可热更的配置文件。

## 2. 范围

### 2.1 In Scope（本任务）

1. 在 **仓库根** 新建 `assets/numeric/` 目录，按模块拆分 JSON 文件：
   ```
   assets/numeric/
     combat.json          # 战斗常量 + 状态修正
     gacha.json           # 抽卡概率/成本/保底/碎片
     gear.json            # 强化曲线 / 分解返还 / 背包 / 消耗公式
     augment.json         # 芯片属性表 + 星级倍率 + 合成消耗
     arena.json           # 日常上限 / 匹配 / 积分公式 / 段位
     battlepass.json      # 50 级 XP 曲线 + free/premium track
     resources.json       # 货币上限
     zone_idle.json       # 离线挂机参数
     starter_pack.json    # Starter Pack 触发 + 奖励
     limited_offers.json  # 限时礼包默认列表
     onboarding.json      # 引导步骤序列 + 跳过门槛
     daily_quests.json    # 前 3 日每日任务
     survivors.json       # 已存在；改为 single source of truth
     zones.json           # 已存在；同上
   ```

2. 实现 `NumericConfig` 聚合对象 + `NumericConfigLoader`：
   - 读取顺序：`-Dnumeric.dir=<path>` → 进程工作目录 `./assets/numeric/` → 仓库根 `./assets/numeric/` → classpath `/numeric/`
   - 只用 Jackson 反序列化，schema 错误必须**抛异常**（fail-fast），不允许默默兜底
   - 提供 unit test 用 in-memory 构造方法（避免每个测试都依赖文件 IO）

3. 在 `GameBootstrap.boot()` 改为：
   ```
   NumericConfig nc = NumericConfigLoader.load();
   ResourceService resource = new ResourceService(analytics, nc.resources());
   GachaService gacha = new GachaService(..., nc.gacha());
   GearService gear   = new GearService(..., nc.gear());
   AugmentService aug = new AugmentService(..., nc.augment());
   ArenaService arena = new ArenaService(..., nc.arena());
   ZoneService zone   = new ZoneService(..., nc.zoneIdle());
   BattlePassConfig bp = nc.battlePass().toBattlePassConfig();
   StarterPackService sp = new StarterPackService(..., nc.starterPack());
   LimitedOfferService lo = new LimitedOfferService(..., nc.limitedOffers());
   DailyQuestService dq = new DailyQuestService(resource, nc.dailyQuests());
   OnboardingService ob = new OnboardingService(analytics, nc.onboarding());
   DamageCalculator dc  = new DamageCalculator(rng, nc.combat());
   ```
   每个 Service 增加「接受 Tuning」的构造器；旧的零参/默认构造器仅给 unit test 用，且内部调用 `NumericConfig.defaults()` 走同一份 JSON。

4. 把仓库根 `assets/numeric/` 用 `maven-resources-plugin` 同步到 server `game-logic` 的 `target/classes/numeric/`，避免开发期手工双写：
   ```xml
   <resource>
     <directory>${project.basedir}/../../../assets/numeric</directory>
     <targetPath>numeric</targetPath>
     <filtering>false</filtering>
   </resource>
   ```

5. 客户端 (`SourceCode/client/assets/resources/config/`) 维持现有 `survivors.json` / `zones.json` 镜像，**本任务不改客户端**。客户端的同步在后续单独任务做。

### 2.2 Out of Scope

- 数值平衡调整。本任务**严格**保持现有数值（即从代码导出的当前默认值）作为初始 JSON。
- ioGame ActionController 路由变化。
- Cocos 客户端任何改动。
- Excel/CSV 中间格式 → JSON 的转换工具链。

## 3. 实施步骤

| # | 子任务 | 验证 |
|---|---|---|
| 1 | 新建 `assets/numeric/*.json` 13 个文件（survivors/zones 沿用现有，其他 11 张全部从源码导出） | 每个 JSON 可被 Jackson `readTree` 解析 |
| 2 | 写 `NumericConfig` (top-level) + 11 个子配置 record / class | `NumericConfig.defaults()` 可在内存里构造一份等价值 |
| 3 | 写 `NumericConfigLoader.load()` 优先级链路 | unit test：写一个临时目录覆盖 `-Dnumeric.dir`，Loader 用临时值，未覆盖字段从 classpath 兜底 |
| 4 | 改 13 个 Service 的构造器 / 静态字段，从 Tuning 取值 | `mvn -pl game-logic test` 全绿 |
| 5 | 改 `GameBootstrap.boot()` 串起来 | `mvn -pl app test` 全绿（含 EndToEndWsTest） |
| 6 | `pom.xml` 加 resource 同步 | `mvn -pl game-logic compile` 后 `target/classes/numeric/` 13 个文件齐全 |
| 7 | 跑全量 `mvn test` | 全绿（基线 46 测试） |

## 4. 关键约束

1. **不静默修改策划数值**。如果发现现有代码字段命名 / 单位与 `tasks/product/NUMERIC-TUNING-DATA.md` 描述不一致（如「`enhance` 消耗公式 = 20 + 10 × level」与 NUMERIC-TUNING-DATA 写的「`50 × (level+1)^1.3`」），**记录到 `tasks/product/NUMERIC-TUNING-FINDINGS.md`**，本任务保持代码当前值，由策划在 TASK-013/后续微调。
2. JSON 文件不带注释（标准 JSON），需要解释字段时写到同目录 `README.md`。
3. 配置文件 schema 一处错（必填字段缺失、类型不匹配），整个进程启动失败 + log 打印缺失字段名。

## 5. 验收标准

- [ ] `assets/numeric/` 下 13 个文件齐全
- [ ] `NumericConfigLoader` 单元测试覆盖：从 classpath 加载 / 从 `-Dnumeric.dir` 覆盖加载 / schema 错误抛异常
- [ ] `mvn test` 全绿，原 46 测试 + 新增 ≥ 5 测试
- [ ] `git grep -nE 'static final (long|int|double).*=' SourceCode/server/game-logic` 检查后，**剩余的常量全部是非数值平衡量**（如 `MAX_LEVEL=20` 这种结构常量、`EnumMap` 的 size）
- [ ] `tasks/product/NUMERIC-TUNING-FINDINGS.md` 列出所有发现的 schema/字段差异
- [ ] commit 到 `main`

## 6. 后续任务

完成后进入 **TASK-013 — Balance Simulation & Validation**：自动化模拟 + 回归断言（gacha 概率分布 / Zone 难度曲线 / BP 50 级 XP / Arena 分数 / 战斗胜率分布等）。
