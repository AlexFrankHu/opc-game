# `assets/numeric/` — 数值配置（策划可调）

本目录是 **数值层 single source of truth**。服务端在启动时通过 `NumericConfigLoader` 加载，
打包时通过 maven-resources-plugin 同步到 `SourceCode/server/game-logic/target/classes/numeric/`。

## 文件清单

| 文件 | 模块 | 字段说明 |
|---|---|---|
| `combat.json` | TASK-002 战斗引擎 | 暴击倍率 / 护甲常数 / 状态修正系数 / Boss Rage 阈值 |
| `gacha.json` | TASK-003 招募 | 概率表 / 保底 / 抽卡成本 / 重复转碎片 |
| `gear.json` | TASK-004 装备 | 强化成功率曲线（1..20）/ 强化消耗 / 分解返还 / 背包容量 |
| `augment.json` | TASK-005 芯片 | 5 类型 × 6 星级属性 / 星级倍率 / 合成消耗 / 卸下成本 |
| `arena.json` | TASK-007 竞技场 | 每日上限 / 购买价 / 匹配窗口 / 胜负积分 / 段位阈值 |
| `battlepass.json` | TASK-009 BP | 50 级 XP 曲线 / free + premium 双轨奖励 |
| `resources.json` | TASK-008 资源 | 货币上限 |
| `zone_idle.json` | TASK-006 离线 | 离线上限 / Premium 上限 / fightsPerHour 节奏 |
| `starter_pack.json` | TASK-009 商业化 | Starter Pack 触发条件 + 奖励 |
| `limited_offers.json` | TASK-009 商业化 | 限时礼包默认列表（可空数组） |
| `onboarding.json` | TASK-010 引导 | 10 步顺序 + 跳过门槛 |
| `daily_quests.json` | TASK-010 任务 | 前 3 日每日任务 |
| `survivors.json` | TASK-003 角色 | 20 名 Survivor 的基础属性 |
| `zones.json` | TASK-006 关卡 | 3 章 40 关阵容/掉落/首通奖励 |

## 修改流程

1. 改 JSON
2. 跑 `mvn -pl game-logic test` 局部回归
3. 跑 `mvn -pl balance-sim test` 全量回归
4. 若 `BalanceRegressionTest` 挂，要么改回 JSON，要么用 `BaselineWriter` 重新生成基线
5. 提 PR 给主程 + 数值双 review

## Schema 约定

- 所有 JSON 文件 **不允许带注释**（标准 JSON）
- 不允许冗余字段（Jackson `FAIL_ON_UNKNOWN_PROPERTIES = true`，缺失字段或多余字段都会抛异常）
- 整数用 JSON `number`（不带引号），不要写 `"5"` 字符串
- 概率写小数（`0.85`），不写百分号

## 优先级

`NumericConfigLoader.load()` 顺序：

1. `-Dnumeric.dir=<path>` 系统属性指定的目录
2. 进程 CWD 下 `./assets/numeric/`
3. classpath `/numeric/`（即打包到 jar 内的 `target/classes/numeric/`）

任何一项缺失文件直接 fail-fast，不静默兜底。
