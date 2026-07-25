package com.xhzb.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.AiMessage;

import java.util.List;

/**
 * AI对话详情Service接口
 */
public interface IAiMessageService extends IService<AiMessage> {

    /**
     * 根据会话ID查询消息列表
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<AiMessage> selectMessagesByConversationId(Long conversationId);

    /**
     * 新增消息
     *
     * @param message 消息信息
     * @return 结果
     */
    int insertMessage(AiMessage message);

    /**
     * 根据会话ID删除消息
     *
     * @param conversationId 会话ID
     * @return 结果
     */
    int deleteByConversationId(Long conversationId);
}
