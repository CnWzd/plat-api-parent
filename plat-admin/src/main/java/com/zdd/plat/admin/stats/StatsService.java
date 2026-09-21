package com.zdd.plat.admin.stats;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.query.QueryBuilder;
import com.zdd.plat.admin.common.security.UserPrincipal;
import com.zdd.plat.admin.entity.AppInfo;
import com.zdd.plat.admin.entity.log.ApiCallLog;
import com.zdd.plat.admin.mapper.ApiCallLogMapper;
import com.zdd.plat.admin.mapper.AppInfoMapper;
import com.zdd.plat.admin.stats.dto.CallLogVO;
import com.zdd.plat.admin.stats.dto.StatsOverviewVO;
import com.zdd.plat.admin.stats.dto.StatsTopApiVO;
import com.zdd.plat.admin.stats.dto.StatsTrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static com.zdd.plat.admin.entity.table.AppInfoTableDef.APP_INFO;
import static com.zdd.plat.admin.entity.log.table.ApiCallLogTableDef.API_CALL_LOG;

/**
 * 调用统计（读日志库）：概览 / 日趋势 / TOP API / 明细分页。
 * <p>开发者视角自动限定自己的应用集合；运营视角为全局。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ApiCallLogMapper callLogMapper;
    private final AppInfoMapper appInfoMapper;

    // ------------------------------------------------------------------
    // 开发者视角
    // ------------------------------------------------------------------

    public StatsOverviewVO myOverview(UserPrincipal principal, int days) {
        List<Long> appIds = myAppIds(principal);
        long appCount = appIds.size();
        if (appCount == 0) {
            return new StatsOverviewVO(0, 0, 0, 0d, null, null, 0);
        }
        return buildOverview(windowFrom(days), appIds, appCount);
    }

    public List<StatsTrendPointVO> myTrend(UserPrincipal principal, int days) {
        List<Long> appIds = myAppIds(principal);
        if (appIds.isEmpty()) {
            return List.of();
        }
        return buildTrend(windowFrom(days), appIds);
    }

    public List<StatsTopApiVO> myTopApis(UserPrincipal principal, int days, int limit) {
        List<Long> appIds = myAppIds(principal);
        if (appIds.isEmpty()) {
            return List.of();
        }
        return buildTopApis(windowFrom(days), appIds, limit);
    }

    public PageResult<CallLogVO> myLogs(UserPrincipal principal, PageQuery query,
                                        String appKey, String apiCode, String gatewayResult) {
        List<Long> appIds = myAppIds(principal);
        if (appIds.isEmpty()) {
            return PageResult.of(0, query.pageNoOrDefault(), query.pageSizeOrDefault(), List.of());
        }
        return buildLogs(query, appIds, appKey, apiCode, gatewayResult);
    }

    // ------------------------------------------------------------------
    // 运营视角
    // ------------------------------------------------------------------

    public StatsOverviewVO consoleOverview(int days) {
        return buildOverview(windowFrom(days), null, 0);
    }

    public List<StatsTrendPointVO> consoleTrend(int days) {
        return buildTrend(windowFrom(days), null);
    }

    public List<StatsTopApiVO> consoleTopApis(int days, int limit) {
        return buildTopApis(windowFrom(days), null, limit);
    }

    public PageResult<CallLogVO> consoleLogs(PageQuery query, String appKey, String apiCode, String gatewayResult) {
        return buildLogs(query, null, appKey, apiCode, gatewayResult);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private List<Long> myAppIds(UserPrincipal principal) {
        return appInfoMapper.selectListByQuery(QueryWrapper.create()
                        .select(APP_INFO.ID)
                        .where(APP_INFO.OWNER_USER_ID.eq(principal.userId())))
                .stream().map(AppInfo::getId).toList();
    }

    private LocalDateTime windowFrom(int days) {
        int bounded = Math.min(Math.max(days, 1), 90);
        return LocalDate.now().minusDays(bounded - 1L).atStartOfDay();
    }

    private StatsOverviewVO buildOverview(LocalDateTime from, List<Long> appIds, long appCount) {
        Map<String, Object> row = callLogMapper.selectOverview(from, LocalDateTime.now(), appIds);
        long total = asLong(row.get("totalCalls"));
        long success = asLong(row.get("successCalls"));
        long fail = asLong(row.get("failCalls"));
        double rate = total == 0 ? 0d : success * 100.0 / total;
        return new StatsOverviewVO(total, success, fail, Math.round(rate * 100) / 100.0,
                asNullableLong(row.get("avgLatencyMs")), asNullableLong(row.get("maxLatencyMs")), appCount);
    }

    private List<StatsTrendPointVO> buildTrend(LocalDateTime from, List<Long> appIds) {
        return callLogMapper.selectDailyTrend(from, LocalDateTime.now(), appIds).stream()
                .map(row -> new StatsTrendPointVO(
                        toLocalDate(row.get("statDate")),
                        asLong(row.get("totalCalls")),
                        asLong(row.get("successCalls")),
                        asLong(row.get("failCalls")),
                        asNullableLong(row.get("avgLatencyMs"))))
                .toList();
    }

    private List<StatsTopApiVO> buildTopApis(LocalDateTime from, List<Long> appIds, int limit) {
        return callLogMapper.selectTopApis(from, LocalDateTime.now(), appIds, Math.min(limit, 50)).stream()
                .map(row -> new StatsTopApiVO(
                        String.valueOf(row.get("apiCode")),
                        asLong(row.get("totalCalls")),
                        asLong(row.get("successCalls")),
                        asNullableLong(row.get("avgLatencyMs"))))
                .toList();
    }

    private PageResult<CallLogVO> buildLogs(PageQuery query, List<Long> appIds,
                                            String appKey, String apiCode, String gatewayResult) {
        QueryWrapper wrapper = QueryWrapper.create();
        if (appIds != null) {
            wrapper.and(API_CALL_LOG.APP_ID.in(appIds));
        }
        if (appKey != null && !appKey.isBlank()) {
            wrapper.and(API_CALL_LOG.APP_KEY.eq(appKey));
        }
        if (apiCode != null && !apiCode.isBlank()) {
            wrapper.and(API_CALL_LOG.API_CODE.eq(apiCode));
        }
        if (gatewayResult != null && !gatewayResult.isBlank()) {
            wrapper.and(API_CALL_LOG.GATEWAY_RESULT.eq(gatewayResult));
        }
        wrapper.orderBy(API_CALL_LOG.CALLED_AT.desc());

        Page<ApiCallLog> page = callLogMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);

        List<CallLogVO> records = page.getRecords().stream().map(log -> new CallLogVO(
                log.getId(), log.getAppKey(), log.getApiCode(), log.getMethod(), log.getPath(),
                log.getStatusCode(), log.getLatencyMs(), log.getClientIp(),
                log.getGatewayResult(), log.getErrorMsg(), log.getCalledAt())).toList();
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(), records);
    }

    private static long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static Long asNullableLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
