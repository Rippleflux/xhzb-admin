

## RoomMapper — 分析

文件: xhzb-nursing-platform/src/main/resources/mapper/nursing/RoomMapper.xml

关键 SQL:
- selectRoomList: 代码/排序/类型/楼层/删除标志的条件过滤
- selectByFloorId / selectByFloorIdWithNur / getRoomsWithDeviceByFloorId: 多表 LEFT JOIN（room->floor->room_type->bed->elder->device）按 r.floor_id 过滤并排序
- getRoomOne: 简单 join floor/room_type by room.id

问题点与建议:
- 过滤条件中 floor_id、code、is_deleted 应有索引以支持 WHERE r.floor_id = ? 以及 r.code = ? 的查找
- selectByFloorId 与 getRoomsWithDeviceByFloorId 是按 floor_id 大量聚合/连接，floor_id 应有索引（通常为外键会有），另外用于 ORDER BY 的 r.sort, r.create_time 如果希望避免 filesort 可考虑联合索引 (floor_id, sort, create_time DESC)
- 多表 JOIN 中 device 的绑定条件（d.binding_location, d.location_type, d.physical_location_type）应有组合索引以加速 LEFT JOIN
- elder 的床位 join b.id = e.bed_id 需要 bed.id 和 e.bed_id 的索引（一般主键/外键已有）
- selectByFloorId 使用 left join room_type rt on rt.name = r.type_name —— 如果 room_type.name 不是主键，按 name join 可能慢，建议 room.type_name 改为 FK 指向 room_type.id 或对 room_type.name 建索引

建议DDL:
ALTER TABLE room ADD INDEX idx_room_floor (floor_id);
ALTER TABLE room ADD INDEX idx_room_code (code);
ALTER TABLE room ADD INDEX idx_room_floor_sort_time (floor_id, sort, create_time DESC);
ALTER TABLE device ADD INDEX idx_device_binding_loc_type_phys (binding_location, location_type, physical_location_type);
-- 如果 room_type.name 频繁用于 join
ALTER TABLE room_type ADD INDEX idx_roomtype_name (name);

回滚:
ALTER TABLE room DROP INDEX idx_room_floor;
ALTER TABLE room DROP INDEX idx_room_code;
ALTER TABLE room DROP INDEX idx_room_floor_sort_time;
ALTER TABLE device DROP INDEX idx_device_binding_loc_type_phys;
ALTER TABLE room_type DROP INDEX idx_roomtype_name;

验证:
- EXPLAIN selectByFloorId / getRoomsWithDeviceByFloorId
- 期望 r 使用 idx_room_floor，device 使用 idx_device_binding_loc_type_phys


