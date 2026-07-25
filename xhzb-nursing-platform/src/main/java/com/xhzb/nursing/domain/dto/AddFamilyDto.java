package com.xhzb.nursing.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增家人Dto")
public class AddFamilyDto {

    /**
     * 家人姓名
     */
    @Schema(title = "家人姓名")
    private String name;

    /**
     * 身份证号
     */
    @Schema(title = "身份证号")
    private String idCard;

    /**
     * 称呼、备注
     */
    @Schema(title = "称呼、备注")
    private String remark;
}
