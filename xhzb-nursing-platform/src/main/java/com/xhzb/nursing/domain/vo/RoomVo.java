package com.xhzb.nursing.domain.vo;

import com.xhzb.nursing.domain.vo.DeviceInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "房间VO")
public class RoomVo {

    @Schema(title = "房间ID")
    private Long id;

    @Schema(title = "楼层名称")
    private String floorName;

    @Schema(title = "楼层ID")
    private String floorId;

    @Schema(title = "房间ID")
    private String roomId;

    @Schema(title = "房间编号")
    private String code;

    @Schema(title = "房间价格")
    private String price;

    @Schema(title = "床位列表")
    private List<BedVo> bedVoList;

    @Schema(title = "关联的设备")
    private List<DeviceInfo> deviceVos;

}