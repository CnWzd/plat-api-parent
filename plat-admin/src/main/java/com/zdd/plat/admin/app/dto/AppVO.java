package com.zdd.plat.admin.app.dto;

import java.time.LocalDateTime;

/**
 * 应用视图（SK 脱敏展示）。
 */
public record AppVO(
        Long id,
        String appName,
        String appKey,
        String secretMasked,
        Integer status,
        Integer rateLimitQps,
        String callbackUrl,
        String remark,
        Long tenantId,
        LocalDateTime createdAt,
        long approvedApiCount,
        long pendingApiCount
) {}
