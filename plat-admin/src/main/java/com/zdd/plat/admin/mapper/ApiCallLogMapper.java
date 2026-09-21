package com.zdd.plat.admin.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zdd.plat.admin.entity.log.ApiCallLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 调用日志 Mapper（日志库 log）。
 * <p>聚合查询采用注解 SQL + 动态 script，直接下推到 MySQL 聚合，避免拉取明细到内存。</p>
 */
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {

    /**
     * 时间窗口内的调用概览：总数 / 成功（网关放行）/ 失败 / 平均与最大耗时。
     *
     * @param appIds 应用 ID 集合；null 表示全部（运营视角）
     */
    @Select("""
            <script>
            SELECT COUNT(*)                                            AS totalCalls,
                   SUM(CASE WHEN gateway_result = 'PASSED' THEN 1 ELSE 0 END)  AS successCalls,
                   SUM(CASE WHEN gateway_result != 'PASSED' THEN 1 ELSE 0 END) AS failCalls,
                   CAST(AVG(CASE WHEN gateway_result = 'PASSED' THEN latency_ms END) AS SIGNED) AS avgLatencyMs,
                   MAX(CASE WHEN gateway_result = 'PASSED' THEN latency_ms END)                AS maxLatencyMs
            FROM api_call_log
            WHERE called_at &gt;= #{from} AND called_at &lt; #{to}
            <if test="appIds != null and appIds.size() > 0">
              AND app_id IN
              <foreach collection="appIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            </script>
            """)
    Map<String, Object> selectOverview(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("appIds") List<Long> appIds);

    /**
     * 按日聚合趋势（近 N 天），返回 statDate / totalCalls / successCalls / failCalls。
     */
    @Select("""
            <script>
            SELECT DATE(called_at)                                           AS statDate,
                   COUNT(*)                                                  AS totalCalls,
                   SUM(CASE WHEN gateway_result = 'PASSED' THEN 1 ELSE 0 END)  AS successCalls,
                   SUM(CASE WHEN gateway_result != 'PASSED' THEN 1 ELSE 0 END) AS failCalls,
                   CAST(AVG(CASE WHEN gateway_result = 'PASSED' THEN latency_ms END) AS SIGNED) AS avgLatencyMs
            FROM api_call_log
            WHERE called_at &gt;= #{from} AND called_at &lt; #{to}
            <if test="appIds != null and appIds.size() > 0">
              AND app_id IN
              <foreach collection="appIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            GROUP BY DATE(called_at)
            ORDER BY statDate
            </script>
            """)
    List<Map<String, Object>> selectDailyTrend(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to,
                                               @Param("appIds") List<Long> appIds);

    /**
     * TOP API 调用排行。
     */
    @Select("""
            <script>
            SELECT api_code                                                     AS apiCode,
                   COUNT(*)                                                     AS totalCalls,
                   SUM(CASE WHEN gateway_result = 'PASSED' THEN 1 ELSE 0 END)   AS successCalls,
                   CAST(AVG(CASE WHEN gateway_result = 'PASSED' THEN latency_ms END) AS SIGNED) AS avgLatencyMs
            FROM api_call_log
            WHERE called_at &gt;= #{from} AND called_at &lt; #{to}
              AND api_code IS NOT NULL
            <if test="appIds != null and appIds.size() > 0">
              AND app_id IN
              <foreach collection="appIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            GROUP BY api_code
            ORDER BY totalCalls DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> selectTopApis(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            @Param("appIds") List<Long> appIds,
                                            @Param("limit") int limit);
}
