package com.xhzb.nursing.domain.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "树形结构VO")
public class TreeVo {

    /**
     * 菜单名称
     */
    @Schema(title = "菜单名称")
    private String label;

    /**
     * 菜单ID
     */
    @Schema(title = "菜单ID")
    private String value;

    /**
     * 子菜单
     */
    @Schema(title = "子菜单")
    private List<TreeVo> children;
}