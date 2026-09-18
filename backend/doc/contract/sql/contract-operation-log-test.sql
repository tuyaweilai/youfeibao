-- 检查合同操作日志表是否存在
SELECT table_name, table_comment 
FROM information_schema.tables 
WHERE table_schema = DATABASE() AND table_name = 'contract_operation_logs';

-- 查看合同操作日志表结构
SHOW COLUMNS FROM contract_operation_logs;

-- 插入一条测试数据
INSERT INTO contract_operation_logs (
  contract_id, 
  version_id, 
  operation_type, 
  operation_description,
  old_status,
  new_status,
  operation_data,
  operator_id,
  operator_name,
  operator_ip,
  tenant_id,
  create_time
)
VALUES (
  1, -- 合同ID
  NULL, -- 版本ID
  1, -- 操作类型: 1-创建
  '测试操作日志记录',
  NULL, -- 操作前状态
  0, -- 操作后状态: 0-草稿
  '{"test":"测试数据"}',
  1, -- 操作人ID
  '系统管理员',
  '127.0.0.1',
  0, -- 租户ID
  NOW()
);

-- 查询测试数据
SELECT * FROM contract_operation_logs ORDER BY id DESC LIMIT 10; 