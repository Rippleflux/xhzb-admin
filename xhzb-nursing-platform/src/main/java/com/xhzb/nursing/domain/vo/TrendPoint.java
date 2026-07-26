package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备历史趋势数据点
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "趋势数据点")
public class TrendPoint {

    @Schema(description = "时间")
    private String time;

    @Schema(description = "数值")
    private Double value;
}
