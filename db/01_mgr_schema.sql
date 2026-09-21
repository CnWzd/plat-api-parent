-- =====================================================================
-- 企业级 API 开放平台 — 管理库 DDL (api_platform_mgr)
-- 存储：租户 / 用户 / 应用(密钥) / API 元数据 / 授权审批
-- =====================================================================
CREATE DATABASE IF NOT EXISTS api_platform_mgr
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE api_platform_mgr;

-- ---------------------------------------------------------------------
-- 租户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tenant` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_code`   VARCHAR(64)  NOT NULL COMMENT '租户编码',
  `tenant_name`   VARCHAR(128) NOT NULL COMMENT '租户名称',
  `contact_name`  VARCHAR(64)  NULL COMMENT '联系人',
  `contact_phone` VARCHAR(32)  NULL COMMENT '联系电话',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE = InnoDB COMMENT '租户表';

-- ---------------------------------------------------------------------
-- 用户表（开发者 + 平台管理员）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`   VARCHAR(64)  NOT NULL COMMENT '登录名',
  `password`   VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname`   VARCHAR(64)  NULL COMMENT '昵称',
  `email`      VARCHAR(128) NULL COMMENT '邮箱',
  `phone`      VARCHAR(32)  NULL COMMENT '手机号',
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER' COMMENT '角色 DEVELOPER-开发者 ADMIN-平台管理员',
  `tenant_id`  BIGINT       NULL COMMENT '所属租户（开发者归属）',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE = InnoDB COMMENT '用户表';

-- ---------------------------------------------------------------------
-- 应用表（AppKey / AppSecret，SK 为 AES-GCM 加密密文）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `app_info` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_name`       VARCHAR(128) NOT NULL COMMENT '应用名称',
  `app_key`        VARCHAR(64)  NOT NULL COMMENT 'AccessKey (AK)',
  `app_secret`     VARCHAR(256) NOT NULL COMMENT 'SecretKey (SK, AES-GCM Base64 密文)',
  `tenant_id`      BIGINT       NOT NULL COMMENT '归属租户',
  `owner_user_id`  BIGINT       NOT NULL COMMENT '归属开发者',
  `rate_limit_qps` INT          NOT NULL DEFAULT 100 COMMENT '应用级限流 QPS',
  `callback_url`   VARCHAR(255) NULL COMMENT '回调地址',
  `remark`         VARCHAR(255) NULL COMMENT '备注',
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用 2-冻结',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_key` (`app_key`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_owner` (`owner_user_id`)
) ENGINE = InnoDB COMMENT '应用表';

-- ---------------------------------------------------------------------
-- API 元数据表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_info` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `api_code`    VARCHAR(64)  NOT NULL COMMENT 'API 唯一编码',
  `api_name`    VARCHAR(128) NOT NULL COMMENT 'API 名称',
  `category`    VARCHAR(64)  NOT NULL DEFAULT 'general' COMMENT '分类',
  `method`      VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法',
  `path`        VARCHAR(255) NOT NULL COMMENT '开放路径，如 /openapi/weather/current',
  `version`     VARCHAR(16)  NOT NULL DEFAULT 'v1' COMMENT '版本',
  `description` VARCHAR(512) NULL COMMENT '描述',
  `auth_type`   VARCHAR(20)  NOT NULL DEFAULT 'SIGN' COMMENT '鉴权方式 SIGN-签名 NONE-公开',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-上架 2-下架',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_code` (`api_code`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT 'API 元数据表';

-- ---------------------------------------------------------------------
-- 应用-API 授权表（审批流载体）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `app_api_auth` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id`        BIGINT       NOT NULL COMMENT '应用 ID',
  `api_id`        BIGINT       NOT NULL COMMENT 'API ID',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-PENDING 1-APPROVED 2-REJECTED',
  `apply_remark`  VARCHAR(255) NULL COMMENT '申请说明',
  `reject_reason` VARCHAR(255) NULL COMMENT '驳回原因',
  `applied_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `approved_by`   BIGINT       NULL COMMENT '审批人',
  `approved_at`   DATETIME     NULL COMMENT '审批时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_api` (`app_id`, `api_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT '应用-API 授权表';
