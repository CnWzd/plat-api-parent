package com.zdd.plat.admin.app;

import com.zdd.plat.admin.app.dto.AppConsolePatchRequest;
import com.zdd.plat.admin.app.dto.AppVO;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营侧应用管理（仅 ADMIN，路径受 SecurityConfig 保护）。
 */
@Tag(name = "运营-应用管理")
@RestController
@RequestMapping("/api/console/apps")
@RequiredArgsConstructor
public class AppConsoleController {

    private final AppService appService;

    @Operation(summary = "全量应用分页")
    @GetMapping
    public Result<PageResult<AppVO>> page(PageQuery query) {
        return Result.ok(appService.pageAllApps(query));
    }

    @Operation(summary = "调整应用状态（0-禁用 1-启用 2-冻结）")
    @PutMapping("/{id}/status")
    public Result<AppVO> updateStatus(@PathVariable Long id, @RequestParam int status) {
        return Result.ok(appService.updateAppStatus(id, status));
    }

    @Operation(summary = "调整应用限流 QPS")
    @PutMapping("/{id}/rate-limit")
    public Result<AppVO> updateRateLimit(@PathVariable Long id,
                                         @Valid @RequestBody AppConsolePatchRequest request) {
        return Result.ok(appService.updateRateLimit(id, request));
    }
}
