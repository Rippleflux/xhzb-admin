# 数据异常报警处理和通知 — 实现计划

> 依据：`数据异常报警处理和通知流程设计.drawio`（4 页流程图）
> 项目：xhzb-parent
> 日期：2025-07-23

---

# 上半部分：业务

---

## 一、流程图解读

Drawio 共 4 页，完整流程如下：

```
Page1: 报警执行流程（主流程）
┌──────────┐    ┌──────────┐    ┌──────────────┐    ┌────────────┐    ┌──────────┐    ┌──────────┐
│ 数据采集  │ → │  设备数据  │ → │ 指标阈值(◇)  │ → │ 生成报警数据 │ → │ 触发报警  │ → │ 短信/站内 │
│          │    │          │    │              │    │ (老人/设备)  │    │ 规则(◇)  │    │   通知    │
└──────────┘    └──────────┘    └──────────────┘    └──────────────┘    └──────────┘    └──────────┘
                                      ↑                                                    │
                                      │               ┌──────────────┐                     │
                                      └─ 异常处理循环 ─│ 持续报警通知  │ ←─ 报警是否解除? ──┘
                                                       └──────────────┘     没有: 继续发通知

Page2: 整体实现思路（步骤编号 1→2→3→4→5）
  开始 → 查询所有设备上报数据 → 数据判空 → 循环处理每个上报数据
       → 查询所有规则 → 规则判空 → 匹配 productKey+functionId 的规则
         → 查全设备规则(iotId=-1) + 查该设备专属规则
       → 判断上报时间(>1分钟跳过) → 确定哪个属性异常

Page3: 设备数据和报警规则比对（核心逻辑）
      判断是否在生效时间内? 否→结束
          │是
      检验数据是否达到阈值? 未达到→删除redis异常计数→结束
          │达到
      查询Redis沉默周期? 非空(沉默中)→结束
          │空
      查询Redis异常次数 → count+1 → 是否等于持续周期?
          │不等于                  │等于
      redis异常数+1             删除redis异常数
      结束                     添加redis沉默周期(TTL=沉默周期分钟)
                                 → 进入Page4保存报警数据

Page4: 报警数据保存和通知
      判断是否是老人数据(alertDataType=0)?
          │是                          │否(设备异常)
      判断位置类型?                查询"维修员"的userId列表
      ├─ 随身设备(locationType=0)
      │   iotId → accessLocation(elderId) → nursing_elder → nursingId
      ├─ 床上设备(locationType=1, physicalLocationType=2)
      │   iotId → bedId → elderId → nursing_elder → nursingId
      └─ 房间设备(locationType=1, physicalLocationType=1)
          iotId → binding_location(roomId) → (无老人绑定, 按设备异常处理)
        │                              │
      查询"护理员"的nursingId      查询"超级管理员"的userId列表
        │                              │
        └──────── 合并所有userId ───────┘
                    │
          批量保存 AlertData(每人一条)
                    │
          短信或站内通知(后续实现)
```

---

## 二、通知人路由

```
insertAlertData(rule, deviceData)
  │
  ├─ alertDataType == 0 (老人异常)
  │   ├─ locationType == 0 (随身设备)
  │   │   └─ elderId = deviceData.accessLocation
  │   │       → selectNursingIdByElderId(elderId)
  │   │
  │   ├─ locationType == 1 && physicalLocationType == 2 (床上设备)
  │   │   └─ bedId = deviceData.accessLocation
  │   │       → selectNursingIdByBedId(bedId)
  │   │         (SQL: bed.id → elder.bed_id → nursing_elder.elder_id → nursing_id)
  │   │
  │   └─ 其他固定设备
  │       └─ 无老人绑定，按设备异常处理
  │
  ├─ alertDataType == 1 (设备异常)
  │   └─ selectUserIdByRoleName("行政")
  │
  └─ 所有人追加 selectUserIdByRoleName("超级管理员")
       → 去重 → 批量保存 AlertData(userId 每人一条)
```

---

## 三、与 Day11 文档的差异分析

| 差异点 | Day11 文档 | Drawio 流程图 | 采用 |
|--------|-----------|-------------|------|
| 设备异常通知角色 | `行政` | `维修员` | **行政**（已存在 role_id=103） |
| 随身设备找护理员 | `accessLocation` 即 elderId | `iotId→accessLocation(elderId)→nursing_elder` | 一致 |
| 床上设备找护理员 | `accessLocation` 即 bedId | `iotId→bedId→elderId→nursing_elder` | 一致 |
| 沉默周期实现 | `SETEX` TTL | Redis String + TTL | 一致 |
| 持续周期计数 | Redis incr | Redis incr | 一致 |
| 通知方式拆分 | 统一 insertAlertData | 分随身/床上/房间三种路径 | **采用 drawio 更精细** |

---

## 四、当前数据库现状

| 表 | 数据 | 说明 |
|----|------|------|
| `nursing_elder` | 3 行 | elder_id→nursing_id 绑定关系 ✅ |
| `elder` | 4 人 | 含 bed_id 字段 ✅ |
| `sys_role` | 超级管理员/行政/护理员/院长/行政主管 | 无"维修员"角色 ❌ |
| `alert_rule` | 2 条 | 测试数据 |
| `alert_data` | 0 条 | 空，待实现 |
| `device_data` | 12 条 | watch001(7条) + smoke003(9条) |

---

## 五、数据库依赖确认

| 表 | 状态 | 说明 |
|----|------|------|
| `alert_rule` | ✅ 2 rows | 报警规则 |
| `alert_data` | ✅ 0 rows | 报警数据（空表待写入） |
| `nursing_elder` | ✅ 3 rows | 老人→护理员绑定 |
| `elder` | ✅ 4 rows | 含 bed_id |
| `sys_user_role` | ✅ 4 rows | 用户角色关联 |
| `sys_role` | ✅ 7 rows | 含超级管理员/行政/护理员 |

---

## 六、角色依赖

| 角色名 | role_id | 用途 | 状态 |
|--------|---------|------|------|
| 超级管理员 | 1 | 所有报警抄送 | ✅ 存在 |
| 护理员 | 102 | 老人异常→通知护理员 | ✅ 存在 |
| 行政 | 103 | 设备异常→通知行政 | ✅ 存在 |
| 维修员 | 无 | drawio 指定，暂用"行政"替代 | ⚠️ 不存在 |

> 建议：如需区分维修员，在 sys_role 表新增"维修员"角色，并在 application-dev.yml 中配置对应的角色名。

---

## 七、Redis Key 设计

| Key | 类型 | TTL | 说明 |
|-----|------|-----|------|
| `iot:device_last_data` | Hash | 永久 | 已实现（DeviceDataServiceImpl 写入） |
| `iot:alert_trigger_count:{iotId}:{functionId}:{ruleId}` | String | 无 | 连续异常计数，正常数据/触发后删除 |
| `iot:alert_silent:{iotId}:{functionId}:{ruleId}` | String | rule.alertSilentPeriod 分钟 | SETEX 自动过期 |

---

# 下半部分：代码

---

## 八、技术选型

| 技术点 | 选型 | 依赖 | 说明 |
|--------|------|------|------|
| 设备数据来源 | `RedisTemplate<String, String>` opsForHash | `spring-boot-starter-data-redis` | 从 `iot:device_last_data` Hash 拉取最新上报数据，避免扫 device_data 大表 |
| 异常计数 | `RedisTemplate` opsForValue | 同上 | Key: `iot:alert_trigger_count:{iotId}:{functionId}:{ruleId}`，每次异常 incr，正常/触发后 delete |
| 沉默周期 | `RedisTemplate` opsForValue + `set(key, val, ttl, TimeUnit)` | 同上 | 利用 Redis TTL 自动过期，到期后可再次报警 |
| ORM 查询 | MyBatis-Plus `lambdaQuery()` + `Wrappers` | `mybatis-plus-spring-boot3-starter 3.5.7` | 报警规则条件查询、设备信息查询等 |
| 自定义 SQL | `@Select` 注解 | MyBatis 3.5.16 | 跨表查询（nursing_elder 关联、sys_user_role 关联） |
| 数值比较 | Hutool `NumberUtil.compare()` | `hutool-all` | 比较设备上报值 vs 规则阈值，支持 `>=` 和 `<` |
| 时间处理 | Hutool `LocalDateTimeUtil` / `LocalTime` | `hutool-all` + JDK | 生效时段判断、上报时间比较 |
| JSON 解析 | Hutool `JSONUtil` | `hutool-all` | Redis 中设备数据反序列化为 `DeviceData` 列表 |
| 属性拷贝 | Hutool `BeanUtil.toBean()` | `hutool-all` | DeviceData → AlertData 对象转换 |
| 字符串格式化 | Hutool `CharSequenceUtil.format()` | `hutool-all` | 生成报警原因文本 |
| 注入方式 | `@Autowired` 字段注入 | Spring | 项目统一风格，不使用构造器注入 |
| 定时调度 | RuoYi Quartz + `@Scheduled` | `xhzb-quartz` | 后台管理可视化启停，`cron = "0 * * * * ?"` 每分钟执行 |
| Mapper 调用 | `ServiceImpl<Mapper, Entity>` 继承 | MyBatis-Plus | 直接用 `baseMapper` 或注入自定义 Mapper |
| 批量保存 | MyBatis-Plus `saveBatch()` | MyBatis-Plus | 一人一条 AlertData，批量写入 |
| IoT平台 | 华为云 IoTDA SDK | `huaweicloud-sdk-iotda` | `showProduct()` 查询产品物模型 |

### 为什么用 RedisTemplate 而不是 Spring Cache

| 需求 | RedisTemplate | @Cacheable |
|------|:---:|:---:|
| 精确 TTL 控制（沉默周期分钟级） | ✅ SETEX | ❌ 注解 `@Cacheable` TTL 需全局配置 |
| Hash 结构（一个 Key 存多个设备） | ✅ opsForHash | ❌ 不支持 |
| 原子自增（异常计数） | ✅ opsForValue().increment() | ❌ 不支持 |
| 运行时动态 Key 拼接 | ✅ 字符串拼接 | ⚠️ SpEL 表达式有限 |

---

## 九、实现步骤

### Phase 1：基础设施（2 个文件）

| # | 文件 | 操作 |
|---|------|------|
| 1 | `CacheConstants.java` | + `ALERT_TRIGGER_COUNT_PREFIX`、`ALERT_SILENT_PREFIX` |
| 2 | `application-dev.yml` | + `alert.deviceMaintainerRole: 行政`、`alert.managerRole: 超级管理员` |

### Phase 2：数据查询支撑（3 个文件）

| # | 文件 | 操作 |
|---|------|------|
| 3 | `AlertRuleMapper.java` | `@Select`：`selectNursingIdByElderId`、`selectNursingIdByBedId` |
| 4 | `SysUserRoleMapper.java` (xhzb-system) | `@Select`：`selectUserIdByRoleName` |
| 5 | `DeviceServiceImpl.java` | `queryProduct(String)` 调用 IoTDA ShowProduct |

### Phase 3：核心报警逻辑（3 个文件）

| # | 文件 | 操作 |
|---|------|------|
| 6 | `IAlertRuleService.java` | + `void alertFilter()` |
| 7 | `AlertRuleServiceImpl.java` | 注入 `RedisTemplate`/`IAlertDataService`/`SysUserRoleMapper`，实现 5 个方法 |
| 8 | `AlertJob.java` | 取消注释 + `@Scheduled(cron = "0 * * * * ?")` |

### Phase 4：补充接口（3 个文件）

| # | 文件 | 操作 |
|---|------|------|
| 9 | `IDeviceService.java` | + `AjaxResult queryProduct(String)` |
| 10 | `DeviceController.java` | + `@GetMapping("/queryProduct/{productKey}")` |
| 11 | `AlertRuleController.java` | `@RequestMapping` 改为 `/alert-rule` |

---

## 十、接口抽象定义

### IAlertRuleService 新增

```java
/**
 * 报警过滤：定时拉取设备数据，匹配启用规则，触发生成报警数据
 */
void alertFilter();
```

### IDeviceService 新增

```java
/**
 * 查询产品详情（含物模型服务及属性列表）
 * @param productKey 产品Key
 * @return 产品服务能力列表
 */
AjaxResult queryProduct(String productKey);
```

### AlertRuleMapper 新增

```java
@Select("SELECT nursing_id FROM nursing_elder WHERE elder_id = #{elderId}")
List<Long> selectNursingIdByElderId(String elderId);

@Select("SELECT ne.nursing_id FROM nursing_elder ne " +
        "LEFT JOIN elder e ON ne.elder_id = e.id " +
        "LEFT JOIN bed b ON e.bed_id = b.id WHERE b.id = #{bedId}")
List<Long> selectNursingIdByBedId(String bedId);
```

### SysUserRoleMapper 新增

```java
@Select("SELECT sur.user_id FROM sys_user_role sur " +
        "LEFT JOIN sys_role sr ON sur.role_id = sr.role_id " +
        "WHERE sr.role_name = #{roleName}")
List<Long> selectUserIdByRoleName(String roleName);
```

### CacheConstants 新增

```java
/** 报警规则连续触发次数，缓存前缀 */
public static final String ALERT_TRIGGER_COUNT_PREFIX = "iot:alert_trigger_count:";
/** 报警规则沉默周期，缓存前缀 */
public static final String ALERT_SILENT_PREFIX = "iot:alert_silent:";
```

### application-dev.yml 新增

```yaml
alert:
  deviceMaintainerRole: 行政
  managerRole: 超级管理员
```

### AlertRuleServiceImpl 核心方法签名

```java
// 注入依赖
@Autowired private RedisTemplate<String, String> redisTemplate;
@Autowired private IAlertDataService alertDataService;
@Autowired private SysUserRoleMapper userRoleMapper;

@Value("${alert.deviceMaintainerRole}") private String deviceMaintainerRole;
@Value("${alert.managerRole}")          private String managerRole;

void alertFilter();                             // 主入口：拉规则→拉数据→匹配
void alertFilter(DeviceData deviceData);         // 匹配规则（全设备+指定设备）
void deviceDataAlarmHandler(AlertRule rule, DeviceData deviceData); // 核心判断
void insertAlertData(AlertRule rule, DeviceData deviceData);        // 保存+通知路由
```

---

## 十一、TODO 清单

### Phase 1 — 基础设施

- [ ] CacheConstants.java — 新增 ALERT_TRIGGER_COUNT_PREFIX、ALERT_SILENT_PREFIX
- [ ] application-dev.yml — 新增 alert.deviceMaintainerRole、alert.managerRole

### Phase 2 — 数据查询支撑

- [ ] SysUserRoleMapper.java — 新增 selectUserIdByRoleName(String)
- [ ] AlertRuleMapper.java — 新增 selectNursingIdByElderId / selectNursingIdByBedId
- [ ] DeviceServiceImpl.java — 新增 queryProduct(String) 调用华为云 IoTDA

### Phase 3 — 核心报警逻辑

- [ ] IAlertRuleService.java — 新增 alertFilter()
- [ ] AlertRuleServiceImpl.java — 实现 5 个方法，注入 RedisTemplate/IAlertDataService/SysUserRoleMapper
- [ ] AlertJob.java — 取消注释 + @Scheduled(cron)

### Phase 4 — 接口补充

- [ ] IDeviceService.java — 新增 queryProduct(String)
- [ ] DeviceController.java — 新增 /queryProduct/{productKey}
- [ ] AlertRuleController.java — @RequestMapping 改为 /alert-rule

---

## 十二、实施总数

| Phase | 文件数 | 操作类型 |
|-------|--------|---------|
| Phase 1 基础设施 | 2 | 修改 |
| Phase 2 数据查询 | 3 | 修改 |
| Phase 3 核心逻辑 | 3 | 修改 |
| Phase 4 接口补充 | 3 | 修改 |
| **合计** | **11** | **0 新建，全部修改已有文件** |
