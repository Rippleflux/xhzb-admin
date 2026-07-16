package com.xhzb.nursing.domain.vo.checkIn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 老人家属信息
 *
 * @author itcast
 * @create 2023/12/18 20:11
 **/
@Schema(description = "老人家属信息")
@Data
public class ElderFamilyVo {

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
