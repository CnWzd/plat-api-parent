package com.zdd.plat.admin.common.security;

import com.zdd.plat.admin.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具（由 {@link JwtAuthFilter} 注入 SecurityContext）。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UserPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw BizException.unauthorized("未登录或令牌已失效");
    }
}
