package com.zdd.plat.admin.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 两级缓存：Caffeine (L1, 短 TTL) → Redis (L2, 长 TTL) → MySQL (loader)。
 * <p>容错设计：Redis 不可用时自动降级为仅 L1 + 直查 DB，并内置简单断路器
 * （连续失败 3 次后 30 秒内跳过 Redis 访问，避免每次请求都等待连接超时）。</p>
 * <p>L1 缓存 {@link Optional} 包装值以防止缓存穿透（null 占位，短 TTL 自动过期）。</p>
 *
 * @param <V> 缓存值类型（须可 JSON 序列化）
 */
@Slf4j
public class TwoLevelCache<V> {

    private static final int CIRCUIT_THRESHOLD = 3;
    private static final long CIRCUIT_COOLDOWN_MS = 30_000;

    private final Cache<String, Optional<V>> l1;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String redisKeyPrefix;
    private final Class<V> type;
    private final Duration l2Ttl;

    private volatile long circuitOpenUntil = 0;
    private int consecutiveFailures = 0;

    TwoLevelCache(long l1TtlSeconds, long l1MaxEntries, long l2TtlSeconds,
                  String topic, Class<V> type,
                  StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.l1 = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(l1TtlSeconds))
                .maximumSize(l1MaxEntries)
                .build();
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.redisKeyPrefix = "plat:cache:" + topic + ":";
        this.type = type;
        this.l2Ttl = Duration.ofSeconds(l2TtlSeconds);
    }

    /**
     * 读取：L1 → L2 → loader（DB），逐级回填。
     */
    public V get(String key, Supplier<V> loader) {
        Optional<V> l1Hit = l1.getIfPresent(key);
        if (l1Hit != null) {
            return l1Hit.orElse(null);
        }

        V fromL2 = readFromRedis(key);
        if (fromL2 != null) {
            l1.put(key, Optional.ofNullable(fromL2));
            return fromL2;
        }

        V value = loader.get();
        l1.put(key, Optional.ofNullable(value));
        writeToRedis(key, value);
        return value;
    }

    /** 直读 L1（网关高频路径辅助，通常经由 get）。 */
    public V getIfL1Present(String key) {
        Optional<V> hit = l1.getIfPresent(key);
        return hit == null ? null : hit.orElse(null);
    }

    /**
     * 失效缓存（写场景调用）：同时清除 L1 与 L2。
     */
    public void evict(String key) {
        l1.invalidate(key);
        if (redisHealthy()) {
            try {
                redis.delete(redisKeyPrefix + key);
            } catch (Exception e) {
                recordFailure(e);
            }
        }
    }

    private V readFromRedis(String key) {
        if (!redisHealthy()) {
            return null;
        }
        try {
            String json = redis.opsForValue().get(redisKeyPrefix + key);
            if (json == null) {
                return null;
            }
            if ("__null__".equals(json)) {
                return null;
            }
            recordSuccess();
            return objectMapper.readValue(json, type);
        } catch (RedisConnectionFailureException | IllegalStateException e) {
            recordFailure(e);
            return null;
        } catch (Exception e) {
            log.warn("缓存反序列化失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeToRedis(String key, V value) {
        if (!redisHealthy()) {
            return;
        }
        try {
            String json = value == null ? "__null__" : objectMapper.writeValueAsString(value);
            redis.opsForValue().set(redisKeyPrefix + key, json, l2Ttl.toMillis(), TimeUnit.MILLISECONDS);
            recordSuccess();
        } catch (RedisConnectionFailureException | IllegalStateException e) {
            recordFailure(e);
        } catch (Exception e) {
            log.warn("缓存序列化失败 key={}: {}", key, e.getMessage());
        }
    }

    private boolean redisHealthy() {
        if (redis == null) {
            return false;
        }
        return System.currentTimeMillis() >= circuitOpenUntil;
    }

    private synchronized void recordFailure(Exception e) {
        consecutiveFailures++;
        if (consecutiveFailures >= CIRCUIT_THRESHOLD) {
            circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_COOLDOWN_MS;
            log.warn("Redis 访问连续失败 {} 次，未来 {} 秒降级为仅本地缓存（原因: {}）",
                    consecutiveFailures, CIRCUIT_COOLDOWN_MS / 1000, e.getMessage());
            consecutiveFailures = 0;
        }
    }

    private synchronized void recordSuccess() {
        consecutiveFailures = 0;
    }
}
