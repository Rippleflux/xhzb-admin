package com.xhzb.nursing.listener;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.service.IAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 报警通知消费者 — 消费 alert-topic 调用 handleAlertTrigger
 * <p>
 * 替代 2.0 版本的 AlertTriggerSubscriber（Redis Pub/Sub），
 * 改为 @KafkaListener 消费 alert-topic。
 * <p>
 * 负责通知人查找 + 报警入库 + WebSocket 推送。
 *
 * @author rippleflux
 * @date 2026-07-27
 */
@Slf4j
@Component
public class AlertNotifier {

    @Autowired
    private IAlertRuleService alertRuleService;

    @KafkaListener(topics = "alert-topic", groupId = "xhzb-alert-notifier")
    public void onAlert(String message) {
        try {
            JSONObject msg = JSONUtil.parseObj(message);
            Long ruleId = msg.getLong("ruleId");
            DeviceData deviceData = JSONUtil.toBean(msg.getStr("deviceData"), DeviceData.class);

            alertRuleService.handleAlertTrigger(ruleId, deviceData);
        } catch (Exception e) {
            log.error("AlertNotifier 处理失败, 将由 Kafka 重试", e);
            throw e;
        }
    }
}
