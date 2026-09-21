package com.zdd.plat.admin.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户（管理库 master）。
 */
@Data
@Table("tenant")
public class Tenant {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String tenantCode;

    private String tenantName;

    private String contactName;

    private String contactPhone;

    /** 0-禁用 1-启用 */
    private Integer status;

    /** 创建时间（数据库端填充，insert 语句生成 now()） */
    @Column(onInsertValue = "now()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updatedAt;
}
