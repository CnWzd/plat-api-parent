package com.zdd.plat.admin.entity.log;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日维度统计（日志库 log，定时任务预聚合）。
 */
@Data
@Table(value = "api_call_stats_daily", dataSource = "log")
public class ApiCallStatsDaily {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDate statDate;

    private Long appId;

    private Long apiId;

    private Long totalCalls;

    private Long successCalls;

    private Long failCalls;

    private Integer avgLatencyMs;

    private Integer maxLatencyMs;

    private LocalDateTime updatedAt;
}
