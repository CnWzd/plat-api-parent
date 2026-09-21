-- =====================================================================
-- 企业级 API 开放平台 — 日志库 DDL (api_platform_log)
-- 存储：调用明细 / 日维度统计（与管理库分离，MyBatis-Flex 多数据源 log）
-- =====================================================================
CREATE DATABASE IF NOT EXISTS api_platform_log
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE api_platform_log;

-- ---------------------------------------------------------------------
-- 调用日志表（网关异步批量写入，毫秒精度）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_call_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id`         BIGINT       NULL COMMENT '应用 ID',
  `app_key`        VARCHAR(64)  NULL COMMENT '应用 AK（冗余，便于直查）',
  `api_id`         BIGINT       NULL COMMENT 'API ID',
  `api_code`       VARCHAR(64)  NULL COMMENT 'API 编码',
  `method`         VARCHAR(10)  NULL COMMENT 'HTTP 方法',
  `path`           VARCHAR(255) NULL COMMENT '请求路径',
  `status_code`    INT          NULL COMMENT '上游响应状态码',
  `latency_ms`     INT          NULL COMMENT '总耗时(含网关)',
  `client_ip`      VARCHAR(64)  NULL COMMENT '客户端 IP',
  `gateway_result` VARCHAR(20)  NULL COMMENT '网关处理结果 PASSED/REJECTED_AUTH/REJECTED_SIGN/REJECTED_LIMIT/REJECTED_DISABLED/UPSTREAM_ERROR',
  `error_msg`      VARCHAR(512) NULL COMMENT '错误信息',
  `called_at`      DATETIME(3)  NOT NULL COMMENT '调用时间(毫秒精度)',
  PRIMARY KEY (`id`),
  KEY `idx_called_at` (`called_at`),
  KEY `idx_app_key` (`app_key`),
  KEY `idx_api_code` (`api_code`)
) ENGINE = InnoDB COMMENT '调用日志表';

-- ---------------------------------------------------------------------
-- 日维度统计表（预聚合，定时任务刷新）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `api_call_stats_daily` (
  `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stat_date`       DATE     NOT NULL COMMENT '统计日期',
  `app_id`          BIGINT   NOT NULL COMMENT '应用 ID',
  `api_id`          BIGINT   NOT NULL COMMENT 'API ID',
  `total_calls`     BIGINT   NOT NULL DEFAULT 0 COMMENT '总调用',
  `success_calls`   BIGINT   NOT NULL DEFAULT 0 COMMENT '成功调用',
  `fail_calls`      BIGINT   NOT NULL DEFAULT 0 COMMENT '失败调用',
  `avg_latency_ms`  INT      NULL COMMENT '平均耗时',
  `max_latency_ms`  INT      NULL COMMENT '最大耗时',
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_app_api` (`stat_date`, `app_id`, `api_id`)
) ENGINE = InnoDB COMMENT '日维度统计表';
