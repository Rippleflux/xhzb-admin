package com.xhzb.nursing.service;

import com.xhzb.nursing.domain.DeviceData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * InfluxDB 时序数据服务接口
 *
 * @author rippleflux
 * @date 2026-07-26
 */
public interface IInfluxDBService {

    /**
     * 批量写入设备数据（内部 try-catch，失败不抛异常）
     *
     * @param list 设备数据列表
     */
    void writeDeviceData(List<DeviceData> list);

    /**
     * 查询设备最近 N 条数据（替代 selectByIotId）
     *
     * @param iotId 设备 ID
     * @param limit 最大条数
     * @return 设备数据列表
     */
    List<DeviceData> queryLatest(String iotId, int limit);

    /**
     * 查询设备指定时间范围 + 物模型的历史数据
     *
     * @param iotId      设备 ID
     * @param functionId 物模型标识（如 HeartRate）
     * @param start      开始时间
     * @param end        结束时间
     * @return 设备数据列表
     */
    List<DeviceData> queryRange(String iotId, String functionId,
                                LocalDateTime start, LocalDateTime end);

    /**
     * 查询设备历史趋势（带聚合）
     *
     * @param iotId      设备 ID
     * @param functionId 物模型标识
     * @param range      时间范围: 1h/24h/7d/30d
     * @param interval   聚合间隔: 1m/5m/15m/1h
     * @return 设备数据列表
     */
    List<DeviceData> queryTrend(String iotId, String functionId,
                                String range, String interval);
}
