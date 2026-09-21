package com.zdd.plat.admin.stats.dto;

/**
 * TOP API 调用排行。
 */
public record StatsTopApiVO(
        String apiCode,
        long totalCalls,
        long successCalls,
        Long avgLatencyMs
) {}
