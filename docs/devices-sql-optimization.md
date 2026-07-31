

## FamilyMemberElderMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/FamilyMemberElderMapper.xml

关键 SQL:
- selectFamilyMemberElderList: where family_member_id = ? or elder_id = ?
- getMyFamilyList: 多表左关联（family_member_elder -> elder -> bed -> room -> device）按 family_member_elder.family_member_id 过滤

问题点与建议:
- family_member_id 与 elder_id 是高频等值查找字段，建议单列索引（如果不存在）或联合索引（取决于查询组合）
- getMyFamilyList 执行多表 LEFT JOIN，关键在于连接字段上存在索引：family_member_elder.elder_id, elder.id, bed.bed_number (用于 join)、bed.room_id, room.id, device.binding_location + device.location_type
- device.join 条件使用 device.binding_location = elder.id AND device.location_type = 0，如果 binding_location 没有索引或组合索引(device.binding_location, device.location_type) 则会导致 device 表全表扫描

建议DDL:
ALTER TABLE family_member_elder ADD INDEX idx_fme_family (family_member_id);
ALTER TABLE family_member_elder ADD INDEX idx_fme_elder (elder_id);
-- 对 device join 建议组合索引
ALTER TABLE device ADD INDEX idx_device_binding_loc_type (binding_location, location_type);

回滚:
ALTER TABLE family_member_elder DROP INDEX idx_fme_family;
ALTER TABLE family_member_elder DROP_INDEX idx_fme_elder;
ALTER TABLE device DROP INDEX idx_device_binding_loc_type;

验证:
- EXPLAIN on getMyFamilyList（检查 join order 与 key 使用）
- 期望：family_member_elder 使用 idx_fme_family，elder 使用 PRIMARY key, bed 使用 idx_bed_bed_number（或 bed_number 的索引），device 使用 idx_device_binding_loc_type


## FamilyMemberMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/FamilyMemberMapper.xml

关键 SQL:
- selectFamilyMemberList: phone = ?, name LIKE '%'

问题点与建议:
- phone 等值查找应有索引（加速找回用户）
- name 使用 leading-wildcard LIKE 无法走普通索引；若常按 name 搜索，建议全文或前缀搜索

建议DDL:
ALTER TABLE family_member ADD INDEX idx_family_phone (phone);

回滚:
ALTER TABLE family_member DROP INDEX idx_family_phone;

验证:
- EXPLAIN SELECT ... WHERE phone = ?


## FloorMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/FloorMapper.xml

关键 SQL:
- selectFloorList: name LIKE '%', code = ?
- selectAllByNur: 多表 LEFT JOIN (floor->room->bed->elder) WHERE b.bed_status = 1 GROUP BY f.id ORDER BY code, create_time DESC
- getRoomAndBedByBedStatus: 从 floor,room,bed where f.id = r.floor_id and r.id = b.room_id and b.bed_status = #{status}
- getAllFloorsWithDevice: 使用 EXISTS 联合 device 与 room/bed

问题点与建议:
- getAllFloorsWithDevice 的 EXISTS 子查询里有 JOINs 与条件 d.binding_location = r.id AND d.location_type = 1 AND d.physical_location_type = 1，建议对 device 建组合索引 (binding_location, location_type, physical_location_type)
- getRoomAndBedByBedStatus JOIN 链上 bed.room_id 与 room.id、room.floor_id 与 floor.id 应被索引（通常主键/外键自带索引）
- selectAllByNur 使用 GROUP BY f.id 与 ORDER BY code, create_time DESC：确保 f.code 与 f.create_time 有索引覆盖（例如索引 f.code 或联合索引 (code, create_time DESC)）可在排序时帮助

建议DDL:
ALTER TABLE device ADD INDEX idx_device_binding_loc_type_phys (binding_location, location_type, physical_location_type);
ALTER TABLE floor ADD INDEX idx_floor_code_create (code, create_time DESC);

回滚:
ALTER TABLE device DROP INDEX idx_device_binding_loc_type_phys;
ALTER TABLE floor DROP INDEX idx_floor_code_create;

验证:
- EXPLAIN on getAllFloorsWithDevice（期望 EXISTS 子查询使用 device 的组合索引）
- EXPLAIN on getRoomAndBedByBedStatus（检查 join keys）


## HealthAssessmentDataCollectionMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/HealthAssessmentDataCollectionMapper.xml

关键 SQL:
- selectHealthAssessmentDataCollectionList: 多字段等值过滤（basic_info, health_assessment, daily_living_activities, mental_state, perception_communication, social_participation, assessment_details)

问题点与建议:
- 表结构看起来以大文本字段为主，如果这些字段需要等值过滤，索引的使用需谨慎（MySQL 对 TEXT 列索引有限制）。如果字段为 JSON/字符串，且需要按其中某些标识过滤，考虑拆分字段或在应用层做筛选
- 若仅少量字段被频繁等值查询，请为这些字段建单列索引（例如 basic_info）

建议DDL:
ALTER TABLE health_assessment_data_collection ADD INDEX idx_health_basic_info (basic_info(100)); -- prefix index, 100 chars, 视字段实际长度调整

回滚:
ALTER TABLE health_assessment_data_collection DROP INDEX idx_health_basic_info;

验证:
- EXPLAIN 代表性查询
- 注意 MySQL 对前缀索引的选择性与性能影响

