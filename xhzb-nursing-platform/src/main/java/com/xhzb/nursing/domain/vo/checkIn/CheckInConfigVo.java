package com.xhzb.nursing.domain.vo.checkIn;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xhzb.common.annotation.Excel;
import com.xhzb.nursing.domain.CheckInConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "入住配置响应信息")
@Data
public class CheckInConfigVo extends CheckInConfig {

    /**
     * 入住开始时间
     */
    @Schema(title = "入住开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "入住开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime startDate;

    /**
     * 入住结束时间
     */
    @Schema(title = "入住结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "入住结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private LocalDateTime endDate;

    /**
     * 入住床位
     */
    @Schema(title = "入住床位")
    @Excel(name = "入住床位")
    private String bedNumber;
}
