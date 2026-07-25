package com.xhzb.nursing.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * LoginVO
 * @author itheima
 */
@Data
@Schema(description = "登录对象")
@Builder
public class LoginVo {

    @Schema(title = "JWT token")
    private String token;

    @Schema(title = "昵称")
    private String nickName;
}