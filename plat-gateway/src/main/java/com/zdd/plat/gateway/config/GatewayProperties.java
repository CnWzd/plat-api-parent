package com.zdd.plat.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关定制配置（前缀 plat.gateway）。
 */
@ConfigurationProperties(prefix = "plat.gateway")
public record GatewayProperties(
        String adminBaseUrl,
        String internalToken,
        Sign sign,
        RateLimit rateLimit,
        Cache cache,
        Log log
) {

    public String adminBaseUrlOrDefault() {
        return adminBaseUrl == null || adminBaseUrl.isBlank() ? "http://127.0.0.1:8081" : adminBaseUrl;
    }

    public String internalTokenOrDefault() {
        return internalToken == null || internalToken.isBlank() ? "dev-internal-token" : internalToken;
    }

    /** sign.mode: strict=HMAC-SHA256 签名校验；simple=仅校验 AppKey。 */
    public record Sign(String mode, Integer clockSkewSeconds) {

        public boolean strict() {
            return !"simple".equalsIgnoreCase(mode);
        }

        public int clockSkewOrDefault() {
            return clockSkewSeconds == null || clockSkewSeconds <= 0 ? 300 : clockSkewSeconds;
        }
    }

    public record RateLimit(Boolean memoryEnabled) {

        public boolean memoryEnabledOrDefault() {
            return memoryEnabled == null || memoryEnabled;
        }
    }

    public record Cache(Integer appAuthTtlSeconds) {

        public long ttlOrDefault() {
            return appAuthTtlSeconds == null || appAuthTtlSeconds <= 0 ? 30 : appAuthTtlSeconds;
        }
    }

    public record Log(Integer batchSize, Integer flushIntervalMs) {

        public int batchOrDefault() {
            return batchSize == null || batchSize <= 0 ? 50 : batchSize;
        }

        public int flushIntervalOrDefault() {
            return flushIntervalMs == null || flushIntervalMs <= 0 ? 2000 : flushIntervalMs;
        }
    }
}
