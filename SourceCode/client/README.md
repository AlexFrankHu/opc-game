# Last Bastion Client (Cocos Creator 3.x)

此目录是 Cocos Creator 3.x 工程骨架。用 Cocos Creator (≥ 3.8) 直接打开根目录即可。

## 结构

```
client/
├── assets/
│   ├── scripts/
│   │   ├── net/           # WebSocket 通信 + cmd/subCmd 路由
│   │   ├── model/         # 与服务端对齐的数据模型
│   │   ├── config/        # 客户端侧策划表（mirror 服务端）
│   │   ├── combat/        # 战斗表现层（播服务端回放 log）
│   │   ├── gameplay/      # 各系统的前端业务封装
│   │   ├── ui/            # 场景/面板/HUD
│   │   └── analytics/     # Firebase/AppsFlyer 适配
│   ├── scenes/            # Cocos 场景 (.scene)
│   └── resources/         # 资源
├── settings/
├── project.json
├── package.json
└── tsconfig.json
```

## 开发流程

1. `Cocos Dashboard → 打开项目`，选择本目录。
2. 首次打开会生成 `library/`、`temp/`、`local/`（这些已在 .gitignore）。
3. 连接本地服务端：修改 `assets/scripts/net/NetConfig.ts` 中的 `WS_URL`。
4. 运行 `pnpm typecheck`（或 `npx tsc --noEmit`）做类型检查。

## 协议

参见 [server/app/src/main/java/com/lastbastion/app/ActionRegistry.java](../server/app/src/main/java/com/lastbastion/app/ActionRegistry.java)。
`assets/scripts/net/ActionCodes.ts` 是对应的 TS 镜像。
