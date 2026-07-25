# IoT 数据存储架构升级 — InfluxDB 时序库集成方案（重新评估版）

> 项目：xhzb-parent
> 日期：2026-07-25
> 版本：v2.0（重新评估）

---

## 一、问题分析

### 1.1 当前架构

```
MQTT → 华为云 IoTDA → AMQP → AmqpClient.processMessage()
                                    ↓
                              DeviceDataServiceImpl.batchInsertDeviceData()
                                    ↓
                         ┌──────────┼──────────┐
                         ↓                     ↓
                  MySQL device_data      Redis iot:device_last_data
                  (全量，永不过期)          (仅最新一份)

读取路径：
  - 智能房间/床位展示 → Redis(优先) → MySQL device_data(兜底)
  - 报警判断 → Redis 独读
  - 管理后台列表 → MySQL device_data
```

### 1.2 已识别问题（5 个）

| # | 问题 | 严重程度 | 根因 | 影响 |
|---|------|---------|------|------|
| **P1** | **device_data 表无限增长** | 高 | 无 TTL/分区/归档机制，IoT 设备每秒上报 | 磁盘耗尽、全表扫描越来越慢 |
| **P2** | **selectByIotId 无 LIMIT** | 高 | `DeviceDataMapper.xml:128-132` 查全部数据仅 ORDER BY，无异味 LIMIT | 一个设备几个月数据=几十万行一次性返回，OOM 风险 |
| **P3** | **读写混合在 MySQL** | 中 | IoT 高频写入 + 业务事务查询共用同一个 MySQL 实例 | 写压力影响业务查询响应时间 |
| **P4** | **无历史趋势查询能力** | 中 | Redis 只存最新一条，MySQL 查历史太慢 | 无法支持「查看心率24h变化曲线」等需求 |
| **P5** | **无存储压缩** | 低 | MySQL InnoDB 行存，无时序压缩 | 存储效率低，成本高 |

### 1.3 需求提炼

| 需求 | 来源 |
|------|------|
| R1: 设备数据按时间自动过期（90天） | P1 |
| R2: 按设备查询历史数据有分页/LIMIT 控制 | P2 |
| R3: IoT 写入不冲击 MySQL 业务库 | P3 |
| R4: 支持按设备+时间范围查询历史趋势 | P4 |
| R5: 存储压缩，降低磁盘成本 | P5 |
| R6: 不改变现有业务代码调用方式 | 架构约束 |
| R7: InfluxDB 不可用时自动降级，不丢数据 | 可用性约束 |

---

## 二、架构设计

### 2.1 目标架构

```
MQTT → 华为云 IoTDA → AMQP → AmqpClient.processMessage()
                                    ↓
                              DeviceDataServiceImpl.batchInsertDeviceData()
                                    ↓
                    ┌───────────────┼───────────────┐
                    ↓               ↓               ↓
            MySQL device_data  Redis Hash       InfluxDB
            (7天热数据，可选)   iot:device_last  device_data measurement
                               (最新一份)        (90天全量，压缩存储)
                                                   ↓
                                            Retention Policy
                                            (90天后自动删除)

读取路径（改造后）：
  - 智能房间/床位实时展示 → Redis(优先) → InfluxDB(兜底，替代MySQL)
  - 设备历史趋势查询     → InfluxDB
  - 报警判断             → Redis(不变)
  - 管理后台列表/导出    → MySQL(不变，7天内热数据)
```

### 2.2 架构如何解决问题

| 问题 | 解决方案 | 具体机制 |
|------|---------|---------|
| P1 无限增长 | InfluxDB Retention Policy | `CREATE RETENTION POLICY "90d" ON "nursing" DURATION 90d DEFAULT` — 自动删除 90 天前数据 |
| P2 无 LIMIT | InfluxDB query 自带 limit | Flux: `\|> limit(n: 20)` 替代原 `selectByIotId` 全量查询 |
| P3 读写混合 | 写吞吐移至 InfluxDB | InfluxDB 百万点/秒写入，MySQL 只承担管理后台低频查询 |
| P4 无历史趋势 | InfluxDB 时间范围查询 | `range(start: -24h) \|> aggregateWindow(every: 5m, fn: mean)` 一条语句完成聚合 |
| P5 无压缩 | InfluxDB 列式+时序压缩 | 时间戳差值编码 + Gorilla 浮点压缩，实测 10:1+ |
| R7 降级 | try-catch 保护 | InfluxDB 写失败 → 只记日志，MySQL+Redis 照写不误 |

### 2.3 重构成本分析

```
                        文件数    改动类型    风险等级
新增文件                     3    全新建       零风险
  InfluxDBConfig.java                    配置Bean
  IInfluxDBService.java                  接口定义
  InfluxDBServiceImpl.java               实现类

修改文件                     3    小改动       低风险
  pom.xml                                加1个dependency
  application-dev.yml                    加4行配置
  DeviceDataServiceImpl.java             batchInsertDeviceData加1行调用 + try-catch

改造文件                     1    中等改动     中风险
  RoomServiceImpl.java                   fillDeviceDataFromRedis 兜底从 MySQL 改 InfluxDB

不变文件                    全部              零风险
  DeviceDataMapper.xml                   保留
  DeviceDataController.java              保留
  AlertRuleServiceImpl.java              保留
  AmqpClient.java                        保留

基础设施                     1               —
  docker-compose.yml                     新增 InfluxDB 服务
```

**结论：总改动 5 个文件（3 新增 + 2 修改 + 1 改造），不改接口签名，不改数据库表结构，风险可控。**

### 2.4 为什么是 InfluxDB（而非其他方案）

| 候选方案 | ❌ 不选理由 |
|---------|------------|
| **MySQL 分区表 + TTL EVENT** | 本质还是关系型，写吞吐上限低，无压缩，查询仍需全分区扫描 |
| **Elasticsearch** | 全文检索强但时序写入弱，索引开销大，1 天 IoT 数据 = 数 GB 索引，无 Retention Policy 原生支持 |
| **ClickHouse** | 功能过重，运维成本高（需要 ZK），单机部署复杂 |
| **TimescaleDB** | PostgreSQL 插件，需要额外维护 PG 实例，压缩比不如 InfluxDB |
| **TDengine** | 国产时序库，社区版功能受限，企业版收费 |
| **InfluxDB 2.7** ✅ | 单 Docker 容器、HTTP 协议、10:1 压缩、Retention Policy 原生、Java SDK 成熟、百万点/秒写入 |

---

## 三、详细设计

### 3.1 环境设计

#### 开发环境（当前唯一环境）

```
┌─────────────────────────────────────────────────┐
│  Windows 开发机（单机 Docker Desktop）             │
│                                                   │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ MySQL 8  │  │ Redis 7  │  │ InfluxDB 2.7  │  │
│  │ :3306    │  │ :6379    │  │ :8086         │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
│                                                   │
│  ┌──────────────────────────────────────────┐    │
│  │  Spring Boot App (xhzb-admin)            │    │
│  │  :8080                                    │    │
│  └──────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

- 所有服务通过 `docker-compose.yml` 统一编排
- InfluxDB 端口 `8086`，Token 通过环境变量注入
- 资源预估：InfluxDB 空闲 ~200MB，写入峰值 ~500MB

#### 生产环境（未来规划，不在本次范围）

- InfluxDB 建议独立部署，不与 MySQL 争抢磁盘 IO
- 如设备数 > 1000 台，考虑 InfluxDB 集群方案

### 3.2 数据设计

#### InfluxDB Bucket & Measurement

```
Bucket: nursing

Measurement: device_data
  Tags (低基数，自动索引):
    iot_id              STRING   设备ID (例: sleep002)
    product_key         STRING   产品Key
    function_id         STRING   物模型标识 (例: HeartRate)
    device_name         STRING   设备名称

  Fields (不可索引，存储值):
    data_value          FLOAT    传感器数值
    product_name        STRING   产品名称
    location_type       INTEGER  位置类型 (0随身/1固定)
    physical_location_type  INTEGER  物理位置 (0楼层/1房间/2床位)
    access_location     STRING   接入位置编号
    device_description  STRING   位置描述

  Timestamp:
    alarm_time          设备上报时间 (ms 精度)
```

#### Retention Policy

```
名称:    rp_90d
时长:    90 天
分片:    1 天
默认:    true
```

#### Line Protocol 写入示例

```
device_data,iot_id=sleep002,product_key=abc123,function_id=HeartRate,device_name=sleep002 \
  data_value=72.5,product_name="睡眠监测带",location_type=1i,physical_location_type=2i,access_location="170",device_description="1,3,170" \
  1753432318000000000
```

#### 与 MySQL device_data 字段映射

| MySQL 列 | InfluxDB | 类型 | 说明 |
|----------|----------|------|------|
| iot_id | Tag | STRING | 高选择性，适合做 Tag |
| product_key | Tag | STRING | 低基数 |
| function_id | Tag | STRING | 低基数（HeartRate/BodyTemp 等有限枚举） |
| device_name | Tag | STRING | 随设备不变 |
| data_value | Field | FLOAT | MySQL 是 VARCHAR，写入时 parseDouble |
| product_name | Field | STRING | |
| location_type | Field | INTEGER | |
| physical_location_type | Field | INTEGER | |
| access_location | Field | STRING | |
| device_description | Field | STRING | |
| alarm_time | Timestamp | — | 写入时间的依据 |
| id/create_by/update_by/remark | **不写入** | — | 管理字段，不属于时序数据 |

**设计原则：Tag 选低基数+常用作过滤条件的字段；Field 存实际测量值；Timestamp 用业务时间而非系统时间。**

### 3.3 写入链路设计

```
DeviceDataServiceImpl.batchInsertDeviceData():
  
  1. saveBatch(list)           → MySQL     (不变)
  2. redisTemplate.opsForHash  → Redis     (不变)
  3. influxDBService.write()   → InfluxDB  (新增，try-catch包裹)
     ├─ 成功: 静默
     └─ 失败: log.error + 不抛异常（降级策略）
```

降级策略：InfluxDB 写入失败时，数据仍然在 MySQL + Redis 中，不影响任何业务功能。

### 3.4 查询链路设计

| 使用方 | 数据源 | 查询方式 |
|--------|--------|---------|
| 智能房间/床位 | Redis(优先) → InfluxDB(兜底) | `influxDBService.queryLatest(iotId, 20)` |
| 报警判断 | Redis | 不变 |
| 管理后台列表 | MySQL | 不变（7天热数据，已有分页） |
| 未来：历史趋势图 | InfluxDB | `influxDBService.queryRange(iotId, functionId, start, end)` |

### 3.5 接口设计

#### InfluxDBConfig.java

```java
@Configuration
public class InfluxDBConfig {
    @Value("${influxdb.url}")
    private String url;
    @Value("${influxdb.token}")
    private String token;
    @Value("${influxdb.org}")
    private String org;
    @Value("${influxdb.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}
```

#### IInfluxDBService.java — 接口方法

```java
public interface IInfluxDBService {
    // 批量写入设备数据（内部 try-catch，失败不抛异常）
    void writeDeviceData(List<DeviceData> list);

    // 查询设备最近 N 条数据（替代 selectByIotId）
    List<DeviceData> queryLatest(String iotId, int limit);

    // 查询设备指定时间范围+物模型的数据（未来趋势图用）
    List<DeviceData> queryRange(String iotId, String functionId,
                                 LocalDateTime start, LocalDateTime end);
}
```

#### InfluxDBServiceImpl.java — 核心实现要点

- `writeDeviceData`: Data → Point → WriteApiBlocking.writePoints()
- `queryLatest`: Flux 查询 `range(-30d) → filter → sort(desc) → limit`
- `queryRange`: Flux 查询 `range(start, end) → filter → sort(asc)`
- Flux table 解析：`_field` 列展开为 DeviceData 属性

---

## 四、任务拆解

按优先级排列，每项为独立可执行任务：

### P0（阻塞依赖 — 先做）

| # | 任务 | 产出 | 估时 |
|---|------|------|------|
| 1 | **创建 docker-compose.yml**，加入 InfluxDB 2.7 服务 | 一键启动全部中间件 | 15min |
| 2 | **启动 InfluxDB，创建 Bucket + Token + Retention Policy** | 可用的 InfluxDB 实例 | 15min |
| 3 | **pom.xml 添加 influxdb-client-java 依赖** | 编译通过 | 5min |
| 4 | **application-dev.yml 添加 influxdb 配置段** | 配置就绪 | 5min |

### P1（核心功能）

| # | 任务 | 产出 | 估时 |
|---|------|------|------|
| 5 | **新建 InfluxDBConfig.java** | Spring Bean 管理 InfluxDBClient | 10min |
| 6 | **新建 IInfluxDBService + InfluxDBServiceImpl** | writeDeviceData() + queryLatest() | 30min |
| 7 | **DeviceDataServiceImpl 改造** — batchInsertDeviceData 追加 InfluxDB 写入 | 数据三写（MySQL+Redis+InfluxDB） | 10min |
| 8 | **RoomServiceImpl 改造** — fillDeviceDataFromRedis 兜底改为 InfluxDB | 解决 P2（无 LIMIT 问题） | 10min |

### P2（验证 + 收尾）

| # | 任务 | 产出 | 估时 |
|---|------|------|------|
| 9 | **Maven 编译验证** — `mvn clean compile` | 确认无编译错误 | 5min |
| 10 | **联调测试** — 启服务 → 模拟设备上报 → 查接口验证 InfluxDB 有数据 | 端到端验证通过 | 20min |
| 11 | **更新 Obsidian** — daily + 每日需求.md | 项目记录同步 | 5min |

### P3（可选 — 后续迭代）

| # | 任务 | 说明 |
|---|------|------|
| 12 | MySQL device_data 表添加 TTL 清理任务 | 定期删除 7 天前数据 |
| 13 | 新建历史趋势查询接口 | `GET /elder/device/{iotId}/trend?functionId=HeartRate&range=24h` |
| 14 | 历史数据迁移脚本 | MySQL → InfluxDB 批量导入已存数据 |

---

## 附录 A：现状代码速查

### 涉及文件清单

| 文件 | 行数 | 当前作用 |
|------|------|---------|
| `DeviceData.java` | 85 | 实体，16 个字段 |
| `DeviceDataMapper.java` | 69 | MyBatis Mapper，含 selectByIotId |
| `DeviceDataMapper.xml` | 132 | SQL 映射，selectByIotId **无 LIMIT** |
| `IDeviceDataService.java` | 65 | Service 接口 |
| `DeviceDataServiceImpl.java` | 160 | batchInsertDeviceData 只写 MySQL+Redis |
| `DeviceDataController.java` | 115 | 管理后台 CRUD（不动） |
| `RoomServiceImpl.java` | 181 | fillDeviceDataFromRedis 兜底用 deviceDataMapper.selectByIotId |
| `xhzb-nursing-platform/pom.xml` | 87 | 无 influxdb 依赖 |
| `application-dev.yml` | 118 | 无 influxdb 配置 |

### batchInsertDeviceData 当前逻辑（第 117-158 行）

```
入参: IotMsgNotifyData (AMQP 解析结果)
  1. 查 device 表确认 iotId 对应的设备存在
  2. 遍历 services[] → 每个 service 的 properties map
  3. 每个 property: BeanUtil.toBean(device) → DeviceData
  4. 设置 functionId=dataValue + alarmTime
  5. saveBatch(list) → MySQL
  6. redisTemplate.opsForHash().put(...) → Redis
```

改造点：第 6 步后追加一步 `influxDBService.writeDeviceData(list)`。
