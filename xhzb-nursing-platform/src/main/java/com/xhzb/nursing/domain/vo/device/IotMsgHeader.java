package com.xhzb.nursing.domain.vo.device;

import lombok.Data;

/**
 * amqp消息-header部分
 *
 * @author itcast
 **/
@Data
public class IotMsgHeader {
    /**
     * 物联网产品id
     */
    private String productId;

    /**
     * 物联网设备id
     */
    private String deviceId;

    /**
     * 设备标识码
     */
    private String nodeId;

    /**
     * appid
     */
    private String appId;

    /**
     * 网关id
     */
    private String gatewayId;
}
