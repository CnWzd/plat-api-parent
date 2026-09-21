package com.zdd.plat.admin.apimeta;

import com.zdd.plat.admin.apimeta.dto.ApiCreateRequest;
import com.zdd.plat.admin.apimeta.dto.ApiUpdateRequest;
import com.zdd.plat.admin.apimeta.dto.ApiVO;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
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
 * 运营侧 API 元数据管理（仅 ADMIN）。
 */
@Tag(name = "运营-API 元数据")
@RestController
@RequestMapping("/api/console/apis")
@RequiredArgsConstructor
public class ApiMetaConsoleController {

    private final ApiMetaService apiMetaService;

    @Operation(summary = "全量 API 分页（可按状态过滤）")
    @GetMapping
    public Result<PageResult<ApiVO>> page(PageQuery query,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(apiMetaService.consolePage(query, status));
    }

    @Operation(summary = "新建 API（默认草稿态）")
    @PostMapping
    public Result<ApiVO> create(@Valid @RequestBody ApiCreateRequest request) {
        return Result.ok(apiMetaService.create(request));
    }

    @Operation(summary = "更新 API")
    @PutMapping("/{id}")
    public Result<ApiVO> update(@PathVariable Long id, @Valid @RequestBody ApiUpdateRequest request) {
        return Result.ok(apiMetaService.update(id, request));
    }

    @Operation(summary = "上架/下架/回草稿（1-上架 2-下架 0-草稿）")
    @PutMapping("/{id}/status")
    public Result<ApiVO> updateStatus(@PathVariable Long id, @RequestParam int status) {
        return Result.ok(apiMetaService.updateStatus(id, status));
    }
}
