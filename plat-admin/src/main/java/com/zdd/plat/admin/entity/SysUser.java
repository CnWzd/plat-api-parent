package com.zdd.plat.admin.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（管理库 master）：开发者 + 平台管理员。
 */
@Data
@Table("sys_user")
public class SysUser {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String username;

    /** BCrypt 哈希 */
    private String password;

    private String nickname;

    private String email;

    private String phone;

    /** DEVELOPER / ADMIN */
    private String role;

    private Long tenantId;

    /** 0-禁用 1-启用 */
    private Integer status;

    /** 创建时间（数据库端填充，insert 语句生成 now()） */
    @Column(onInsertValue = "now()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updatedAt;
}
