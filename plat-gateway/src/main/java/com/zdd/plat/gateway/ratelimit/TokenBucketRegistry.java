package com.zdd.plat.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存令牌桶限流器（Redis 不可用时的兜底实现，单机维度）。
 * <p>算法：按 QPS 补充令牌，容量=QPS（允许 1 秒突发）；
 * 通过 CAS 补充+扣减，无锁并发安全。</p>
 * <p>启用 Redis 分布式限流时，请关闭 plat.gateway.ratelimit.memory-enabled
 * 并在 spring.cloud.gateway 中启用 RequestRateLimiter 过滤器。</p>
 */
@Component
public class TokenBucketRegistry {

    private record Bucket(AtomicLong tokens, AtomicLong lastRefillNanos) {}

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流维度（appKey）
     * @param qps 每秒补充速率与桶容量
     * @return true=放行 false=限流
     */
    public boolean tryAcquire(String key, int qps) {
        Bucket bucket = buckets.get(key, k -> new Bucket(new AtomicLong(qps), new AtomicLong(System.nanoTime())));
        refill(bucket, qps);
        while (true) {
            long current = bucket.tokens().get();
            if (current <= 0) {
                return false;
            }
            if (bucket.tokens().compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }

    private void refill(Bucket bucket, int qps) {
        long now = System.nanoTime();
        long last = bucket.lastRefillNanos().get();
        while (true) {
            long elapsed = now - last;
            if (elapsed <= 0) {
                return;
            }
            long newTokens = elapsed * qps / 1_000_000_000L;
            if (newTokens <= 0) {
                return;
            }
            long expectedLast = last;
            if (bucket.lastRefillNanos().compareAndSet(expectedLast, last + newTokens * 1_000_000_000L / qps)) {
                long current;
                long updated;
                do {
                    current = bucket.tokens().get();
                    updated = Math.min(qps, current + newTokens);
                } while (!bucket.tokens().compareAndSet(current, updated));
                return;
            }
            last = bucket.lastRefillNanos().get();
        }
    }
}
