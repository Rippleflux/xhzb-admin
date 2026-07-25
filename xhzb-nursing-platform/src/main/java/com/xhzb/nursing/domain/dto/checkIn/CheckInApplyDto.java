package com.xhzb.nursing.domain.dto.checkIn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 申请入住请求模型
 *
 * @author itcast
 **/
@Schema(description = "申请入住请求模型")
@Data
public class CheckInApplyDto {

    /**
     * 健康评估ID
     */
    private Long healthAssessmentId;

    /**
     * 老人信息
     */
    @Schema(title = "老人信息")
    private CheckInElderDto checkInElderDto;

    /**
     * 家属信息
     */
    @Schema(title = "家属信息")
    private List<ElderFamilyDto> elderFamilyDtoList;

    /**
     * 入住配置
     */
    @Schema(title = "入住配置")
    private CheckInConfigDto checkInConfigDto;

    /**
     * 签约办理
     */
    @Schema(title = "签约办理")
    private CheckInContractDto checkInContractDto;
}
