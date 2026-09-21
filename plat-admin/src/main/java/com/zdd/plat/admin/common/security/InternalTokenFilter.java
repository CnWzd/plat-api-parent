package com.zdd.plat.admin.common.security;

import com.zdd.plat.admin.common.config.PlatProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内部接口令牌过滤器：网关调用管理服务 /internal/** 时必须携带 X-Internal-Token。
 * <p>该路径不走 JWT（调用方是网关而非终端用户），令牌不匹配直接 401。</p>
 */
@Component
@RequiredArgsConstructor
public class InternalTokenFilter extends OncePerRequestFilter {

    private final PlatProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader("X-Internal-Token");
        String expected = properties.securityOrDefault().internalTokenOrDefault();
        if (token == null || !token.equals(expected)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":40100,\"message\":\"内部调用令牌无效\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }
}
