package com.zdd.plat.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zdd.plat.gateway.client.AdminInternalClient;
import com.zdd.plat.gateway.config.GatewayProperties;
import com.zdd.plat.gateway.log.CallLogBuffer;
import com.zdd.plat.gateway.log.CallLogItem;
import com.zdd.plat.gateway.model.AppAuthBundle;
import com.zdd.plat.gateway.ratelimit.TokenBucketRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 开放 API 鉴权过滤器（/openapi/**），完整链路：
 * <pre>
 * X-Api-Key → 鉴权数据包(Caffeine 缓存→管理服务) → 应用状态校验
 *   → path+method 匹配已授权 API → HMAC-SHA256 签名校验(时间窗)
 *   → 应用级 QPS 限流(内存令牌桶) → 转发 → 响应后异步记录调用日志
 * </pre>
 * <p>签名算法：sign = hex_lower(HMAC_SHA256(appSecret, appKey \n timestamp \n method \n path))，
 * Header 传递 X-Api-Key / X-Timestamp / X-Sign；时间窗默认 ±300 秒。</p>
 * <p>所有请求（含被拒）均写入调用日志，便于审计与统计。</p>
 */
@Slf4j
@Component
public class OpenApiAuthFilter implements GlobalFilter, Ordered {

    public static final String RESULT_PASSED = "PASSED";
    public static final String RESULT_REJECTED_AUTH = "REJECTED_AUTH";
    public static final String RESULT_REJECTED_SIGN = "REJECTED_SIGN";
    public static final String RESULT_REJECTED_LIMIT = "REJECTED_LIMIT";
    public static final String RESULT_REJECTED_DISABLED = "REJECTED_DISABLED";
    public static final String RESULT_UPSTREAM_ERROR = "UPSTREAM_ERROR";

    private final AdminInternalClient adminClient;
    private final TokenBucketRegistry tokenBuckets;
    private final CallLogBuffer logBuffer;
    private final GatewayProperties properties;

    /** appKey → 鉴权数据包本地缓存（null 占位防穿透，短 TTL 保证授权变更快速生效）。 */
    private final Cache<String, Optional<AppAuthBundle>> authCache;

    public OpenApiAuthFilter(AdminInternalClient adminClient,
                             TokenBucketRegistry tokenBuckets,
                             CallLogBuffer logBuffer,
                             GatewayProperties properties) {
        this.adminClient = adminClient;
        this.tokenBuckets = tokenBuckets;
        this.logBuffer = logBuffer;
        this.properties = properties;
        this.authCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.cache().ttlOrDefault()))
                .maximumSize(50_000)
                .build();
    }

    @Override
    public int getOrder() {
        return -90;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        if (!path.startsWith("/openapi/")) {
            return chain.filter(exchange);
        }

        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String appKey = request.getHeaders().getFirst("X-Api-Key");
        String clientIp = resolveClientIp(request);

        if (appKey == null || appKey.isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 40101, "缺少 X-Api-Key 请求头",
                    null, null, method, path, clientIp, RESULT_REJECTED_AUTH, start);
        }

        return getBundle(appKey)
                .flatMap(bundle -> {
                    if (bundle == null) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, 40101, "AppKey 不存在或已失效",
                                null, appKey, method, path, clientIp, RESULT_REJECTED_AUTH, start);
                    }
                    if (bundle.status() == null || bundle.status() != 1) {
                        return reject(exchange, HttpStatus.FORBIDDEN, 40301, "应用已被禁用或冻结",
                                bundle.appId(), appKey, method, path, clientIp, RESULT_REJECTED_DISABLED, start);
                    }

                    AppAuthBundle.AuthorizedApi api = matchApi(bundle.authorizedApis(), path, method);
                    if (api == null) {
                        return reject(exchange, HttpStatus.FORBIDDEN, 40301, "无权访问该接口，或接口不存在",
                                bundle.appId(), appKey, method, path, clientIp, RESULT_REJECTED_AUTH, start);
                    }

                    if (properties.sign().strict() && "SIGN".equals(api.authType())) {
                        Mono<Void> signFailure = verifySignature(exchange, bundle, method, path, clientIp, start);
                        if (signFailure != null) {
                            return signFailure;
                        }
                    }

                    if (properties.rateLimit().memoryEnabledOrDefault()
                            && !tokenBuckets.tryAcquire(appKey, bundle.rateLimitQps())) {
                        return reject(exchange, HttpStatus.TOO_MANY_REQUESTS, 42901,
                                "请求超出应用限流阈值 (" + bundle.rateLimitQps() + " QPS)",
                                bundle.appId(), appKey, method, path, clientIp, RESULT_REJECTED_LIMIT, start);
                    }

                    long appId = bundle.appId();
                    Long apiId = api.apiId();
                    String apiCode = api.apiCode();
                    return chain.filter(exchange)
                            .then(Mono.fromRunnable(() -> {
                                int statusCode = exchange.getResponse().getStatusCode() == null
                                        ? 500 : exchange.getResponse().getStatusCode().value();
                                String result = statusCode >= 500 ? RESULT_UPSTREAM_ERROR : RESULT_PASSED;
                                logBuffer.offer(new CallLogItem(appId, appKey, apiId, apiCode, method, path,
                                        statusCode, (int) (System.currentTimeMillis() - start), clientIp,
                                        result, null, start));
                            }));
                });
    }

    // ------------------------------------------------------------------
    // 签名校验（返回 null 表示通过，非 null 为拒绝响应）
    // ------------------------------------------------------------------

    private Mono<Void> verifySignature(ServerWebExchange exchange, AppAuthBundle bundle,
                                       String method, String path, String clientIp, long start) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String timestamp = headers.getFirst("X-Timestamp");
        String sign = headers.getFirst("X-Sign");

        if (timestamp == null || sign == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 40102, "缺少 X-Timestamp / X-Sign 签名头",
                    bundle.appId(), bundle.appKey(), method, path, clientIp, RESULT_REJECTED_SIGN, start);
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 40102, "X-Timestamp 必须为毫秒时间戳",
                    bundle.appId(), bundle.appKey(), method, path, clientIp, RESULT_REJECTED_SIGN, start);
        }

        long now = System.currentTimeMillis();
        long skew = properties.sign().clockSkewOrDefault() * 1000L;
        if (Math.abs(now - ts) > skew) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 40103, "请求时间戳超出容忍窗口",
                    bundle.appId(), bundle.appKey(), method, path, clientIp, RESULT_REJECTED_SIGN, start);
        }

        String expected = hmacSha256Hex(bundle.appSecret(),
                bundle.appKey() + "\n" + timestamp + "\n" + method + "\n" + path);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sign.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, 40104, "签名校验失败",
                    bundle.appId(), bundle.appKey(), method, path, clientIp, RESULT_REJECTED_SIGN, start);
        }
        return null;
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    private Mono<AppAuthBundle> getBundle(String appKey) {
        Optional<AppAuthBundle> cached = authCache.getIfPresent(appKey);
        if (cached != null) {
            return Mono.justOrEmpty(cached.orElse(null));
        }
        return adminClient.fetchAppAuth(appKey)
                .doOnNext(bundle -> authCache.put(appKey, Optional.ofNullable(bundle)))
                .switchIfEmpty(Mono.defer(() -> {
                    // 管理服务查询失败：不缓存 null，避免把瞬时故障放大为 30 秒不可用
                    return Mono.empty();
                }));
    }

    private AppAuthBundle.AuthorizedApi matchApi(List<AppAuthBundle.AuthorizedApi> apis,
                                                  String path, String method) {
        if (apis == null) {
            return null;
        }
        return apis.stream()
                .filter(api -> path.equals(api.path()) && method.equalsIgnoreCase(api.method()))
                .findFirst()
                .orElse(null);
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null
                ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }

    /** 统一拒绝响应：写 JSON + 落调用日志。 */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, int code, String message,
                               Long appId, String appKey, String method, String path,
                               String clientIp, String gatewayResult, long start) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        logBuffer.offer(new CallLogItem(appId, appKey, null, null, method, path,
                status.value(), (int) (System.currentTimeMillis() - start), clientIp,
                gatewayResult, message, start));
        return response.writeWith(Mono.just(buffer));
    }
}
