package com.xhzb.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.domain.vo.device.IotMsgNotifyData;
import com.xhzb.nursing.mapper.DeviceMapper;
import com.xhzb.nursing.util.DateTimeZoneConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.DeviceDataMapper;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.service.IDeviceDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;

/**
 * 设备数据表Service业务层处理
 *
 * @author rippleflux
 * @date 2026-07-20
 */
@Service
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements IDeviceDataService {
    @Autowired
    private DeviceDataMapper deviceDataMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询设备数据表
     *
     * @param id 设备数据表主键
     * @return 设备数据表
     */
    @Override
    public DeviceData selectDeviceDataById(Long id) {
        return getById(id);
    }

    /**
     * 查询设备数据表列表
     *
     * @param deviceData 设备数据表
     * @return 设备数据表
     */
    @Override
    public List<DeviceData> selectDeviceDataList(DeviceData deviceData) {
        return deviceDataMapper.selectDeviceDataList(deviceData);
    }

    /**
     * 新增设备数据表
     *
     * @param deviceData 设备数据表
     * @return 结果
     */
    @Override
    public int insertDeviceData(DeviceData deviceData) {
        return save(deviceData) ? 1 : 0;
    }

    /**
     * 修改设备数据表
     *
     * @param deviceData 设备数据表
     * @return 结果
     */
    @Override
    public int updateDeviceData(DeviceData deviceData) {
        return updateById(deviceData) ? 1 : 0;
    }

    /**
     * 批量删除设备数据表
     *
     * @param ids 需要删除的设备数据表主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备数据表信息
     *
     * @param id 设备数据表主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDataById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchInsertDeviceData(IotMsgNotifyData iotMsgNotifyData) {
        String iotId = iotMsgNotifyData.getHeader().getDeviceId();
        //查询设备信息
        Device device = deviceMapper.selectOne(Wrappers.<Device>lambdaQuery().eq(Device::getIotId, iotId));
        if (ObjectUtil.isEmpty(device)) {
            log.error("设备不存在");
            return;
        }
        //批量保存设备数据
        iotMsgNotifyData.getBody().getServices().forEach(s -> {
            //判断属性是否为空
            Map<String, Object> properties = s.getProperties();
            if (CollUtil.isEmpty(properties)) {
                return;
            }

            //上报时间处理
            String eventTimeStr = s.getEventTime();
            LocalDateTime localDateTime = LocalDateTimeUtil.parse(eventTimeStr, "yyyyMMdd'T'HHmmss'Z'");
            LocalDateTime eventTime = DateTimeZoneConverter.utcToShanghai(localDateTime);

            List<DeviceData> list = new ArrayList<>();

            //key:属性id，value:属性值
            properties.forEach((k, v) -> {
                CopyOptions copyOptions = CopyOptions.create()
                        .setIgnoreNullValue(true)                // 忽略 null 值
                        .setIgnoreProperties("id", "createTime", "updateTime", "remark", "updateBy", "createBy");// 忽略敏

                DeviceData deviceData = BeanUtil.toBean(device, DeviceData.class, copyOptions);
                deviceData.setAlarmTime(eventTime);
                deviceData.setFunctionId(k);
                deviceData.setDataValue(v + "");
                deviceData.setAccessLocation(device.getBindingLocation());
                list.add(deviceData);
            });
            //批量保存设备数据
            this.saveBatch(list);
            // 写入Redis缓存：每次上报后覆盖，保证存储最新数据
            redisTemplate.opsForHash().put(CacheConstants.IOT_DEVICE_LAST_DATA, device.getIotId(), JSONUtil.toJsonStr(list));
        });

    }
}
