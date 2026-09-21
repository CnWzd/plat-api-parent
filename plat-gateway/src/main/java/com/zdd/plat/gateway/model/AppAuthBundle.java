package com.zdd.plat.gateway.model;

import java.util.List;

/**
 * 网关侧鉴权数据包（与管理服务 AppAuthBundle 结构一致）。
 */
public record AppAuthBundle(
        Long appId,
        String appKey,
        String appSecret,
        Integer status,
        Integer rateLimitQps,
        List<AuthorizedApi> authorizedApis
) {

    public record AuthorizedApi(Long apiId, String apiCode, String method, String path, String authType) {}
}
