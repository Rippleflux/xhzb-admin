# 设备数据（DeviceData）SQL 与 Mapper 优化清单

日期: 2026-07-31
仓库: Rippleflux/xhzb-admin (feature/develop 已扫描)

概述
- 本文档汇总了 Nursing 模块中与设备/告警相关的 Mapper 与表的性能优化建议（索引、SQL 改写、MyBatis 层落地方案、上线注意）。
- 已覆盖：device_data（DeviceDataMapper）、alert_data（AlertDataMapper）、alert_rule（AlertRuleMapper）、device（DeviceMapper）。

目录
- 总体结论与优先级
- DeviceData（详尽 SQL 逐条分析）
- AlertData / AlertRule / Device（摘要建议）
- 推荐索引 DDL 与回滚语句
- 部署与验证步骤

------------------------------------------------------------------------

总体结论与优先级（摘要）
- 常见问题：leading-wildcard LIKE ('%x%') 导致全表扫描；ORDER BY time DESC + LIMIT 在无合适索引下出现 filesort；动态且可选的 WHERE 字段集合使查询难以命中索引。
- 优先级（优先执行索引 & 验证）：
  1) device_data：为 selectByIotId 建 (iot_id, alarm_time DESC)；为常见等值字段补充单列/联合索引
  2) alert_data：为按 create_time 排序与按规则筛选添加复合索引 (iot_id, create_time DESC) 及 alert_rule_id 等索引
  3) alert_rule：为规则实时匹配建立联合索引 (product_key, function_id, iot_id)
  4) device：保证 iot_id 唯一性或索引，加速设备定位

------------------------------------------------------------------------

DeviceData — 逐条 SQL 深入分析（来自 xhzb-nursing-platform/src/main/resources/mapper/nursing/DeviceDataMapper.xml）

表假设与前置说明
- 假设表名：device_data（请在测试库执行 SHOW CREATE TABLE device_data 并提供如需最精确建议）
- 重要字段（Mapper 映射）：id, device_name, iot_id, product_key, product_name, function_id, access_location, location_type, physical_location_type, device_description, data_value, alarm_time, create_time, update_time, create_by, update_by, remark

SQL A: selectDeviceDataList (动态筛选)
- Mapper 源：
  - 动态 WHERE 包含：deviceName LIKE '%...%', iotId = ?, productKey = ?, productName LIKE '%...%', functionId = ?, accessLocation = ?, locationType, physicalLocationType, deviceDescription = ?, dataValue = ?, alarmTime = ?
- 问题点：
  1) device_name/product_name 使用 leading wildcard -> B-Tree 无法利用索引
  2) 多个可选条件（不同组合）使单一联合索引不一定命中
  3) 若前端分页使用 OFFSET 大页码，查询代价高
- 优化目标：
  - 让最常见的等值筛选字段走索引（例如 iot_id, product_key, function_id）
  - 为模糊搜索提供替代（前缀搜索或 FULLTEXT）
  - 改进分页为 Keyset（seek）以避免大量 OFFSET
- 建议索引（依据 query patterns）:
  - 如果常见查询带有 function_id 和 product_key：
    CREATE INDEX idx_device_func_prod_alarm ON device_data (function_id, product_key, alarm_time DESC);
  - 如果常见查询以 iot_id 为主键过滤（常见于按设备查看历史）：
    CREATE INDEX idx_device_iot_alarm ON device_data (iot_id, alarm_time DESC);
  - 单列索引（补充）：
    CREATE INDEX idx_device_product_key ON device_data (product_key);
    CREATE INDEX idx_device_function_id ON device_data (function_id);
    CREATE INDEX idx_device_location ON device_data (location_type, access_location);
- SQL 改写建议（MyBatis 层）：
  1) 避免 '%...%'：如果业务允许将搜索改为前缀 like concat(#{deviceName}, '%')，B-Tree 索引可用。
  2) 使用 FULLTEXT：若必须子串匹配，创建 FULLTEXT(device_name, product_name) 并在 Mapper 中用 MATCH ... AGAINST（需语义调整）。
  3) Keyset 分页示例（按 alarm_time DESC）：
     - 传入参数 lastAlarmTime (上次最底行的 alarm_time) 与 optional lastId（避免相同 alarm_time 的顺序问题）
     - WHERE ( (alarm_time < #{lastAlarmTime}) OR (alarm_time = #{lastAlarmTime} AND id < #{lastId}) ) [其他筛选条件] ORDER BY alarm_time DESC, id DESC LIMIT #{pageSize}
     - 该查询在存在 (iot_id, alarm_time DESC) 或 (alarm_time DESC, id DESC) 时更高效
- EXPLAIN 预期（执行前/后对比）:
  - 修改前：key = NULL, type = ALL, Extra 包含 Using where; Using filesort 或 Using temporary
  - 修改后：key = idx_device_func_prod_alarm 或 idx_device_iot_alarm, type = ref/range, rows 大幅下降, Extra 不再包含 Using filesort
- 覆盖索引建议：如果某些列表页面只需 id, iot_id, product_key, alarm_time, data_value，可建立复合覆盖索引 (iot_id, alarm_time DESC, data_value, id)（使查询无需回表）

SQL B: selectByIotId
- Mapper 源：
  WHERE iot_id = #{iotId} ORDER BY alarm_time DESC
- 问题点：若无合适复合索引，ORDER BY 会 filesort
- 目标：让查询走索引并避免 filesort
- 建议索引（高优先级，必做）：
  CREATE INDEX idx_device_iot_alarm ON device_data (iot_id, alarm_time DESC);
  - 该索引允许 MySQL 使用索引顺序直接返回按 alarm_time DESC 排序的行
- 如果数据按 iot_id 大量分布，单列 iot_id 索引也应存在：
  CREATE INDEX idx_device_iot ON device_data (iot_id);
- 分页优化：keyset pagination（见 SQL A）
- EXPLAIN 预期：key = idx_device_iot_alarm, possible_keys 包含 idx_device_iot_alarm, type = ref, Extra 不包含 Using filesort

SQL C: selectDeviceDataById
- Mapper 源：WHERE id = #{id}
- 问题点：应使用主键查找，主键存在（PRIMARY KEY id），查询已最优
- 建议：无索引改动；确认 id 为主键且使用合适数据类型
- EXPLAIN 预期：type = const 或 eq_ref, key = PRIMARY, rows = 1

SQL D: insertDeviceData
- Mapper 插入使用动态字段（<trim>）
- 问题点：增加索引会提高写放大，insert 的成本上升
- 建议：
  - 在写入高峰期慎增索引；新增索引前在测试环境衡量写入延迟增幅
  - 使用批量插入时考虑事务分批提交
- 设计注意：对 device_data 表，若写入 QPS 很高，应限制索引数量，优先保留对读取最有帮助的索引

SQL E: updateDeviceData / deleteDeviceDataById / deleteDeviceDataByIds
- 更新与删除在有索引时会触发索引维护，注意高写入场景下影响
- 建议：
  - 仅为真正需要的查询列建索引
  - 大批量删除使用分批删除或以打标（soft delete）结合后台异步清理

示例：Keyset 分页的 MyBatis Mapper 改写（片段）
- 在 Mapper 方法中传入 lastAlarmTime,lastId,pageSize
- WHERE 子句加上：
  <if test="lastAlarmTime != null and lastId != null">
    and (alarm_time &lt; #{lastAlarmTime} OR (alarm_time = #{lastAlarmTime} AND id &lt; #{lastId}))
  </if>
- ORDER BY alarm_time DESC, id DESC

测试与验证清单（DeviceData）
1) 在测试库执行：
   - EXPLAIN SELECT ... (selectDeviceDataList) —— 记录 key/type/rows/Extra
   - EXPLAIN SELECT ... WHERE iot_id = ? ORDER BY alarm_time DESC
2) 在测试库添加建议索引（逐条）并重复 EXPLAIN，比对变化
3) 在测试环境跑代表性负载（并发读取/写入），观察平均延迟、p95、写入延迟的变化
4) 如果在生产部署，使用在线 DDL 并在低峰期执行

------------------------------------------------------------------------

AlertData / AlertRule / Device — 摘要建议（见上文详细项）
- AlertData：优先为 (iot_id, create_time DESC) 与 alert_rule_id、function_id、product_key 建索引；避免 %like% 或使用 FULLTEXT
- AlertRule：建立 (product_key, function_id, iot_id) 类型联合索引以支持规则实时匹配
- Device：为 iot_id 建唯一或非唯一索引以加速设备查找，并为 node_id/product_key 建索引

推荐的起始 ALTER 语句（测试库先执行）
-- DeviceData
ALTER TABLE device_data ADD INDEX idx_device_iot_alarm (iot_id, alarm_time DESC);
ALTER TABLE device_data ADD INDEX idx_device_func_prod_alarm (function_id, product_key, alarm_time DESC);
ALTER TABLE device_data ADD INDEX idx_device_product_key (product_key);
ALTER TABLE device_data ADD INDEX idx_device_function_id (function_id);
ALTER TABLE device_data ADD INDEX idx_device_location (location_type, access_location);

-- AlertData
ALTER TABLE alert_data ADD INDEX idx_alert_iot_create (iot_id, create_time DESC);
ALTER TABLE alert_data ADD INDEX idx_alert_rule (alert_rule_id);
ALTER TABLE alert_data ADD INDEX idx_alert_function (function_id);
ALTER TABLE alert_data ADD INDEX idx_alert_product_key (product_key);

-- AlertRule
ALTER TABLE alert_rule ADD INDEX idx_rule_prod_func (product_key, function_id);
ALTER TABLE alert_rule ADD INDEX idx_rule_iot (iot_id);

-- Device
ALTER TABLE device ADD UNIQUE INDEX ux_device_iot (iot_id);
ALTER TABLE device ADD INDEX idx_device_product_key (product_key);
ALTER TABLE device ADD INDEX idx_device_node (node_id);

回滚示例
ALTER TABLE device_data DROP INDEX idx_device_iot_alarm;
ALTER TABLE alert_data DROP INDEX idx_alert_iot_create;
ALTER TABLE alert_rule DROP INDEX idx_rule_prod_func;
ALTER TABLE device DROP INDEX ux_device_iot;

上线注意
- 新增索引会增加写成本（插入/更新）
- 使用在线 DDL 工具（pt-online-schema-change 或 MySQL 8 在线 ALTER），在低峰期执行
- 在生产上线前请在测试环境复核 EXPLAIN 与负载影响

下一步（我将做的）
- 我已把 DeviceData 的逐 SQL 分析追加到本 Markdown 文档；如果你需要，我可以把该文件放到仓库（docs/ 或 sql/ 目录）作为变更建议的可审阅文档。
- 如需我把上述 ALTER 语句打包为 SQL 补丁文件或生成 PR，我可以继续（请回复“生成 SQL 补丁”或“生成 PR”）。
- 如果你愿意把测试库的 SHOW CREATE TABLE（device_data / alert_data / alert_rule / device）和 1-3 条慢查询 EXPLAIN 返回，我会基于实际 DDL 给出最优联合索引列顺序并微调建议。

