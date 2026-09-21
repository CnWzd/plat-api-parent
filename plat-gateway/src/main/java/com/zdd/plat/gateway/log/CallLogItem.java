package com.zdd.plat.gateway.log;

/**
 * 网关侧调用日志项（上报给管理服务落日志库）。
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
        String gatewayResult,
        String errorMsg,
        Long calledAtEpochMs
) {}
