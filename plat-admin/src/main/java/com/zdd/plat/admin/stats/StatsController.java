package com.zdd.plat.admin.stats;

import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.common.security.CurrentUser;
import com.zdd.plat.admin.stats.dto.CallLogVO;
import com.zdd.plat.admin.stats.dto.StatsOverviewVO;
import com.zdd.plat.admin.stats.dto.StatsTopApiVO;
import com.zdd.plat.admin.stats.dto.StatsTrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开发者侧调用统计。
 */
@Tag(name = "调用统计")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "我的调用概览（近 N 天）")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.myOverview(CurrentUser.get(), days));
    }

    @Operation(summary = "我的按日趋势（近 N 天）")
    @GetMapping("/trend")
    public Result<List<StatsTrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.myTrend(CurrentUser.get(), days));
    }

    @Operation(summary = "我的 TOP API 排行")
    @GetMapping("/top-apis")
    public Result<List<StatsTopApiVO>> topApis(@RequestParam(defaultValue = "7") int days,
                                               @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.myTopApis(CurrentUser.get(), days, limit));
    }

    @Operation(summary = "我的调用明细分页")
    @GetMapping("/logs")
    public Result<PageResult<CallLogVO>> logs(PageQuery query,
                                              @RequestParam(required = false) String appKey,
                                              @RequestParam(required = false) String apiCode,
                                              @RequestParam(required = false) String gatewayResult) {
        return Result.ok(statsService.myLogs(CurrentUser.get(), query, appKey, apiCode, gatewayResult));
    }
}
