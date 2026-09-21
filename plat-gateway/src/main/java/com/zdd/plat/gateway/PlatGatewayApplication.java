package com.zdd.plat.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API 开放平台网关：路由转发 · JWT 鉴权前置 · 开放 API 签名校验 · 限流 · 熔断降级 · 调用日志。
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.zdd.plat.gateway.config")
public class PlatGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatGatewayApplication.class, args);
    }
}
