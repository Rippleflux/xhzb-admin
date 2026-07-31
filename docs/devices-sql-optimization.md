

## CheckInConfigMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/CheckInConfigMapper.xml

关键 SQL:
- selectCheckInConfigList: dynamic WHERE with check_in_id, nursing_level_id, nursing_level_name LIKE '%', fee_start_date, fee_end_date, deposit, nursing_fee, bed_fee, insurance_payment, government_subsidy, other_fees, sort_order
- selectCheckInConfigById: WHERE id = #{id}

问题点与建议:
- check_in_id 与 nursing_level_id 是典型的等值过滤字段，建议为其添加单列索引或联合索引（如果查询经常按 check_in_id + nursing_level_id 组合）
- nursing_level_name 使用 LIKE '%...%' 无法被普通索引利用，若业务要求模糊匹配，考虑改为前缀匹配或全文索引

建议DDL:
ALTER TABLE check_in_config ADD INDEX idx_cic_checkin (check_in_id);
ALTER TABLE check_in_config ADD INDEX idx_cic_nursing_level (nursing_level_id);
-- 若经常按 check_in_id + nursing_level_id 组合查询
ALTER TABLE check_in_config ADD INDEX idx_cic_checkin_level (check_in_id, nursing_level_id);

回滚:
ALTER TABLE check_in_config DROP INDEX idx_cic_checkin;
ALTER TABLE check_in_config DROP INDEX idx_cic_nursing_level;
ALTER TABLE check_in_config DROP INDEX idx_cic_checkin_level;

验证:
- EXPLAIN SELECT ... WHERE check_in_id = ? [AND nursing_level_id = ?]
- 期望: key 包含新增索引，type = ref 或 range


## CheckInMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/CheckInMapper.xml

关键 SQL:
- selectCheckInList: dynamic WHERE with elder_name LIKE '%', elder_id, id_card_no, start_date, end_date, nursing_level_name LIKE '%', bed_number, status, sort_order
- selectCheckInById: WHERE id = #{id}

问题点与建议:
- elder_name 和 nursing_level_name 使用 leading wildcard LIKE，建议前缀或全文索引替代
- 常见等值查询：elder_id、id_card_no、bed_number、status 应有单列索引（尤其 elder_id 与 id_card_no 用于定位具体老人记录）
- 如果查询常按 (elder_id, status) 或 (bed_number, status) 组合筛选，考虑联合索引

建议DDL:
ALTER TABLE check_in ADD INDEX idx_checkin_elder (elder_id);
ALTER TABLE check_in ADD INDEX idx_checkin_idcard (id_card_no);
ALTER TABLE check_in ADD INDEX idx_checkin_bed (bed_number);
ALTER TABLE check_in ADD INDEX idx_checkin_status (status);
-- 可选联合索引
ALTER TABLE check_in ADD INDEX idx_checkin_elder_status (elder_id, status);

回滚:
ALTER TABLE check_in DROP INDEX idx_checkin_elder;
ALTER TABLE check_in DROP INDEX idx_checkin_idcard;
ALTER TABLE check_in DROP INDEX idx_checkin_bed;
ALTER TABLE check_in DROP INDEX idx_checkin_status;
ALTER TABLE check_in DROP INDEX idx_checkin_elder_status;

验证:
- EXPLAIN on representative queries (elder_id lookup, id_card_no lookup)


## ContractMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/ContractMapper.xml

关键 SQL:
- selectContractList: dynamic WHERE with elder_id, contract_name LIKE '%', contract_number, agreement_path, third_party_phone, third_party_name LIKE '%', elder_name LIKE '%', start_date, end_date, status, sign_date, termination_submitter, termination_date, termination_agreement_path, sort_order
- selectContractById: WHERE id = #{id}

问题点与建议:
- 多处使用 leading-wildcard LIKE (contract_name, third_party_name, elder_name) → consider FULLTEXT or prefix search
- 常见等值字段：elder_id, contract_number, third_party_phone, status, sign_date — add indexes
- If contract_number is unique, create a UNIQUE index

建议DDL:
ALTER TABLE contract ADD INDEX idx_contract_elder (elder_id);
ALTER TABLE contract ADD INDEX idx_contract_number (contract_number);
ALTER TABLE contract ADD INDEX idx_contract_phone (third_party_phone);
ALTER TABLE contract ADD INDEX idx_contract_status (status);
-- 如果 contract_number 全局唯一
ALTER TABLE contract ADD UNIQUE INDEX ux_contract_number (contract_number);

回滚:
ALTER TABLE contract DROP INDEX idx_contract_elder;
ALTER TABLE contract DROP INDEX idx_contract_number;
ALTER TABLE contract DROP INDEX idx_contract_phone;
ALTER TABLE contract DROP INDEX idx_contract_status;
ALTER TABLE contract DROP INDEX ux_contract_number;

验证:
- EXPLAIN representative queries (lookup by contract_number, elder_id)
- 对 LIKE 性能敏感的字段，建议转为全文或外部搜索

