package com.xhzb.nursing.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author itcast
 */
@Data
@Schema(description = "设备详情响应模型")
public class DeviceDetailVo {

    /**
     * 设备id
     */
    @Schema(title = "设备id")
    private Long id;

    /**
     * 物联网设备id
     */
    @Schema(title = "物联网设备id")
    private String iotId;

    /**
     * 设备名称
     */
    @Schema(title = "设备名称")
    private String deviceName;

    /**
     * 设备标识码
     */
    @Schema(title = "设备标识码")
    private String nodeId;

    /**
     * 设备秘钥
     */
    @Schema(title = "设备秘钥")
    private String secret;

    /**
     * 产品id
     */
    @Schema(title = "产品id")
    public String productKey;

    /**
     * 产品名称
     */
    @Schema(title = "产品名称")
    public String productName;

    /**
     * 位置类型 0 随身设备 1固定设备
     */
    @Schema(title = "位置类型 0 随身设备 1固定设备")
    private Integer locationType;

    /**
     * 绑定位置,如果是随身设备为老人id，如果是固定设备为位置的最后一级id
     */
    @Schema(title = "绑定位置,如果是随身设备为老人id，如果是固定设备为位置的最后一级id")
    private Long bindingLocation;


    /**
     * 接入位置
     */
    @Schema(title = "接入位置")
    private String remark;

    /**
     * 设备状态，ONLINE：设备在线，OFFLINE：设备离线，ABNORMAL：设备异常，INACTIVE：设备未激活，FROZEN：设备冻结
     */
    @Schema(title = "设备状态，ONLINE：设备在线，OFFLINE：设备离线，ABNORMAL：设备异常，INACTIVE：设备未激活，FROZEN：设备冻结")
    private String deviceStatus;

    /**
     * 激活时间
     */
    @Schema(title = "激活时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime activeTime;

    /**
     * 创建时间
     */
    @Schema(title = "创建时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 创建人id
     */
    @Schema(title = "创建人id")
    private Long createBy;

    /**
     * 创建人名称
     */
    @Schema(title = "创建人名称")
    private String creator;

    /** 位置备注 */
    @Schema(title = "位置备注")
    private String deviceDescription;
}