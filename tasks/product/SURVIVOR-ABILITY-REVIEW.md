# Survivor 技能策划 Review 表 — Phase E

> 用途：把当前 main 上 20 个 Survivor 的"独有技能 + 基础属性"逐人摆出来给策划过一遍。
> 任何字段需要调整时，请在最右边「策划批注」列写 **change to ... reason ...**，
> 程序按批注一次性改 `assets/numeric/survivors.json` 与 `combat-engine` 的 `AbilityLibrary`，
> **不要直接动现有数值表 / 代码** —— 与 [NUMERIC-TUNING-FINDINGS.md](NUMERIC-TUNING-FINDINGS.md) 的约定一致。

代码位置：
- 基础属性表：[`assets/numeric/survivors.json`](../../assets/numeric/survivors.json)
- 独有技能：[`AbilityLibrary.java`](../../SourceCode/server/game-logic/src/main/java/com/lastbastion/game/combat/AbilityLibrary.java)
- 战斗常量（致命伤害 / 暴击 / 状态时长等）：[`assets/numeric/combat.json`](../../assets/numeric/combat.json)
- 战斗模拟基线：[`tasks/product/BALANCE-REPORT-SAMPLE.md`](BALANCE-REPORT-SAMPLE.md) + 下文 §4

---

## 1. Legendary（5 人；权重 2%）

| 角色 | HP | ATK | DEF | SPD | 主动技能 | 数值 | 被动 | 数值 | 策划批注 |
|---|---:|---:|---:|---:|---|---|---|---|---|
| L_COMMANDER_REX  | 1200 | 130 | 90 | 110 | Rallying Cry — 单体 200% ATK 伤害 + 全队 ATK_UP 30%/2T | CD 4, 能量 60 | War Banner — 回合开始全队 SHIELD（HP 15%）/2T | 触发 CD 1 |  |
| L_GHOST_WRAITH   |  820 | 165 | 55 | 140 | Phantom Strike — 单体 220% ATK + 单体 POISON 25%/3T | CD 3, 能量 55 | Fear Grip — 攻击时单体 STUN 1T | 触发 CD 2 |  |
| L_DOC_SERAPH     |  900 | 115 | 60 | 115 | Purify Light — 全队治疗 150% ATK + 净化 | CD 3, 能量 55 | Field Triage — 回合开始最低血队友 +30% ATK 治疗 | 触发 CD 1 |  |
| L_FORGE_TITAN    | 1050 | 150 | 75 |  95 | Shield Wall — 全队 SHIELD（200% ATK）/2T + DEF_UP 25%/2T | CD 4, 能量 60 | Pain Feedback — 受击反伤 50% ATK 给攻击者 | 触发 CD 2 |  |
| L_HAVOC_QUEEN    |  860 | 155 | 55 | 135 | Annihilate — 全体敌人 160% ATK + BURN 15%/2T | CD 4, 能量 70 | Rising Fury — 回合开始自身 ATK_UP 15%/1T | 触发 CD 1 |  |

> **风险点**：`L_GHOST_WRAITH` 被动 STUN 1T 没有命中判定，只要攻击就触发；策划是否需要加抵抗概率？
> **风险点**：`L_HAVOC_QUEEN` 主动 AOE 160% × 5 = 800% ATK 实际期望，比单体 220% 高很多 —— 是否要把 AOE 系数压到 130%？

## 2. Epic（7 人；权重 13%）

| 角色 | HP | ATK | DEF | SPD | 技能 | 数值 | 策划批注 |
|---|---:|---:|---:|---:|---|---|---|
| E_IRONHIDE      | 980 | 100 | 75 | 100 | Iron Taunt — 单体 100% ATK + 自身 DEF_UP 30%/2T | CD 3, 能量 50 |  |
| E_SWIFT_BLADE   | 700 | 130 | 45 | 130 | Whirlwind — 全体 140% ATK | CD 3, 能量 55 |  |
| E_MENDER        | 780 |  95 | 55 | 110 | First Aid — 最低血治疗 200% ATK + 净化 | CD 2, 能量 40 |  |
| E_SAPPER        | 830 | 120 | 60 |  90 | Sticky Bomb — 单体 150% ATK + SPD_DOWN 30%/2T | CD 3, 能量 50 |  |
| E_RONIN         | 900 | 118 | 70 | 108 | Iaido Strike — 单体 250% ATK | CD 3, 能量 55 |  |
| E_SILENT_STEP   | 720 | 128 | 48 | 128 | Backstab — 单体 180% ATK + POISON 10%/3T | CD 3, 能量 50 |  |
| E_FIELD_MEDIC   | 800 |  92 | 55 | 112 | Stim Pack — 全队 80% ATK 治疗 + ATK_UP 15%/2T | CD 3, 能量 50 |  |

> **风险点**：`E_RONIN` 单体 250% 与 `L_GHOST_WRAITH` 220% 倒挂 —— Epic 强度盖过 Legendary 的单点伤害，需要确认是否预期。

## 3. Rare（8 人；权重 85%）

| 角色 | HP | ATK | DEF | SPD | 技能 | 数值 | 策划批注 |
|---|---:|---:|---:|---:|---|---|---|
| R_GRUNT        | 760 | 78 | 55 |  92 | Gun Down — 单体 120% ATK | CD 2, 能量 40 |  |
| R_SCOUT_ALPHA  | 600 |100 | 40 | 120 | Recon Shot — 单体 130% ATK + DEF_DOWN 15%/2T | CD 2, 能量 40 |  |
| R_CORPSMAN     | 680 | 75 | 48 | 100 | Patch Up — 最低血治疗 120% ATK | CD 2, 能量 40 |  |
| R_WRENCHJACK   | 720 | 92 | 52 |  88 | Repair Drone — 最低血 SHIELD 80% ATK + DEF_UP 20%/2T | CD 3, 能量 45 |  |
| R_BRAWLER      | 790 | 82 | 58 |  90 | Pummel — 单体 140% ATK | CD 2, 能量 40 |  |
| R_RANGER       | 620 | 95 | 42 | 115 | Suppressing Fire — 全体 100% ATK + SPD_DOWN 20%/1T | CD 3, 能量 50 |  |
| R_NURSE        | 660 | 72 | 45 | 105 | Bandage — 最低血治疗 140% ATK | CD 2, 能量 35 |  |
| R_TECHIE       | 700 | 88 | 50 |  85 | Sabotage — 单体 110% ATK + ATK_DOWN 20%/2T | CD 3, 能量 45 |  |

> **风险点**：8 个 Rare 之间的功能重叠（Grunt/Brawler 都是单体物伤；Corpsman/Nurse 都是单体治疗）—— 是否要做差异化（Brawler 加击晕、Nurse 加群疗等）？

## 4. balance-sim 当前基线（large size）

下面这组数字来自 `mvn -pl balance-sim exec:java -Dexec.mainClass=com.lastbastion.balance.BalanceSimMain -Dexec.args="--sim=all --size=large"`，是策划评估时的 baseline。

### 4.1 Gacha（10w 抽样本）
- RARE: 期望 85.0%，实测 84.5%（Δ −0.5）
- EPIC: 期望 13.0%，实测 13.0%（Δ −0.04）
- LEGENDARY: 期望 2.0%，实测 **2.5%**（Δ +0.5；保底命中 531 次，最长 79 抽内必出）
- 80 抽保底覆盖正常；偏热是有保底 + 自然 Legendary 叠加导致。

### 4.2 装备强化（2000 个样本，目标 +20）
- 100% 样本能强到 +20；总尝试次数 74215，平均 ~37 次 / 样本
- 失败率 46.1%（与策划表期望 60% 成功 → 40% 失败 接近，偏高 6%）
- **风险点**：策划表 `gear.successRate` 在 +15 ~ +20 段是否还要继续下调？

### 4.3 战斗胜率（每个 profile 200 局）
- **同战力**对线胜率 **56%**（策划目标 50% ± 5%；当前略偏向先手）
- 强战力（+30%）vs 弱战力：100% 胜（无翻车）
- 弱战力（−30%）vs 强战力：0% 胜（无奇迹）
- **风险点**：56% 同战力胜率说明先手优势 / 装备数值上限略偏强，建议把 SPD 系数（决定先手）下调 5%。

### 4.4 Zone 挂机收益（每小时 120 战）
- 12h cap：1440 场 / 期望 72 000 CREDITS
- 24h cap（VIP/付费）：2880 场 / 期望 144 000 CREDITS
- **风险点**：F-002（NUMERIC-TUNING-FINDINGS）已记录 —— Zone 离线"金币 / 装备 / 经验"三档奖励比例需要策划复核。

### 4.5 Battle Pass（30 天 / 60 天）
- 日免费 XP 600 → 30 天 18 000 XP / 等级 28
- 60 天 36 000 XP / 等级 42（终点 50 级需 48 250 XP）
- **结论**：免费玩家 60 天能到 42 级，剩 8 级靠付费层 / 周末活动补 ✓

### 4.6 Arena（100 人 / 30 天模拟）
- 分数区间 0 ~ 1455，平均 444.95
- 段位分布：Bronze 100 / Silver 8 / Gold+: 0
- **风险点**：F-001（NUMERIC-TUNING-FINDINGS）—— Master+ 渗透率 0% 需要策划重新画 Arena 段位曲线，要么降 Gold 门槛、要么提高每日 free challenge 数量。

---

## 5. 策划交付物 / 验收

策划在每行表格"策划批注"列写完后，提交到本仓库：
1. 我（程序）按批注一次性改 `assets/numeric/*.json` 与 `AbilityLibrary.java`
2. 重跑 `mvn test`（应保持 65 全绿）
3. 重跑 `mvn -pl balance-sim exec:java ...` 把上面 §4 的数字刷新
4. 把刷新后的报告 append 到 [`BALANCE-REPORT-SAMPLE.md`](BALANCE-REPORT-SAMPLE.md)，标注「调整版 vN」。

---

## 6. 已记录的字段设计问题（待策划裁决）

详见 [`NUMERIC-TUNING-FINDINGS.md`](NUMERIC-TUNING-FINDINGS.md)：

| ID | 标题 | 当前阻塞点 |
|---|---|---|
| F-001 | ARENA_HONOR 不在 `CurrencyType` 枚举里 | 决定升级为正式货币 or 改用 ARENA_TICKET |
| F-002 | Zone 离线奖励三档比例没在 `zones.json` 里 | 缺策划字段 |
| F-003 | `gear.json` 的 `successRate` 没有 +20 上限策略 | 是否要把成功率改成"+15 后每级 −5%" |
| F-004 | `augment.json` 合成槽位数与 `tasks` 文档对不上 | 4 槽 vs 文档 5 槽 |
| F-005 | `battlepass.json` 的 `xpCurve` 长度 = 50，缺第 0 级 | 与文档对齐 0~50 共 51 个值 |
| F-006 | `gacha.json` 概率字段名 `legendary` vs 代码 `LEGENDARY` 大小写不一 | 改 schema or 改代码读时 toUpper |
| F-007 | `combat.json` 缺 `critDmgMultiplier` 默认值时 fall back 1.5，未文档化 | 加进策划表 |

七项都不动数据，只动文档。
