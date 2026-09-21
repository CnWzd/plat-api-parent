package com.zdd.plat.admin.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带 HTTP 状态码与业务码，由全局异常处理器统一转换为 Result 结构。
 */
@Getter
public class BizException extends RuntimeException {

    private final int status;
    private final int code;

    public BizException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, 40000, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(401, 40100, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(403, 40300, message);
    }

    public static BizException notFound(String message) {
        return new BizException(404, 40400, message);
    }

    public static BizException conflict(String message) {
        return new BizException(409, 40900, message);
    }
}
