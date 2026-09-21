package com.zdd.plat.admin.common.security;

import com.zdd.plat.admin.common.config.PlatProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT 签发与校验（jjwt 0.12 API，HS256 无状态双令牌）。
 * <p>Access Token 有效期 ≤ 15 分钟（文档约束）；Refresh Token 用于换取新 Access Token。</p>
 */
@Component
public class JwtTokenService {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final PlatProperties.Security.Jwt jwtProps;

    public JwtTokenService(PlatProperties properties) {
        this.jwtProps = properties.security().jwt();
        this.key = Keys.hmacShaKeyFor(jwtProps.secret().getBytes(StandardCharsets.UTF_8));
    }

    public record TokenPair(String accessToken, String refreshToken, long accessExpiresInSeconds) {}

    public TokenPair issueTokens(UserPrincipal principal) {
        Instant now = Instant.now();
        String access = buildToken(principal, TYPE_ACCESS, now, jwtProps.accessTtl().toSeconds());
        String refresh = buildToken(principal, TYPE_REFRESH, now, jwtProps.refreshTtl().toSeconds());
        return new TokenPair(access, refresh, jwtProps.accessTtl().toSeconds());
    }

    private String buildToken(UserPrincipal p, String type, Instant now, long ttlSeconds) {
        return Jwts.builder()
                .subject(String.valueOf(p.userId()))
                .claims(Map.of(
                        CLAIM_USERNAME, p.username(),
                        CLAIM_ROLE, p.role(),
                        CLAIM_TENANT_ID, p.tenantId() == null ? "" : String.valueOf(p.tenantId()),
                        CLAIM_TYPE, type
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验签名与有效期，返回声明集合；非法令牌抛出 JwtException。
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** 将声明转换为认证主体（仅接受指定类型的令牌）。 */
    public UserPrincipal toPrincipal(Claims claims, String expectedType) {
        String type = String.valueOf(claims.get(CLAIM_TYPE));
        if (!expectedType.equals(type)) {
            throw new JwtException("令牌类型不匹配");
        }
        String tenantId = (String) claims.get(CLAIM_TENANT_ID);
        return new UserPrincipal(
                Long.valueOf(claims.getSubject()),
                (String) claims.get(CLAIM_USERNAME),
                (String) claims.get(CLAIM_ROLE),
                (tenantId == null || tenantId.isBlank()) ? null : Long.valueOf(tenantId)
        );
    }
}
