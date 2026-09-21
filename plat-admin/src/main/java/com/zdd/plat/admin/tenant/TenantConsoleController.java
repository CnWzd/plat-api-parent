package com.zdd.plat.admin.tenant;

import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.tenant.TenantService.TenantCreateRequest;
import com.zdd.plat.admin.tenant.TenantService.TenantUpdateRequest;
import com.zdd.plat.admin.tenant.TenantService.TenantVO;
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

/**
 * 运营侧租户管理（仅 ADMIN）。
 */
@Tag(name = "运营-租户管理")
@RestController
@RequestMapping("/api/console/tenants")
@RequiredArgsConstructor
public class TenantConsoleController {

    private final TenantService tenantService;

    @Operation(summary = "租户分页")
    @GetMapping
    public Result<PageResult<TenantVO>> page(PageQuery query) {
        return Result.ok(tenantService.page(query));
    }

    @Operation(summary = "新建租户")
    @PostMapping
    public Result<TenantVO> create(@Valid @RequestBody TenantCreateRequest request) {
        return Result.ok(tenantService.create(request));
    }

    @Operation(summary = "更新租户信息")
    @PutMapping("/{id}")
    public Result<TenantVO> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateRequest request) {
        return Result.ok(tenantService.update(id, request));
    }

    @Operation(summary = "启用/禁用租户（1-启用 0-禁用）")
    @PutMapping("/{id}/status")
    public Result<TenantVO> updateStatus(@PathVariable Long id, @RequestParam int status) {
        return Result.ok(tenantService.updateStatus(id, status));
    }
}
