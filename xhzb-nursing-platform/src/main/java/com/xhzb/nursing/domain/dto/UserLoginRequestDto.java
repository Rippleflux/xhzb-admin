package com.xhzb.nursing.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * C端用户登录
 */
@Data
public class UserLoginRequestDto {

    @Schema(title = "昵称")
    private String nickName;

    @Schema(title = "登录临时凭证")
    private String code;

    @Schema(title = "手机号临时凭证")
    private String phoneCode;
}