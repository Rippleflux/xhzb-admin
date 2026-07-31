

## AiConversationMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/AiConversationMapper.xml

关键 SQL:
- selectConversationListByUserId: select id, name, create_by, update_by, create_time, update_time from ai_conversation where create_by = #{createBy} order by create_time desc

问题点与建议:
- create_by 字段用于等值过滤且按 create_time 排序，应确保存在复合索引 (create_by, create_time DESC) 以避免 filesort 并支持索引排序。
- 若 create_by 基本是低基数（少量用户），单列索引加上排序可能仍扫描较多行，但复合索引仍然优于无索引。

建议DDL:
ALTER TABLE ai_conversation ADD INDEX idx_ai_conv_create_by_time (create_by, create_time DESC);

回滚:
ALTER TABLE ai_conversation DROP INDEX idx_ai_conv_create_by_time;

验证:
- EXPLAIN SELECT ... WHERE create_by = ? ORDER BY create_time DESC
- 期望: key = idx_ai_conv_create_by_time, type = ref 或 range, Extra 不包含 Using filesort


## AiMessageMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/AiMessageMapper.xml

关键 SQL:
- selectMessagesByConversationId: select * from ai_message where conversation_id = #{conversationId} order by create_time
- deleteByConversationId: delete from ai_message where conversation_id = #{conversationId}

问题点与建议:
- conversation_id 用于按会话拉取消息，通常该表消息量大（状态不确定），需高效按 conversation_id 查询并按 create_time 排序/读历史
- 建议复合索引 (conversation_id, create_time)（升序或 DESC 取决于常见查询方向）以让 ORDER BY 利用索引并支持分页
- select 使用 SELECT *，建议仅查询必要字段以减少 IO（或者在 Mapper 中提供轻量 list 查询）
- deleteByConversationId 在会话级别清理大量数据时可能耗时，建议改为按批次删除或做软删除并异步归档

建议DDL:
ALTER TABLE ai_message ADD INDEX idx_ai_msg_conv_time (conversation_id, create_time);

回滚:
ALTER TABLE ai_message DROP INDEX idx_ai_msg_conv_time;

验证:
- EXPLAIN SELECT * FROM ai_message WHERE conversation_id = ? ORDER BY create_time
- 期望: key = idx_ai_msg_conv_time, type = ref 或 range, Extra 不包含 Using filesort

