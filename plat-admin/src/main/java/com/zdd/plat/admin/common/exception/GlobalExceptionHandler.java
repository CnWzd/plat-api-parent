package com.zdd.plat.admin.common.exception;

import com.zdd.plat.admin.common.api.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有错误统一转换为 Result JSON，避免堆栈泄露到客户端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e, HttpServletRequest req) {
        log.warn("业务异常 [{}] {}: {}", req.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(Result.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数错误");
        return ResponseEntity.badRequest().body(Result.of(40000, msg));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(Result.of(40400, "接口不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("系统异常 [{}]: {}", req.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.internalServerError().body(Result.of(50000, "系统繁忙，请稍后重试"));
    }
}
