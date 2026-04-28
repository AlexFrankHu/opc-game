# TASK-013 — 平衡模拟与回归验证（Balance Simulation & Validation）

> 前置：[TASK-012](TASK-012-numeric-config-integration.md) 完成。所有数值已外置到 `assets/numeric/`。

## 1. 背景

数值外置后，策划每改一版 `assets/numeric/*.json`，必须有一套**自动化模拟**确认：

- 战斗胜率/平均回合数没有被改炸；
- Gacha 概率分布在统计意义上仍然贴合策划写的 `RATES`；
- Zone 关卡难度爬坡曲线在玩家战力线性增长下仍然线性；
- Battle Pass 50 级总 XP 与日均产出匹配；
- Arena 积分变化在长期循环下不发散。

只用单元测试样例不够 —— 需要 Monte Carlo 跑大样本，并把指标 dump 出来给策划看。

## 2. 范围

### 2.1 In Scope

1. 新建 maven module **`balance-sim`**（在 `SourceCode/server/balance-sim/`，依赖 `combat-engine` + `game-logic`）。该模块只在测试时跑，不打进生产 jar。

2. **6 个模拟器**：

   | 模拟器 | 输入 | 输出 | 断言 |
   |---|---|---|---|
   | `GachaDistributionSim` | 池 / 抽次（10 万） | 各稀有度命中率 / 80 抽内出 L 比例 | 稀有度命中率与 `gacha.json#rates` 偏差 ≤ 0.5% |
   | `GearEnhanceSim` | 装备 0→20 / 1000 次 | 平均消耗 / 失败次数 / 期望最高等级 | 15 级到达率 ≥ 50%（与 `gear.json#successRates` 一致） |
   | `CombatBalanceSim` | 5v5 推图 1-1..3-15，每关 100 场 | 胜率 / 平均回合数 | 战力 1.0×：胜率 ∈ [40%,75%]；战力 1.5×：胜率 ∈ [80%,100%]；平均回合 ∈ [4, 18] |
   | `ZoneIdleSim` | 12h / 24h 离线 | 各币种产出 | 与 `zone_idle.json#fightsPerHour × drops` 计算值一致（±5%） |
   | `BattlePassSim` | 30 天日均 BP XP（来自 zone + arena + quest） | 60 日完成度分布 | 日均 BP XP × 30 ≥ `battlepass.json#xpCurve[40]`（即免费玩家 30 天能到 40 级） |
   | `ArenaScoreSim` | 100 玩家 / 30 天每天 5 场 | 积分分布 / 段位人数 | 大师及以上 ≤ 5%；最高 / 最低分差 ≤ 3000 |

3. **CLI 入口** `BalanceSimMain`：
   ```
   mvn -pl balance-sim exec:java -Dexec.mainClass=com.lastbastion.balance.BalanceSimMain -Dexec.args="--sim=all --out=balance-report.json"
   ```
   - 读取 `assets/numeric/`（同 TASK-012 链路），不重复造一份配置
   - 输出 JSON 报告 + Markdown 表格双格式
   - exit code = 0 全绿 / 1 至少一个断言不通过 / 2 配置加载失败

4. **回归测试** `BalanceRegressionTest`：
   - JUnit 测试，每次 `mvn test` 跑（小样本：每模拟器 1000 次 / 10 关 / 30 玩家）
   - 与「锁定基线值」对比（保存在 `balance-sim/src/test/resources/baseline.json`）
   - 偏差超阈值断言失败，错误消息打印「实际值 vs 基线值 vs 阈值」便于定位

5. **基线生成工具** `BaselineWriter`：
   - 主程一次跑 10 万样本，生成新 baseline 写入 `baseline.json`
   - 仅当策划主动调整数值后，由数值人手动跑一次 + 在 PR 中 review baseline diff

### 2.2 Out of Scope

- 与第三方平衡分析平台对接
- 实时在服务端运行（这是离线工具）
- 客户端可视化
- 自动按指标反推数值最优解（只验证，不优化）

## 3. 实施步骤

1. 新建 `balance-sim` module，加入 root `pom.xml`；
2. 建 `domain/`：`SimRandomSource`（固定种子）、`MockPlayer`、`MockEnemyTeam`；
3. 实现 6 个 Sim，每个 Sim 一个 `*Result` POJO；
4. 实现 `BalanceSimMain` CLI；
5. 跑一次 `BalanceSimMain --sim=all` 生成 `baseline.json`；
6. 实现 `BalanceRegressionTest`，断言全绿；
7. CI（即 `mvn test`）跑回归测试。

## 4. 验收标准

- [ ] `balance-sim` module 编译通过
- [ ] `BalanceSimMain --sim=all` 在 main 上跑出来全绿（每个模拟器都有断言通过）
- [ ] `mvn test` 包含 `BalanceRegressionTest`，且在主线代码不改时全绿
- [ ] `baseline.json` 已 checkin
- [ ] 报告样例：`tasks/product/BALANCE-REPORT-SAMPLE.md`（一份运行输出）

## 5. 风险

- **Combat 胜率受 RNG 影响大**：固定 seed 的同时，仍然可能出现 5% 偶发偏差。基线阈值需要给「上下阈值范围」，不是单点等值。
- **测试运行时长**：每次 `mvn test` 跑 1000 场战斗 ≈ 30 秒。若超 60 秒，把样本量降一档（500），并用 `@Tag("slow")` 把 10 万样本版放到 nightly。
- **数值改动后基线必须刷**：要求 PR 流程在改 `assets/numeric/` 时同步刷新 `baseline.json`，否则 `BalanceRegressionTest` 必挂。
