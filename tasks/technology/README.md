# Tasks / Technology

此目录用于存放需要 Devin 开发的技术任务。

每个任务为一个独立 Markdown 文件，命名规则：`TASK-[编号]-[模块]-[简述].md`

---

## 任务列表

| 编号 | 文件 | 模块 | 优先级 | 状态 |
|------|------|------|--------|------|
| TASK-001 | [TASK-001-MVP-overview.md](./TASK-001-MVP-overview.md) | MVP 总览 | P0 | MVP 骨架完成 |
| TASK-002 | [TASK-002-combat-engine.md](./TASK-002-combat-engine.md) | 战斗引擎 | P0 | MVP 骨架完成 |
| TASK-003 | [TASK-003-survivor-system.md](./TASK-003-survivor-system.md) | Survivor 角色系统 | P0 | MVP 骨架完成 |
| TASK-004 | [TASK-004-gear-system.md](./TASK-004-gear-system.md) | 装备系统 | P0 | MVP 骨架完成 |
| TASK-005 | [TASK-005-augment-fusion.md](./TASK-005-augment-fusion.md) | 芯片合成系统 | P1 | MVP 骨架完成 |
| TASK-006 | [TASK-006-zone-idle.md](./TASK-006-zone-idle.md) | Zone 关卡 + 挂机 | P0 | MVP 骨架完成 |
| TASK-007 | [TASK-007-arena.md](./TASK-007-arena.md) | Arena 竞技场 | P0 | MVP 骨架完成 |
| TASK-008 | [TASK-008-resources-items.md](./TASK-008-resources-items.md) | 资源与道具 | P0 | MVP 骨架完成 |
| TASK-009 | [TASK-009-monetization.md](./TASK-009-monetization.md) | 商业化系统 | P0 | MVP 骨架完成 |
| TASK-010 | [TASK-010-onboarding.md](./TASK-010-onboarding.md) | 新手引导 | P0 | MVP 骨架完成 |
| TASK-011 | [TASK-011-analytics.md](./TASK-011-analytics.md) | 埋点系统 | P0 | MVP 骨架完成 |

> 状态 "MVP 骨架完成" 对应首轮服务端实现 + 单元测试 + Cocos 客户端骨架，代码位于 [../SourceCode/](../SourceCode/)。技能表、图形资源、运行时 ioGame 网络与支付 SDK 接入属于后续迭代。

---

## 开发规则

- 所有任务统一推送到 `tasks/technology/` 目录
- 只推 `main` 分支，不开其他分支
- 任务状态更新时同步修改本 README 的状态列
- 依赖关系见各任务文件头部说明

---

## P0 开发顺序建议

```
TASK-002 战斗引擎          ← 最先开始，其他模块依赖它
    ↓
TASK-008 资源与道具        ← 与 002 并行
    ↓
TASK-003 Survivor 系统    ← 依赖 002
TASK-004 装备系统          ← 依赖 003
TASK-006 Zone + 挂机      ← 依赖 002
    ↓
TASK-007 Arena            ← 依赖 002、003
TASK-009 商业化            ← 依赖 003、008
TASK-011 埋点              ← 与其他并行
    ↓
TASK-010 新手引导          ← 最后集成，依赖所有 P0 模块
```

---

*Last Bastion · tasks/technology · 更新于 2026-04-28*
