package com.xhzb.nursing.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.event.DeviceDataEvent;
import com.xhzb.nursing.enums.AlertState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 报警检测监听器 — 事件驱动实时规则匹配
 * 替代原 Quartz AlertJob 60s 轮询，数据到达即检测
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class AlertDetectionListener {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Async
    @EventListener
    public void onDeviceDataReceived(DeviceDataEvent event) {
        List<DeviceData> dataList = event.getDeviceDataList();
        if (CollUtil.isEmpty(dataList)) return;

        for (DeviceData deviceData : dataList) {
            // ① 消息幂等检查
            String msgKey = deviceData.getIotId() + ":" + deviceData.getFunctionId() + ":" + deviceData.getAlarmTime();
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                    CacheConstants.IOT_MESSAGE_PROCESSED + msgKey, "1", 1, TimeUnit.HOURS))) {
                evaluateRules(deviceData);
            }
        }
    }

    /**
     * 根据 functionId 从规则索引查找匹配规则，逐条判定
     */
    private void evaluateRules(DeviceData deviceData) {
        String functionId = deviceData.getFunctionId();
        if (StrUtil.isEmpty(functionId)) return;

        // ② 规则索引查找: iot:rule:index:{functionId} → Set<ruleId>
        Set<String> ruleIdSet = redisTemplate.opsForSet()
                .members(CacheConstants.IOT_RULE_INDEX_PREFIX + functionId);
        if (CollUtil.isEmpty(ruleIdSet)) return;

        for (String ruleIdStr : ruleIdSet) {
            Long ruleId = Long.valueOf(ruleIdStr);
            // ③ 报警状态机检查
            if (!canTrigger(deviceData.getIotId(), ruleId)) continue;
            // ④ 生效时段检查
            if (!isInEffectivePeriod(ruleId)) continue;
            // ⑤ 阈值判断
            if (!isThresholdExceeded(ruleId, deviceData)) {
                clearTriggerCount(deviceData.getIotId(), ruleId);
                // 数据恢复正常 → 从ALARM恢复为NORMAL
                recoverIfAlarming(deviceData.getIotId(), ruleId);
                continue;
            }
            // ⑥ 沉默周期检查
            if (isInSilentPeriod(deviceData.getIotId(), ruleId)) continue;
            // ⑦ 持续周期判定
            int count = incrTriggerCount(deviceData.getIotId(), ruleId);
            int duration = getRuleDuration(ruleId);
            if (count >= duration) {
                triggerAlert(ruleId, deviceData);
            }
        }
    }

    // ═══════════════════════════════════════════
    //  报警状态机
    // ═══════════════════════════════════════════

    private boolean canTrigger(String iotId, Long ruleId) {
        String stateKey = CacheConstants.IOT_ALERT_STATE_PREFIX + iotId + ":" + ruleId;
        String state = redisTemplate.opsForValue().get(stateKey);
        if (StrUtil.isEmpty(state)) return true;
        try {
            return AlertState.valueOf(state).canTrigger();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private void setAlertState(String iotId, Long ruleId, AlertState state) {
        String stateKey = CacheConstants.IOT_ALERT_STATE_PREFIX + iotId + ":" + ruleId;
        redisTemplate.opsForValue().set(stateKey, state.name());
    }

    /**
     * 若当前已处于报警状态且数据恢复正常，清除状态标记
     */
    private void recoverIfAlarming(String iotId, Long ruleId) {
        String stateKey = CacheConstants.IOT_ALERT_STATE_PREFIX + iotId + ":" + ruleId;
        String state = redisTemplate.opsForValue().get(stateKey);
        if (AlertState.ALARM.name().equals(state)) {
            redisTemplate.delete(stateKey);
            log.info("报警恢复 iotId={} ruleId={}", iotId, ruleId);
        }
    }

    // ═══════════════════════════════════════════
    //  规则判定辅助方法
    // ═══════════════════════════════════════════

    private boolean isInEffectivePeriod(Long ruleId) {
        String period = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "alertEffectivePeriod");
        if (StrUtil.isEmpty(period)) return true;
        try {
            String[] parts = period.split("~");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            LocalTime now = LocalTime.now();
            if (start.isBefore(end) || start.equals(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            return true;
        }
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
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int getRuleDuration(Long ruleId) {
        String dur = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "duration");
        return dur != null ? Integer.parseInt(dur) : 1;
    }

    // ═══════════════════════════════════════════
    //  Redis 计数/沉默/触发
    // ═══════════════════════════════════════════

    private boolean isInSilentPeriod(String iotId, Long ruleId) {
        String key = CacheConstants.ALERT_SILENT_PREFIX + iotId + ":" + ruleId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private int incrTriggerCount(String iotId, Long ruleId) {
        String key = CacheConstants.ALERT_TRIGGER_COUNT_PREFIX + iotId + ":" + ruleId;
        Long count = redisTemplate.opsForValue().increment(key);
        return count != null ? count.intValue() : 1;
    }

    private void clearTriggerCount(String iotId, Long ruleId) {
        String key = CacheConstants.ALERT_TRIGGER_COUNT_PREFIX + iotId + ":" + ruleId;
        redisTemplate.delete(key);
    }

    private void setSilentPeriod(String iotId, Long ruleId, int minutes) {
        String key = CacheConstants.ALERT_SILENT_PREFIX + iotId + ":" + ruleId;
        redisTemplate.opsForValue().set(key, "1", minutes, TimeUnit.MINUTES);
    }

    // ═══════════════════════════════════════════
    //  报警触发 — 委托给 AlertRuleServiceImpl
    // ═══════════════════════════════════════════

    private void triggerAlert(Long ruleId, DeviceData deviceData) {
        clearTriggerCount(deviceData.getIotId(), ruleId);

        // 获取规则中的沉默周期
        String silentStr = (String) redisTemplate.opsForHash()
                .get(CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId, "alertSilentPeriod");
        int silentMin = silentStr != null ? Integer.parseInt(silentStr) : 5;
        setSilentPeriod(deviceData.getIotId(), ruleId, silentMin);

        // 设置报警状态
        setAlertState(deviceData.getIotId(), ruleId, AlertState.ALARM);

        // 发布报警事件 — 由 AlertTriggerSubscriber 消费后调用 handleAlertTrigger
        Map<String, String> msg = new HashMap<>();
        msg.put("ruleId", String.valueOf(ruleId));
        msg.put("deviceData", JSONUtil.toJsonStr(deviceData));
        redisTemplate.convertAndSend("alert:trigger:channel", JSONUtil.toJsonStr(msg));

        log.info("报警触发 ruleId={} iotId={} functionId={} value={}",
                ruleId, deviceData.getIotId(), deviceData.getFunctionId(), deviceData.getDataValue());
    }
}
