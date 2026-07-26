package com.xhzb.nursing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis Cluster 配置 — 支持集群模式 + 单节点兼容
 * 开发环境走单节点 JedisPooled，生产环境走 JedisCluster
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Configuration
public class RedisClusterConfig {

    /**
     * Redis Cluster 模式开关（默认 false = 单节点兼容）
     */
    @Value("${redis.cluster.enabled:false}")
    private boolean clusterEnabled;

    @Value("${redis.cluster.nodes:localhost:6379}")
    private String clusterNodes;

    @Value("${redis.cluster.password:123456}")
    private String clusterPassword;

    /**
     * JedisCluster Bean — 集群模式
     * 仅在 redis.cluster.enabled=true 时生效
     */
    @Bean
    public JedisCluster jedisCluster() {
        if (!clusterEnabled) {
            return null;  // 单节点模式由 JedisPooled 处理
        }
        Set<HostAndPort> nodes = new HashSet<>();
        for (String node : clusterNodes.split(",")) {
            String[] parts = node.trim().split(":");
            nodes.add(new HostAndPort(parts[0], Integer.parseInt(parts[1])));
        }
        return new JedisCluster(nodes, 2000, 2000, 5, clusterPassword, null);
    }
}
