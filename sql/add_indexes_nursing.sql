-- SQL 补丁：为 nursing 模块添加“优先（必做）”索引
-- 位置：sql/add_indexes_nursing.sql
-- 说明：在测试库先执行下列 ALTER 语句并对比 EXPLAIN。生产请使用在线 DDL（pt-online-schema-change 或 MySQL8 在线 ALTER）在低峰期执行。

/*
  变更清单（优先/必做）
  1) device_data: 支持 WHERE iot_id = ? ORDER BY alarm_time DESC 的查询避免 filesort
     索引：idx_device_iot_alarm (iot_id, alarm_time DESC)
  2) alert_data: 支持按 iot_id + create_time DESC 的检索（告警按时间倒序浏览）
     索引：idx_alert_iot_create (iot_id, create_time DESC)
  3) alert_rule: 支持按 product_key + function_id 的等值匹配（规则快速匹配）
     索引：idx_rule_prod_func (product_key, function_id)
  4) device: 加速通过 iot_id 查设备（若 iot_id 全局唯一，则建立唯一索引）
     索引：ux_device_iot (iot_id)
*/

-- ========== 添加索引（测试库逐条执行） ==========
ALTER TABLE device_data ADD INDEX idx_device_iot_alarm (iot_id, alarm_time DESC);

ALTER TABLE alert_data ADD INDEX idx_alert_iot_create (iot_id, create_time DESC);

ALTER TABLE alert_rule ADD INDEX idx_rule_prod_func (product_key, function_id);

ALTER TABLE device ADD UNIQUE INDEX ux_device_iot (iot_id);

-- ========== 回滚语句（如需回退） ==========
-- ALTER TABLE device_data DROP INDEX idx_device_iot_alarm;
-- ALTER TABLE alert_data DROP INDEX idx_alert_iot_create;
-- ALTER TABLE alert_rule DROP INDEX idx_rule_prod_func;
-- ALTER TABLE device DROP INDEX ux_device_iot;

-- ========== 验证步骤（在添加前后都执行） ==========
-- 1) 以典型查询为例，先在添加索引前执行 EXPLAIN 并保存输出：
--    EXPLAIN FORMAT=JSON SELECT id, device_name, iot_id, product_key, product_name, function_id, access_location, alarm_time FROM device_data WHERE iot_id = 'SOME_ID' ORDER BY alarm_time DESC LIMIT 50;
--    目标（修改前）可能看到："key": null, "type": "ALL", 或者 "Extra" 包含 "Using filesort"（表示全表扫描或 filesort）

-- 2) 添加 idx_device_iot_alarm 后，执行相同的 EXPLAIN：
--    期望看到："key": "idx_device_iot_alarm"（或类似），"type": "ref"/"range"，"rows" 估算显著下降，"Extra" 不再包含 "Using filesort"。

-- 例：为什么会变快（device_data 示例）
--  问题（优化前）：
--    查询 WHERE iot_id = ? ORDER BY alarm_time DESC 如果只有 iot_id 单列索引或没有索引，MySQL 需要先检索满足 iot_id 的行（可能通过索引查到主键再回表），随后还要按 alarm_time 排序 -> 产生 filesort 或临时表，IO 和 CPU 占用较高，响应慢。
--  优化后：
--    复合索引 (iot_id, alarm_time DESC) 按索引就包含了查询的过滤前缀与排序列，MySQL 可以直接通过索引有序读取（index range scan），避免额外 filesort，也可能减少回表（若查询列包含在索引中则为覆盖索引）。因此行扫描数和排序开销大幅下降，延迟降低。

-- 例：alert_data（告警列表按时间倒序）
--  问题（优化前）：ORDER BY create_time DESC 对整表 filesort；当同时带 iot_id 等筛选时，若没有 (iot_id, create_time) 联合索引，也会出现 filesort。
--  优化后：新增 (iot_id, create_time DESC) 使得按设备筛选并按时间倒序可直接走索引，避免 filesort，响应更快。

-- 例：alert_rule（规则匹配）
--  问题（优化前）：规则匹配经常按 product_key + function_id 等值查找，若没有合适联合索引，MySQL 可能做全表扫描对每条 device_data 触发匹配，或者仅用单列索引导致��多回表操作。
--  优化后：联合索引 (product_key, function_id) 允许针对给定 product_key 和 function_id 做快速精确匹配（index lookup），匹配速度从 O(N)→O(logN) 或 O(1) 范围（视基数而定），显著降低延迟并提高吞吐。

-- 例：device（iot_id 唯一索引）
--  问题（优化前）：若 iot_id 没有索引，按 iot_id 查找设备会全表扫描。
--  优化后：加 UNIQUE 索引后查找为常数时间（索引查找 + 回表或覆盖），响应更快且并发友好。

-- ========== 上线注意 ==========
-- 1) 新增索引会增加写入成本：INSERT/UPDATE/DELETE 时索引也要维护，评估写负载影响。建议在测试环境测量写延迟变化。
-- 2) 对大表执行 ALTER TABLE 建索引请使用在线工具（pt-online-schema-change 或 MySQL 8 的在线 DDL），以避免长时间表锁。
-- 3) 在上线时段选择低峰并监控慢查询、CPU/I/O 与锁等待指标。

-- ========== 如果需要，我可以：
--  - 把以上 SQL 补丁提交到仓库（我将把该文件写入 sql/add_indexes_nursing.sql 并提交到 feature/develop）；
--  - 在你提供测试库 EXPLAIN 输出后，帮你确认索引是否按预期命中并微调索引顺序。

