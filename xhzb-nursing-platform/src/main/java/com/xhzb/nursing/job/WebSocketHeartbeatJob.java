package com.xhzb.nursing.job;

import com.xhzb.nursing.config.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket 心跳维护任务
 * 刷新节点心跳 + 清理过期 session
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
public class WebSocketHeartbeatJob {

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /** 节点标识初始化时由 WebSocketServer 注入 */

    /**
     * 每 10 秒刷新本节点心跳 + session TTL
     */
    @Scheduled(fixedRate = 10_000)
    public void refreshHeartbeat() {
        try {
            String nodeId = WebSocketServer.NODE_ID;

            // 节点心跳（15s TTL，过期=节点宕机）
            redisTemplate.opsForValue().set(
                    "ws:heartbeat:" + nodeId,
                    String.valueOf(System.currentTimeMillis()),
                    15, TimeUnit.SECONDS);

            // 刷新本节点所有 session TTL
            webSocketServer.refreshSessionTTL();
        } catch (Exception e) {
            log.debug("心跳刷新异常: {}", e.getMessage());
        }
    }

    /**
     * 每 30 秒清理过期 session（无心跳超过 90s）
     */
    @Scheduled(fixedRate = 30_000)
    public void cleanExpiredSessions() {
        try {
            webSocketServer.cleanExpiredSessions();
        } catch (Exception e) {
            log.debug("过期session清理异常: {}", e.getMessage());
        }
    }
}
