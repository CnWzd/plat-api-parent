package com.zdd.plat.admin.entity.log;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调用日志（日志库 log，实体级数据源路由）。
 */
@Data
@Table(value = "api_call_log", dataSource = "log")
public class ApiCallLog {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long appId;

    private String appKey;

    private Long apiId;

    private String apiCode;

    private String method;

    private String path;

    private Integer statusCode;

    private Integer latencyMs;

    private String clientIp;

    /** PASSED / REJECTED_AUTH / REJECTED_SIGN / REJECTED_LIMIT / REJECTED_DISABLED / UPSTREAM_ERROR */
    private String gatewayResult;

    private String errorMsg;

    /** 毫秒精度 */
    private LocalDateTime calledAt;
}
