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
import org.springframework.transaction.annotation.Transactional;

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
    //  规则索引构建 + 事件驱动报警处理
    // ════════════════════════════════════════════════════════════════

    /**
     * 报警过滤入口（保留接口兼容，实际由 AlertDetectionListener 事件驱动）
     * 该方法仍可被 Quartz 定时调用作为补偿检测
     */
    @Override
    public void alertFilter() {
        // 已迁移至 AlertDetectionListener 事件驱动检测
        // 保留此方法供 Quartz 补偿检测使用（P5）
        log.debug("alertFilter called — 实时检测已由 AlertDetectionListener 接管");
    }

    /**
     * 构建规则索引 — 规则变更时调用
     * 将启用规则加载到 Redis：规则缓存 Hash + 规则索引 Set
     */
    @Override
    public void buildRuleIndex() {
        List<AlertRule> rules = alertRuleMapper.selectEnabledRules();
        if (CollUtil.isEmpty(rules)) return;

        // 清除旧索引
        redisTemplate.delete(CacheConstants.IOT_RULE_INDEX_PREFIX + "*");

        for (AlertRule rule : rules) {
            String ruleId = String.valueOf(rule.getId());

            // ① 规则详情缓存 (Hash)
            String cacheKey = CacheConstants.IOT_RULE_CACHE_PREFIX + ruleId;
            redisTemplate.opsForHash().put(cacheKey, "operator", nvl(rule.getOperator()));
            redisTemplate.opsForHash().put(cacheKey, "value", String.valueOf(rule.getValue()));
            redisTemplate.opsForHash().put(cacheKey, "duration", String.valueOf(rule.getDuration()));
            redisTemplate.opsForHash().put(cacheKey, "alertSilentPeriod", String.valueOf(rule.getAlertSilentPeriod()));
            redisTemplate.opsForHash().put(cacheKey, "alertEffectivePeriod", nvl(rule.getAlertEffectivePeriod()));
            redisTemplate.opsForHash().put(cacheKey, "alertDataType", String.valueOf(rule.getAlertDataType()));
            redisTemplate.opsForHash().put(cacheKey, "productKey", nvl(rule.getProductKey()));
            redisTemplate.opsForHash().put(cacheKey, "productName", nvl(rule.getProductName()));
            redisTemplate.opsForHash().put(cacheKey, "functionName", nvl(rule.getFunctionName()));

            // ② 规则索引 (Set): functionId → ruleIds
            if (StrUtil.isNotBlank(rule.getFunctionId())) {
                redisTemplate.opsForSet().add(
                        CacheConstants.IOT_RULE_INDEX_PREFIX + rule.getFunctionId(), ruleId);
            }
        }
        log.info("规则索引构建完成, 启用规则数={}", rules.size());
    }

    /**
     * 处理报警触发 — 由 AlertDetectionListener 通过 Pub/Sub 调用
     * 负责通知人查找 + 报警入库 + WebSocket 推送
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlertTrigger(Long ruleId, DeviceData deviceData) {
        AlertRule rule = getById(ruleId);
        if (rule == null) {
            log.warn("规则不存在 ruleId={}", ruleId);
            return;
        }
        resolveNotifier(rule, deviceData);
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
     * 保存报警数据 — 合并为一条，推送用户ID列表记录到 remark
     */
    private void saveAlert(AlertRule rule, DeviceData deviceData, List<Long> userIds) {
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
        alertData.setUserId(userIds.get(0)); // 主通知人

        // 报警原因
        String reason = String.format("%s%s%s,持续%d个周期就报警",
                rule.getFunctionName(),
                rule.getOperator(),
                rule.getValue(),
                rule.getDuration());
        alertData.setAlertReason(reason);

        // 推送用户ID列表记入 remark
        String userIdsStr = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        alertData.setRemark("推送用户ID: " + userIdsStr);

        alertDataService.save(alertData);
        log.info("报警数据已保存 ruleId={} iotId={} 通知人数={}", rule.getId(), deviceData.getIotId(), userIds.size());

        // 构建WebSocket推送消息
        AlertNotifyVo notifyVo = AlertNotifyVo.builder()
                .id(alertData.getId())
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
                .userIds(userIdsStr)
                .build();
        webSocketServer.sendMessageToConsumer(notifyVo, userIds);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
