package com.xhzb.common.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka 设备数据消息体（跨模块共享）
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDataMessage {

    /** 消息唯一ID（幂等） */
    private String messageId;

    /** 设备ID */
    private String deviceId;

    /** 产品Key */
    private String productKey;

    /** 物模型标识 */
    private String functionId;

    /** 传感器数值 */
    private String dataValue;

    /** 设备上报时间 (epoch ms) */
    private Long eventTime;

    /** 服务端接收时间 (epoch ms) */
    private Long receiveTime;

    // 扩展字段
    private String deviceName;
    private String productName;
    private Integer locationType;
    private Integer physicalLocationType;
    private String accessLocation;
    private String deviceDescription;
}
