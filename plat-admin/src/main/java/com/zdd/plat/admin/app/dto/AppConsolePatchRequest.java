package com.zdd.plat.admin.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 运营侧应用参数调整请求。
 */
public record AppConsolePatchRequest(

        @NotNull(message = "QPS 不能为空")
        @Min(value = 1, message = "QPS 至少为 1")
        @Max(value = 10000, message = "QPS 上限 10000")
        Integer rateLimitQps
) {}
