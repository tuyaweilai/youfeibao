-- 危险废物转移模块数据库 bit 类型默认值修复脚本
-- 执行日期：2024-12-02
-- 说明：修复 bit 类型默认值语法问题

-- 修改所有表中的 bit 类型默认值
ALTER TABLE waste_transfer_order MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_vehicle_weighing_record MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_order_allocation_record 
MODIFY COLUMN `is_manual_adjustment` bit(1) DEFAULT 0,
MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_order_status_history 
MODIFY COLUMN `milestone_flag` bit(1) DEFAULT 0,
MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_producer_payment_config 
MODIFY COLUMN `is_default_config` bit(1) DEFAULT 0,
MODIFY COLUMN `invoice_required` bit(1) DEFAULT 0,
MODIFY COLUMN `auto_payment_enabled` bit(1) DEFAULT 0,
MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_company_payment_voucher 
MODIFY COLUMN `confirmed_by_producer` bit(1) DEFAULT 0,
MODIFY COLUMN `confirmed_by_recycling` bit(1) DEFAULT 0,
MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_appointment_quotation MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_price_benchmark MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_recycler_price_config 
MODIFY COLUMN `is_negotiable` bit(1) DEFAULT 0,
MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_recycler_customer_price MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_recycler_business_config 
MODIFY COLUMN `allow_client_mode_selection` bit(1) DEFAULT 0,
MODIFY COLUMN `auto_accept_single_quotation` bit(1) DEFAULT 0,
MODIFY COLUMN `enable_price_negotiation` bit(1) DEFAULT 0,
MODIFY COLUMN `is_enabled` bit(1) NOT NULL DEFAULT 1,
MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_order_price_adjustment MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_payment_status_history MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

ALTER TABLE waste_operation_audit_log MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 验证修改
SELECT 
    TABLE_NAME as '表名',
    COLUMN_NAME as '字段名',
    COLUMN_TYPE as '字段类型',
    COLUMN_DEFAULT as '默认值'
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME LIKE 'waste_%'
    AND COLUMN_TYPE = 'bit(1)'
ORDER BY TABLE_NAME, COLUMN_NAME; 

-- 修改waste_transfer_appointment表的deleted字段默认值
ALTER TABLE waste_transfer_appointment MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_recycler_assignment_rule表的deleted字段默认值
ALTER TABLE waste_recycler_assignment_rule MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_appointment_assignment_history表的deleted字段默认值
ALTER TABLE waste_appointment_assignment_history MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_appointment_status_log表的deleted字段默认值
ALTER TABLE waste_appointment_status_log MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_transfer_order表的deleted字段默认值
ALTER TABLE waste_transfer_order MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_order_status_history表的deleted字段默认值
ALTER TABLE waste_order_status_history MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_vehicle_weighing_record表的deleted字段默认值
ALTER TABLE waste_vehicle_weighing_record MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_payment_status_history表的deleted字段默认值
ALTER TABLE waste_payment_status_history MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_operation_audit_log表的deleted字段默认值
ALTER TABLE waste_operation_audit_log MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0;

-- 修改waste_company_payment_voucher表的deleted字段默认值
ALTER TABLE waste_company_payment_voucher MODIFY COLUMN `deleted` bit(1) NOT NULL DEFAULT 0; 