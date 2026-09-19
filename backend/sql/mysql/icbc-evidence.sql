-- 连接字符集：文件里有中文种子数据。客户端默认不是 utf8mb4 时（例如没有 LANG 的
-- `docker exec -i <mysql容器> mysql`，其默认是 latin1），中文会被双重编码存进库，
-- 界面上就是乱码。这里显式声明，导入时不必再依赖客户端参数。
SET NAMES utf8mb4;

-- 一票一档证据表
-- 系统能从业务表自动取到的证据（资金流=支付单、发票流=发票+原件、信息流=台账条目）不落本表；
-- 本表只收需要人工补录的材料，主要是合同流（框架协议 / 该笔收购合同）与货物流（磅单 / 运输凭证）。
CREATE TABLE IF NOT EXISTS `icbc_evidence` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `invoice_order_id` bigint NOT NULL COMMENT '发票订单ID',
    `partner_order_id` varchar(64) NOT NULL COMMENT '合作方订单号',
    `flow` varchar(20) NOT NULL COMMENT '所属流：CONTRACT/GOODS/CAPITAL/INVOICE/INFO',
    `evidence_type` varchar(40) NOT NULL COMMENT '证据类型：FRAMEWORK_AGREEMENT/ACQUISITION_CONTRACT/WEIGHBRIDGE_TICKET/TRANSPORT_VOUCHER/TRANSFER_RECEIPT/INVOICE_COPY/LEDGER_ENTRY',
    `title` varchar(200) DEFAULT NULL COMMENT '证据标题',
    `file_url` varchar(500) DEFAULT NULL COMMENT '证据文件地址',
    `file_name` varchar(200) DEFAULT NULL COMMENT '证据文件名称',
    `occurred_time` datetime DEFAULT NULL COMMENT '证据对应业务发生时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_partner_order_id` (`partner_order_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='一票一档证据表';
