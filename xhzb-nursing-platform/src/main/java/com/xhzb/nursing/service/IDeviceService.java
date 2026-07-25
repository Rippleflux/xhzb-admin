package com.xhzb.nursing.service;

import java.util.List;

import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.nursing.domain.Device;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.dto.DeviceDetailVo;
import com.xhzb.nursing.domain.dto.DeviceDto;
import com.xhzb.nursing.domain.vo.ProductVo;

/**
 * 设备表Service接口
 * 
 * @author ripple
 * @date 2026-07-20
 */
public interface IDeviceService extends IService<Device>
{
    /**
     * 查询设备表
     * 
     * @param id 设备表主键
     * @return 设备表
     */
    public Device selectDeviceById(Long id);

    /**
     * 查询设备表列表
     * 
     * @param device 设备表
     * @return 设备表集合
     */
    public List<Device> selectDeviceList(Device device);

    /**
     * 新增设备表
     * 
     * @param device 设备表
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备表
     * 
     * @param device 设备表
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 批量删除设备表
     * 
     * @param ids 需要删除的设备表主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备表信息
     * 
     * @param id 设备表主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    void getSyncProductList();

    List<ProductVo> getAllProductList();

    void registerDevice(DeviceDto deviceDto);

    DeviceDetailVo queryDeviceDetail(String iotId);

    AjaxResult queryServiceProperties(String iotId);

    AjaxResult queryProduct(String productKey);
}
