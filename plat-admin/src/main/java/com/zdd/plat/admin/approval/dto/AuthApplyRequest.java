package com.zdd.plat.admin.approval.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AuthApplyRequest(

        @NotEmpty(message = "请选择要申请的 API")
        List<Long> apiIds,

        @Size(max = 255)
        String applyRemark
) {}
