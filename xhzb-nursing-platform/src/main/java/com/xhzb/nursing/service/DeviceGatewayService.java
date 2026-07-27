package com.xhzb.nursing.service;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.message.DeviceDataMessage;
import com.xhzb.nursing.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 设备数据网关 — DeviceData → Kafka Message → device-data-topic
 * <p>
 * 2.1: 从 xhzb-kafka 模块迁入 xhzb-nursing-platform，解决循环依赖。
 * xhzb-kafka 模块现在仅包含纯 Kafka 消费者和配置。
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Service
public class DeviceGatewayService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "device-data-topic";

    /**
     * 将设备数据列表转换为 Kafka 消息并发送
     */
    public void send(List<DeviceData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        List<DeviceDataMessage> messages = dataList.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());

        for (DeviceDataMessage msg : messages) {
            try {
                kafkaTemplate.send(TOPIC, msg.getDeviceId(), JSONUtil.toJsonStr(msg));
            } catch (Exception e) {
                log.error("Kafka发送失败 deviceId={}", msg.getDeviceId(), e);
            }
        }
        log.debug("Kafka发送完成, count={}", messages.size());
    }

    private DeviceDataMessage toMessage(DeviceData d) {
        return DeviceDataMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .deviceId(d.getIotId())
                .productKey(d.getProductKey())
                .functionId(d.getFunctionId())
                .dataValue(d.getDataValue())
                .eventTime(d.getAlarmTime() != null
                        ? d.getAlarmTime().atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
                        : System.currentTimeMillis())
                .receiveTime(System.currentTimeMillis())
                .deviceName(d.getDeviceName())
                .productName(d.getProductName())
                .locationType(d.getLocationType())
                .physicalLocationType(d.getPhysicalLocationType())
                .accessLocation(d.getAccessLocation())
                .deviceDescription(d.getDeviceDescription())
                .build();
    }
}
