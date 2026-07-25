package com.xhzb.nursing.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description =  "设备注册参数")
public class DeviceDto {

    private Long id;

    /** 备注 */
    private String remark;

    /**
     * 设备标识码，通常使用IMEI、MAC地址或Serial No作为node_id
     */
    @Schema(title = "设备标识码")
    private String nodeId;

    @Schema(title = "设备id")
    public String iotId;

    @Schema(title = "产品的id")
    public String productKey;

    @Schema(title = "产品名称")
    private String productName;

    @Schema(title = "位置名称回显字段")
    private String deviceDescription;

    @Schema(title = "位置类型 0 老人 1位置")
    Integer locationType;

    @Schema(title = "绑定位置")
    Long bindingLocation;

    @Schema(title = "设备名称")
    String deviceName;

    @Schema(title = "物理位置类型 0楼层 1房间 2床位")
    Integer physicalLocationType;
}