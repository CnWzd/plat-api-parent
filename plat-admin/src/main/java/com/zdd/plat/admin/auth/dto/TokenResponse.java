package com.zdd.plat.admin.auth.dto;

/**
 * 登录/刷新响应。
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserVO user
) {
    public record UserVO(Long id, String username, String nickname, String role, Long tenantId) {}
}
