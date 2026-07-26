package com.xhzb.nursing.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;

import com.xhzb.common.exception.base.BaseException;
import com.xhzb.nursing.domain.vo.AlertNotifyVo;
import com.xhzb.nursing.domain.vo.CrossNodeMessage;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 服务器 — 集群版
 *
 * 单节点: 本地 ConcurrentHashMap 直发
 * 多节点: Redis Pub/Sub 跨节点广播 + Session Registry
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
@EnableWebSocket
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    /** 本节点标识 */
    public static final String NODE_ID = UUID.randomUUID().toString().substring(0, 8);

    /** 本地连接池（线程安全） */
    private static final Map<String, Session> localSessions = new ConcurrentHashMap<>();

    /** Redis — 通过静态持有者注入（@ServerEndpoint 不支持 @Autowired） */
    private static RedisTemplate<String, String> redis;

    @Autowired
    public void setRedis(RedisTemplate<String, String> redisTemplate) {
        WebSocketServer.redis = redisTemplate;
    }

    // ═══════════════════════════════════════════════════
    //  WebSocket 生命周期
    // ═══════════════════════════════════════════════════

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String userId) {
        log.info("WS连接: userId={} node={}", userId, NODE_ID);
        localSessions.put(userId, session);

        // Redis Session Registry
        if (redis != null) {
            Map<String, String> info = new HashMap<>();
            info.put("nodeId", NODE_ID);
            info.put("connectedAt", String.valueOf(System.currentTimeMillis()));
            info.put("lastHeartbeat", String.valueOf(System.currentTimeMillis()));
            redis.opsForHash().putAll("ws:session:" + userId, info);
            redis.expire("ws:session:" + userId, 90, TimeUnit.SECONDS);

            redis.opsForHash().increment("ws:node:" + NODE_ID, "sessionCount", 1);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("sid") String userId) {
        log.info("WS断开: userId={} node={}", userId, NODE_ID);
        localSessions.remove(userId);

        if (redis != null) {
            redis.delete("ws:session:" + userId);
            redis.opsForHash().increment("ws:node:" + NODE_ID, "sessionCount", -1);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message, @PathParam("sid") String userId) {
        if ("PING".equals(message) || message.contains("\"PING\"")) {
            // 心跳响应 — 刷新 session TTL
            if (redis != null) {
                redis.expire("ws:session:" + userId, 90, TimeUnit.SECONDS);
                redis.opsForHash().put("ws:session:" + userId, "lastHeartbeat",
                        String.valueOf(System.currentTimeMillis()));
            }
        }
    }

    @OnError
    public void onError(Session session, @PathParam("sid") String userId, Throwable e) {
        log.error("WS错误: userId={}", userId, e);
    }

    // ═══════════════════════════════════════════════════
    //  Pub/Sub 订阅（启动时注册）
    // ═══════════════════════════════════════════════════

    @PostConstruct
    public void initPubSub() {
        if (redis == null) return;
        new Thread(() -> {
            try {
                redis.getConnectionFactory().getConnection().subscribe((message, pattern) -> {
                    try {
                        CrossNodeMessage cross = JSONUtil.toBean(
                                new String(message.getBody()), CrossNodeMessage.class);
                        if (NODE_ID.equals(cross.getTargetNode())) {
                            Session s = localSessions.get(cross.getUserId());
                            if (s != null && s.isOpen()) {
                                s.getBasicRemote().sendText(cross.getPayload());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Pub/Sub消息处理失败", e);
                    }
                }, "ws:alert:broadcast".getBytes());
            } catch (Exception e) {
                log.error("Pub/Sub订阅失败, 降级为本地模式", e);
            }
        }, "ws-pubsub-" + NODE_ID).start();
        log.info("WebSocket Pub/Sub 已启动, nodeId={}", NODE_ID);
    }

    // ═══════════════════════════════════════════════════
    //  消息推送 — 本节点直发 + 跨节点 Pub/Sub
    // ═══════════════════════════════════════════════════

    /**
     * 发送报警消息给指定用户列表
     */
    public void sendMessageToConsumer(AlertNotifyVo alertNotifyVo, Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) return;

        String payload = JSONUtil.toJsonStr(alertNotifyVo);

        for (Long userId : userIds) {
            String uid = String.valueOf(userId);

            // ① 本节点直发
            Session local = localSessions.get(uid);
            if (local != null && local.isOpen()) {
                try {
                    local.getBasicRemote().sendText(payload);
                    continue;
                } catch (IOException e) {
                    log.warn("本地WebSocket发送失败 userId={}", uid, e);
                }
            }

            // ② 查用户在哪个节点
            if (redis == null) continue;
            String targetNode = (String) redis.opsForHash().get("ws:session:" + uid, "nodeId");
            if (targetNode == null || targetNode.equals(NODE_ID)) continue;

            // ③ 跨节点 Pub/Sub
            CrossNodeMessage cross = CrossNodeMessage.builder()
                    .targetNode(targetNode)
                    .userId(uid)
                    .payload(payload)
                    .timestamp(System.currentTimeMillis())
                    .build();
            redis.convertAndSend("ws:alert:broadcast", JSONUtil.toJsonStr(cross));
        }
    }

    /**
     * 广播消息给所有连接
     */
    public void sendMessageToAll(String message) {
        for (Session session : localSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.warn("广播发送失败", e);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    //  心跳 + 清理（供 WebSocketHeartbeatJob 调用）
    // ═══════════════════════════════════════════════════

    /**
     * 刷新本节点所有 session 的 Redis TTL
     */
    public void refreshSessionTTL() {
        if (redis == null) return;
        for (String userId : localSessions.keySet()) {
            redis.expire("ws:session:" + userId, 90, TimeUnit.SECONDS);
        }
    }

    /**
     * 清理无心跳超时的本地 session
     */
    public void cleanExpiredSessions() {
        List<String> expired = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Session> entry : localSessions.entrySet()) {
            if (!entry.getValue().isOpen()) {
                expired.add(entry.getKey());
                continue;
            }
            // 检查 Redis 中的最后心跳时间
            if (redis != null) {
                String hb = (String) redis.opsForHash().get("ws:session:" + entry.getKey(), "lastHeartbeat");
                if (hb != null && now - Long.parseLong(hb) > 90_000) {
                    expired.add(entry.getKey());
                }
            }
        }

        for (String uid : expired) {
            try {
                localSessions.get(uid).close();
            } catch (IOException ignored) {}
            localSessions.remove(uid);
            if (redis != null) redis.delete("ws:session:" + uid);
        }

        if (!expired.isEmpty()) {
            log.info("清理过期session: {}个", expired.size());
        }
    }
}
