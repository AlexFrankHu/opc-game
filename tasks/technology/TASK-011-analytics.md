# TASK-011 · Analytics 基础埋点系统

**项目**：Last Bastion  
**模块**：埋点与数据分析基础层  
**优先级**：P0  
**状态**：待开始

---

## 目标

在上线前建立基础埋点体系，覆盖欧美市场关键留存、付费和行为指标。

---

## 任务清单

### 11.1 埋点 SDK 接入
- [ ] 接入 Firebase Analytics（免费，iOS + Android 通用）
- [ ] 接入 AppsFlyer（归因，投放必须）
- [ ] 事件上报接口封装（统一 event name + properties 格式）

### 11.2 关键事件列表

**用户生命周期：**
- [ ] `user_register`：首次注册
- [ ] `user_login`：每次登录
- [ ] `tutorial_step_complete`：引导每步完成（带 step_id）
- [ ] `tutorial_skip`：引导跳过（带 step_id）

**核心行为：**
- [ ] `zone_attempt`：关卡开始（带 zone_id / level_id）
- [ ] `zone_complete`：关卡通关
- [ ] `zone_fail`：关卡失败
- [ ] `arena_challenge`：Arena 挑战（带结果 win/lose）
- [ ] `survivor_levelup`：Survivor 升级
- [ ] `survivor_starup`：Survivor 进阶
- [ ] `gear_enhance`：装备强化（带结果 success/fail）
- [ ] `gacha_pull`：招募抽卡（带类型 single/ten）

**付费行为：**
- [ ] `iap_attempt`：发起内购
- [ ] `iap_success`：内购成功（带 product_id / price）
- [ ] `iap_fail`：内购失败（带 error_code）
- [ ] `battlepass_purchase`：Battle Pass 购买
- [ ] `startpack_purchase`：Starter Pack 购买
- [ ] `ad_watch`：看广告（带位置 idle_reward / etc.）

### 11.3 用户属性
- [ ] 注册时间
- [ ] 当前 Zone 进度
- [ ] 当前最高战力
- [ ] 累计充值金额
- [ ] Battle Pass 是否激活

---

## 验收标准
- [ ] Firebase 后台可接收所有事件，无丢失
- [ ] AppsFlyer 归因数据与 Firebase 一致（误差 < 2%）
- [ ] iap_success 与实际内购订单一一对应

---

*Last Bastion · TASK-011 · 创建于 2026-04-28*
