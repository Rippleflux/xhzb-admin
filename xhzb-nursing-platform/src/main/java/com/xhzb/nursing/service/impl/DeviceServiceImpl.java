package com.xhzb.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.*;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.constant.Constants;
import com.xhzb.common.constant.HttpStatus;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.StringUtils;
import com.xhzb.common.utils.uuid.IdUtils;
import com.xhzb.framework.config.IotClientConfig;
import com.xhzb.nursing.domain.dto.DeviceDetailVo;
import com.xhzb.nursing.domain.dto.DeviceDto;
import com.xhzb.nursing.domain.vo.ProductVo;
import com.xhzb.nursing.util.DateTimeZoneConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.DeviceMapper;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.service.IDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 设备表Service业务层处理
 *
 * @author ripple
 * @date 2026-07-20
 */
@Slf4j
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements IDeviceService {
    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private IoTDAClient ioTDAClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询设备表
     *
     * @param id 设备表主键
     * @return 设备表
     */
    @Override
    public Device selectDeviceById(Long id) {
        return getById(id);
    }

    /**
     * 查询设备表列表
     *
     * @param device 设备表
     * @return 设备表
     */
    @Override
    public List<Device> selectDeviceList(Device device) {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 新增设备表
     *
     * @param device 设备表
     * @return 结果
     */
    @Override
    public int insertDevice(Device device) {
        return save(device) ? 1 : 0;
    }

    /**
     * 修改设备表
     *
     * @param device 设备表
     * @return 结果
     */
    @Override
    public int updateDevice(Device device) {
        return updateById(device) ? 1 : 0;
    }

    /**
     * 批量删除设备表
     *
     * @param ids 需要删除的设备表主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除设备表信息
     *
     * @param id 设备表主键
     * @return 结果
     */
    @Override
    public int deleteDeviceById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    @Override
    public void getSyncProductList() {
        ListProductsRequest queryParams = new ListProductsRequest();
        ListProductsResponse result = ioTDAClient.listProducts(queryParams);
        if (result.getHttpStatusCode() != HttpStatus.SUCCESS) {
            throw new ServiceException("查询所有产品列表失败");
        }
        List<ProductSummary> products = result.getProducts();
        redisTemplate.opsForValue().set(CacheConstants.IOT_ALL_PRODUCT_LIST, JSONUtil.toJsonStr(products));
    }

    @Override
    public List<ProductVo> getAllProductList() {
        String productStr = redisTemplate.opsForValue().get(CacheConstants.IOT_ALL_PRODUCT_LIST);
        return JSONUtil.toList(productStr, ProductVo.class);
    }

    @Override
    public void registerDevice(DeviceDto deviceDto) {
        //    1、校验设备标识码 nodeId
        String nodeId = deviceDto.getNodeId();
        Long hasNodeId = this.lambdaQuery().eq(Device::getNodeId, nodeId).count();
        if (hasNodeId != 0) {
            throw new ServiceException("当前设备标识码已存在！");
        }
        //    2、设备名称 deviceName
        String deviceName = deviceDto.getDeviceName();
        Long hasDeviceName = this.lambdaQuery().eq(Device::getDeviceName, deviceName).count();
        if (hasDeviceName != 0) {
            throw new ServiceException("当前设备名称已存在！");
        }
        //    3、同一位置不允许两台相同设备
        LambdaQueryWrapper<Device> lambdaQueryWrapper = Wrappers.lambdaQuery(Device.class)
                .eq(deviceDto.getPhysicalLocationType() != null, Device::getPhysicalLocationType, deviceDto.getPhysicalLocationType())
                .eq(Device::getBindingLocation, deviceDto.getBindingLocation())
                .eq(Device::getLocationType, deviceDto.getLocationType())
                .eq(Device::getProductKey, deviceDto.getProductKey())
                .eq(Device::getProductName, deviceDto.getProductName());
        long count = this.count(lambdaQueryWrapper);
        if (count != 0) {
            throw new ServiceException("同一个位置不允许绑定相同产品！");
        }


        // 封装必传字段给iot平台
        AddDeviceRequest addDeviceRequest = new AddDeviceRequest();
        AddDevice addDevice = new AddDevice();
        addDevice.withNodeId(deviceDto.getNodeId());
        addDevice.withDeviceName(deviceDto.getDeviceName());
        addDevice.withProductId(deviceDto.getProductKey());


        AuthInfo authInfo = new AuthInfo();
        String uuid = IdUtils.simpleUUID();
        authInfo.setSecret(uuid);
        addDevice.withAuthInfo(authInfo);
        addDeviceRequest.withBody(addDevice);

        AddDeviceResponse addDeviceResponse = ioTDAClient.addDevice(addDeviceRequest);

        if (addDeviceResponse.getHttpStatusCode() != 201) {
            throw new ServiceException("Iot平台注册产品失败！");
        }

        // 响应成功 获取iotId
        String iotId = addDeviceResponse.getDeviceId();

        Device bean = BeanUtil.toBean(deviceDto, Device.class);
        // 复制对象生成Device 对象 拿到返回值 iotId 和 secret
        bean.setIotId(iotId);
        bean.setSecret(uuid);

        this.save(bean);

    }

    @Override
    public DeviceDetailVo queryDeviceDetail(String iotId) {

        Device device = this.getOne(Wrappers.lambdaQuery(Device.class).eq(Device::getIotId, iotId));
        if (device == null) {
            throw new ServiceException("设备表查询失败！");
        }

        //1-查询iot设备的详情：获取设备状态+激活时间
        ShowDeviceRequest showDeviceRequest = new ShowDeviceRequest();
        showDeviceRequest.withDeviceId(iotId);
        ShowDeviceResponse result = ioTDAClient.showDevice(showDeviceRequest);
        if (result.getHttpStatusCode() != 200) {
            throw new ServiceException("Iot平台查询异常！");
        }
        //2-查询设备表
        String activeTime = result.getActiveTime();
        String status = result.getStatus();
        LocalDateTime activeTimeTemp = null;
        if (StringUtils.isNotEmpty(activeTime)) {
            LocalDateTime parse = LocalDateTimeUtil.parse(activeTime, DatePattern.UTC_MS_PATTERN);
            activeTimeTemp = DateTimeZoneConverter.utcToShanghai(parse);
        }

        //3-构建返回数据：DeviceDetailVo
        DeviceDetailVo deviceDetail = BeanUtil.toBean(device, DeviceDetailVo.class);
        deviceDetail.setDeviceStatus(status);
        deviceDetail.setActiveTime(activeTimeTemp);

        return deviceDetail;

    }

    @Override
    public AjaxResult queryServiceProperties(String iotId) {
        //     1、调用iot平台设备 影子接口 获取设备影子数据

        ShowDeviceShadowRequest queryParams = new ShowDeviceShadowRequest();
        queryParams.withDeviceId(iotId);
        ShowDeviceShadowResponse response = ioTDAClient.showDeviceShadow(queryParams);

        if (response.getHttpStatusCode() != 200) {
            throw new ServiceException("当前未查询到影子数据");
        }
        List<DeviceShadowData> shadowDataList = response.getShadow();

        if (CollUtil.isEmpty(shadowDataList)) {
            return AjaxResult.success(List.of());
        }
        log.info("-------------------shadowDataList={}", shadowDataList);

        // 构建返回的数据 List<Map<String,Object>>
        DeviceShadowProperties reported = shadowDataList.get(0).getReported();
        log.info("-------------------reported={}", reported);

        //  "20151212T121212Z"
        String eventTime = reported.getEventTime();
        LocalDateTime utcEventTime = LocalDateTimeUtil.parse(eventTime, "yyyyMMdd'T'HHmmss'Z'");
        LocalDateTime localDateTime = DateTimeZoneConverter.utcToShanghai(utcEventTime);
        Object properties = reported.getProperties();

        List<Map<String, Object>> listData = new ArrayList<>();

        JSONObject entries = JSONUtil.parseObj(properties);
        entries.forEach((k, v) -> {
            Map<String, Object> map = MapUtil.<String, Object>builder()
                    .put("functionId", k)
                    .put("value", v)
                    .put("eventTime", localDateTime)
                    .build();
            listData.add(map);
        });

        return AjaxResult.success(listData);
    }

    /**
     * 查询产品详情
     * @param productKey
     * @return
     */
    @Override
    public AjaxResult queryProduct(String productKey) {
        // 参数校验
        if (StrUtil.isEmpty(productKey)) {
            throw new BaseException("请输入正确的参数");
        }
        // 调用华为云物联网接口
        ShowProductRequest showProductRequest = new ShowProductRequest();
        showProductRequest.setProductId(productKey);
        ShowProductResponse response;

        try {
            response = ioTDAClient.showProduct(showProductRequest);
        } catch (Exception e) {
            throw new BaseException("查询产品详情失败");
        }
        // 判断是否存在服务数据
        List<ServiceCapability> serviceCapabilities = response.getServiceCapabilities();
        if (CollUtil.isEmpty(serviceCapabilities)) {
            return AjaxResult.success(Collections.emptyList());
        }

        return AjaxResult.success(serviceCapabilities);
    }


}
