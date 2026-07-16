package com.xhzb.nursing.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.nursing.domain.AiConversation;
import com.xhzb.nursing.domain.AiMessage;
import com.xhzb.nursing.enums.MessageType;
import com.xhzb.nursing.mapper.AiConversationMapper;
import com.xhzb.nursing.service.IAiConversationService;
import com.xhzb.nursing.service.IAiMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI会话历史Service业务层处理
 */
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation> implements IAiConversationService {

    @Autowired
    private AiConversationMapper aiConversationMapper;

    @Autowired
    private IAiMessageService aiMessageService;

    /**
     * 根据用户ID查询会话列表
     *
     * @param createBy 创建人ID
     * @return 会话列表
     */
    @Override
    public List<AiConversation> selectConversationListByUserId(Long createBy) {
        return aiConversationMapper.selectConversationListByUserId(createBy);
    }

    /**
     * 根据会话ID查询会话信息
     *
     * @param id 会话ID
     * @return 会话信息
     */
    @Override
    public AiConversation selectConversationById(Long id) {
        return getById(id);
    }

    /**
     * 新增会话
     *
     * @param conversation 会话信息
     * @return 结果
     */
    @Override
    public int insertConversation(AiConversation conversation) {
        return save(conversation) ? 1 : 0;
    }

    /**
     * 删除会话
     *
     * @param id 会话ID
     * @return 结果
     */
    @Override
    public int deleteConversationById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 保存会话记录（新对话创建会话历史 + 保存用户消息，已有对话保存用户消息）
     * 通过查询数据库判断是否为新会话，而非简单判空
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String saveChatRecord(String prompt, String chatId, Long userId) {
        // 判断是否为新会话：chatId为空，或数据库中不存在该会话记录
        boolean isNewConversation = StrUtil.isBlank(chatId) || getById(Long.parseLong(chatId)) == null;

        if (isNewConversation) {
            // 新对话：创建会话历史记录
            AiConversation conversation = new AiConversation();
            String name = prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt;
            conversation.setName(name);
            conversation.setCreateBy(userId);
            save(conversation);
            chatId = conversation.getId().toString();
        }

        // 将用户消息存入会话详情表
        AiMessage message = new AiMessage();
        message.setConversationId(Long.parseLong(chatId));
        message.setType(MessageType.USER.getValue());
        message.setContent(prompt);
        message.setCreateBy(userId);
        aiMessageService.insertMessage(message);

        return chatId;
    }

    /**
     * 获取或创建会话ID
     * 新会话时创建会话历史记录并返回ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String getOrCreateConversationId(String chatId, String prompt, Long userId, boolean isNewConversation) {
        if (isNewConversation) {
            AiConversation conversation = new AiConversation();
            String name = prompt.length() > 20 ? prompt.substring(0, 20) + "..." : prompt;
            conversation.setName(name);
            conversation.setCreateBy(userId);
            save(conversation);
            return conversation.getId().toString();
        }
        return chatId;
    }

    /**
     * 保存用户消息和AI响应到会话
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveMessages(String chatId, String prompt, String aiResponse, Long userId) {
        Long conversationId = Long.parseLong(chatId);

        // 保存用户消息
        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setType(MessageType.USER.getValue());
        userMessage.setContent(prompt);
        userMessage.setCreateBy(userId);
        aiMessageService.insertMessage(userMessage);

        // 保存AI响应
        AiMessage aiMessage = new AiMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setType(MessageType.ASSISTANT.getValue());
        aiMessage.setContent(aiResponse);
        aiMessage.setCreateBy(userId);
        aiMessageService.insertMessage(aiMessage);
    }
}
