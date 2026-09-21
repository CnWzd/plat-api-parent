package com.zdd.plat.admin.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 REST 响应结构。
 *
 * @param code    业务码：0 成功；401xx 认证；403xx 权限；404xx 资源；400xx 参数；500xx 系统
 * @param message 提示信息
 * @param data    业务数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "OK", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "OK", null);
    }

    public static <T> Result<T> of(int code, String message) {
        return new Result<>(code, message, null);
    }
}
