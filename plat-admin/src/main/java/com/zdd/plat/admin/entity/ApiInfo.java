package com.zdd.plat.admin.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 元数据（管理库 master）。
 */
@Data
@Table("api_info")
public class ApiInfo {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String apiCode;

    private String apiName;

    private String category;

    /** GET / POST / PUT / DELETE */
    private String method;

    /** 开放路径，如 /openapi/weather/current */
    private String path;

    private String version;

    private String description;

    /** SIGN-签名鉴权 NONE-公开 */
    private String authType;

    /** 0-草稿 1-上架 2-下架 */
    private Integer status;

    /** 创建时间（数据库端填充，insert 语句生成 now()） */
    @Column(onInsertValue = "now()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updatedAt;
}
