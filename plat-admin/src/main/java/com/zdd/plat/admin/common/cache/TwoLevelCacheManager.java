package com.zdd.plat.admin.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zdd.plat.admin.common.config.PlatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 两级缓存工厂：按业务域创建独立 TTL 配置的缓存实例。
 * <p>StringRedisTemplate 由 Spring Boot 自动装配；Redis 未启动时 Bean 仍可创建
 * （懒连接），实际访问失败由 {@link TwoLevelCache} 断路降级。</p>
 */
@Component
@RequiredArgsConstructor
public class TwoLevelCacheManager {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlatProperties properties;

    public <V> TwoLevelCache<V> create(String topic, Class<V> type) {
        PlatProperties.Cache cache = properties.cacheOrDefault();
        return new TwoLevelCache<>(
                cache.l1TtlSeconds(),
                10_000,
                cache.l2TtlSeconds(),
                topic,
                type,
                redisTemplate,
                objectMapper);
    }
}
