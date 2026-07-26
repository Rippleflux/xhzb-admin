package com.xhzb.nursing.listener;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.service.IAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 报警触发订阅者 — 消费 alert:trigger:channel 消息
 * <p>
 * 架构解耦：AlertDetectionListener 检测到报警后 publish 到 Redis，
 * 由本订阅者消费后调用 AlertRuleServiceImpl.handleAlertTrigger()，
 * 完成通知人查找 + 报警入库 + WebSocket 推送。
 * <p>
 * 优势：高并发下 AlertDetectionListener 快速返回不阻塞；
 * 通知入库与 WebSocket 推送失败不影响检测循环。
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class AlertTriggerSubscriber implements MessageListener {

    @Autowired
    private IAlertRuleService alertRuleService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        log.debug("收到报警触发消息 channel={} body={}", channel, body);

        try {
            JSONObject msg = JSONUtil.parseObj(body);
            Long ruleId = msg.getLong("ruleId");
            DeviceData deviceData = JSONUtil.toBean(msg.getStr("deviceData"), DeviceData.class);

            alertRuleService.handleAlertTrigger(ruleId, deviceData);
        } catch (Exception e) {
            log.error("报警触发消息处理失败 body={}", body, e);
        }
    }
}
