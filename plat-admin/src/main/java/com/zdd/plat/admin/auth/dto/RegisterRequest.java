package com.zdd.plat.admin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 开发者注册请求。
 */
public record RegisterRequest(

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名为 4~32 位字母/数字/下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度 6~64 位")
        String password,

        @Size(max = 64, message = "昵称过长")
        String nickname,

        @Email(message = "邮箱格式不正确")
        String email,

        /** 关联已有租户编码（可选） */
        String tenantCode
) {}
