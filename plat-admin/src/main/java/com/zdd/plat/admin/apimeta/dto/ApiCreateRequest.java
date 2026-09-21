package com.zdd.plat.admin.apimeta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApiCreateRequest(

        @NotBlank(message = "API 编码不能为空")
        @Pattern(regexp = "^[a-z0-9_.-]{2,64}$", message = "API 编码为 2~64 位小写字母/数字/._-")
        String apiCode,

        @NotBlank(message = "API 名称不能为空")
        @Size(max = 128)
        String apiName,

        @Size(max = 64)
        String category,

        @NotBlank(message = "HTTP 方法不能为空")
        @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH", message = "仅支持 GET/POST/PUT/DELETE/PATCH")
        String method,

        @NotBlank(message = "路径不能为空")
        @Pattern(regexp = "^/openapi/[a-zA-Z0-9/_{}.-]*$", message = "路径必须以 /openapi/ 开头")
        @Size(max = 255)
        String path,

        @Size(max = 16)
        String version,

        @Size(max = 512)
        String description,

        @Pattern(regexp = "SIGN|NONE", message = "鉴权方式仅支持 SIGN/NONE")
        String authType
) {}
