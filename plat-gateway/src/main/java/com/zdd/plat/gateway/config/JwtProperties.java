package com.zdd.plat.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关侧 JWT 配置（与 plat-admin 的 plat.security.jwt 保持一致）。
 */
@ConfigurationProperties(prefix = "plat.security.jwt")
public record JwtProperties(String secret) {

    public String secretOrDefault() {
        return secret == null || secret.isBlank()
                ? "dev-only-jwt-secret-0123456789abcdef0123456789abcdef" : secret;
    }
}
