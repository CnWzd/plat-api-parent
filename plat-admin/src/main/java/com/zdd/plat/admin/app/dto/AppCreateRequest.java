package com.zdd.plat.admin.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppCreateRequest(

        @NotBlank(message = "应用名称不能为空")
        @Size(max = 128, message = "应用名称过长")
        String appName,

        @Size(max = 255)
        String callbackUrl,

        @Size(max = 255)
        String remark
) {}
