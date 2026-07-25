package com.xhzb.nursing.controller;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.core.domain.R;
import com.xhzb.common.core.redis.RedisCache;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.AiConversation;
import com.xhzb.nursing.domain.AiMessage;
import com.xhzb.nursing.service.IAiConversationService;
import com.xhzb.nursing.service.IAiMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI大模型交互控制器
 */
@RestController
@RequestMapping(path = "/ai")
@Tag(name = "AI对话相关接口")
public class ChatController {

    /** 会话列表Redis缓存key前缀 */
    private static final String CONVERSATIONS_CACHE_PREFIX = "ai:conversations:user:";

    /** 会话详情Redis缓存key前缀 */
    private static final String CONVERSATION_CACHE_PREFIX = "ai:conversation:messages:";

    /** 缓存过期时间（小时） */
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private IAiConversationService aiConversationService;

    @Autowired
    private IAiMessageService aiMessageService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ChatMemory chatMemory;

    /**
     * AI对话接口（同步返回）
     * 流程：判断是否新会话 -> 创建会话获取ID -> 调用AI -> 保存消息
     */
    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public R<Map<String, Object>> chat(String prompt, String chatId) {
        Long userId = SecurityUtils.getUserId();

        // 判断是否为新会话：chatId为空，或数据库中不存在该会话
        boolean isNewConversation = StrUtil.isBlank(chatId)
                || aiConversationService.selectConversationById(Long.parseLong(chatId)) == null;

        // 1. 获取或创建会话ID（新会话时先创建，确保ChatMemory使用真实ID）
        String finalChatId = aiConversationService.getOrCreateConversationId(chatId, prompt, userId, isNewConversation);

        // 2. 如果是已有会话，检查ChatMemory是否为空，为空则从数据库加载历史消息
        if (!isNewConversation) {
            List<Message> memoryMessages = chatMemory.get(finalChatId);
            if (memoryMessages.isEmpty()) {
                // 从数据库加载历史消息并添加到ChatMemory
                List<AiMessage> dbMessages = aiMessageService.selectMessagesByConversationId(Long.parseLong(finalChatId));
                List<Message> historyMessages = dbMessages.stream()
                        .map(msg -> {
                            if ("user".equals(msg.getType())) {
                                return (Message) new UserMessage(msg.getContent());
                            } else {
                                return (Message) new AssistantMessage(msg.getContent());
                            }
                        })
                        .toList();
                if (!historyMessages.isEmpty()) {
                    chatMemory.add(finalChatId, historyMessages);
                }
            }
        }

        // 3. 将当前用户消息添加到ChatMemory
        chatMemory.add(finalChatId, List.of(new UserMessage(prompt)));

        // 4. 调用AI模型，使用真实会话ID
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalChatId))
                .call()
                .content();

        // 5. 将AI响应添加到ChatMemory
        chatMemory.add(finalChatId, List.of(new AssistantMessage(aiResponse)));

        // 6. 保存用户消息和AI响应到数据库
        aiConversationService.saveMessages(finalChatId, prompt, aiResponse, userId);

        // 4. 更新Redis缓存：清除会话列表缓存，下次查询时重新加载
        redisCache.deleteObject(CONVERSATIONS_CACHE_PREFIX + userId);
        redisCache.deleteObject(CONVERSATION_CACHE_PREFIX + finalChatId);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("chatId", finalChatId);
        result.put("content", aiResponse);
        return R.ok(result);
    }

    /**
     * 获取会话详情（消息列表）
     * 优先从Redis缓存获取，未命中则查询数据库并写入缓存
     */
    @GetMapping("/history/{conversationId}")
    @Operation(summary = "获取会话详情")
    public R<List<AiMessage>> getConversationDetail(@PathVariable Long conversationId) {
        String cacheKey = CONVERSATION_CACHE_PREFIX + conversationId;

        // 先查Redis缓存
        @SuppressWarnings("unchecked")
        List<AiMessage> cachedMessages = redisCache.getCacheObject(cacheKey);
        if (cachedMessages != null) {
            return R.ok(cachedMessages);
        }

        // 缓存未命中，查询数据库
        AiConversation conversation = aiConversationService.selectConversationById(conversationId);
        if (conversation == null) {
            return R.fail("会话不存在");
        }

        // 查询消息列表（包含type字段）
        List<AiMessage> messages = aiMessageService.selectMessagesByConversationId(conversationId);

        // 写入Redis缓存
        redisCache.setCacheObject(cacheKey, messages, (int) (CACHE_EXPIRE_HOURS * 3600), TimeUnit.SECONDS);

        return R.ok(messages);
    }

    /**
     * 获取当前用户的历史会话列表（id + name）
     * 优先从Redis缓存获取，未命中则查询数据库并写入缓存
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史会话列表")
    public R<List<Map<String, Object>>> getConversationList() {
        Long userId = SecurityUtils.getUserId();
        String cacheKey = CONVERSATIONS_CACHE_PREFIX + userId;

        // 先查Redis缓存
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedList = redisCache.getCacheObject(cacheKey);
        if (cachedList != null) {
            return R.ok(cachedList);
        }

        // 缓存未命中，查询数据库，返回id和name
        List<Map<String, Object>> list = aiConversationService.selectConversationListByUserId(userId)
                .stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    return map;
                })
                .toList();

        // 写入Redis缓存
        redisCache.setCacheObject(cacheKey, list, (int) (CACHE_EXPIRE_HOURS * 3600), TimeUnit.SECONDS);

        return R.ok(list);
    }

    /**
     * 删除会话历史记录
     * 同时删除会话详情表中对应conversationId的记录，清除Redis缓存
     */
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/history/{conversationId}")
    @Operation(summary = "删除会话历史记录")
    public AjaxResult deleteConversation(@PathVariable Long conversationId) {
        Long userId = SecurityUtils.getUserId();

        // 删除会话历史记录
        int result = aiConversationService.deleteConversationById(conversationId);
        if (result <= 0) {
            return AjaxResult.error("会话不存在或已被删除");
        }

        // 删除会话详情表中对应的记录
        aiMessageService.deleteByConversationId(conversationId);

        // 清除Redis中的聊天记忆
        chatMemory.clear(conversationId.toString());

        // 清除Redis缓存：会话详情缓存
        redisCache.deleteObject(CONVERSATION_CACHE_PREFIX + conversationId);

        // 清除Redis缓存：用户的会话列表缓存
        redisCache.deleteObject(CONVERSATIONS_CACHE_PREFIX + userId);

        return AjaxResult.success("删除成功");
    }
}
