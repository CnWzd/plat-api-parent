package com.zdd.plat.admin.apimeta;

import com.zdd.plat.admin.apimeta.dto.ApiCreateRequest;
import com.zdd.plat.admin.apimeta.dto.ApiUpdateRequest;
import com.zdd.plat.admin.apimeta.dto.ApiVO;
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

import java.util.List;

/**
 * 开发者侧 API 目录（只读：上架 API）。
 */
@Tag(name = "API 目录")
@RestController
@RequestMapping("/api/apis")
@RequiredArgsConstructor
public class ApiMetaController {

    private final ApiMetaService apiMetaService;

    @Operation(summary = "已上架 API 列表")
    @GetMapping
    public Result<List<ApiVO>> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String category) {
        return Result.ok(apiMetaService.listPublished(keyword, category));
    }

    @Operation(summary = "API 详情")
    @GetMapping("/{id}")
    public Result<ApiVO> detail(@PathVariable Long id) {
        return Result.ok(apiMetaService.getById(id));
    }
}
