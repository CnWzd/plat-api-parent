package com.zdd.plat.admin.internal.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 网关上报的调用日志项（毫秒时间戳传输）。
 * <p>appKey 允许为 null：网关对「未携带 X-Api-Key」请求的拒绝记录同样需要审计落库。</p>
 */
public record CallLogItem(
        Long appId,
        String appKey,
        Long apiId,
        String apiCode,
        String method,
        String path,
        Integer statusCode,
        Integer latencyMs,
        String clientIp,
        @NotBlank String gatewayResult,
        String errorMsg,
        Long calledAtEpochMs
) {}
