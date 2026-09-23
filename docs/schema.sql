-- ============================================================================
-- iot-data-platform · 数据库初始化脚本
-- ----------------------------------------------------------------------------
-- 库名   ：iot_db
-- 来源   ：2026-09-22 从本机 MySQL 8.0.46 用 SHOW CREATE TABLE 导出，**非手写**
-- 用法   ：mysql -u root -p < docs/schema.sql
-- ----------------------------------------------------------------------------
-- 与本机真实库的两处有意差异（其余字段/类型/注释/索引完全一致）：
--   1. 加了 `IF NOT EXISTS`，让脚本可重复执行
--   2. 剔除了建表语句尾部的 `AUTO_INCREMENT=<N>` ——
--      那是**运行期计数器状态**，不属于表结构（原因见文末「注意」）
--   3. 已包含 2026-09-22 为分页查询新增的索引 idx_device_data_received_at
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `iot_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `iot_db`;

-- ---------------------------------------------------------------------------
-- 设备上报数据（MQTT 上行入库）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `device_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` varchar(50) NOT NULL COMMENT '设备ID',
  `temperature` decimal(5,2) DEFAULT NULL COMMENT '温度',
  `humidity` decimal(5,2) DEFAULT NULL COMMENT '湿度',
  `received_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_data_received_at` (`received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 告警记录（策略模式告警引擎写入）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `alert_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` varchar(50) NOT NULL,
  `alert_type` varchar(50) NOT NULL COMMENT '告警类型: TEMP_HIGH/HUMIDITY_ABNORMAL等',
  `alert_message` text COMMENT '告警详情',
  `alert_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_resolved` tinyint DEFAULT '0' COMMENT '0未处理 1已处理',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- 操作日志（AOP @Log 切面写入）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `method_name` varchar(200) DEFAULT NULL COMMENT '方法名',
  `params` text COMMENT '入参',
  `return_value` text COMMENT '返回值',
  `execution_time` bigint DEFAULT NULL COMMENT '执行耗时(ms)',
  `log_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================================
-- 注意：为什么这里不写 `AUTO_INCREMENT = <N>`
-- ----------------------------------------------------------------------------
-- 它是**运行期计数器状态**，不是表结构，而且它**不代表行数**：
--   2026-09-22 实测：`device_data` 实际 10027 行、`MAX(id)` = 10027，
--   而 `SHOW CREATE TABLE` 显示 `AUTO_INCREMENT=16411` —— 三者互不相等。
--
-- 官方文档依据（InnoDB Auto-Increment Handling）：
--   · "there may be gaps in the values stored in an AUTO_INCREMENT column of a table"
--   · 批量插入（`INSERT ... SELECT` 这类）"the exact number of auto-increment values
--     required by each statement may not be known and overestimation is possible"，
--     多分配的号会被丢弃（"Excess numbers are lost"）
--
-- 结论：**不要把 auto-increment 值当行数用，也不要假设 id 连续**。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 索引说明（2026-09-22 新增）
-- ---------------------------------------------------------------------------
-- idx_device_data_received_at 是分页查询 `ORDER BY received_at DESC` 的支撑索引。
-- 加它之前该查询走全表扫描 + filesort（EXPLAIN: type=ALL / Using filesort）；
-- 加它之后首页查询从约 4 ms 降到约 0.07 ms（读取行数 10027 → 10）。
-- 完整实验数据见：E:\Desktop\笔记\MySQL\索引与分页性能优化笔记.md
-- ---------------------------------------------------------------------------
