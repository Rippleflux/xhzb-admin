package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.nursing.domain.AiMessage;
import com.xhzb.nursing.mapper.AiMessageMapper;
import com.xhzb.nursing.service.IAiMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI对话详情Service业务层处理
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements IAiMessageService {

    @Autowired
    private AiMessageMapper aiMessageMapper;

    /**
     * 根据会话ID查询消息列表
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @Override
    public List<AiMessage> selectMessagesByConversationId(Long conversationId) {
        return aiMessageMapper.selectMessagesByConversationId(conversationId);
    }

    /**
     * 新增消息
     *
     * @param message 消息信息
     * @return 结果
     */
    @Override
    public int insertMessage(AiMessage message) {
        return save(message) ? 1 : 0;
    }

    /**
     * 根据会话ID删除消息
     *
     * @param conversationId 会话ID
     * @return 结果
     */
    @Override
    public int deleteByConversationId(Long conversationId) {
        return aiMessageMapper.deleteByConversationId(conversationId);
    }
}
