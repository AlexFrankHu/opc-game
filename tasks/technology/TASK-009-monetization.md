# TASK-009 · Monetization 商业化系统

**项目**：Last Bastion  
**模块**：商业化（Battle Pass / Starter Pack / Limited Offer）  
**优先级**：P0  
**状态**：待开始  
**参考原体系**：`doc/策划数据文档/挂机策划案/礼包相关/` + VIP权限表  
**注意**：原 VIP 体系改造为 Battle Pass，详见立项文档

---

## 目标

实现欧美市场主流付费结构：Battle Pass 为核心，首购礼包和限时礼包为辅。

---

## 任务清单

### 9.1 Battle Pass

#### 赛季配置
- [ ] 赛季时长：30天
- [ ] 赛季等级上限：50级
- [ ] 每级所需赛季经验值配置表
- [ ] 赛季经验来源：每日任务 / 关卡通关 / Arena 胜利

#### 轨道设计
- [ ] Free Track（免费轨道）：每级有基础奖励，所有玩家可领取
- [ ] Premium Track（付费轨道，$4.99/月）：每级有额外奖励，购买后激活
- [ ] Premium+ Track（可选扩展，$9.99/月）：额外 XP 加成 + 独占外观奖励

#### 奖励配置
- [ ] 50级奖励配置表（Free + Premium 各一列）
- [ ] 奖励类型：Credits / Alloy / Tech Cores / Recruit Tokens / 独占 Survivor / 独占外观
- [ ] 赛季独占 Legendary Survivor（Premium Track 第 50 级）

#### 系统功能
- [ ] 购买 Battle Pass 接口（对接内购 SDK）
- [ ] 赛季经验增减接口
- [ ] 奖励领取接口（逐级领取，不可跳级）
- [ ] 赛季结束时未领取奖励自动结算
- [ ] 赛季进度持久化
- [ ] 补签（购买过去等级奖励）接口，消耗 Premium Chips

### 9.2 Starter Pack 首购礼包
- [ ] 触发条件：玩家首次达到 Zone 1-3 通关后弹出
- [ ] 内容：Premium Chips ×300 + Recruit Tokens ×10 + Epic Gear Box ×3
- [ ] 定价：$0.99（超低门槛首购）
- [ ] 每账号仅可购买一次
- [ ] 购买后永不再弹出

### 9.3 Limited Offer 限时礼包
- [ ] 礼包数据结构（ID / 内容 / 价格 / 开始时间 / 结束时间 / 购买上限）
- [ ] 限时礼包展示入口（商店首页 Banner）
- [ ] 倒计时显示
- [ ] 购买次数限制（单账号）
- [ ] MVP 阶段预设 2 个礼包模板：
  - Newcomer Bundle：$2.99，Credits×50000 + Alloy×500 + Recruit Token×5
  - Power Bundle：$9.99，Premium Chips×800 + Epic Gear Box×5 + Tech Core×200

### 9.4 内购 SDK 接入
- [ ] Apple IAP 接入（iOS）
- [ ] Google Play Billing 接入（Android）
- [ ] 购买回调服务端验证（防刷单）
- [ ] 购买失败重试与补单机制
- [ ] 内购日志记录（OrderID / 金额 / 商品 / 时间 / 账号）

---

## 验收标准
- [ ] Battle Pass 经验在所有来源正确累加
- [ ] 奖励领取幂等（重复请求不重复发放）
- [ ] 内购服务端验证通过率 > 99.9%（测试环境沙盒）
- [ ] Starter Pack 每账号严格只购买一次
- [ ] 限时礼包超时后自动下架

---

*Last Bastion · TASK-009 · 创建于 2026-04-28*
