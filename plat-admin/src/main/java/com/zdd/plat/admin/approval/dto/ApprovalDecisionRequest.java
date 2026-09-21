package com.zdd.plat.admin.approval.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(

        @Size(max = 255, message = "驳回原因过长")
        String reason
) {}
