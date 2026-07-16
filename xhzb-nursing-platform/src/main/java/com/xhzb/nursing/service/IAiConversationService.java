package com.xhzb.nursing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.AiConversation;
import com.xhzb.nursing.domain.AiMessage;

import java.util.List;

/**
 * AI会话历史Service接口
 */
public interface IAiConversationService extends IService<AiConversation> {

    /**
     * 根据用户ID查询会话列表
     *
     * @param createBy 创建人ID
     * @return 会话列表
     */
    List<AiConversation> selectConversationListByUserId(Long createBy);

    /**
     * 根据会话ID查询会话信息
     *
     * @param id 会话ID
     * @return 会话信息
     */
    AiConversation selectConversationById(Long id);

    /**
     * 新增会话
     *
     * @param conversation 会话信息
     * @return 结果
     */
    int insertConversation(AiConversation conversation);

    /**
     * 删除会话
     *
     * @param id 会话ID
     * @return 结果
     */
    int deleteConversationById(Long id);

    /**
     * 保存会话记录（新对话创建会话历史 + 保存用户消息，已有对话保存用户消息）
     * 事务方法，保证AiConversation和AiMessage同时入库
     *
     * @param prompt  用户输入
     * @param chatId  会话ID（为空表示新对话）
     * @param userId  当前用户ID
     * @return 最终的chatId
     */
    String saveChatRecord(String prompt, String chatId, Long userId);

    /**
     * 获取或创建会话ID
     * 新会话时创建会话历史记录并返回ID，已有会话直接返回chatId
     *
     * @param chatId          会话ID（为空则创建新会话）
     * @param prompt          用户输入的prompt（用于新会话命名）
     * @param userId          用户ID
     * @param isNewConversation 是否为新会话
     * @return 会话ID
     */
    String getOrCreateConversationId(String chatId, String prompt, Long userId, boolean isNewConversation);

    /**
     * 保存用户消息和AI响应到会话
     *
     * @param chatId     会话ID
     * @param prompt     用户输入的prompt
     * @param aiResponse AI模型的响应内容
     * @param userId     用户ID
     */
    void saveMessages(String chatId, String prompt, String aiResponse, Long userId);
}
