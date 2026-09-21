package com.zdd.plat.admin.app;

import com.zdd.plat.admin.app.dto.AppConsolePatchRequest;
import com.zdd.plat.admin.app.dto.AppCreateRequest;
import com.zdd.plat.admin.app.dto.AppUpdateRequest;
import com.zdd.plat.admin.app.dto.AppVO;
import com.zdd.plat.admin.app.dto.SecretRevealVO;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "应用管理")
@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @Operation(summary = "我的应用分页")
    @GetMapping
    public Result<PageResult<AppVO>> page(PageQuery query) {
        return Result.ok(appService.pageMyApps(CurrentUser.get(), query));
    }

    @Operation(summary = "创建应用（返回一次性 SK）")
    @PostMapping
    public Result<SecretRevealVO> create(@Valid @RequestBody AppCreateRequest request) {
        return Result.ok(appService.createApp(CurrentUser.get(), request));
    }

    @Operation(summary = "应用详情")
    @GetMapping("/{id}")
    public Result<AppVO> detail(@PathVariable Long id) {
        return Result.ok(appService.getMyApp(CurrentUser.get(), id));
    }

    @Operation(summary = "更新应用基本信息")
    @PutMapping("/{id}")
    public Result<AppVO> update(@PathVariable Long id, @Valid @RequestBody AppUpdateRequest request) {
        return Result.ok(appService.updateMyApp(CurrentUser.get(), id, request));
    }

    @Operation(summary = "重置 SecretKey（返回一次性新 SK）")
    @PostMapping("/{id}/reset-secret")
    public Result<SecretRevealVO> resetSecret(@PathVariable Long id) {
        return Result.ok(appService.resetSecret(CurrentUser.get(), id));
    }
}
