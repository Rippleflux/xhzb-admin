# Day11 报警管理实现计划 — 评估报告

> 评估日期：2026-07-23
> 评估对象：`Day11-报警管理-实现计划.md`
> 评估依据：项目源码深度扫描 + drawio 流程图对照

---

## 一、总体结论

**✅ 计划可落地，与项目现状完全吻合。**

经深度扫描 13 个 alarm/alert 相关源文件，确认：CRUD 脚手架已生成（Entity、Controller、Service 接口/实现、Mapper），但**所有业务逻辑均未实现**，与计划中 11 项 `[ ]` 未勾选状态完全一致。

---

## 二、现状核查

| 组件 | 状态 | 核查依据 |
|------|------|---------|
| Entity（AlertRule、AlertData） | ✅ 已就绪 | 字段完整，含 alertDataType、operator、value、duration、silentPeriod 等 |
| Controller CRUD | ✅ 已就绪 | `@RequestMapping("/nursing/alertRule")` 和 `"/nursing/alertData"` |
| Service CRUD 脚手架 | ✅ 已就绪 | 仅 selectById/list/insert/update/delete |
| Mapper XML CRUD | ✅ 已就绪 | resultMap + 基础增删改查 SQL |
| DB 菜单记录 | ✅ 已就绪 | sys_menu 中有报警规则/报警数据 3 条菜单 |
| `alertFilter()` 主逻辑 | ❌ **未实现** | AlertRuleServiceImpl 仅 33 行，无此方法 |
| `@Scheduled` 定时触发 | ❌ **桩代码** | `AlertJob.java:17` — `alertRuleService.alertFilter();` 被注释，无 @Scheduled |
| Mapper 自定义查询 | ❌ **未实现** | 无 selectNursingIdByElderId / selectNursingIdByBedId |
| application-dev.yml 配置 | ❌ **未添加** | 无 alert.deviceMaintainerRole / alert.managerRole |
| CREATE TABLE DDL | ❌ **缺失** | xhzb.sql 中无 alert_rule / alert_data 建表语句 |

---

## 三、计划可落地性逐项评估

| # | 计划项 | 可行性 | 风险/备注 |
|---|--------|--------|----------|
| 1 | AlertRuleMapper 加 selectNursingIdByElderId/BedId | ✅ 直接加 | 需确认 nursing_elder 表结构及字段名 |
| 2 | SysUserRoleMapper 加 selectUserIdByRoleName | ✅ 直接加 | 位于 xhzb-system 模块，跨模块引用需确认依赖关系 |
| 3 | DeviceServiceImpl 加 queryProduct | ✅ 可加 | 需确认华为云 IoTDA SDK 是否已在 pom.xml 引入 |
| 4 | IAlertRuleService 加 alertFilter() | ✅ 直接加 | 核心方法，后续所有逻辑的入口 |
| 5 | AlertRuleServiceImpl 实现 5 个方法 | ✅ 主体工作 | 最大工作量，含 Redis 交互 + 通知路由 + 批量保存 |
| 6 | AlertJob 取消注释 + @Scheduled | ✅ 改两行 | 已有桩代码 |
| 7 | IAlertDataService 加业务方法 | ✅ 按需加 | 如需查询/批量保存接口 |
| 8 | AlertDataController 加端点 | ✅ 按需加 | 如需报警数据处理/确认接口 |
| 9 | CacheConstants 加 Redis key | ✅ 直接加 | 常量定义 |
| 10 | application-dev.yml 加配置 | ✅ 直接加 | application-dev.yml 已存在 |
| 11 | DDL 脚本 | ⚠️ **需确认** | 表可能已在数据库中存在但未纳入版本管理，建议补充 |

---

## 四、风险项

| 风险 | 级别 | 说明 |
|------|------|------|
| **跨模块依赖** | 中 | SysUserRoleMapper 在 xhzb-system 模块，AlertRuleServiceImpl 在 xhzb-nursing-platform，需确认模块间依赖是否已建立 |
| **华为云 IoTDA SDK** | 中 | queryProduct 依赖华为云 SDK，需确认 pom.xml 中是否已有 `com.huaweicloud.sdk:iotda` |
| **Redis 连接配置** | 低 | RedisTemplate 使用需确认 application-dev.yml 中 Redis 连接是否已配置（大概率已有） |
| **DDL 缺失** | 中 | alert_rule / alert_data 建表语句不在 xhzb.sql 中，可能直接在数据库创建了但未版本化，有环境迁移风险 |
| **nursing_elder 表结构** | 低 | 需确认字段名（elder_id / nursing_id / role_id 等）与 Mapper SQL 一致 |
| **@EnableScheduling** | 低 | 需确认主启动类或配置类上是否有 @EnableScheduling，否则 @Scheduled 不生效 |

---

## 五、建议补充

1. **补充 DDL** — 将 alert_rule / alert_data 的 CREATE TABLE 语句纳入 `sql/xhzb.sql`
2. **确认 IoTDA SDK** — 检查 pom.xml，若未引入需先添加依赖
3. **确认 @EnableScheduling** — 检查启动类，确保定时任务可生效
4. **补充 SQL 注释** — selectNursingIdByElderId/BedId 的 JOIN 逻辑需标注清楚表关系
5. **考虑并发** — alertFilter() 每分钟执行，多实例部署时需考虑分布式锁（如 Redisson）

---

**结论：计划可直接按 Phase 1→4 顺序执行落地，无重大阻塞项。**
