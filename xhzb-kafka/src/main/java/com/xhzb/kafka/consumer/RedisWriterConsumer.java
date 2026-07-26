package com.xhzb.kafka.consumer;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.kafka.message.DeviceDataMessage;
import com.xhzb.nursing.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Redis 写入消费者 — 消费 device-data-topic → 更新 Redis 状态
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class RedisWriterConsumer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = "device-data-topic", groupId = "xhzb-redis-writer")
    public void consume(String message) {
        DeviceDataMessage msg = JSONUtil.toBean(message, DeviceDataMessage.class);

        // ① 更新设备最新数据
        DeviceData data = toDeviceData(msg);
        redisTemplate.opsForHash().put(
                CacheConstants.IOT_DEVICE_LAST_DATA,
                msg.getDeviceId(),
                JSONUtil.toJsonStr(Collections.singletonList(data)));

        // ② 更新最后上报时间 ZSET
        redisTemplate.opsForZSet().add(
                CacheConstants.IOT_DEVICE_LAST_REPORT,
                msg.getDeviceId(),
                msg.getEventTime() != null ? msg.getEventTime() : System.currentTimeMillis());
    }

    private DeviceData toDeviceData(DeviceDataMessage msg) {
        DeviceData d = new DeviceData();
        d.setIotId(msg.getDeviceId());
        d.setProductKey(msg.getProductKey());
        d.setFunctionId(msg.getFunctionId());
        d.setDataValue(msg.getDataValue());
        d.setDeviceName(msg.getDeviceName());
        d.setProductName(msg.getProductName());
        d.setLocationType(msg.getLocationType());
        d.setPhysicalLocationType(msg.getPhysicalLocationType());
        d.setAccessLocation(msg.getAccessLocation());
        d.setDeviceDescription(msg.getDeviceDescription());
        return d;
    }
}
