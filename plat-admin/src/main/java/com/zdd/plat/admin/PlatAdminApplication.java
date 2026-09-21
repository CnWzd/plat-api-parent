package com.zdd.plat.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API 开放平台管理服务。
 * <p>职责：用户/租户/应用(密钥)/API 元数据/授权审批/调用统计，多数据源(master/log) + 多级缓存(Caffeine+Redis)。</p>
 */
@SpringBootApplication
@MapperScan("com.zdd.plat.admin.mapper")
@ConfigurationPropertiesScan("com.zdd.plat.admin.common.config")
@EnableScheduling
public class PlatAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatAdminApplication.class, args);
    }
}
