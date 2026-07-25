package com.xhzb.nursing.domain.dto.checkIn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 老人入住请求模型
 *
 * @author itcast
 **/
@Schema(description = "老人入住请求模型")
@Data
public class CheckInElderDto {
    /**
     * 姓名
     */
    @Schema(title = "姓名")
    private String name;

    /**
     * 身份证号
     */
    @Schema(title = "身份证号")
    private String idCardNo;

    /**
     * 出生日期，格式：yyyy-MM-dd
     */
    @Schema(title = "出生日期，格式：yyyy-MM-dd")
    private String birthday;

    /**
     * 年龄
     */
    @Schema(title = "年龄")
    private Integer age;

    /**
     * 性别，0：男，1：女，2：未知
     */
    @Schema(title = "性别，0：男，1：女，2：未知")
    private Integer sex;

    /**
     * 手机号
     */
    @Schema(title = "手机号")
    private String phone;

    /**
     * 民族
     */
    @Schema(title = "民族")
    private String nation;

    /**
     * 文化程度
     */
    @Schema(title = "文化程度")
    private String educationLevel;

    /**
     * 社保卡号
     */
    @Schema(title = "社保卡号")
    private String socialSecurityCard;

    /**
     * 居住情况
     */
    @Schema(title = "居住情况")
    private String livingSituation;

    /**
     * 宗教信仰
     */
    @Schema(title = "宗教信仰")
    private String religiousBelief;

    /**
     * 经济来源
     */
    @Schema(title = "经济来源")
    private String economicSource;

    /**
     * 婚姻状况
     */
    @Schema(title = "婚姻状况")
    private String maritalStatus;

    /**
     * 医疗费用支付方式
     */
    @Schema(title = "医疗费用支付方式")
    private String medicalPaymentMethod;

    /**
     *  核心建议
     */
    @Schema(title = "核心建议")
    private String coreSuggestion;

    /**
     * 家庭住址
     */
    @Schema(title = "家庭住址")
    private String address;

    /**
     * 一寸照片
     */
    @Schema(title = "一寸照片")
    private String image;

    /**
     * 身份证国徽面
     */
    @Schema(title = "身份证国徽面")
    private String idCardNationalEmblemImg;

    /**
     * 身份证人像面
     */
    @Schema(title = "身份证人像面")
    private String idCardPortraitImg;

}
