package com.zdd.plat.gateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Redis 限流维度解析器：按 X-Api-Key 限流。
 * <p>仅当启用 spring.cloud.gateway.default-filters 的 RequestRateLimiter（Redis 模式）时生效。</p>
 */
@Component("apiKeyResolver")
public class ApiKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Api-Key"))
                .defaultIfEmpty("anonymous");
    }
}
