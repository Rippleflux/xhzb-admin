package com.xhzb.kafka.consumer;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.message.DeviceDataMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 写入消费者 — 消费 device-data-topic → 更新 Redis 设备状态
 * <p>
 * 2.1 改造：单 Hash (iot:device_last_data) → 设备维度 Key (iot:{deviceId}:state)
 * 消除单 Key 热点，支持 Redis Cluster 分片。
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

        // ① 更新设备最新状态 (设备维度 Key)
        String stateKey = CacheConstants.IOT_DEVICE_STATE_PREFIX + msg.getDeviceId();
        redisTemplate.opsForHash().put(stateKey, "iotId", msg.getDeviceId());
        redisTemplate.opsForHash().put(stateKey, "productKey", nvl(msg.getProductKey()));
        redisTemplate.opsForHash().put(stateKey, "productName", nvl(msg.getProductName()));
        redisTemplate.opsForHash().put(stateKey, "deviceName", nvl(msg.getDeviceName()));
        redisTemplate.opsForHash().put(stateKey, "functionId", msg.getFunctionId());
        redisTemplate.opsForHash().put(stateKey, "dataValue", msg.getDataValue());
        redisTemplate.opsForHash().put(stateKey, "locationType", String.valueOf(msg.getLocationType() != null ? msg.getLocationType() : ""));
        redisTemplate.opsForHash().put(stateKey, "physicalLocationType", String.valueOf(msg.getPhysicalLocationType() != null ? msg.getPhysicalLocationType() : ""));
        redisTemplate.opsForHash().put(stateKey, "accessLocation", nvl(msg.getAccessLocation()));
        redisTemplate.opsForHash().put(stateKey, "deviceDescription", nvl(msg.getDeviceDescription()));
        redisTemplate.opsForHash().put(stateKey, "lastReportTime", String.valueOf(System.currentTimeMillis()));

        // ② 更新最后上报时间 ZSET（离线检测用）
        redisTemplate.opsForZSet().add(
                CacheConstants.IOT_DEVICE_LAST_REPORT,
                msg.getDeviceId(),
                msg.getEventTime() != null ? msg.getEventTime() : System.currentTimeMillis());

        // ③ 也维护兼容的旧 Key（过渡期，后续移除）
        redisTemplate.opsForHash().put(CacheConstants.IOT_DEVICE_LAST_DATA,
                msg.getDeviceId(), JSONUtil.toJsonStr(message));
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
