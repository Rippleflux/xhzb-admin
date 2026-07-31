-- SQL patch: add recommended indexes for Nursing module (Batch: Device/Alert/DeviceData + Family/Room etc)
-- Run in test DB first. Add indices incrementally and run EXPLAIN on representative queries.

-- DeviceData / AlertData / AlertRule / Device (already suggested earlier)
ALTER TABLE device_data ADD INDEX IF NOT EXISTS idx_device_iot_alarm (iot_id, alarm_time DESC);
ALTER TABLE device_data ADD INDEX IF NOT EXISTS idx_device_func_prod_alarm (function_id, product_key, alarm_time DESC);
ALTER TABLE device_data ADD INDEX IF NOT EXISTS idx_device_product_key (product_key);
ALTER TABLE device_data ADD INDEX IF NOT EXISTS idx_device_function_id (function_id);
ALTER TABLE device_data ADD INDEX IF NOT EXISTS idx_device_location (location_type, access_location);

ALTER TABLE alert_data ADD INDEX IF NOT EXISTS idx_alert_iot_create (iot_id, create_time DESC);
ALTER TABLE alert_data ADD INDEX IF NOT EXISTS idx_alert_rule (alert_rule_id);
ALTER TABLE alert_data ADD INDEX IF NOT EXISTS idx_alert_function (function_id);
ALTER TABLE alert_data ADD INDEX IF NOT EXISTS idx_alert_product_key (product_key);

ALTER TABLE alert_rule ADD INDEX IF NOT EXISTS idx_rule_prod_func (product_key, function_id);
ALTER TABLE alert_rule ADD INDEX IF NOT EXISTS idx_rule_iot (iot_id);

ALTER TABLE device ADD INDEX IF NOT EXISTS idx_device_product_key (product_key);
ALTER TABLE device ADD INDEX IF NOT EXISTS idx_device_node (node_id);
-- unique iot_id depends on data; create only if safe
-- ALTER TABLE device ADD UNIQUE INDEX IF NOT EXISTS ux_device_iot (iot_id);

-- FamilyMemberElder / FamilyMember
ALTER TABLE family_member_elder ADD INDEX IF NOT EXISTS idx_fme_family (family_member_id);
ALTER TABLE family_member_elder ADD INDEX IF NOT EXISTS idx_fme_elder (elder_id);
ALTER TABLE family_member ADD INDEX IF NOT EXISTS idx_family_phone (phone);

-- Floor / Room / Device binding
ALTER TABLE device ADD INDEX IF NOT EXISTS idx_device_binding_loc_type (binding_location, location_type);
ALTER TABLE device ADD INDEX IF NOT EXISTS idx_device_binding_loc_type_phys (binding_location, location_type, physical_location_type);
ALTER TABLE floor ADD INDEX IF NOT EXISTS idx_floor_code_create (code, create_time DESC);
ALTER TABLE room ADD INDEX IF NOT EXISTS idx_room_floor (floor_id);
ALTER TABLE room ADD INDEX IF NOT EXISTS idx_room_code (code);
ALTER TABLE room ADD INDEX IF NOT EXISTS idx_room_floor_sort_time (floor_id, sort, create_time DESC);
ALTER TABLE room_type ADD INDEX IF NOT EXISTS idx_roomtype_name (name);

-- HealthAssessment / HealthAssessmentReport
ALTER TABLE health_assessment ADD INDEX IF NOT EXISTS idx_ha_idcard (id_card);
ALTER TABLE health_assessment ADD INDEX IF NOT EXISTS idx_ha_elder (elder_id);
ALTER TABLE health_assessment ADD INDEX IF NOT EXISTS idx_ha_checkin_status (check_in_status);

ALTER TABLE health_assessment_report ADD INDEX IF NOT EXISTS idx_har_assessment (health_assessment_id);
ALTER TABLE health_assessment_report ADD INDEX IF NOT EXISTS idx_har_time (assessment_time);

-- KnowledgeBase
ALTER TABLE knowledge_base ADD INDEX IF NOT EXISTS idx_kb_category (category);
ALTER TABLE knowledge_base ADD INDEX IF NOT EXISTS idx_kb_status (status);
ALTER TABLE knowledge_base ADD INDEX IF NOT EXISTS idx_kb_priority (priority);
-- For title/tags fulltext search, uncomment if using MySQL fulltext
-- ALTER TABLE knowledge_base ADD FULLTEXT INDEX ft_kb_title_tags (title, tags);

-- HealthAssessmentDataCollection (prefix index for TEXT-like fields)
-- Adjust prefix length according to column charset and selectivity
ALTER TABLE health_assessment_data_collection ADD INDEX IF NOT EXISTS idx_health_basic_info (basic_info(100));

-- DeviceData: optional covering index example (uncomment if needed and tested)
-- CREATE INDEX idx_device_iot_alarm_cover ON device_data (iot_id, alarm_time DESC, data_value(100), id);

-- End of patch
