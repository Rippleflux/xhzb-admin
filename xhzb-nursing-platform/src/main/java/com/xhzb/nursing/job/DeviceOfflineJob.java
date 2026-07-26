package com.xhzb.nursing.job;

import cn.hutool.core.collection.CollUtil;
import com.xhzb.common.constant.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 设备离线检测定时任务
 * 对比全量设备注册表 vs 最后上报时间 ZSET，发现离线设备
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class DeviceOfflineJob {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /** 默认上报间隔（秒），用于判断离线阈值 = 2 * interval */
    private static final long DEFAULT_REPORT_INTERVAL = 60;
    private static final long OFFLINE_MULTIPLIER = 2;

    /**
     * 每 60 秒检测一次离线设备
     */
    @Scheduled(fixedRate = 60_000)
    public void checkDeviceOffline() {
        try {
            // ① 获取全量设备注册表
            Set<Object> deviceKeys = redisTemplate.opsForHash()
                    .keys(CacheConstants.IOT_ALL_PRODUCT_LIST);
            // 也检查 device registry
            Set<Object> registryKeys = redisTemplate.opsForHash()
                    .keys("iot:device:registry");

            if (CollUtil.isEmpty(deviceKeys)) deviceKeys = registryKeys;
            if (CollUtil.isEmpty(deviceKeys)) return;

            long now = System.currentTimeMillis();
            long threshold = DEFAULT_REPORT_INTERVAL * OFFLINE_MULTIPLIER * 1000;

            int offlineCount = 0;
            for (Object obj : deviceKeys) {
                String deviceId = String.valueOf(obj);
                // ② 查 ZSET 最后上报时间
                Double score = redisTemplate.opsForZSet()
                        .score(CacheConstants.IOT_DEVICE_LAST_REPORT, deviceId);
                if (score == null) {
                    // 从未上报过 — 初始状态，跳过
                    continue;
                }
                // ③ 超过 2 倍上报间隔 → 离线
                if (now - score.longValue() > threshold) {
                    // 标记离线状态
                    redisTemplate.opsForHash().put(
                            "iot:device:offline", deviceId,
                            "lastReport=" + score.longValue() + ",detectedAt=" + now);
                    offlineCount++;
                    log.warn("设备离线: deviceId={}, lastReport={}ms ago",
                            deviceId, (now - score.longValue()) / 1000);
                } else {
                    // 恢复在线 — 清除离线标记
                    redisTemplate.opsForHash().delete("iot:device:offline", deviceId);
                }
            }

            if (offlineCount > 0) {
                log.info("离线设备检测完成: {} 台离线", offlineCount);
            }
        } catch (Exception e) {
            log.error("离线检测异常", e);
        }
    }
}
