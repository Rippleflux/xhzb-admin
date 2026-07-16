package com.xhzb.nursing.domain.dto.checkIn;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author itcast
 */
@Schema(description = "入住配置信息")
@Data
public class CheckInConfigDto {

    @Schema(title = "入住开始时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @Schema(title = "入住结束时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    @Schema(title = "费用开始时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feeStartDate;

    @Schema(title = "费用结束时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feeEndDate;

    @Schema(title = "护理等级ID")
    private Long nursingLevelId;

    @Schema(title = "护理等级名称")
    private String nursingLevelName;

    @Schema(title = "床位Id")
    private Long bedId;

    @Schema(title = "押金金额")
    private BigDecimal deposit;

    @Schema(title = "护理费用")
    private BigDecimal nursingFee;

    @Schema(title = "床位费用")
    private BigDecimal bedFee;

    @Schema(title = "其他费用")
    private BigDecimal otherFees;

    @Schema(title = "医保支付")
    private BigDecimal insurancePayment;

    @Schema(title = "政府补贴")
    private BigDecimal governmentSubsidy;

    @Schema(title = "房间ID")
    private Long roomId;

    @Schema(title = "楼层id")
    private Long floorId;

    @Schema(title = "楼层名称")
    private String floorName;

    @Schema(title = "房间编号")
    private String code;

}
