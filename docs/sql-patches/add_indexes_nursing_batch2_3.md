## SQL Patch: add_indexes_nursing_batch2_3.sql

This SQL patch contains recommended index additions for the Nursing module, combining earlier high-priority Device/Alert indexes with Batch2/Batch3 additions (FamilyMemberElder, FamilyMember, Floor, Room, Device binding, HealthAssessment*, KnowledgeBase, HealthAssessmentDataCollection).

Important:
- Run in test environment first. Add indexes incrementally — do not apply everything to production at once.
- Use EXPLAIN FORMAT=JSON before/after each index to confirm optimizer changes.
- Some ALTER statements use "IF NOT EXISTS" syntax which is supported in recent MySQL/MariaDB versions. If your MySQL version does not support it, run a prior check (SHOW INDEX FROM <table> WHERE Key_name = 'idx_name') or remove IF NOT EXISTS and handle errors.

How to validate (example):
1) Before adding:
   EXPLAIN FORMAT=JSON SELECT * FROM device_data WHERE iot_id = 'xxx' ORDER BY alarm_time DESC LIMIT 50;
   Save output as before.json
2) Add index (single):
   ALTER TABLE device_data ADD INDEX idx_device_iot_alarm (iot_id, alarm_time DESC);
3) After adding:
   EXPLAIN FORMAT=JSON SELECT * FROM device_data WHERE iot_id = 'xxx' ORDER BY alarm_time DESC LIMIT 50;
   Save output as after.json
4) Compare before.json vs after.json: key, type, rows, Extra fields should improve.

If you want, I can:
- Split this patch into separate files per table so you can apply them gradually.
- Generate a PR with this patch applied to a new branch (feature/sql-indexes) so the team can review the changes before running them.

