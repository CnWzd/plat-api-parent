package com.zdd.plat.admin.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 平台定制配置（前缀 plat，构造器绑定嵌套 record）。
 * <p>敏感项生产环境通过环境变量覆盖：PLAT_SECURITY_JWT_SECRET / PLAT_SECURITY_INTERNAL_TOKEN / PLAT_CRYPTO_SECRET_KEY。</p>
 */
@ConfigurationProperties(prefix = "plat")
public record PlatProperties(Security security, Crypto crypto, Cache cache, Seed seed) {

    public Security securityOrDefault() {
        return security == null ? new Security(null, "dev-internal-token") : security;
    }

    public record Security(Jwt jwt, String internalToken) {

        public Jwt jwtOrDefault() {
            return jwt == null
                    ? new Jwt("dev-only-jwt-secret-0123456789abcdef0123456789abcdef", Duration.ofMinutes(15), Duration.ofDays(7))
                    : jwt;
        }

        public String internalTokenOrDefault() {
            return internalToken == null || internalToken.isBlank() ? "dev-internal-token" : internalToken;
        }

        public record Jwt(String secret, Duration accessTtl, Duration refreshTtl) {}
    }

    public Crypto cryptoOrDefault() {
        return crypto == null ? new Crypto("dev-only-aes-key-0123456789abcdef") : crypto;
    }

    public record Crypto(String secretKey) {}

    public Cache cacheOrDefault() {
        return cache == null ? new Cache(30, 600) : cache;
    }

    public record Cache(long l1TtlSeconds, long l2TtlSeconds) {}

    public Seed seedOrDefault() {
        return seed == null ? new Seed(false) : seed;
    }

    public record Seed(boolean enabled) {}
}
