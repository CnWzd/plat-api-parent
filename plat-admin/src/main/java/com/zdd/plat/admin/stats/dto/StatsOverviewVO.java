package com.zdd.plat.admin.stats.dto;

/**
 * 调用统计概览。
 */
public record StatsOverviewVO(
        long totalCalls,
        long successCalls,
        long failCalls,
        double successRate,
        Long avgLatencyMs,
        Long maxLatencyMs,
        long appCount
) {}
