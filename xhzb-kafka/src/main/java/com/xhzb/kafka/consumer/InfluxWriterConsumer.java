package com.xhzb.kafka.consumer;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.message.DeviceDataMessage;
import com.xhzb.nursing.service.IInfluxDBService;
import com.xhzb.nursing.domain.DeviceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

/**
 * InfluxDB 写入消费者 — 消费 device-data-topic → 写入 InfluxDB
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class InfluxWriterConsumer {

    @Autowired
    private IInfluxDBService influxDBService;

    @KafkaListener(topics = "device-data-topic", groupId = "xhzb-influx-writer")
    public void consume(String message) {
        DeviceDataMessage msg = JSONUtil.toBean(message, DeviceDataMessage.class);
        DeviceData data = toDeviceData(msg);
        influxDBService.writeDeviceData(Collections.singletonList(data));
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
            d.setAlarmTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(msg.getEventTime()), ZoneId.of("Asia/Shanghai")));
        }
        return d;
    }
}
