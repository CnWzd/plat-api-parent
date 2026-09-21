package com.zdd.plat.gateway.client;

import com.zdd.plat.gateway.config.GatewayProperties;
import com.zdd.plat.gateway.log.CallLogItem;
import com.zdd.plat.gateway.model.AppAuthBundle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 管理服务内部接口客户端（WebClient 非阻塞）：
 * <ul>
 *   <li>GET /internal/gateway/app-auth?appKey=xx — 查询鉴权数据包</li>
 *   <li>POST /internal/gateway/call-logs — 批量上报调用日志</li>
 * </ul>
 */
@Slf4j
@Component
public class AdminInternalClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /** 统一响应包装 */
    private record ApiEnvelope<T>(int code, String message, T data) {}

    private final WebClient webClient;
    private final GatewayProperties properties;

    public AdminInternalClient(WebClient.Builder builder, GatewayProperties properties) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.adminBaseUrlOrDefault())
                .build();
    }

    public Mono<AppAuthBundle> fetchAppAuth(String appKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/gateway/app-auth")
                        .queryParam("appKey", appKey).build())
                .header("X-Internal-Token", properties.internalTokenOrDefault())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<AppAuthBundle>>() {})
                .mapNotNull(ApiEnvelope::data)
                .timeout(TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("查询鉴权数据包失败 appKey={}: {}", appKey, e.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<Integer> pushCallLogs(List<CallLogItem> items) {
        return webClient.post()
                .uri("/internal/gateway/call-logs")
                .header("X-Internal-Token", properties.internalTokenOrDefault())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(items)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<Integer>>() {})
                .mapNotNull(ApiEnvelope::data)
                .timeout(TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("调用日志上报失败（{} 条将被丢弃）: {}", items.size(), e.getMessage());
                    return Mono.just(0);
                });
    }
}
