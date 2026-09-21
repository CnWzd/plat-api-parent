package com.zdd.plat.admin.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用（管理库 master）：AppKey/AppSecret。
 * <p>appSecret 存 AES-GCM 密文，明文仅在创建/重置时一次性返回。</p>
 */
@Data
@Table("app_info")
public class AppInfo {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String appName;

    private String appKey;

    /** AES-GCM Base64 密文 */
    private String appSecret;

    private Long tenantId;

    private Long ownerUserId;

    private Integer rateLimitQps;

    private String callbackUrl;

    private String remark;

    /** 0-禁用 1-启用 2-冻结 */
    private Integer status;

    /** 创建时间（数据库端填充，insert 语句生成 now()） */
    @Column(onInsertValue = "now()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updatedAt;
}
