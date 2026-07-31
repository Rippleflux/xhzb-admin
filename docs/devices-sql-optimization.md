

## HealthAssessmentMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/HealthAssessmentMapper.xml

关键 SQL:
- selectHealthAssessmentList: elder_name LIKE '%', id_card = ?, elder_id = ?, core_suggestion = ?, check_in_status, evaluation_progress

问题点与建议:
- elder_name 使用 leading-wildcard LIKE，建议改为前缀或 FULLTEXT，如果频繁按 elder_id、id_card 查询请加索引
- 建议为 id_card、elder_id、check_in_status 建索引

建议DDL:
ALTER TABLE health_assessment ADD INDEX idx_ha_idcard (id_card);
ALTER TABLE health_assessment ADD INDEX idx_ha_elder (elder_id);
ALTER TABLE health_assessment ADD INDEX idx_ha_checkin_status (check_in_status);

回滚:
ALTER TABLE health_assessment DROP INDEX idx_ha_idcard;
ALTER TABLE health_assessment DROP INDEX idx_ha_elder;
ALTER TABLE health_assessment DROP INDEX idx_ha_checkin_status;

验证:
- EXPLAIN SELECT ... WHERE id_card = ?; EXPLAIN SELECT ... WHERE elder_id = ?


## HealthAssessmentReportMapper — 分析（摘要）

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/HealthAssessmentReportMapper.xml

关键 SQL:
- selectHealthAssessmentReportList: 多字段动态等值与少量 LIKE

问题点与建议:
- 表字段很多，重点索引应聚焦在高频等值查找字段：health_assessment_id、assessment_time、check_in_status
- 建议为 health_assessment_id 建索引，必要时为 assessment_time 建索引以支持时间范围查询

建议DDL:
ALTER TABLE health_assessment_report ADD INDEX idx_har_assessment (health_assessment_id);
ALTER TABLE health_assessment_report ADD INDEX idx_har_time (assessment_time);

回滚:
ALTER TABLE health_assessment_report DROP INDEX idx_har_assessment;
ALTER TABLE health_assessment_report DROP INDEX idx_har_time;

验证:
- EXPLAIN on representative queries


## KnowledgeBaseMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/KnowledgeBaseMapper.xml

关键 SQL:
- selectKnowledgeBaseList: title = ?, category = ?, tags = ?, status, priority

问题点与建议:
- 对于 category/status/priority 等低基数字段，单列索引有利于过滤
- tags 和 title 若用于全文搜索，建议 FULLTEXT 索引或外部搜索系统

建议DDL:
ALTER TABLE knowledge_base ADD INDEX idx_kb_category (category);
ALTER TABLE knowledge_base ADD INDEX idx_kb_status (status);
ALTER TABLE knowledge_base ADD INDEX idx_kb_priority (priority);
-- 对标题/标签的全文搜索
-- ALTER TABLE knowledge_base ADD FULLTEXT INDEX ft_kb_title_tags (title, tags);

回滚:
ALTER TABLE knowledge_base DROP INDEX idx_kb_category;
ALTER TABLE knowledge_base DROP INDEX idx_kb_status;
ALTER TABLE knowledge_base DROP INDEX idx_kb_priority;
-- ALTER TABLE knowledge_base DROP INDEX ft_kb_title_tags;

验证:
- EXPLAIN SELECT ... WHERE category = ?; EXPLAIN SELECT ... WHERE priority = ?


## NursingLevelMapper & NursingPlanMapper — 分析（合并摘要）

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/NursingLevelMapper.xml
          xhzb-nursing-platform/src/main/resources/mapper/nursing/NursingPlanMapper.xml

关键 SQL:
- NursingLevel: join nursing_plan on lplan_id; where nl.name LIKE '%', nl.status = ?
- NursingPlan: plan_name LIKE '%', status = ?

问题点与建议:
- name/plan_name 的 leading wildcard LIKE 无法命中索引；若需要模糊搜索，建议前缀或 FULLTEXT
- status 为常用过滤字段，添加单列索引能加速
- nl.lplan_id 是 join 字段（nl.left join nursing_plan np on nl.lplan_id = np.id），应确保 lplan_id 与 np.id 有索引

建议DDL:
ALTER TABLE nursing_level ADD INDEX idx_nl_status (status);
ALTER TABLE nursing_plan ADD INDEX idx_np_status (status);
ALTER TABLE nursing_level ADD INDEX idx_nl_lplan (lplan_id);

回滚:
ALTER TABLE nursing_level DROP INDEX idx_nl_status;
ALTER TABLE nursing_plan DROP INDEX idx_np_status;
ALTER TABLE nursing_level DROP INDEX idx_nl_lplan;

验证:
- EXPLAIN selectNursingLevelList（检查 join 使用 np.id）

