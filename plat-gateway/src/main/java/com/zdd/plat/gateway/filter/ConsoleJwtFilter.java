package com.zdd.plat.gateway.filter;

import com.zdd.plat.gateway.security.JwtVerifier;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
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

import java.nio.charset.StandardCharsets;

/**
 * 管理后台 API 鉴权前置过滤器（/api/**）：
 * <ol>
 *   <li>白名单（登录/注册/刷新）直接放行</li>
 *   <li>解析并校验 Bearer JWT（与 plat-admin 共用 HS256 密钥）</li>
 *   <li>校验通过后将用户信息注入请求头（X-User-Id / X-User-Name / X-User-Roles / X-Tenant-Id），
 *       供下游服务信任消费；Authorization 原样透传，下游可二次校验</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleJwtFilter implements GlobalFilter, Ordered {

    private static final String JSON_401 =
            "{\"code\":40100,\"message\":\"令牌无效或已过期，请重新登录\",\"data\":null}";

    private final JwtVerifier jwtVerifier;

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (!path.startsWith("/api/") || isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return writeUnauthorized(exchange.getResponse());
        }

        try {
            Claims claims = jwtVerifier.parse(authorization.substring(7));
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Name", String.valueOf(claims.get("username")))
                    .header("X-User-Roles", String.valueOf(claims.get("role")))
                    .header("X-Tenant-Id", String.valueOf(claims.getOrDefault("tenantId", "")))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.debug("网关 JWT 校验失败 path={}: {}", path, e.getMessage());
            return writeUnauthorized(exchange.getResponse());
        }
    }

    private boolean isWhitelisted(String path) {
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/refresh".equals(path);
    }

    private Mono<Void> writeUnauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory()
                .wrap(JSON_401.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
