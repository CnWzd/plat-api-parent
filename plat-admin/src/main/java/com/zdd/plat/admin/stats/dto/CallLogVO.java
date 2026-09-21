package com.zdd.plat.admin.stats.dto;

import java.time.LocalDateTime;

/**
 * 调用日志视图。
 */
public record CallLogVO(
        Long id,
        String appKey,
        String apiCode,
        String method,
        String path,
        Integer statusCode,
        Integer latencyMs,
        String clientIp,
        String gatewayResult,
        String errorMsg,
        LocalDateTime calledAt
) {}
