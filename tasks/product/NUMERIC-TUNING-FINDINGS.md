# NUMERIC-TUNING — 字段设计问题清单（TASK-012 阶段）

> 本文记录 TASK-012「数值配置接入」过程中发现的策划表 / 协议层字段设计冲突。
> **不在本次实现里静默修改策划表**；以下条目需要由策划/产品方决策后再回写到 `assets/numeric/*.json`、`NUMERIC-TUNING-DATA.md`、以及对应的 Java enum / DTO。

记录格式：编号 / 字段 / 现状 / 影响 / 建议。

---

## F-001 `ARENA_HONOR` 出现在 Arena 奖励但未列入 CurrencyType 枚举

- **来源**
  - `assets/numeric/arena.json` `ranks[].seasonRewardHonor`
  - 历史代码 `ArenaService` 调用 `resource.add(..., ArenaService.SourceTag.ARENA_DAILY)` 时只用过 PREMIUM_CHIPS，没有 ARENA_HONOR 这一货币。
  - `tasks/product/NUMERIC-TUNING-DATA.md` 第 3 节同时提及"荣誉点 (ArenaHonor)"。
- **现状**
  - `com.lastbastion.common.CurrencyType` 只定义 5 种：CREDITS / ALLOY / TECH_CORES / RECRUIT_TOKENS / PREMIUM_CHIPS。
  - `assets/numeric/resources.json` 初版同时声明了 ARENA_HONOR 容量上限，导致 Jackson 反序列化抛 `InvalidFormatException`。
  - 当前妥协：`resources.json` 中已去掉 ARENA_HONOR 一行，让 NumericConfig 能成功加载；但 `arena.json` 中的 `seasonRewardHonor` 字段仍是数据流中的悬空字段（没人消费）。
- **影响**
  - Arena 段位赛奖励里"荣誉点"没有运行时载体，玩家结算时拿不到；纯数据保留，未真正发放。
  - 后续若加 Arena 商店（用荣誉点兑换），必须先扩 `CurrencyType`，否则资源系统、上限、流水埋点都无法承接。
- **建议**
  1. 由策划确认 ARENA_HONOR 是否升级为正式货币（是否进背包、是否走 ResourceService 流水）。
  2. 若是 → 在 `CurrencyType` 添加 `ARENA_HONOR`，把它也加回 `resources.json.currencyCaps`，并在 `ArenaService` 季末发奖时调用 `resource.add(...)`。
  3. 若否 → 把 `arena.json.ranks[].seasonRewardHonor` 改名为 `seasonRewardCustomItem`（或干脆移除），并在策划文档里说明它只是 UI 文案，不参与资源核算。

---

## F-002 `arena.json` 缺失 4 个分数 delta 字段

- **来源**
  - 战斗结算逻辑 `ArenaService#challenge` 使用 4 种分数变化：胜+换位 +25 / 单胜 +10 / 单败 -5 / 被换位的对手 -15。
  - 初版 `NUMERIC-TUNING-DATA.md` 第 5 节只列了 `scoreOnWinDelta` / `scoreOnLossDelta` 两条，schema 与代码行为不匹配。
- **现状**
  - 已在 `assets/numeric/arena.json` 落地为四字段：`scoreWinSwap` / `scoreWinNoSwap` / `scoreLossSelf` / `scoreLossOpponentOnSwap`。
  - 同步修改了 `ArenaTuning` POJO 与 `ArenaService` 读取路径。
  - **没有改动策划默认值**（仍按代码原值 25/10/-5/-15）。
- **影响**
  - 之前"两条 delta 字段"的策划文档与实现不一致，会让数值同学误以为 Arena 只有两个挡位。
- **建议**
  - 在 `tasks/product/NUMERIC-TUNING-DATA.md` 第 5 节加上四字段说明，并标注"换位失败方"的负值需要单独配置。
  - 后续若想做"主动放弃换位"或"段位保护"等策略，schema 已留好扩展位。

---

## F-003 `ranks[].id` 与 `BattlePassReward.payload` 类似的"自由字符串"字段缺约束

- **来源**
  - `arena.json.ranks[].id` 当前用字符串（BRONZE / SILVER / …），运行时只在 UI 文案里使用。
  - `battlepass.json` 中 `EXCLUSIVE_SURVIVOR` 奖励的 `payload` 也是自由字符串（如 `S_LEG_SEASON1`），与 `survivors.json` 内 ID 没有外键校验。
- **现状**
  - NumericConfigLoader 不做 cross-file 引用检查，配错也只在玩家领奖时才抛异常。
- **建议**
  - 增加一个独立的 lint 任务（TASK-013 阶段顺手做）：扫描所有 `payload` 字段，与 `survivors.json` / `gear` / `augment` 的主键集合对账。
  - 或要求策划在 schema 里把这些字符串改成枚举/外键引用。

---

## F-004 `gacha.json` 所有 `Rarity` 等级未覆盖 COMMON

- **来源**
  - `Rarity` 枚举有 4 档：COMMON / RARE / EPIC / LEGENDARY。
  - `gacha.json.rates` 只配置了后 3 档（0.85/0.13/0.02 = 1.0），没有 COMMON。
- **现状**
  - `GachaTuning` 校验"概率和 ≈ 1.0"，所以并不报错；但策划意图是"招募池保底 RARE 起步"，而不是"COMMON 不存在于这个表"。
- **建议**
  - 如果策划坚持招募池不会出 COMMON，schema 文档里应明确"招募池的 rarity 集合 = {RARE, EPIC, LEGENDARY}，COMMON 仅由 Zone 推图掉落"。
  - 否则 `rates` 应补 `"COMMON": 0.0` 让 schema 自完整。

---

## F-005 `BattlePassConfig.Reward` 缺 `ITEM_ID` 字段

- **来源**
  - 现有 `Reward` POJO 只有 `kind` + `currency` + `payload` + `amount`。
  - 但 BP / Daily Quest / Starter Pack 等系统都会引用"道具 ID"，目前都塞在 `payload` 里。
- **建议**
  - 长期方案：单独抽 `ItemRef { itemType, itemId }`，统一给 BP / DailyQuest / Starter Pack / LimitedOffer 复用。
  - 短期方案：保持现状，但在策划文档里写明 `payload` 字符串规范（前缀 + ID）。

---

## F-006 `OnboardingTuning.skipAllowedAfterStep` 与硬编码常量并存

- **来源**
  - `OnboardingStep.SKIP_ALLOWED_AFTER` 是个 static int 常量。
  - 新加的 `OnboardingTuning.skipAllowedAfterStep` 是字符串/枚举，运行时优先生效。
- **影响**
  - 配置已生效，但代码里的常量字面值仍存在；如果策划改了 JSON，编译期常量会脱节，调试时容易困惑。
- **建议**
  - TASK-013 阶段把 `OnboardingStep.SKIP_ALLOWED_AFTER` 删掉，强制依赖 NumericConfig。
  - 同步在 NUMERIC-TUNING-DATA.md 中说明"硬编码常量已废弃"。

---

## F-007 `LimitedOffersTuning` 字段尚未接入服务

- **来源**
  - `assets/numeric/limited_offers.json` 已落地（窗口期、价格、补给包）。
  - 但当前没有 `LimitedOfferService` 去消费这份 schema —— 仅 IAP 回调里的 `iap_validate` action 写死了几款 SKU。
- **建议**
  - TASK-013 在做 LTV 模拟时可以读 `limited_offers.json` 算预期收益曲线。
  - 长远方案：新增 `LimitedOfferService` 走 NumericConfig.limitedOffers()，弃用 IAP 回调里硬编码 SKU。

---

## 后续追加

> 在 TASK-013 自动化模拟里如果再发现新的字段冲突，请按 `F-00X` 编号继续追加在本文件末尾。
