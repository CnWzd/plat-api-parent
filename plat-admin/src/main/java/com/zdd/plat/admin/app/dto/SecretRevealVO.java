package com.zdd.plat.admin.app.dto;

import java.time.LocalDateTime;

/**
 * SK 明文响应：仅创建与重置时一次性返回，平台不再提供明文查询。
 */
public record SecretRevealVO(
        Long appId,
        String appName,
        String appKey,
        String appSecret,
        String notice,
        LocalDateTime generatedAt
) {}
