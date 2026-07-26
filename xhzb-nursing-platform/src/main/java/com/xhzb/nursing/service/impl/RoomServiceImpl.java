package com.xhzb.nursing.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.Room;
import com.xhzb.nursing.domain.vo.DeviceInfo;
import com.xhzb.nursing.mapper.RoomMapper;
import com.xhzb.nursing.service.IInfluxDBService;
import com.xhzb.nursing.service.IRoomService;
import com.xhzb.nursing.domain.vo.RoomVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 房间Service业务层处理
 *
 * @author ruoyi
 * @date 2025-03-28
 */
@Slf4j
@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements IRoomService {
    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IInfluxDBService influxDBService;

    /**
     * 查询房间
     *
     * @param id 房间主键
     * @return 房间
     */
    @Override
    public Room selectRoomById(Long id) {
        return getById(id);
    }

    /**
     * 查询房间列表
     *
     * @param room 房间
     * @return 房间
     */
    @Override
    public List<Room> selectRoomList(Room room) {
        return roomMapper.selectRoomList(room);
    }

    /**
     * 新增房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int insertRoom(Room room) {
        return save(room) ? 1 : 0;
    }

    /**
     * 修改房间
     *
     * @param room 房间
     * @return 结果
     */
    @Override
    public int updateRoom(Room room) {
        return updateById(room) ? 1 : 0;
    }

    /**
     * 批量删除房间
     *
     * @param ids 需要删除的房间主键
     * @return 结果
     */
    @Override
    public int deleteRoomByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 根据楼层 id 获取房间视图对象列表
     *
     * @param floorId
     * @return
     */
    @Override
    public List<RoomVo> getRoomsByFloorId(Long floorId) {
        return roomMapper.selectByFloorId(floorId);
    }


    /**
     * 获取所有房间（负责老人）
     *
     * @param floorId
     * @return
     */
    @Override
    public List<RoomVo> getRoomsWithNurByFloorId(Long floorId) {
        return roomMapper.selectByFloorIdWithNur(floorId);
    }

    @Override
    public RoomVo getRoomOne(Long id) {
        return roomMapper.getRoomOne(id);

    }

    /**
     * 根据楼层ID获取房间中的智能设备及数据
     * 1. 查MySQL获取房间/床位/老人/设备基础信息
     * 2. 从Redis读取设备最新上报数据并填充
     *
     * @param floorId 楼层ID
     * @return 房间VO列表
     */
    @Override
    public List<RoomVo> getRoomsWithDeviceByFloorId(Long floorId) {
        // 第一步：查询基础数据
        List<RoomVo> roomVos = roomMapper.getRoomsWithDeviceByFloorId(floorId);

        // 第二步：遍历填充Redis中的设备最新数据
        roomVos.forEach(roomVo -> {
            // 填充房间绑定的设备数据
            List<DeviceInfo> roomDevices = roomVo.getDeviceVos();
            if (roomDevices != null) {
                roomDevices.forEach(deviceInfo -> fillDeviceDataFromRedis(deviceInfo));
            }
            // 填充床位绑定的设备数据
            if (roomVo.getBedVoList() != null) {
                roomVo.getBedVoList().forEach(bedVo -> {
                    List<DeviceInfo> bedDevices = bedVo.getDeviceVos();
                    if (bedDevices != null) {
                        bedDevices.forEach(deviceInfo -> fillDeviceDataFromRedis(deviceInfo));
                    }
                });
            }
        });

        return roomVos;
    }

    /**
     * 从Redis读取设备最新数据并填充到DeviceInfo（Redis无数据时兜底查询DB）
     */
    private void fillDeviceDataFromRedis(DeviceInfo deviceInfo) {
        try {
            String jsonStr = (String) redisTemplate.opsForHash()
                    .get(CacheConstants.IOT_DEVICE_LAST_DATA, deviceInfo.getIotId());
            if (StrUtil.isNotEmpty(jsonStr)) {
                deviceInfo.setDeviceDataVos(JSONUtil.toList(jsonStr, DeviceData.class));
                return;
            }
            // Redis 无缓存，兜底查询 InfluxDB（解决 selectByIotId 无 LIMIT 问题）
            List<DeviceData> dbList = influxDBService.queryLatest(deviceInfo.getIotId(), 20);
            if (CollUtil.isNotEmpty(dbList)) {
                deviceInfo.setDeviceDataVos(dbList);
            }
        } catch (Exception e) {
            log.error("读取设备缓存失败, iotId={}", deviceInfo.getIotId(), e);
        }
    }


}
