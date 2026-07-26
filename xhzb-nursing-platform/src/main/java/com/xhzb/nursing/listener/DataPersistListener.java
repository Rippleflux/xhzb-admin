package com.xhzb.nursing.listener;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.nursing.domain.event.DeviceDataEvent;
import com.xhzb.nursing.service.IInfluxDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 数据持久化监听器 — 异步消费 DeviceDataEvent 写入 Redis + InfluxDB
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class DataPersistListener {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private IInfluxDBService influxDBService;

    @Async
    @EventListener
    public void onDeviceDataReceived(DeviceDataEvent event) {
        // 1. Redis 缓存（覆盖最新数据）
        String iotId = event.getDevice().getIotId();
        if (iotId != null && !event.getDeviceDataList().isEmpty()) {
            redisTemplate.opsForHash().put(
                    CacheConstants.IOT_DEVICE_LAST_DATA,
                    iotId,
                    JSONUtil.toJsonStr(event.getDeviceDataList()));
        }

        // 2. InfluxDB 写入（降级保护）
        try {
            influxDBService.writeDeviceData(event.getDeviceDataList());
        } catch (Exception e) {
            log.error("InfluxDB 写入失败 iotId={} count={}",
                    iotId, event.getDeviceDataList().size(), e);
        }
    }
}
