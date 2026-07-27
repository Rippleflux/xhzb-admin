package com.xhzb.nursing.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.message.DeviceDataMessage;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.enums.AlertState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 报警引擎 — Kafka 消费者，消费 device-data-topic 进行实时规则匹配
 * <p>
 * 替代 2.0 版本的 AlertDetectionListener（@EventListener），
 * 改为 @KafkaListener 实现持久化 + 回放 + Consumer Group 并发。
 * <p>
 * 七步判定：幂等 → 规则索引 → 状态机 → 时段 → 阈值 → 沉默 → 持续
 * 命中后 publish alert-topic → AlertNotifier 消费入库 + WebSocket
 *
 * @author rippleflux
 * @date 2026-07-27
 */
@Slf4j
@Component
public class AlertEngine {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String ALERT_TOPIC = "alert-topic";

    @KafkaListener(topics = "device-data-topic", groupId = "xhzb-alert-engine")
    public void onDeviceData(String message) {
        try {
            DeviceDataMessage msg = JSONUtil.toBean(message, DeviceDataMessage.class);
            DeviceData deviceData = toDeviceData(msg);

            evaluateRules(deviceData);
        } catch (Exception e) {
            log.error("AlertEngine 处理失败, 将由 Kafka 重试", e);
            throw e; // re-throw → DefaultErrorHandler 重试
        }
    }

    private void evaluateRules(DeviceData deviceData) {
        String functionId = deviceData.getFunctionId();
        if (StrUtil.isEmpty(functionId)) return;

        // ② 规则索引 O(1) 查找
        Set<String> ruleIdSet = redisTemplate.opsForSet()
                .members(CacheConstants.IOT_RULE_INDEX_PREFIX + functionId);
        if (CollUtil.isEmpty(ruleIdSet)) return;

        for (String ruleIdStr : ruleIdSet) {
            Long ruleId = Long.valueOf(ruleIdStr);
            // ③ 状态机检查
            if (!canTrigger(deviceData.getIotId(), ruleId)) continue;
            // ④ 生效时段检查
            if (!isInEffectivePeriod(ruleId)) continue;
            // ⑤ 阈值判断
            if (!isThresholdExceeded(ruleId, deviceData)) {
                clearTriggerCount(deviceData.getIotId(), ruleId);
                recoverIfAlarming(deviceData.getIotId(), ruleId);
                continue;
            }
            // ⑥ 沉默周期检查
            if (isInSilentPeriod(deviceData.getIotId(), ruleId)) continue;
            // ⑦ 持续周期判定
            int count = incrTriggerCount(deviceData.getIotId(), ruleId);
            int duration = getRuleDuration(ruleId);
            if (count >= duration) {
                publishAlert(ruleId, deviceData);
            }
        }
    }

    private void publishAlert(Long ruleId, DeviceData deviceData) {
        // 报警幂等：同一设备+规则+时间窗口内不重复发送
        String publishKey = CacheConstants.IOT_MESSAGE_PROCESSED + "alert:"
                + deviceData.getIotId() + ":" + ruleId + ":" + deviceData.getAlarmTime();
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(publishKey, "1", 1, TimeUnit.HOURS))) {
            return; // 已发布过，跳过
        }

        // 先写状态 — 立即阻塞后续消息，消除 race window
        clearTriggerCount(deviceData.getIotId(), ruleId);

        String silentStr = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "alertSilentPeriod");
        int silentMin = silentStr != null ? Integer.parseInt(silentStr) : 5;
        setSilentPeriod(deviceData.getIotId(), ruleId, silentMin);
        setAlertState(deviceData.getIotId(), ruleId, AlertState.ALARM, silentMin);

        // 再同步发送 → 失败时清除状态允许重试
        Map<String, String> alert = new HashMap<>();
        alert.put("ruleId", String.valueOf(ruleId));
        alert.put("deviceData", JSONUtil.toJsonStr(deviceData));
        String alertJson = JSONUtil.toJsonStr(alert);

        try {
            kafkaTemplate.send(ALERT_TOPIC, alertJson).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            // 清除状态，允许 Kafka 重试时重新触发
            redisTemplate.delete(publishKey);
            redisTemplate.delete(CacheConstants.ALERT_SILENT_PREFIX + deviceData.getIotId() + ":" + ruleId);
            redisTemplate.delete(CacheConstants.IOT_ALERT_STATE_PREFIX + deviceData.getIotId() + ":" + ruleId);
            log.error("AlertEngine 发送 alert-topic 失败 ruleId={} iotId={}",
                    ruleId, deviceData.getIotId(), e);
            throw new RuntimeException("Kafka alert-topic 发送失败", e);
        }

        log.info("AlertEngine 触发 ruleId={} iotId={} functionId={} value={}",
                ruleId, deviceData.getIotId(), deviceData.getFunctionId(), deviceData.getDataValue());
    }

    // ═══ 以下辅助方法复用自 AlertDetectionListener 逻辑 ═══

    private boolean canTrigger(String iotId, Long ruleId) {
        String stateKey = CacheConstants.IOT_ALERT_STATE_PREFIX + iotId + ":" + ruleId;
        String state = redisTemplate.opsForValue().get(stateKey);
        if (StrUtil.isEmpty(state)) return true;
        try { return AlertState.valueOf(state).canTrigger(); }
        catch (IllegalArgumentException e) { return true; }
    }

    private void setAlertState(String iotId, Long ruleId, AlertState state, int ttlMinutes) {
        redisTemplate.opsForValue().set(
                CacheConstants.IOT_ALERT_STATE_PREFIX + iotId + ":" + ruleId,
                state.name(), ttlMinutes, TimeUnit.MINUTES);
    }

    private void recoverIfAlarming(String iotId, Long ruleId) {
        String stateKey = CacheConstants.IOT_ALERT_STATE_PREFIX + iotId + ":" + ruleId;
        if (AlertState.ALARM.name().equals(redisTemplate.opsForValue().get(stateKey))) {
            redisTemplate.delete(stateKey);
            log.info("报警恢复 iotId={} ruleId={}", iotId, ruleId);
        }
    }

    private boolean isInEffectivePeriod(Long ruleId) {
        String period = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "alertEffectivePeriod");
        if (StrUtil.isEmpty(period)) return true;
        try {
            String[] parts = period.split("~");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            LocalTime now = LocalTime.now();
            if (start.isBefore(end) || start.equals(end))
                return !now.isBefore(start) && !now.isAfter(end);
            else
                return !now.isBefore(start) || !now.isAfter(end);
        } catch (Exception e) { return true; }
    }

    private boolean isThresholdExceeded(Long ruleId, DeviceData deviceData) {
        String dataValue = deviceData.getDataValue();
        if (StrUtil.isEmpty(dataValue)) return false;
        String op = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "operator");
        String thresholdStr = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "value");
        if (StrUtil.isEmpty(op) || StrUtil.isEmpty(thresholdStr)) return false;
        try {
            double val = Double.parseDouble(dataValue);
            double threshold = Double.parseDouble(thresholdStr);
            return switch (op) {
                case ">" -> val > threshold;
                case "<" -> val < threshold;
                case ">=" -> val >= threshold;
                case "<=" -> val <= threshold;
                case "==" -> val == threshold;
                default -> false;
            };
        } catch (NumberFormatException e) { return false; }
    }

    private int getRuleDuration(Long ruleId) {
        String dur = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "duration");
        return dur != null ? Integer.parseInt(dur) : 1;
    }

    private boolean isInSilentPeriod(String iotId, Long ruleId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(
                CacheConstants.ALERT_SILENT_PREFIX + iotId + ":" + ruleId));
    }

    private int incrTriggerCount(String iotId, Long ruleId) {
        Long count = redisTemplate.opsForValue().increment(
                CacheConstants.ALERT_TRIGGER_COUNT_PREFIX + iotId + ":" + ruleId);
        return count != null ? count.intValue() : 1;
    }

    private void clearTriggerCount(String iotId, Long ruleId) {
        redisTemplate.delete(CacheConstants.ALERT_TRIGGER_COUNT_PREFIX + iotId + ":" + ruleId);
    }

    private void setSilentPeriod(String iotId, Long ruleId, int minutes) {
        redisTemplate.opsForValue().set(
                CacheConstants.ALERT_SILENT_PREFIX + iotId + ":" + ruleId,
                "1", minutes, TimeUnit.MINUTES);
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
        if (msg.getEventTime() != null) {
            d.setAlarmTime(java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(msg.getEventTime()),
                    java.time.ZoneId.of("Asia/Shanghai")));
        }
        return d;
    }
}
