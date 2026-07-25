package com.xhzb.nursing.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.mapper.AlertRuleMapper;
import com.xhzb.nursing.service.IAlertDataService;
import com.xhzb.nursing.service.IAlertRuleService;
import com.xhzb.nursing.config.WebSocketServer;
import com.xhzb.nursing.domain.vo.AlertNotifyVo;
import com.xhzb.system.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 报警规则Service业务层处理
 *
 * @author rippleflux
 * @date 2026-07-23
 */
@Slf4j
@Service
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements IAlertRuleService {

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private IAlertDataService alertDataService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${alert.caregiverRole:nursing_elder}")
    private String caregiverRole;

    @Value("${alert.maintainerRole:administrator}")
    private String maintainerRole;

    @Value("${alert.managerRole:admin}")
    private String managerRole;

    @Override
    public AlertRule selectAlertRuleById(Long id) {
        return getById(id);
    }

    @Override
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule) {
        return alertRuleMapper.selectAlertRuleList(alertRule);
    }

    @Override
    public int insertAlertRule(AlertRule alertRule) {
        return save(alertRule) ? 1 : 0;
    }

    @Override
    public int updateAlertRule(AlertRule alertRule) {
        return updateById(alertRule) ? 1 : 0;
    }

    @Override
    public int deleteAlertRuleByIds(Long[] ids) {
        return removeByIds(java.util.Arrays.asList(ids)) ? 1 : 0;
    }

    @Override
    public int deleteAlertRuleById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    // ════════════════════════════════════════════════════════════════
    //  Phase 1: 加载规则
    // ════════════════════════════════════════════════════════════════

    /**
     * 报警过滤主入口
     */
    @Override
    public void alertFilter() {
        List<AlertRule> rules = loadRules();
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        List<DeviceData> deviceDatas = loadDeviceData();
        if (CollUtil.isEmpty(deviceDatas)) {
            return;
        }
        for (DeviceData deviceData : deviceDatas) {
            evaluateRule(rules, deviceData);
        }
    }

    /**
     * 加载所有启用的报警规则
     */
    private List<AlertRule> loadRules() {
        return alertRuleMapper.selectEnabledRules();
    }

    // ════════════════════════════════════════════════════════════════
    //  Phase 2: 加载设备数据
    // ════════════════════════════════════════════════════════════════

    /**
     * 从Redis拉取所有设备最新上报数据
     */
    private List<DeviceData> loadDeviceData() {
        Map<Object, Object> entries = redisTemplate.opsForHash()
                .entries(CacheConstants.IOT_DEVICE_LAST_DATA);
        if (CollUtil.isEmpty(entries)) {
            return new ArrayList<>();
        }
        List<DeviceData> result = new ArrayList<>();
        for (Object value : entries.values()) {
            String jsonStr = (String) value;
            if (StrUtil.isEmpty(jsonStr)) {
                continue;
            }
            try {
                result.addAll(JSONUtil.toList(jsonStr, DeviceData.class));
            } catch (Exception e) {
                log.warn("设备数据JSON解析失败: {}", jsonStr, e);
            }
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  Phase 3: 规则比对
    // ════════════════════════════════════════════════════════════════

    /**
     * 逐条数据匹配规则
     */
    private void evaluateRule(List<AlertRule> rules, DeviceData deviceData) {
        // 过滤：上报时间超过1分钟视为历史数据
        LocalDateTime alarmTime = deviceData.getAlarmTime();
        if (alarmTime != null
                && LocalDateTimeUtil.between(alarmTime, LocalDateTime.now(), ChronoUnit.SECONDS) > 60) {
            return;
        }

        // 找出所有匹配的规则
        List<AlertRule> matched = rules.stream()
                .filter(r -> ObjectUtil.equal(r.getProductKey(), deviceData.getProductKey())
                        && ObjectUtil.equal(r.getFunctionId(), deviceData.getFunctionId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(matched)) {
            return;
        }

        for (AlertRule rule : matched) {
            // ① 生效时间判断
            if (!isInEffectivePeriod(rule)) {
                clearTriggerCount(deviceData.getIotId(), rule.getId());
                continue;
            }

            // ② 阈值判断
            if (!isThresholdExceeded(rule, deviceData)) {
                clearTriggerCount(deviceData.getIotId(), rule.getId());
                continue;
            }

            // ③ 沉默周期检查
            if (isInSilentPeriod(deviceData.getIotId(), rule.getId())) {
                continue;
            }

            // ④⑤ 异常次数累计 + 持续周期判定
            int count = incrTriggerCount(deviceData.getIotId(), rule.getId());
            if (count >= rule.getDuration()) {
                // 触发报警
                clearTriggerCount(deviceData.getIotId(), rule.getId());
                setSilentPeriod(deviceData.getIotId(), rule.getId(), rule.getAlertSilentPeriod());
                resolveNotifier(rule, deviceData);
            }
        }
    }

    /**
     * 判断当前时间是否在规则的生效时段内
     * 格式: 00:00:00~23:59:59
     */
    private boolean isInEffectivePeriod(AlertRule rule) {
        String period = rule.getAlertEffectivePeriod();
        if (StrUtil.isEmpty(period)) {
            return true;
        }
        try {
            String[] parts = period.split("~");
            LocalTime start = LocalTime.parse(parts[0]);
            LocalTime end = LocalTime.parse(parts[1]);
            LocalTime now = LocalTime.now();
            if (start.isBefore(end) || start.equals(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                // 跨夜时段（如 22:00:00~06:00:00）
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            log.warn("解析生效时段失败: {} -> {}", rule.getId(), period, e);
            return true; // 解析失败默认全部时段生效
        }
    }

    /**
     * 判断设备数据是否达到规则阈值
     */
    private boolean isThresholdExceeded(AlertRule rule, DeviceData deviceData) {
        String dataValue = deviceData.getDataValue();
        if (StrUtil.isEmpty(dataValue)) {
            return false;
        }
        try {
            double val = Double.parseDouble(dataValue);
            double threshold = rule.getValue();
            String op = rule.getOperator();
            if (">".equals(op)) return val > threshold;
            if ("<".equals(op)) return val < threshold;
            if (">=".equals(op)) return val >= threshold;
            if ("<=".equals(op)) return val <= threshold;
            if ("==".equals(op)) return val == threshold;
            return false;
        } catch (NumberFormatException e) {
            log.debug("数据值无法转换为数字: {} -> {}", deviceData.getIotId(), dataValue);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Redis 辅助方法
    // ════════════════════════════════════════════════════════════════

    private boolean isInSilentPeriod(String iotId, Long ruleId) {
        String key = CacheConstants.ALERT_SILENT_PREFIX + iotId + ":" + ruleId;
        String val = redisTemplate.opsForValue().get(key);
        return StrUtil.isNotEmpty(val);
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

    private void setSilentPeriod(String iotId, Long ruleId, int silentMinutes) {
        String key = CacheConstants.ALERT_SILENT_PREFIX + iotId + ":" + ruleId;
        redisTemplate.opsForValue().set(key, "1", silentMinutes, TimeUnit.MINUTES);
    }

    // ════════════════════════════════════════════════════════════════
    //  Phase 4: 通知人查找
    // ════════════════════════════════════════════════════════════════

    /**
     * 根据设备位置类型查找通知人并保存报警数据
     */
    private void resolveNotifier(AlertRule rule, DeviceData deviceData) {
        List<Long> userIds = new ArrayList<>();

        if (rule.getAlertDataType() != null && rule.getAlertDataType() == 0) {
            // 老人异常数据
            Integer locationType = deviceData.getLocationType();
            Integer phyLocationType = deviceData.getPhysicalLocationType();

            if (locationType != null && locationType == 0) {
                // 随身设备：accessLocation = elderId
                String elderId = deviceData.getAccessLocation();
                if (StrUtil.isNotBlank(elderId)) {
                    try {
                        userIds.addAll(alertRuleMapper.selectNursingIdByElderId(Long.valueOf(elderId)));
                    } catch (Exception e) {
                        log.warn("随身设备查护理员失败 elderId={}", elderId, e);
                    }
                }
            } else if (locationType != null && locationType == 1
                    && phyLocationType != null && phyLocationType == 2) {
                // 床上设备：accessLocation = bedId
                String bedId = deviceData.getAccessLocation();
                if (StrUtil.isNotBlank(bedId)) {
                    try {
                        userIds.addAll(alertRuleMapper.selectNursingIdByBedId(Long.valueOf(bedId)));
                    } catch (Exception e) {
                        log.warn("床上设备查护理员失败 bedId={}", bedId, e);
                    }
                }
            }
            // 房间设备(physical_location_type=1) 无老人绑定，不查护理员
        } else {
            // 设备异常数据：通知维修人员
            // TODO: 后续细化维修人员角色，当前暂用 administrator(行政)
            try {
                userIds.addAll(userRoleMapper.selectUserIdByRoleKey(maintainerRole));
            } catch (Exception e) {
                log.warn("查维修人员失败 roleKey={}", maintainerRole, e);
            }
        }

        // 所有报警追加超级管理员
        try {
            userIds.addAll(userRoleMapper.selectUserIdByRoleKey(managerRole));
        } catch (Exception e) {
            log.warn("查超管失败 roleKey={}", managerRole, e);
        }

        // 去重
        userIds = userIds.stream().distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(userIds)) {
            log.warn("报警通知人列表为空 ruleId={} iotId={}", rule.getId(), deviceData.getIotId());
            return;
        }

        saveAlert(rule, deviceData, userIds);
    }

    // ════════════════════════════════════════════════════════════════
    //  Phase 5: 入库
    // ════════════════════════════════════════════════════════════════

    /**
     * 批量保存报警数据，每个通知人一条
     */
    private void saveAlert(AlertRule rule, DeviceData deviceData, List<Long> userIds) {
        List<AlertData> alertList = new ArrayList<>();
        for (Long userId : userIds) {
            AlertData alertData = new AlertData();
            alertData.setIotId(deviceData.getIotId());
            alertData.setDeviceName(deviceData.getDeviceName());
            alertData.setProductKey(deviceData.getProductKey());
            alertData.setProductName(deviceData.getProductName());
            alertData.setFunctionId(deviceData.getFunctionId());
            alertData.setAccessLocation(deviceData.getAccessLocation());
            alertData.setLocationType(deviceData.getLocationType());
            alertData.setPhysicalLocationType(deviceData.getPhysicalLocationType());
            alertData.setDeviceDescription(deviceData.getDeviceDescription());
            alertData.setDataValue(deviceData.getDataValue());
            alertData.setAlertRuleId(rule.getId().intValue());
            alertData.setType(rule.getAlertDataType());
            alertData.setStatus(0); // 待处理
            alertData.setUserId(userId);

            // 报警原因：功能名称+运算符+阈值,持续N个周期就报警
            String reason = String.format("%s%s%s,持续%d个周期就报警",
                    rule.getFunctionName(),
                    rule.getOperator(),
                    rule.getValue(),
                    rule.getDuration());
            alertData.setAlertReason(reason);

            alertList.add(alertData);
        }
        alertDataService.saveBatch(alertList);
        log.info("报警数据已保存 ruleId={} iotId={} 通知人数={}", rule.getId(), deviceData.getIotId(), userIds.size());

        // 构建WebSocket推送消息
        AlertNotifyVo notifyVo = AlertNotifyVo.builder()
                .id(alertList.get(0).getId())
                .accessLocation(deviceData.getAccessLocation())
                .locationType(deviceData.getLocationType())
                .physicalLocationType(deviceData.getPhysicalLocationType())
                .deviceDescription(deviceData.getDeviceDescription())
                .productName(rule.getProductName())
                .functionName(rule.getFunctionName())
                .dataValue(deviceData.getDataValue())
                .alertDataType(rule.getAlertDataType())
                .voiceNotifyStatus(1)
                .notifyType(1)
                .isAllConsumer(false)
                .build();
        webSocketServer.sendMessageToConsumer(notifyVo, userIds);
    }
}
