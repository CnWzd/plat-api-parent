package com.zdd.plat.admin.stats.dto;

import java.time.LocalDate;

/**
 * 按日趋势点。
 */
public record StatsTrendPointVO(
        LocalDate statDate,
        long totalCalls,
        long successCalls,
        long failCalls,
        Long avgLatencyMs
) {}
