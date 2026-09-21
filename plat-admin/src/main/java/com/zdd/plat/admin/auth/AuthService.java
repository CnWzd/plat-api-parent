package com.zdd.plat.admin.auth;

import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.auth.dto.LoginRequest;
import com.zdd.plat.admin.auth.dto.RefreshRequest;
import com.zdd.plat.admin.auth.dto.RegisterRequest;
import com.zdd.plat.admin.auth.dto.TokenResponse;
import com.zdd.plat.admin.common.exception.BizException;
import com.zdd.plat.admin.common.security.JwtTokenService;
import com.zdd.plat.admin.common.security.UserPrincipal;
import com.zdd.plat.admin.entity.SysUser;
import com.zdd.plat.admin.entity.Tenant;
import com.zdd.plat.admin.mapper.SysUserMapper;
import com.zdd.plat.admin.mapper.TenantMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import static com.zdd.plat.admin.entity.table.SysUserTableDef.SYS_USER;
import static com.zdd.plat.admin.entity.table.TenantTableDef.TENANT;

/**
 * 认证服务：注册（开发者）、登录、刷新令牌。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public TokenResponse register(RegisterRequest request) {
        SysUser exists = userMapper.selectOneByQuery(QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq(request.username())));
        if (exists != null) {
            throw BizException.conflict("用户名已被占用: " + request.username());
        }

        Long tenantId = resolveTenantId(request.tenantCode());

        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() == null ? request.username() : request.nickname());
        user.setEmail(request.email());
        user.setRole("DEVELOPER");
        user.setTenantId(tenantId);
        user.setStatus(1);
        userMapper.insert(user);

        return buildResponse(user);
    }

    public TokenResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOneByQuery(QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq(request.username())));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            // 统一错误文案，避免暴露"用户不存在/密码错误"差异
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用，请联系平台管理员");
        }
        return buildResponse(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        try {
            Claims claims = jwtTokenService.parse(request.refreshToken());
            UserPrincipal principal = jwtTokenService.toPrincipal(claims, JwtTokenService.TYPE_REFRESH);
            SysUser user = userMapper.selectOneById(principal.userId());
            if (user == null || user.getStatus() != 1) {
                throw BizException.unauthorized("用户不存在或已被禁用");
            }
            return buildResponse(user);
        } catch (JwtException | IllegalArgumentException e) {
            throw BizException.unauthorized("刷新令牌无效或已过期，请重新登录");
        }
    }

    public TokenResponse me(UserPrincipal principal) {
        SysUser user = userMapper.selectOneById(principal.userId());
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return buildResponse(user);
    }

    private Long resolveTenantId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return null;
        }
        Tenant tenant = tenantMapper.selectOneByQuery(QueryWrapper.create()
                .where(TENANT.TENANT_CODE.eq(tenantCode)));
        if (tenant == null) {
            throw BizException.badRequest("租户编码不存在: " + tenantCode);
        }
        return tenant.getId();
    }

    private TokenResponse buildResponse(SysUser user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getRole(), user.getTenantId());
        JwtTokenService.TokenPair pair = jwtTokenService.issueTokens(principal);
        return new TokenResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessExpiresInSeconds(),
                new TokenResponse.UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getTenantId()));
    }
}
