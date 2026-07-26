package com.xhzb.nursing.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨节点 WebSocket 消息体
 * 通过 Redis Pub/Sub 在集群节点间传递
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossNodeMessage {

    /** 目标节点 ID */
    private String targetNode;

    /** 目标用户 ID */
    private String userId;

    /** 序列化后的 AlertNotifyVo JSON */
    private String payload;

    /** 消息时间戳 */
    private long timestamp;
}
