package com.xhzb.nursing.domain.dto.checkIn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 老人家属信息
 *
 * @author itcast
 **/
@Schema(description = "老人家属信息")
@Data
public class ElderFamilyDto {

    /**
     * 姓名
     */
    @Schema(title = "姓名")
    private String name;


    /**
     * 联系方式
     */
    @Schema(title = "联系方式")
    private String phone;

    /**
     * 亲属关系
     */
    @Schema(title = "亲属关系")
    private String kinship;

}
