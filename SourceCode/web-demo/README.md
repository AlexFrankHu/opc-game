# Last Bastion · Web Demo (test harness)

不依赖游戏引擎、无需构建步骤的纯静态 HTML+ES Module demo。
用于在浏览器里点一遍服务端 13 个核心 Action，作为「客户端最小可玩闭环」前置验证手段。

## 启动

1. 启动服务端（默认 JSON 网关 `ws://localhost:10100`）：
   ```bash
   cd ../server
   mvn install -DskipTests -q
   mvn -pl app exec:java -Dexec.mainClass=com.lastbastion.app.Main
   ```
2. 用任意静态 HTTP 服务器伺候 `web-demo/`：
   ```bash
   cd ../web-demo
   python3 -m http.server 8080
   ```
3. 浏览器打开 <http://localhost:8080/>，输入任意 userId 登录。

可通过 URL query 覆盖服务端：`http://localhost:8080/?ws=ws://test-server.example.com:10100/`。

## 已覆盖流程

- 登录 / 心跳（自动 30s）/ 切换账号
- 主城面板 + 货币展示
- 抽卡（单抽 / 十连，按服务端配的稀有度比例展示卡背颜色）
- 推图 Zone 1~3 章 ×15 关 + 战斗回放日志
- 离线收益结算
- Arena 匹配 / 挑战 / 排行榜 / 购买挑战券
- Battle Pass 50 级网格 + 任意等级领奖 + 购买

## 与 Cocos 客户端的关系

Cocos `client/assets/scripts/` 下的 `NetClient.ts` / `GameFacade.ts` / `CombatReplayer.ts` 与本 demo 完全同源。
本 demo 仅用于：
- 服务端 / 接口冒烟测试
- 测试服快速验收
- QA & 数值无需等美术资源

正式版仍以 Cocos 工程为准。
