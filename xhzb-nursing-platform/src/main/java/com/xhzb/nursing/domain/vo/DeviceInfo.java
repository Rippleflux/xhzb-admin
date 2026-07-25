package com.xhzb.nursing.domain.vo;

import com.xhzb.nursing.domain.DeviceData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 设备信息响应模型
 *
 * @author rippleflux
 * @date 2025-07-23
 */
@Data
@Schema(description = "设备信息响应模型")
public class DeviceInfo {

    @Schema(title = "主键")
    private Long id;

    @Schema(title = "物联网设备ID")
    private String iotId;

    @Schema(title = "设备名称")
    private String deviceName;

    @Schema(title = "产品key")
    private String productKey;

    @Schema(title = "产品名称")
    private String productName;

    @Schema(title = "设备数据")
    private List<DeviceData> deviceDataVos;
}
