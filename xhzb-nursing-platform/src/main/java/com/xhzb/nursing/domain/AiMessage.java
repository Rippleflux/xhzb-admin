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
 * AI对话详情表
 */
@Data
@TableName("ai_message")
@Schema(description = "AI对话详情记录")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(title = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "会话ID")
    private Long conversationId;

    @Schema(title = "消息类型（user:用户消息, assistant:AI响应）")
    private String type;

    @Schema(title = "对话内容(JSON)")
    private String content;

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
