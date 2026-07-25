package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.DeviceData;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.vo.device.IotMsgNotifyData;

/**
 * 设备数据表Service接口
 * 
 * @author rippleflux
 * @date 2026-07-20
 */
public interface IDeviceDataService extends IService<DeviceData>
{
    /**
     * 查询设备数据表
     * 
     * @param id 设备数据表主键
     * @return 设备数据表
     */
    public DeviceData selectDeviceDataById(Long id);

    /**
     * 查询设备数据表列表
     * 
     * @param deviceData 设备数据表
     * @return 设备数据表集合
     */
    public List<DeviceData> selectDeviceDataList(DeviceData deviceData);

    /**
     * 新增设备数据表
     * 
     * @param deviceData 设备数据表
     * @return 结果
     */
    public int insertDeviceData(DeviceData deviceData);

    /**
     * 修改设备数据表
     * 
     * @param deviceData 设备数据表
     * @return 结果
     */
    public int updateDeviceData(DeviceData deviceData);

    /**
     * 批量删除设备数据表
     * 
     * @param ids 需要删除的设备数据表主键集合
     * @return 结果
     */
    public int deleteDeviceDataByIds(Long[] ids);

    /**
     * 删除设备数据表信息
     * 
     * @param id 设备数据表主键
     * @return 结果
     */
    public int deleteDeviceDataById(Long id);

    void batchInsertDeviceData(IotMsgNotifyData iotMsgNotifyData);
}
