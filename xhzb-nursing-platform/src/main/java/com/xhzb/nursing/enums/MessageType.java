package com.xhzb.nursing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {

    /**
     * 系统消息：设定模型的角色定位、行为准则或特定任务指令
     */
    SYSTEM("system", "系统消息"),

    /**
     * 用户消息：用户向模型提出的问题、指令或输入内容
     */
    USER("user", "用户消息"),

    /**
     * 助手消息：模型的回复内容，在多轮对话中传入以维持上下文
     */
    ASSISTANT("assistant", "助手消息");

    /**
     * 消息类型值
     */
    private final String value;

    /**
     * 消息类型描述
     */
    private final String description;
}
