package com.zdd.plat.admin.internal;

import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.internal.dto.AppAuthBundle;
import com.zdd.plat.admin.internal.dto.CallLogItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 网关内部接口（X-Internal-Token 校验，见 InternalTokenFilter）。
 */
@Slf4j
@Tag(name = "网关内部接口")
@RestController
@RequestMapping("/internal/gateway")
@RequiredArgsConstructor
public class InternalController {

    private final InternalSupportService internalSupportService;

    @Operation(summary = "查询应用鉴权数据包（appKey → 身份+授权）")
    @GetMapping("/app-auth")
    public Result<AppAuthBundle> getAppAuth(@RequestParam("appKey") String appKey) {
        return Result.ok(internalSupportService.getAppAuthBundle(appKey));
    }

    @Operation(summary = "批量写入调用日志")
    @PostMapping("/call-logs")
    public Result<Integer> saveCallLogs(@Valid @RequestBody List<CallLogItem> items) {
        int saved = internalSupportService.saveCallLogs(items);
        log.debug("网关调用日志批量落库 {} 条", saved);
        return Result.ok(saved);
    }
}
