package com.zdd.plat.admin.approval;

import com.zdd.plat.admin.approval.dto.AppApiAuthVO;
import com.zdd.plat.admin.approval.dto.ApprovalDecisionRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营侧审批工作台（仅 ADMIN）。
 */
@Tag(name = "运营-审批工作台")
@RestController
@RequestMapping("/api/console/approvals")
@RequiredArgsConstructor
public class ApprovalConsoleController {

    private final ApprovalService approvalService;

    @Operation(summary = "审批队列分页（status 缺省为 0-PENDING）")
    @GetMapping
    public Result<PageResult<AppApiAuthVO>> page(PageQuery query,
                                                  @RequestParam(required = false) Integer status) {
        return Result.ok(approvalService.consolePage(query, status));
    }

    @Operation(summary = "通过申请")
    @PostMapping("/{id}/approve")
    public Result<AppApiAuthVO> approve(@PathVariable Long id) {
        return Result.ok(approvalService.approve(CurrentUser.get(), id));
    }

    @Operation(summary = "驳回申请")
    @PostMapping("/{id}/reject")
    public Result<AppApiAuthVO> reject(@PathVariable Long id,
                                       @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return Result.ok(approvalService.reject(CurrentUser.get(), id,
                request == null ? new ApprovalDecisionRequest(null) : request));
    }
}
