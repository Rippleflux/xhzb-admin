package com.xhzb.nursing.config;

import com.xhzb.nursing.listener.AlertTriggerSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 报警消息配置
 * <p>
 * 注册 alert:trigger:channel 的订阅者，实现 AlertDetectionListener
 * → Redis Pub/Sub → AlertTriggerSubscriber → handleAlertTrigger 的解耦链路。
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Configuration
public class AlertPubSubConfig {

    @Autowired
    private AlertTriggerSubscriber alertTriggerSubscriber;

    @Bean
    public RedisMessageListenerContainer alertTriggerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(alertTriggerSubscriber,
                new ChannelTopic("alert:trigger:channel"));
        return container;
    }
}
