package com.zdd.plat.admin.common.security;

import com.zdd.plat.admin.common.api.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 无状态配置：
 * <ul>
 *   <li>禁用 CSRF / Session（纯 JWT）</li>
 *   <li>/api/auth/**、/openapi/**、/internal/**、文档与监控端点放行（各自由专用过滤器把关）</li>
 *   <li>/api/console/** 仅平台管理员</li>
 *   <li>其余 /api/** 需登录</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalTokenFilter internalTokenFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/openapi/**").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/console/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) -> writeJson(res, 401, Result.of(40100, "未登录或令牌已失效")))
                        .accessDeniedHandler((req, res, ex) -> writeJson(res, 403, Result.of(40300, "无权限访问该资源"))))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalTokenFilter, JwtAuthFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse res, int status, Result<Void> body) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
