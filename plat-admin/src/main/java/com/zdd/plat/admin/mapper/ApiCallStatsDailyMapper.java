package com.zdd.plat.admin.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zdd.plat.admin.entity.log.ApiCallStatsDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 日维度统计 Mapper（日志库 log）。
 */
public interface ApiCallStatsDailyMapper extends BaseMapper<ApiCallStatsDaily> {

    /**
     * 按时间窗口从调用明细预聚合并 UPSERT 日统计表（幂等，可重复执行）。
     */
    @Insert("""
            INSERT INTO api_call_stats_daily
              (stat_date, app_id, api_id, total_calls, success_calls, fail_calls, avg_latency_ms, max_latency_ms)
            SELECT DATE(called_at),
                   app_id,
                   api_id,
                   COUNT(*),
                   SUM(CASE WHEN gateway_result = 'PASSED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN gateway_result <> 'PASSED' THEN 1 ELSE 0 END),
                   CAST(AVG(CASE WHEN gateway_result = 'PASSED' THEN latency_ms END) AS SIGNED),
                   MAX(CASE WHEN gateway_result = 'PASSED' THEN latency_ms END)
            FROM api_call_log
            WHERE called_at >= #{from} AND called_at < #{to}
              AND app_id IS NOT NULL AND api_id IS NOT NULL
            GROUP BY DATE(called_at), app_id, api_id
            ON DUPLICATE KEY UPDATE
              total_calls = VALUES(total_calls),
              success_calls = VALUES(success_calls),
              fail_calls = VALUES(fail_calls),
              avg_latency_ms = VALUES(avg_latency_ms),
              max_latency_ms = VALUES(max_latency_ms)
            """)
    int refreshDailyStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
