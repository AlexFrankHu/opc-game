# Last Bastion — 源码

末日废土题材放置 RPG MVP 的客户端与服务端代码。对应需求见上级目录 [tasks/technology](../tasks/technology)。

## 目录结构

```
SourceCode/
├── server/                   # Java 21 + ioGame 21.26 多模块服务端
│   ├── pom.xml               # 父 POM
│   ├── common/               # 公共类型：AttributeType / Stats / CurrencyType / ErrorCode / ...
│   ├── combat-engine/        # TASK-002 回合制战斗引擎
│   ├── game-logic/           # TASK-003 ~ TASK-011 所有玩法系统
│   └── app/                  # ioGame NettyRunOne 入口 + JSON dev 网关 + ActionRegistry
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

需要：**JDK 21**（ioGame 21.x 的注解处理器 class 版本为 65，需要 JDK21 编译；运行时 JDK 21+），Maven 3.6+。

```bash
cd SourceCode/server
mvn clean install           # 编译 + 单测
mvn -pl combat-engine test  # 单模块测试
```

目前包含 64 个单元/集成测试，覆盖：5v5 战斗主循环、BOSS Rage、CC 免疫、DOT、盾吸收、Gacha 概率/80 抽保底、强化成功率（15 级 60%±3%）、Zone 线性解锁与离线 cap、Arena 换位积分、Battle Pass 升级与付费门槛、引导步骤顺序与条件跳过、货币上限/批量扣款原子性、20 个 Survivor 技能注册完备性、JdbcPlayerStore 与 H2 (MySQL 模式) 的 round-trip 与 upsert、HMAC 鉴权服务（开放/启用模式 / 时间戳 skew / 篡改签名拒绝），以及启动真实 WebSocket 服务端 → 连接 → 登录 → 推图 → 抽卡 → 未登录拒绝 → 未知 Action 拒绝 + 启用 HMAC 后未签名/篡改签名拒绝、合法签名通过的端到端 E2E 路径。

## 服务端启动（本地）

```bash
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main \
    -Dexec.cleanupDaemonThreads=false
```

启动后会**同时拉起两个网关**，二者共享同一套业务 Service：

| 网关 | 端口 | 用途 | 对应运行时类 |
|---|---|---|---|
| ioGame NettyRunOne（broker） | 10210 | 内部消息路由（bolt） | `IoGameNettyRuntime` |
| ioGame NettyRunOne（external WS） | 10110 | 二进制 BarMessage 帧（Cocos 接 ioGame SDK） | `IoGameNettyRuntime` |
| JSON Gateway (WS) | 10100 | 明文 JSON 帧（web-demo / 简化 Cocos / QA 调试 / 测试服） | `IoGameRuntime` |

> 两套网关在测试服都是「正式入口」；选哪一套取决于客户端侧的协议实现。
> Cocos TS 客户端目前只接 JSON 网关；想跑 ioGame 原生编解码请自行实现 BarMessage codec。

自定义端口与持久化：
```bash
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main \
    -DjsonPort=10100 -Diogame.externalPort=10110 -Diogame.brokerPort=10210
# 只启 JSON 网关（禁 ioGame 原生）：
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main -Diogame.enable=false

# 切到 MySQL 持久化（test/prod）：
MYSQL_URL='jdbc:mysql://127.0.0.1:3306/lastbastion?useSSL=false&serverTimezone=UTC' \
MYSQL_USER=root MYSQL_PASSWORD=secret \
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main -Dstore.kind=mysql

# 切到 Redis：
REDIS_URI='redis://127.0.0.1:6379/0' \
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main -Dstore.kind=redis
```

### 登录鉴权（HMAC-SHA256）
启动时若读到 `LOGIN_SHARED_SECRET`（或系统属性 `auth.secret`）就开启签名校验，
登录请求必须携带 `{deviceId, ts, sig}`，服务端用同一 secret 重算签名校验。
未配置时退化为「开放模式」，任何 `userId` 都可登录，仅供本地开发 / web-demo 使用。

```bash
LOGIN_SHARED_SECRET='change-me-in-prod' \
mvn -pl app -am exec:java -Dexec.mainClass=com.lastbastion.app.Main
```

请求体（JSON 网关）：
```json
{"id":1,"action":"user.login","payload":{
  "userId":"alice",
  "deviceId":"web-abc12345",
  "ts":1735200000123,
  "sig":"<lower-hex(HMAC-SHA256(secret, userId|deviceId|ts))>"
}}
```
响应中的 `data.authStatus` 为 `OK`（启用签名校验）或 `OPEN_MODE`（未配置 secret）。

> web-demo 登录页有可选的 `鉴权 secret` 输入框，浏览器里用 `crypto.subtle.sign("HMAC", ...)` 计算签名再发出。
> 真实生产建议通过 OAuth / 第三方账号系统签发后端 token，再转换成短期 session — 不要把 `LOGIN_SHARED_SECRET` 嵌入正式包。

### JSON Gateway 帧协议（`:10100`）
```json
// 请求
{"id":1,"action":"survivor.pullGacha","payload":{"pool":"FREE","count":1}}
// 响应
{"id":1,"ok":true,"data":{"results":[{"configId":"R_GRUNT","rarity":"RARE","duplicate":false,"shardsAdded":0}]}}
// 错误
{"id":1,"ok":false,"code":"INSUFFICIENT_CURRENCY","error":"RECRUIT_TOKENS need 1 have 0"}
```
已注册的 13 个 Action 见 `IoGameRuntime#registerActions`。

### ioGame 原生网关（`:10110`）
- 二进制 BarMessage 帧（长度前缀 + cmd/subCmd/userId/bizCode/data bytes）。
- 由 `GameLogicStartup` 注册 `UserCmdAction` / `SurvivorCmdAction` / `ZoneCmdAction` 到 BarSkeleton，由 Broker 分发。
- cmd/subCmd 映射与 JSON Dev 网关完全一致（见 `ActionRegistry`），方便两边共用协议文档。
- Cocos 端接入可直接使用 [ioGame 官方客户端 SDK](https://iohao.github.io/game) 或手写 BarMessage 编解码器。

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

- ioGame 原生网关当前只注册了 `UserCmdAction` / `SurvivorCmdAction` / `ZoneCmdAction` 3 个 ActionController 演示；其余 10 个 Action 仍只在 JSON Dev Gateway 中提供，需要时按同样模板拷贝到 `iogame.action` 包并在 `GameLogicStartup#createBarSkeleton` 注册即可。
- Cocos TS 客户端当前只实现了 JSON Dev Gateway 对接；若要走 ioGame 原生二进制协议，需要实现 BarMessage 编解码器或引入 ioGame JS SDK。
- 20 位 Survivor 的主/被动技能已在 `AbilityLibrary` 内置一版可玩数值；后续若有数值策划调整，只需修改该表。
- `TestIapVerifier` 总是通过；正式环境需替换为 Apple / Google 校验器并配合 `serverVerifyReceipt` 接口。
- `PlayerContext` 当前为进程内存储，进程重启数据丢失；上线前需要接入 MySQL/Redis 持久化。
