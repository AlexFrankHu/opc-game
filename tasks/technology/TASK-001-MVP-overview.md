# TASK-001 · Last Bastion MVP — 开发总任务总览

**项目**：Last Bastion  
**分支规则**：只推 main，不开其他分支  
**状态**：待开始  
**优先级**：P0

---

## 项目定位

末日废土放置 RPG，目标市场欧美（iOS/Android）。  
基于 opc-game 已有挂机体系迁移，替换题材包装，改造商业化结构。

---

## MVP 范围（Phase 1）

### 1. 战斗引擎
- [ ] 自动战斗主循环（基于 opc-game 战斗规则迁移）
- [ ] Status Effects 状态系统（Buff/Debuff）
- [ ] BOSS Rage Mode 狂暴机制
- [ ] CC Immunity 免控机制
- [ ] Ability 技能触发系统

### 2. Survivor 角色系统
- [ ] 角色数据结构（等级、星级、属性、技能槽）
- [ ] 等级经验曲线（基于 opc-game 原始数值表迁移）
- [ ] 星级进阶系统（碎片合成）
- [ ] Ability 技能等级升级

### 3. Gear 装备系统
- [ ] 装备数据结构（类型/品质/词条）
- [ ] 装备强化（消耗 Alloy）
- [ ] 装备分解
- [ ] 装备锁定（Gear Lock）
- [ ] 装备穿戴与替换

### 4. Augment Fusion 合成系统
- [ ] 碎片/芯片数据结构
- [ ] 合成规则（基于 opc-game 宝石合成迁移）
- [ ] 合成界面逻辑

### 5. 关卡 / Zone 推图
- [ ] Zone 章节数据结构（怪物波次、BOSS 节点）
- [ ] 自动推图主循环
- [ ] 离线挂机收益计算与结算
- [ ] Zone 1–3 章节配置

### 6. Arena 竞技场
- [ ] 基础 PVP 匹配逻辑（基于战力区间）
- [ ] Arena 排行榜
- [ ] 挑战次数系统（含购买次数）
- [ ] 胜负结算与奖励发放

### 7. 资源与道具系统
- [ ] 五种核心资源：Credits / Alloy / Tech Cores / Recruit Tokens / Premium Chips
- [ ] 道具 item 数据结构与背包系统
- [ ] 资源增减接口

### 8. 商业化
- [ ] Battle Pass（Free Track + Premium Track，$4.99/月）
- [ ] Battle Pass 等级任务与奖励发放
- [ ] Starter Pack 首购礼包触发逻辑
- [ ] Limited Offer 限时礼包框架
- [ ] 内购 SKU 配置接口

### 9. 新手引导
- [ ] 前 15 分钟强引导流程脚本
- [ ] 主线任务驱动系统（前 3 天）
- [ ] 引导步骤触发与跳过逻辑

### 10. 基础埋点
- [ ] 关键节点事件埋点：登录、首充、关卡通关、Arena 对战、Battle Pass 购买

---

## 验收标准

进入联调前全部满足：
- 完整自动战斗闭环可运行
- 至少 20 个 Survivor 角色数据配置完成
- Zone 1–3 章节可完整通关
- 离线挂机收益可正确结算
- Arena 基础匹配与排行榜可运行
- Battle Pass（Free + Premium Track）可购买与领取
- Starter Pack 可正常触发与购买
- 新手引导前 15 分钟流程完整无卡死

---

## 参考文档

- 立项文档：`doc/` 目录（opc-game 原始策划体系）
- 产品立项文档 v0.1：见 Perplexity 输出的 `last_bastion_GDD_v0.1.md`

---

*Last Bastion · TASK-001 · 创建于 2026-04-28*
