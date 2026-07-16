package com.xhzb.nursing.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI会话历史表
 */
@Data
@TableName("ai_conversation")
@Schema(description = "AI会话历史记录")
public class AiConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(title = "会话ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "会话名称")
    private String name;

    @Schema(title = "创建人ID")
    private Long createBy;

    @Schema(title = "更新人ID")
    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "创建时间")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "更新时间")
    private Date updateTime;
}
