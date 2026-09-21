package com.zdd.plat.admin.approval.dto;

import java.time.LocalDateTime;

/**
 * 授权记录视图（开发者侧与运营侧共用）。
 */
public record AppApiAuthVO(
        Long id,
        Long appId,
        String appName,
        String appKey,
        Long apiId,
        String apiCode,
        String apiName,
        String apiMethod,
        String apiPath,
        Integer status,
        String applyRemark,
        String rejectReason,
        LocalDateTime appliedAt,
        LocalDateTime approvedAt,
        String ownerUsername
) {}
