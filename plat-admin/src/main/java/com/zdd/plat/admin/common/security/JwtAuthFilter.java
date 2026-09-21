package com.zdd.plat.admin.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer &lt;token&gt;，注入 SecurityContext。
 * <p>白名单路径（登录/注册/刷新）与 /internal/**（内部令牌校验）不经过本过滤器。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 精确白名单：仅登录/注册/刷新免认证。
        // 不可使用 /api/auth/** 前缀匹配 —— /api/auth/me 需要本过滤器注入 SecurityContext，
        // 否则 CurrentUser.get() 抛 40100「未登录或令牌已失效」。
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/refresh")
                || path.startsWith("/internal/")
                || path.startsWith("/openapi/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenService.parse(token);
                UserPrincipal principal = jwtTokenService.toPrincipal(claims, JwtTokenService.TYPE_ACCESS);
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                writeUnauthorized(response, "令牌无效或已过期，请重新登录");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40100,\"message\":\"" + message + "\",\"data\":null}");
    }
}
