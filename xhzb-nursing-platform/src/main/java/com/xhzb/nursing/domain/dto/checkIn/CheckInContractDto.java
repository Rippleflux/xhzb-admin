
package com.xhzb.nursing.domain.dto.checkIn;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author itcast
 */
@Schema(description = "合同信息")
@Data
public class CheckInContractDto {
    /**
     * 合同名称
     */
    @Schema(title = "合同名称")
    private String contractName;

    /**
     * 签约时间
     */
    @Schema(title = "签约时间,格式：yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signDate;

    /**
     * 丙方名称
     */
    @Schema(title = "丙方名称")
    private String thirdPartyName;

    /**
     * 丙方手机号
     */
    @Schema(title = "丙方手机号")
    private String thirdPartyPhone;

    /**
     * 合同pdf文件地址
     */
    @Schema(title = "合同pdf文件地址")
    private String agreementPath;

}

