package com.zdd.plat.admin.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用-API 授权（管理库 master，审批流载体）。
 */
@Data
@Table("app_api_auth")
public class AppApiAuth {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long appId;

    private Long apiId;

    /** 0-PENDING 1-APPROVED 2-REJECTED */
    private Integer status;

    private String applyRemark;

    private String rejectReason;

    /** 申请时间（数据库端填充，insert 语句生成 now()） */
    @Column(onInsertValue = "now()")
    private LocalDateTime appliedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;
}
