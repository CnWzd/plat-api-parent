package com.zdd.plat.gateway.security;

import com.zdd.plat.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 校验器（网关侧，HS256 与管理服务共用密钥）。
 */
@Component
public class JwtVerifier {

    private final SecretKey key;

    public JwtVerifier(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secretOrDefault().getBytes(StandardCharsets.UTF_8));
    }

    /** 解析并校验签名/有效期；非法令牌抛出 JwtException / IllegalArgumentException。 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
