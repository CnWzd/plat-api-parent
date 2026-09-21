package com.zdd.plat.admin.approval;

import com.zdd.plat.admin.approval.dto.AppApiAuthVO;
import com.zdd.plat.admin.approval.dto.AuthApplyRequest;
import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开发者侧授权申请。
 */
@Tag(name = "接口授权申请")
@RestController
@RequestMapping("/api/apps/{appId}")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "查看应用全部授权记录")
    @GetMapping("/auths")
    public Result<List<AppApiAuthVO>> list(@PathVariable Long appId) {
        return Result.ok(approvalService.listByApp(CurrentUser.get(), appId));
    }

    @Operation(summary = "批量申请 API 授权")
    @PostMapping("/auth-apply")
    public Result<List<AppApiAuthVO>> apply(@PathVariable Long appId,
                                            @Valid @RequestBody AuthApplyRequest request) {
        return Result.ok(approvalService.apply(CurrentUser.get(), appId, request));
    }
}
