package com.zdd.plat.admin.stats;

import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
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
 * 运营侧全局统计（仅 ADMIN）。
 */
@Tag(name = "运营-全局统计")
@RestController
@RequestMapping("/api/console/stats")
@RequiredArgsConstructor
public class StatsConsoleController {

    private final StatsService statsService;

    @Operation(summary = "全局调用概览（近 N 天）")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.consoleOverview(days));
    }

    @Operation(summary = "全局按日趋势")
    @GetMapping("/trend")
    public Result<List<StatsTrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.consoleTrend(days));
    }

    @Operation(summary = "全局 TOP API 排行")
    @GetMapping("/top-apis")
    public Result<List<StatsTopApiVO>> topApis(@RequestParam(defaultValue = "7") int days,
                                               @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.consoleTopApis(days, limit));
    }

    @Operation(summary = "全局调用明细分页")
    @GetMapping("/logs")
    public Result<PageResult<CallLogVO>> logs(PageQuery query,
                                              @RequestParam(required = false) String appKey,
                                              @RequestParam(required = false) String apiCode,
                                              @RequestParam(required = false) String gatewayResult) {
        return Result.ok(statsService.consoleLogs(query, appKey, apiCode, gatewayResult));
    }
}
