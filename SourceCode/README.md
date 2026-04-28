# Last Bastion — 源码

末日废土题材放置 RPG MVP 的客户端与服务端代码。对应需求见上级目录 [tasks/technology](../tasks/technology)。

## 目录结构

```
SourceCode/
├── server/                   # Java 17 + ioGame 21.26 多模块服务端
│   ├── pom.xml               # 父 POM
│   ├── common/               # 公共类型：AttributeType / Stats / CurrencyType / ErrorCode / ...
│   ├── combat-engine/        # TASK-002 回合制战斗引擎
│   ├── game-logic/           # TASK-003 ~ TASK-011 所有玩法系统
│   └── app/                  # ioGame 启动入口 + ActionRegistry
└── client/                   # Cocos Creator 3.8 + TypeScript 客户端骨架
    ├── assets/scripts/
    │   ├── net/              # WebSocket + cmd/subCmd 路由
    │   ├── model/            # 与服务端对齐的数据类型
    │   ├── config/           # 策划表加载
    │   ├── combat/           # 战斗回放
    │   ├── gameplay/         # GameFacade —— UI 调用入口
    │   ├── ui/               # 场景与面板（依赖 `cc` 运行时）
    │   └── analytics/        # Firebase/AppsFlyer 适配
    ├── assets/resources/config/  # 客户端可读的策划 JSON（复制自 server）
    ├── project.json          # Cocos Creator 工程声明
    ├── package.json
    └── tsconfig.json
```

## 任务映射

| 任务 | 代码位置 | 核心入口 |
|---|---|---|
| [TASK-002 战斗引擎](../tasks/technology/TASK-002-combat-engine.md) | `server/combat-engine/` | `CombatSimulator`, `DamageCalculator`, `AbilityExecutor` |
| [TASK-003 Survivor](../tasks/technology/TASK-003-survivor-system.md) | `server/game-logic/src/main/java/.../survivor/` | `SurvivorService`, `GachaService`, `LevelCurve` |
| [TASK-004 装备](../tasks/technology/TASK-004-gear-system.md) | `.../gear/` | `GearService`, `GearFactory` |
| [TASK-005 Augment](../tasks/technology/TASK-005-augment-fusion.md) | `.../augment/` | `AugmentService` |
| [TASK-006 Zone 挂机](../tasks/technology/TASK-006-zone-idle.md) | `.../zone/` | `ZoneService` (推图 + 离线结算) |
| [TASK-007 Arena](../tasks/technology/TASK-007-arena.md) | `.../arena/` | `ArenaService` (匹配 / 排行 / 购买挑战) |
| [TASK-008 资源 & 道具](../tasks/technology/TASK-008-resources-items.md) | `.../resource/` | `ResourceService`, `ItemService` |
| [TASK-009 商业化](../tasks/technology/TASK-009-monetization.md) | `.../monetization/` | `BattlePassService`, `StarterPackService`, `LimitedOfferService`, `IapService` |
| [TASK-010 新手引导](../tasks/technology/TASK-010-onboarding.md) | `.../onboarding/` | `OnboardingService`, `DailyQuestService` |
| [TASK-011 Analytics](../tasks/technology/TASK-011-analytics.md) | `.../analytics/` & 各业务 service | `AnalyticsService`, `AnalyticsEvent` |

## 服务端编译/测试

需要：JDK 17+，Maven 3.6+。

```bash
cd SourceCode/server
mvn clean install           # 编译 + 单测
mvn -pl combat-engine test  # 单模块测试
```

目前包含 36 个单元测试，覆盖：5v5 战斗主循环、BOSS Rage、CC 免疫、DOT、盾吸收、Gacha 概率/80 抽保底、强化成功率（15 级 60%±3%）、Zone 线性解锁与离线 cap、Arena 换位积分、Battle Pass 升级与付费门槛、引导步骤顺序与条件跳过、货币上限/批量扣款原子性等。

## 服务端启动（本地）

```bash
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main
```

`Main` 启动后会装配所有 Service + 打印可用的 ActionRegistry 路由表。真实 ioGame 网络运行时由 `IoGameRuntime` 占位，按 [ioGame 官方文档](https://iohao.github.io/game) 补 `NettyRunOne` 启动即可。

## 客户端启动

1. 用 Cocos Dashboard 打开 `SourceCode/client/`。
2. 修改 `assets/scripts/net/NetConfig.ts#WS_URL` 指向服务端。
3. 在 `MainScene` 场景中挂载脚本，即可运行。

Node 环境做类型检查：

```bash
cd SourceCode/client
npm install
npx tsc --noEmit
```

## 架构原则

- **单一事实源**：所有玩法数值计算都在服务端，客户端仅负责 UI 与战斗回放。
- **事件驱动**：服务层在关键 State 变更处通过 `AnalyticsService` 派发事件，天然覆盖 TASK-011 埋点需求。
- **可替换**：支付校验、埋点 Sink 均为接口（`IapVerifier` / `AnalyticsService.Sink`），切换后端只需换实现。
- **幂等 & 验算**：IAP 通过 `canonicalOrderId` 防重；Gacha 保底与强化成功率用真随机 + 单元回归保证。

## 已知限制（留给后续扩代）

- `IoGameRuntime` 只是占位；生产需接入 `com.iohao.game:run-one-netty` 的 `NettyRunOne` 并注册 Action 类。
- 20 位 Survivor 的主动/被动技能 id 当前在 combat-engine 中以 "Precision Strike" 默认技能实现；需要策划 & 程序一起补真实技能表。
- `TestIapVerifier` 总是通过；正式环境需替换为 Apple / Google 校验器并配合 `serverVerifyReceipt` 接口。
