## ElderMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/ElderMapper.xml

关键 SQL:
- selectElderList: 动态 WHERE 包含 name LIKE '%', id_card_no, sex, status, phone, birthday, address, bed_number, bed_id, social_security_card, core_suggestion 等
- selectElderById: WHERE id = #{id}

问题点与建议:
- name 使用 leading-wildcard LIKE '%name%' 无法命中 B-Tree 索引，若有大量关键词搜索建议考虑 FULLTEXT 或改为前缀匹配
- 常见精确查找字段：id_card_no、phone、bed_id、bed_number、status，应添加单列索引以提高定位速度
- 若常常按 (name, status) 或 (bed_id, status) 做过滤（例如房间床位下查询住户），考虑 (bed_id, status) 联合索引

建议DDL:
ALTER TABLE elder ADD INDEX idx_elder_idcard (id_card_no);
ALTER TABLE elder ADD INDEX idx_elder_phone (phone);
ALTER TABLE elder ADD INDEX idx_elder_bed (bed_id);
ALTER TABLE elder ADD INDEX idx_elder_bed_number (bed_number);
ALTER TABLE elder ADD INDEX idx_elder_status (status);
-- 如果按 bed_id+status 频繁过滤
ALTER TABLE elder ADD INDEX idx_elder_bed_status (bed_id, status);

回滚:
ALTER TABLE elder DROP INDEX idx_elder_idcard;
ALTER TABLE elder DROP INDEX idx_elder_phone;
ALTER TABLE elder DROP INDEX idx_elder_bed;
ALTER TABLE elder DROP INDEX idx_elder_bed_number;
ALTER TABLE elder DROP INDEX idx_elder_status;
ALTER TABLE elder DROP INDEX idx_elder_bed_status;

验证:
- EXPLAIN SELECT ... WHERE id_card_no = ?
- EXPLAIN SELECT ... WHERE bed_id = ? AND status = ?


