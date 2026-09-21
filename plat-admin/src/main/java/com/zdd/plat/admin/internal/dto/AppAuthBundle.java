package com.zdd.plat.admin.internal.dto;

import java.util.List;

/**
 * 网关鉴权数据包：appKey 对应的应用身份 + 已授权 API 明细。
 * <p>由管理服务解密 SK 后经内网返回给网关（调用链路需保证网络隔离）。</p>
 */
public record AppAuthBundle(
        Long appId,
        String appKey,
        String appSecret,
        Integer status,
        Integer rateLimitQps,
        List<AuthorizedApi> authorizedApis
) {

    /**
     * 已授权且上架的 API 明细（网关按 path+method 匹配）。
     */
    public record AuthorizedApi(Long apiId, String apiCode, String method, String path, String authType) {}
}
