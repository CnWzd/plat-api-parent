package com.zdd.plat.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 熔断降级响应（CircuitBreaker fallbackUri 指向本控制器）。
 */
@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<Map<String, Object>>> fallback() {
        log.warn("熔断降级触发：下游管理服务不可用或响应超时");
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", 50300, "message", "服务暂时不可用，请稍后重试", "data", Map.of())));
    }
}
