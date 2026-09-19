-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- 工行发票下载记录表
CREATE TABLE `icbc_invoice_download` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `invoice_order_id` bigint NOT NULL COMMENT '发票订单ID',
    `partner_order_id` varchar(64) NOT NULL COMMENT '合作方订单号',
    `order_number` varchar(64) NOT NULL COMMENT '工行订单号',
    `invoice_number` varchar(32) DEFAULT NULL COMMENT '发票号码',
    `download_url` varchar(500) DEFAULT NULL COMMENT '发票下载URL',
    `file_path` varchar(500) DEFAULT NULL COMMENT '本地文件存储路径',
    `file_name` varchar(200) DEFAULT NULL COMMENT '文件名称',
    `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
    `download_status` tinyint NOT NULL DEFAULT 0 COMMENT '下载状态：0-待下载，1-下载中，2-下载成功，3-下载失败',
    `download_time` datetime DEFAULT NULL COMMENT '下载时间',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
    `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_partner_order_id` (`partner_order_id`, `deleted`),
    KEY `idx_order_number` (`order_number`),
    KEY `idx_invoice_number` (`invoice_number`),
    KEY `idx_download_status` (`download_status`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行发票下载记录表';

-- 工行发票文件表
CREATE TABLE `icbc_invoice_file` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `download_id` bigint NOT NULL COMMENT '下载记录ID',
    `invoice_number` varchar(32) NOT NULL COMMENT '发票号码',
    `file_type` varchar(20) NOT NULL COMMENT '文件类型：PDF、XML、OFD等',
    `file_path` varchar(500) NOT NULL COMMENT '文件存储路径',
    `file_name` varchar(200) NOT NULL COMMENT '文件名称',
    `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
    `file_md5` varchar(32) DEFAULT NULL COMMENT '文件MD5值',
    `upload_time` datetime DEFAULT NULL COMMENT '上传时间',
    `access_count` int NOT NULL DEFAULT 0 COMMENT '访问次数',
    `last_access_time` datetime DEFAULT NULL COMMENT '最后访问时间',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_download_file` (`download_id`, `file_type`, `deleted`),
    KEY `idx_invoice_number` (`invoice_number`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工行发票文件表'; 