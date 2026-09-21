package com.zdd.plat.admin.common.security;

/**
 * 认证主体（JWT 解析后注入 SecurityContext）。
 *
 * @param userId   用户 ID
 * @param username 登录名
 * @param role     角色：ADMIN / DEVELOPER
 * @param tenantId 所属租户 ID（平台管理员为 null）
 */
public record UserPrincipal(Long userId, String username, String role, Long tenantId) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
