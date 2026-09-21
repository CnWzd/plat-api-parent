package com.zdd.plat.admin.demo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 演示开放 API（/openapi/**）：模拟真实业务后端。
 * <p>生产环境中该路径族由独立业务服务提供，网关完成签名校验/限流/日志后转发；
 * 本演示将其与管理服务同进程部署，仅用于打通端到端链路。</p>
 */
@Tag(name = "演示开放 API")
@RestController
@RequestMapping("/openapi")
public class DemoOpenApiController {

    @Operation(summary = "查询当前天气（演示）")
    @GetMapping("/weather/current")
    public Map<String, Object> weather(@RequestParam(defaultValue = "北京") String city) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("city", city);
        body.put("weather", new String[]{"晴", "多云", "小雨", "阴"}[random.nextInt(4)]);
        body.put("temperature", random.nextInt(-5, 35));
        body.put("humidity", random.nextInt(20, 95));
        body.put("wind", random.nextInt(0, 8) + " 级");
        body.put("observedAt", LocalDateTime.now().withNano(0).toString());
        return body;
    }

    @Operation(summary = "服务器时间（演示）")
    @GetMapping("/demo/time")
    public Map<String, Object> time() {
        return Map.of(
                "epochMillis", System.currentTimeMillis(),
                "datetime", LocalDateTime.now().withNano(0).toString(),
                "timezone", "Asia/Shanghai");
    }

    @Operation(summary = "回显请求体（演示）")
    @PostMapping("/demo/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> payload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("received", payload);
        body.put("echoedAt", LocalDateTime.now().withNano(0).toString());
        return body;
    }
}
