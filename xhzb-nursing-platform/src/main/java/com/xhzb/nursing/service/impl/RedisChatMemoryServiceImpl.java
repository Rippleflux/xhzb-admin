package com.xhzb.nursing.service.impl;

import cn.hutool.json.JSONUtil;
import com.xhzb.nursing.domain.vo.Msg;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于Redis实现的聊天记忆存储服务
 */
@Service
public class RedisChatMemoryServiceImpl implements ChatMemory {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String CHAT_MEMORY_KEY_PREFIX = "chat:memory:";

    /**
     * 添加聊天消息记忆到redis
     *
     * @param conversationId 会话Id
     * @param messages       消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        // 判断聊天消息是否为空
        if (messages == null || messages.isEmpty()) {
            return;
        }
        // 将SpringAI的消息对象转化为可序列化的消息对象, 之后转化为JSON字符串
        List<String> messageList = messages.stream()
                // 将SpringAI的消息对象转化为可序列化的消息对象
                .map(message -> new Msg(message))
                // 将Msg对象转化为JSON字符串
                .map(msg -> JSONUtil.toJsonStr(msg))
                // 采集流中的数据
                .collect(Collectors.toList());

        // 定义聊天记忆数据存储到redis中的key   chat:memory:{conversationId} : List<每一次聊天的JSON字符串>
        String key = CHAT_MEMORY_KEY_PREFIX + conversationId;
        // 将聊天历史保存到redis中
        redisTemplate.opsForList().rightPushAll(key, messageList);
    }

    /**
     * 根据会话Id获取聊天消息记忆
     *
     * @param conversationId
     * @return
     */
    @Override
    public List<Message> get(String conversationId) {
        // 定义聊天记忆数据存储到redis中的key
        String key = CHAT_MEMORY_KEY_PREFIX + conversationId;
        if (!redisTemplate.hasKey(key)) {
            return Collections.emptyList();
        }
        // 从redis中获取会话的聊天历史
        List<String> messageList = redisTemplate.opsForList().range(key, 0, -1);
        if (messageList == null || messageList.isEmpty()) {
            return Collections.emptyList();
        }
        // 将JSON格式的聊天消息转化为SpringAI 的 Message 消息对象
        return messageList.stream()
                // 将JSON字符串转化为Msg对象
                .map(json -> JSONUtil.toBean(json, Msg.class))
                // 将Msg对象转化为SpringAI 的 Message 消息对象
                .map(msg -> msg.toMessage())
                // 采集流中的数据
                .collect(Collectors.toList());
    }

    /***
     * 根据会话Id清楚会话记忆
     * @param conversationId
     */
    @Override
    public void clear(String conversationId) {
        // 定义聊天记忆数据存储到redis中的key
        String key = CHAT_MEMORY_KEY_PREFIX + conversationId;
        // 从redis中删除key
        redisTemplate.delete(key);
    }
}