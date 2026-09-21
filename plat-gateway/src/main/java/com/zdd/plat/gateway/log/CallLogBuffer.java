package com.zdd.plat.gateway.log;

import com.zdd.plat.gateway.client.AdminInternalClient;
import com.zdd.plat.gateway.config.GatewayProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 调用日志攒批缓冲：网关将调用明细放入内存队列，
 * 定时（flushIntervalMs）或满批（batchSize）通过 WebClient 异步上报管理服务落库。
 * <p>故障语义：上报失败整批丢弃并记 WARN（调用日志允许尽力而为，不阻塞转发链路）。</p>
 */
@Slf4j
@Component
public class CallLogBuffer {

    private final BlockingQueue<CallLogItem> queue;
    private final AdminInternalClient client;
    private final int batchSize;
    private final ScheduledExecutorService scheduler;

    private volatile long lastWarnAt = 0;

    public CallLogBuffer(AdminInternalClient client, GatewayProperties properties) {
        this.client = client;
        this.batchSize = properties.log().batchOrDefault();
        this.queue = new ArrayBlockingQueue<>(10_000);
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "gateway-call-log-flusher");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleWithFixedDelay(this::flush,
                properties.log().flushIntervalOrDefault(),
                properties.log().flushIntervalOrDefault(),
                TimeUnit.MILLISECONDS);
        log.info("调用日志缓冲已启动：batchSize={}, flushInterval={}ms", batchSize, properties.log().flushIntervalOrDefault());
    }

    public void offer(CallLogItem item) {
        if (!queue.offer(item)) {
            throttledWarn("调用日志队列已满，丢弃日志: " + item.path());
        }
    }

    private void flush() {
        try {
            List<CallLogItem> batch = drain();
            if (batch.isEmpty()) {
                return;
            }
            client.pushCallLogs(batch)
                    .subscribe(saved -> log.debug("调用日志已上报 {} 条", saved));
        } catch (Exception e) {
            throttledWarn("调用日志刷写异常: " + e.getMessage());
        }
    }

    private List<CallLogItem> drain() {
        List<CallLogItem> batch = new ArrayList<>(batchSize);
        queue.drainTo(batch, batchSize);
        return batch;
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
        try {
            if (scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** WARN 限频（每 30 秒最多一条），防止故障时刷屏。 */
    private void throttledWarn(String message) {
        long now = System.currentTimeMillis();
        if (now - lastWarnAt > 30_000) {
            lastWarnAt = now;
            log.warn(message);
        }
    }
}
