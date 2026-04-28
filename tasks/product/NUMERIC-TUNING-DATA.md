# 数值调整所需数据清单 — Last Bastion

> 目的：交给策划/数值去「调整数值」时，要先知道 **哪些表** 决定了体验，字段分别代表什么，
> 以及当前代码里默认写死的一版数值在哪里、怎么改。
>
> 一旦策划开始调整，所有下列表应统一走 `SourceCode/server/.../resources/*.json`
> 或代码里 `defaultSeason()` / 常量的位置。做策划表时建议用 Google Sheets 同步，
> 每天导出一次 JSON 覆盖到仓库。

分 8 个大块。每块给出：
1. **该块决定的体验**（设计目的）
2. **当前默认数值的出处**（代码 / JSON 位置）
3. **数值表字段列表**（填 Excel 时的表头）
4. **关键平衡参数**（最该拉出来单独讨论的）
5. **推荐回归测试点**（改完要验证的）

---

## 1. 战斗引擎（TASK-002）

对应 `SourceCode/server/combat-engine/`，核心类 `DamageCalculator`、`CombatSimulator`、`AbilityExecutor`、`StatusEffectEngine`。

### 1.1 全局战斗常量（`DamageCalculator.java` / `StatusType.java`）

| 字段 | 当前默认 | 含义 |
|---|---|---|
| `CRIT_MULT` | 1.5 | 暴击倍率 |
| `ARMOR_K` | 100 | 护甲减伤曲线常数；减伤% = DEF / (DEF+100) |
| `MIN_DAMAGE` | 1 | 每次攻击最低保底 |
| `BOSS_RAGE_THRESHOLD` | 0.5 HP | Boss 进入 Rage 的血量阈值 |
| `RAGE_ATK_MULT` | 1.3 | Rage 期间攻击倍率 |
| `RAGE_SPD_MULT` | 1.2 | Rage 期间速度倍率 |
| `DOT_TICK_INTERVAL` | 1 回合 | DOT 每回合跳一次 |
| `SHIELD_DURATION_DEFAULT` | 3 回合 | 护盾默认持续 |

**需要策划单独给的**：
- 12 种 `StatusType`（STUN / FROZEN / BURN / POISON / BLEED / SHOCK / SHIELD / HASTE / WEAKEN / ROOT / SILENCE / TAUNT）各自的 **命中抗性曲线**、**免疫机制**（BOSS 免控层数）。
- 暴击/暴伤属性上限 cap。
- 护甲穿透 / 元素抗性系统是否 MVP 做。

### 1.2 属性加法 vs 乘法（`Stats.java`）

属性合并 = `base × (1 + Σpct) + Σflat`。每个属性（见 `AttributeType`）都支持 flat 与 pct 两条：HP / HP_PCT / ATK / ATK_PCT / DEF / DEF_PCT / SPD / CRIT / CRIT_DMG / ACC / EVA / RES。**策划需明确**每个来源（Survivor 基础、等级加成、Gear、Augment、技能 Buff、BP 被动）**落在 flat 还是 pct**，否则一次性改基础值会伤害全链。

### 1.3 回归验证点
- 5v5 BOSS 战平均回合数（目标 8~12 回合）。
- 对 R 稀有度单人血量比例（应 ~0.6 × L 稀有度）。
- CC 技能在 Boss 身上的实际生效率（策划写 10%，实战应在 9~11%）。

---

## 2. Survivor 系统（TASK-003）

### 2.1 Survivor 配置表 `survivors.json`

代码：`SourceCode/server/game-logic/src/main/resources/survivors.json`（20 行），同时镜像到 `SourceCode/client/assets/resources/config/survivors.json`。
类：`SurvivorConfig.java`。

| 字段 | 类型 | 说明 |
|---|---|---|
| `configId` | string | 唯一 ID，美术/代码共用 |
| `name` | string | 显示名 |
| `rarity` | enum `LEGENDARY/EPIC/RARE` | 决定 Gacha 概率与皮肤描边 |
| `role` | enum `DPS/TANK/SUPPORT/CONTROL` | 队伍搭配数值基础 |
| `baseHP` | int | 1 级基础生命 |
| `baseATK` | int | 1 级基础攻击 |
| `baseDEF` | int | 1 级基础防御 |
| `baseSPD` | int | 速度（决定出手顺序，≥ 100） |
| `growthHP` | double | 每级 HP 成长系数 |
| `growthATK` | double | 每级 ATK 成长系数 |
| `growthDEF` | double | 每级 DEF 成长系数 |
| `abilities` | list<abilityId> | 见下 §2.3 |

**策划需给**：20 人的完整基础值 & 成长值表。当前默认见 JSON 第 10-120 行。

### 2.2 等级曲线 `LevelCurve.java`

```
stat(level) = base × (1 + growth × (level - 1))
```
升级消耗：`levelUpCost(level) = 100 × level^1.4`（向上取整）金币。

**可调**：`growth` 指数、`1.4` 曲线常数（若改变会显著影响 D30 留存）。

### 2.3 技能 / Ability 表 `AbilityLibrary.java`（代码常量）

20 个 `configId` 每人挂 1-2 个技能 ID。每个技能里字段：

| 字段 | 含义 |
|---|---|
| `id` | string，例如 `L_COMMANDER_X_MAIN` |
| `targeting` | `SINGLE / ALL_ENEMIES / ALL_ALLIES / LOWEST_HP / ...` |
| `effects[]` | 数组：`DAMAGE(atkPct)`、`HEAL(hpPct)`、`APPLY_STATUS(type, turns)`、`BUFF(attr, pct, turns)`、`SHIELD(hpPct, turns)` 等 |
| `cooldown` | 回合 |
| `energyCost` | 能量消耗（0=被动） |
| `triggerHooks` | `PASSIVE_ON_TURN_START / ON_ATTACK / ON_TAKE_HIT`（被动） |

**策划需给**：25 条技能的完整效果数值表（Excel 每行一个 `effect` 拆开填）。当前代码里已经有一版可玩数值，改时定位 `AbilityLibrary.register*` 方法。

### 2.4 Gacha 抽卡 `GachaService.java`

| 参数 | 当前默认 | 含义 |
|---|---|---|
| `BASE_RATES` | L=1%, E=10%, R=89% | 普通池基础概率 |
| `HARD_PITY_AT` | 80 | 保底抽数（未出 L 必出） |
| `SOFT_PITY_START` | 70 | 软保底开始拉爆率 |
| `FREE_PULL_COST` | 1 `RECRUIT_TOKENS` | |
| `PREMIUM_PULL_COST` | 100 `PREMIUM_CHIPS` | |
| `DUPLICATE_SHARDS_L` | 50 | 重复 L 转碎片数量 |
| `DUPLICATE_SHARDS_E` | 20 | |
| `DUPLICATE_SHARDS_R` | 5 | |

**关键平衡参数**：
- 保底 80 或 90？直接影响月留存与付费深度。
- 限定 UP 池权重（MVP 未做，要上得先配置 UP_RATES 表）。

回归点：代码里 `GachaProbabilityTest` 会跑 10 万次抽样验证概率分布，改参数后测试需跟着调。

---

## 3. Gear 装备系统（TASK-004）

代码：`GearService.java` / `GearFactory.java`。

### 3.1 装备槽 & 品质

6 个 `GearSlot`（WEAPON / ARMOR / HELMET / BOOTS / ACCESSORY_A / ACCESSORY_B），4 个 `GearQuality`（COMMON / RARE / EPIC / LEGENDARY）。

### 3.2 主副词条表

| 字段 | 说明 |
|---|---|
| `slot` | 槽位 |
| `quality` | 品质 |
| `mainStatCandidates[]` | 该槽位允许的主词条 `AttributeType` |
| `mainStatBase[quality]` | 主词条基础数值（按 4 档品质表） |
| `subStatCount[quality]` | 副词条条数上限（COMMON 2，RARE 3，EPIC 4，LEGENDARY 4 初始 + 升级解锁） |
| `subStatRange[attr]` | 每个属性的副词条范围 `[min, max]` |

**策划需填** 一张 `gear_main_stats.csv` 和 `gear_sub_stats.csv`。

### 3.3 强化曲线

代码：`GearService.enhanceSuccessRate(level)` —— 当前：

| Level | 成功率 |
|---|---|
| 0→1 | 100% |
| 1→5 | 90% |
| 5→10 | 70% |
| 10→15 | 60% |

强化失败不降级但消耗材料。消耗公式：`alloyCost(level) = 50 × (level+1)^1.3`。

**策划要给完整 15 级的成功率 + 材料消耗表**；`GearEnhanceTest` 会做 1000 次抽样验证 15 级成功率 60%±3%。

### 3.4 分解返还

当前：分解 = 返还 30% 强化消耗 + 5% 概率返还一个稀有副词条材料。**需策划给分品质的具体数字。**

---

## 4. Augment 芯片（TASK-005）

代码：`AugmentService.java`，`AugmentInstance.toStats()`。

### 4.1 芯片类型表

5 种 `AugmentType`（ATK / DEF / HP / SPD / CRIT），每种 3 星。属性贡献当前代码写死：

| 类型 | 1 星 | 2 星 | 3 星 |
|---|---|---|---|
| ATK | +8% ATK | +16% ATK | +24% ATK |
| DEF | +10% DEF | +20% DEF | +30% DEF |
| HP | +12% HP | +24% HP | +36% HP |
| SPD | +5 SPD | +10 SPD | +15 SPD |
| CRIT | +5% CRIT / +10% CRIT_DMG | +10% / +20% | +15% / +30% |

星级倍率：`starMultiplier(star)` = `1.0 / 2.0 / 3.0`。**策划可能要拉成非线性曲线**。

### 4.2 3 合 1 合成

- 3 个 N 星 → 1 个 N+1 星（3→1）。
- 保留属性倾向（已实现），不保留具体子词条随机性（MVP 简化）。
- 消耗：`alloyCost(star) = 200 × star^2`。

**策划需要给**：稀有度补偿概率（大保底 / 暴击出货）目前未开。

---

## 5. Zone 挂机（TASK-006）

代码：`ZoneService.java`，配置：`resources/zones.json`。

### 5.1 章节/关卡表 `zones.json`

字段：

| 字段 | 说明 |
|---|---|
| `chapterId` | 1/2/3 |
| `chapterName` | 中文名 |
| `requirePowerRating` | 最低推荐战力 |
| `stages[]` | 40 关 |
| `stages[].stageId` | 1..N |
| `stages[].difficulty` | 敌方阵容战力 |
| `stages[].rewards.credits` | 通关金币 |
| `stages[].rewards.alloy` | 通关合金 |
| `stages[].rewards.premiumChips` | 通关钻石（仅通关一次） |
| `stages[].rewards.tokens` | 招募券 |
| `stages[].firstClearExtra` | 首通额外（图解锁，3 ⭐ 等） |
| `stages[].bossTemplateId` | 关联 Boss 模板 |

**策划填表时要平衡的**：
- 章节解锁节奏（1 章 10 关 vs 15 关？）
- 章节间推荐战力断崖幅度（一般 1.5×）
- 金币/素材掉落随关卡 ID 的爬升曲线（推荐 log 增长）

### 5.2 离线收益

`ZoneService.settleIdle(ctx)` 当前：
- `baseRewardPerHour`：按玩家已通关最高关的奖励 × 0.5
- 离线上限：**12 小时免费**，**24 小时上限**（Battle Pass 解锁 24h 上限）
- 掉落 cap：按单日金币/合金上限限制

**策划需给**：
- 离线时长（12h / 24h / 48h）
- 付费加速倍率（1h / 2h / 4h 多种道具）
- 不同关卡的离线收益比率（章节越后越高？）

---

## 6. Arena 竞技场（TASK-007）

代码：`ArenaService.java`，`ArenaState.java`。

### 6.1 每日挑战次数

| 参数 | 默认 | 说明 |
|---|---|---|
| `dailyFreeLeft` | 5 | 每日免费挑战 |
| `dailyMaxBuy` | 10 | 每日最多购买次数 |
| `buyCostPerTime` | 50 `PREMIUM_CHIPS` | 每次购买成本 |
| `pvpFormula` | 换位积分制 | 胜：+score，败：-score/2 |

### 6.2 段位 / 排名奖励

段位表：青铜/白银/黄金/白金/钻石/大师/王者（7 段位 × 每段 3 小阶 = 21 阶）。**策划需给** 每阶最低积分阈值。

每赛季结束发段位奖励 `seasonEndRewards(grade)`：**策划需给**（当前代码 stub 里每段固定返 50 钻 + 50 荣誉）。

### 6.3 匹配区间

代码：`ArenaService.match` 按 ±100 积分找对手，扩展到 ±500 兜底。**策划可调**：初始 window、扩展 step、兜底 window。

---

## 7. Battle Pass & 商业化（TASK-009）

代码：`BattlePassService.java` / `BattlePassConfig.java` / `BattlePassConfig.defaultSeason()`。

### 7.1 BP 等级经验表

每级升级所需 XP：当前 `xpRequired(level) = 100 × level`。

**策划需给** 50 级完整表，通常是 100/200/300/400/.../5000 且每 10 级一个「台阶」。

### 7.2 BP 奖励表（免费/付费双轨）

| 字段 | 说明 |
|---|---|
| `level` | 1..50 |
| `freeReward.kind` | CURRENCY / GEAR / SURVIVOR_SHARD / COSMETIC / ITEM |
| `freeReward.id` | 对应 configId / currency name |
| `freeReward.amount` | |
| `premiumReward.*` | 同上 |
| `premiumPlusReward.*` | Pass+ 额外奖励（MVP 不做可空） |

**策划重点**：L50 奖励必须是付费独占史诗物（提升 ARPU）。

### 7.3 BP 价格

| 档位 | 当前默认 |
|---|---|
| Pass 基础 | $4.99 |
| Pass+ 进阶 | $9.99 |
| 单级购买（已购 Pass） | $0.99 |

**策划需同步财务** 看 LTV / ROAS 目标。

### 7.4 Starter Pack & Limited Offer（`StarterPackService`, `LimitedOfferService`）

- 触发条件：首登陆 > 24h 且未付费，弹 Starter Pack（$0.99）。
- 限时折扣：玩家每周有 1 次限时礼包机会，价格 $2.99 / $4.99 两档，折扣 80% off 参考价。

**策划要给** 3 档礼包的具体奖励包（当前代码里是 placeholder：500 钻 + 3 招募券 + 5 金色碎片）。

---

## 8. 新手引导 & 每日任务（TASK-010）

代码：`OnboardingService.java` / `OnboardingStep.java` / `DailyQuestService.java`。

### 8.1 10 步引导顺序（已在代码枚举定死）

| 步骤 | 触发条件 | 策划可调点 |
|---|---|---|
| INTRO_CINEMATIC | 游戏启动 | 跳过后是否返回 |
| FIRST_SURVIVOR | 剧情结束 | 指定首赠送 L_COMMANDER_X？ |
| ZONE_1_1_FIGHT | 拿到首角色 | 胜利条件是否强制 |
| EQUIP_GEAR | 1-1 通关 | 赠送装备品质 |
| LEVELUP_SURVIVOR | 装备后 | 赠送经验书数量 |
| ZONE_1_2_FIGHT | 升级后 | |
| STARTER_PACK_POPUP | 1-2 通关 | 弹出延迟 |
| ARENA_ENTRY | 战力 ≥ X | **策划给 X 阈值** |
| BATTLE_PASS_HIGHLIGHT | 首 Arena 胜利 | |
| COMPLETE | 上一步完成 | 送完引导奖 |

### 8.2 每日/三日任务表

3 日任务当前默认 9 个（每日 3 个），奖励约 300 金 + 1 招募券。
**策划需给** 完整 D1/D2/D3 任务列表（含条件、计数目标、奖励）。

---

## 9. 资源 & 道具（TASK-008）

代码：`ResourceService.java`，货币 `CurrencyType` 5 种。

### 9.1 货币上限

| 货币 | 当前上限 |
|---|---|
| CREDITS | 999,999,999 |
| ALLOY | 9,999,999 |
| PREMIUM_CHIPS | 9,999,999 |
| RECRUIT_TOKENS | 9,999 |
| ARENA_HONOR | 99,999 |

**策划可调**：上限 & 溢出政策（删 / 转金 / 返还）。

### 9.2 道具 `items` Map

每道具字段：`itemId / name / stackable / useType / effect`。当前代码里仅保留 Map 结构，**实际 item 清单待策划填**（经验书档位 / 升阶石稀有度 / 临时加成药剂 等）。

---

## 10. Analytics / 埋点（TASK-011）

代码：`AnalyticsService.java`，事件类 `AnalyticsEvent`。

### 10.1 现已打点事件（21 种）

以下事件 **key** 策划可以改名字但不要乱删：

| key | 触发点 |
|---|---|
| `user_login` | user.login |
| `zone_attempt` | 每次进 Zone |
| `zone_complete` | 每次 Zone 胜利 |
| `currency_add` | 任意货币增加（含 source） |
| `currency_spend` | 任意货币消耗（含 source） |
| `gacha_pull` | 抽卡（pool / count） |
| `survivor_grant` | 获得 Survivor（cfgId） |
| `gear_enhance` | 强化（level_before/after） |
| `augment_fuse` | 芯片合成 |
| `arena_match` / `arena_result` | 竞技场 |
| `bp_xp_gain` / `bp_level_up` / `bp_claim` / `bp_buy` | Battle Pass |
| `starter_pack_shown` / `starter_pack_purchase` | 新手包 |
| `onboarding_step_complete` / `onboarding_skip` | 引导 |
| `iap_verify_success` / `iap_verify_fail` | 支付校验 |

**策划需要决定**：
- 每个事件的统计维度（dau / retention / 漏斗中哪一步）。
- 埋点追加项（例如 `zone_complete` 是否加 `combat_duration_ms`、`team_power_rating`）。
- 导出目标（Firebase Analytics / AppsFlyer / 自建）。

---

## 11. 汇总：策划需要新建的 Excel / JSON

| 文件 | 字段来源 | 对应游戏模块 |
|---|---|---|
| `survivors.xlsx` (→ JSON) | §2.1 | Survivor 基础数值 |
| `abilities.xlsx` | §2.3 | 技能效果 |
| `gear_main_stats.csv` | §3.2 | 主词条 |
| `gear_sub_stats.csv` | §3.2 | 副词条 |
| `gear_enhance.csv` | §3.3 | 强化成功率 & 消耗 |
| `augments.xlsx` | §4.1 | 芯片属性 |
| `zones.xlsx` (→ JSON) | §5.1 | 关卡奖励 & 难度 |
| `idle_rates.xlsx` | §5.2 | 离线收益 |
| `arena_ranks.xlsx` | §6.2 | 段位阈值 |
| `bp_levels.xlsx` | §7.1/7.2 | BP 经验 & 奖励 |
| `shop_packs.xlsx` | §7.4 | 礼包 |
| `onboarding_steps.md` | §8.1 | 引导条件 |
| `daily_quests.xlsx` | §8.2 | 每日任务 |
| `items.xlsx` | §9.2 | 道具 |
| `analytics_events.md` | §10 | 埋点字段 |

---

## 12. 迭代流程建议

1. 策划在 Sheets 维护上述表；
2. 每版导出到 `SourceCode/server/**/resources/*.json` 并提 PR；
3. CI 跑单元测试，若概率/数值相关测试挂了（`GachaProbabilityTest` / `GearEnhanceTest` / `BattlePassTest`），必须同步改测试上限 / 下限；
4. 测试服部署前检查数值 diff；
5. 上线后用 Analytics 看真实分布（付费率 / 通关率 / 留存）再微调。

---

**维护人**：策划主策 + 服务端程序。
