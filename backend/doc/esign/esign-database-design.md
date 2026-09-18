# 电子签章数据库设计文档

## 1. 概述

本文档描述了电子签章模块的数据库设计，包括配置管理、签署记录、回调日志等相关表结构。

## 2. 数据库表设计

### 2.1 电子签章配置表 (esign_config)

```sql
CREATE TABLE esign_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    environment VARCHAR(10) NOT NULL COMMENT '环境：test-测试环境，prod-生产环境',
    api_url VARCHAR(255) NOT NULL COMMENT 'API服务地址',
    app_id VARCHAR(100) NOT NULL COMMENT '应用ID',
    app_secret VARCHAR(255) NOT NULL COMMENT '应用密钥（加密存储）',
    callback_url VARCHAR(255) COMMENT '回调地址',
    connect_timeout INT DEFAULT 30000 COMMENT '连接超时时间（毫秒）',
    read_timeout INT DEFAULT 60000 COMMENT '读取超时时间（毫秒）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    description VARCHAR(500) COMMENT '配置描述',
    last_test_time DATETIME COMMENT '最后测试时间',
    test_result TINYINT COMMENT '测试结果：0-失败，1-成功',
    test_error_msg VARCHAR(1000) COMMENT '测试错误信息',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    UNIQUE KEY uk_environment (environment, deleted, tenant_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '电子签章配置表';
```

### 2.2 签署记录表 (esign_record)

```sql
CREATE TABLE esign_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    module_code VARCHAR(50) NOT NULL COMMENT '业务模块代码',
    business_id BIGINT NOT NULL COMMENT '业务对象ID',
    esign_contract_id VARCHAR(100) COMMENT 'e签宝合同ID',
    contract_title VARCHAR(200) NOT NULL COMMENT '合同标题',
    sign_type TINYINT NOT NULL COMMENT '签署类型：1-单方签署，2-多方签署',
    sign_order TINYINT DEFAULT 1 COMMENT '签署顺序：1-串行，2-并行',
    sign_status TINYINT DEFAULT 1 COMMENT '签署状态：1-待签署，2-签署中，3-已完成，4-已撤销，5-已失败',
    signer_count INT DEFAULT 0 COMMENT '签署方数量',
    signed_count INT DEFAULT 0 COMMENT '已签署数量',
    contract_file_url VARCHAR(500) COMMENT '合同文件地址',
    signed_file_url VARCHAR(500) COMMENT '已签署文件地址',
    callback_url VARCHAR(500) COMMENT '回调地址',
    error_message VARCHAR(1000) COMMENT '错误信息',
    sign_start_time DATETIME COMMENT '签署开始时间',
    sign_end_time DATETIME COMMENT '签署完成时间',
    expire_time DATETIME COMMENT '签署过期时间',
    ext_params JSON COMMENT '扩展参数',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    INDEX idx_module_business (module_code, business_id),
    INDEX idx_esign_contract_id (esign_contract_id),
    INDEX idx_sign_status (sign_status),
    INDEX idx_create_time (create_time)
) COMMENT '电子签章记录表';
```

### 2.3 签署方信息表 (esign_signer)

```sql
CREATE TABLE esign_signer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '签署记录ID',
    esign_signer_id VARCHAR(100) COMMENT 'e签宝签署方ID',
    signer_type TINYINT NOT NULL COMMENT '签署方类型：1-个人，2-企业',
    signer_name VARCHAR(100) NOT NULL COMMENT '签署方名称',
    contact VARCHAR(100) COMMENT '联系方式（手机号或邮箱）',
    id_type VARCHAR(20) COMMENT '证件类型',
    id_number VARCHAR(50) COMMENT '证件号码',
    sign_order INT DEFAULT 1 COMMENT '签署顺序',
    sign_status TINYINT DEFAULT 1 COMMENT '签署状态：1-待签署，2-已签署，3-已拒绝',
    sign_url VARCHAR(500) COMMENT '签署链接',
    sign_time DATETIME COMMENT '签署时间',
    sign_ip VARCHAR(50) COMMENT '签署IP',
    sign_position JSON COMMENT '签署位置信息',
    refuse_reason VARCHAR(500) COMMENT '拒绝原因',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    INDEX idx_record_id (record_id),
    INDEX idx_esign_signer_id (esign_signer_id),
    INDEX idx_sign_status (sign_status),
    INDEX idx_contact (contact)
) COMMENT '电子签章签署方表';
```

### 2.4 回调日志表 (esign_callback_log)

```sql
CREATE TABLE esign_callback_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT COMMENT '签署记录ID',
    esign_contract_id VARCHAR(100) COMMENT 'e签宝合同ID',
    callback_type VARCHAR(50) NOT NULL COMMENT '回调类型',
    callback_data TEXT NOT NULL COMMENT '回调数据',
    callback_signature VARCHAR(255) COMMENT '回调签名',
    signature_valid TINYINT COMMENT '签名验证结果：0-失败，1-成功',
    process_status TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理成功，2-处理失败',
    process_result TEXT COMMENT '处理结果',
    process_time DATETIME COMMENT '处理时间',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1000) COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_record_id (record_id),
    INDEX idx_esign_contract_id (esign_contract_id),
    INDEX idx_callback_type (callback_type),
    INDEX idx_process_status (process_status),
    INDEX idx_create_time (create_time)
) COMMENT '电子签章回调日志表';
```

### 2.5 签署模板表 (esign_template)

```sql
CREATE TABLE esign_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    module_code VARCHAR(50) NOT NULL COMMENT '适用模块',
    template_type TINYINT NOT NULL COMMENT '模板类型：1-合同模板，2-签署位置模板',
    template_content TEXT COMMENT '模板内容',
    template_file_url VARCHAR(500) COMMENT '模板文件地址',
    sign_positions JSON COMMENT '签署位置配置',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    description VARCHAR(500) COMMENT '模板描述',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    UNIQUE KEY uk_template_code (template_code, deleted, tenant_id),
    INDEX idx_module_code (module_code),
    INDEX idx_status (status)
) COMMENT '电子签章模板表';
```

### 2.6 操作日志表 (esign_operation_log)

```sql
CREATE TABLE esign_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    module_code VARCHAR(50) COMMENT '业务模块',
    business_id BIGINT COMMENT '业务对象ID',
    record_id BIGINT COMMENT '签署记录ID',
    operation_desc VARCHAR(500) NOT NULL COMMENT '操作描述',
    operation_params TEXT COMMENT '操作参数',
    operation_result TEXT COMMENT '操作结果',
    success TINYINT NOT NULL COMMENT '是否成功：0-失败，1-成功',
    error_message VARCHAR(1000) COMMENT '错误信息',
    duration_ms BIGINT COMMENT '操作耗时（毫秒）',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operator_ip VARCHAR(50) COMMENT '操作IP',
    user_agent VARCHAR(500) COMMENT '用户代理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    INDEX idx_operation_type (operation_type),
    INDEX idx_module_business (module_code, business_id),
    INDEX idx_record_id (record_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_create_time (create_time)
) COMMENT '电子签章操作日志表';
```

### 2.7 环境切换记录表 (esign_environment_log)

```sql
CREATE TABLE esign_environment_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    from_environment VARCHAR(10) COMMENT '切换前环境',
    to_environment VARCHAR(10) NOT NULL COMMENT '切换后环境',
    switch_reason VARCHAR(500) COMMENT '切换原因',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operator_ip VARCHAR(50) COMMENT '操作IP',
    switch_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '切换时间',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    INDEX idx_to_environment (to_environment),
    INDEX idx_operator_id (operator_id),
    INDEX idx_switch_time (switch_time)
) COMMENT '电子签章环境切换记录表';
```

## 3. 索引设计说明

### 3.1 主要查询场景

1. **按业务对象查询签署记录**：`(module_code, business_id)`
2. **按e签宝合同ID查询**：`esign_contract_id`
3. **按签署状态查询**：`sign_status`
4. **按时间范围查询**：`create_time`
5. **回调日志查询**：`(esign_contract_id, callback_type)`

### 3.2 复合索引设计

```sql
-- 签署记录表复合索引
ALTER TABLE esign_record ADD INDEX idx_module_status_time (module_code, sign_status, create_time);
ALTER TABLE esign_record ADD INDEX idx_status_time (sign_status, create_time);

-- 签署方表复合索引
ALTER TABLE esign_signer ADD INDEX idx_record_order (record_id, sign_order);
ALTER TABLE esign_signer ADD INDEX idx_record_status (record_id, sign_status);

-- 回调日志表复合索引
ALTER TABLE esign_callback_log ADD INDEX idx_contract_type_time (esign_contract_id, callback_type, create_time);
ALTER TABLE esign_callback_log ADD INDEX idx_status_time (process_status, create_time);

-- 操作日志表复合索引
ALTER TABLE esign_operation_log ADD INDEX idx_type_time (operation_type, create_time);
ALTER TABLE esign_operation_log ADD INDEX idx_operator_time (operator_id, create_time);
```

## 4. 数据字典

### 4.1 签署状态枚举

```sql
-- 签署记录状态
-- 1: 待签署 (PENDING)
-- 2: 签署中 (SIGNING) 
-- 3: 已完成 (COMPLETED)
-- 4: 已撤销 (CANCELLED)
-- 5: 已失败 (FAILED)

-- 签署方状态
-- 1: 待签署 (PENDING)
-- 2: 已签署 (SIGNED)
-- 3: 已拒绝 (REFUSED)
```

### 4.2 签署类型枚举

```sql
-- 签署类型
-- 1: 单方签署 (SINGLE)
-- 2: 多方签署 (MULTI)

-- 签署顺序
-- 1: 串行签署 (SERIAL)
-- 2: 并行签署 (PARALLEL)

-- 签署方类型
-- 1: 个人 (PERSONAL)
-- 2: 企业 (ENTERPRISE)
```

### 4.3 模块代码定义

```sql
-- 业务模块代码
-- CONTRACT: 合同管理模块
-- WASTE_TRANSFER: 危废转移模块
-- LOGISTICS: 物流模块
-- PAYMENT: 支付模块
```

## 5. 数据初始化脚本

### 5.1 基础配置数据

```sql
-- 插入测试环境配置
INSERT INTO esign_config (
    environment, api_url, app_id, app_secret, callback_url, 
    status, description, creator, tenant_id
) VALUES (
    'test', 
    'https://smlopenapi.esign.cn', 
    'test-app-id', 
    'encrypted-test-app-secret', 
    'https://your-domain.com/api/esign/callback',
    1, 
    '测试环境配置', 
    'system', 
    0
);

-- 插入生产环境配置
INSERT INTO esign_config (
    environment, api_url, app_id, app_secret, callback_url, 
    status, description, creator, tenant_id
) VALUES (
    'prod', 
    'https://openapi.esign.cn', 
    'prod-app-id', 
    'encrypted-prod-app-secret', 
    'https://your-domain.com/api/esign/callback',
    0, 
    '生产环境配置', 
    'system', 
    0
);
```

### 5.2 签署模板数据

```sql
-- 插入合同签署模板
INSERT INTO esign_template (
    template_code, template_name, module_code, template_type,
    sign_positions, status, description, creator, tenant_id
) VALUES (
    'CONTRACT_SIGN_TEMPLATE', 
    '标准合同签署模板', 
    'CONTRACT', 
    2,
    JSON_OBJECT(
        'positions', JSON_ARRAY(
            JSON_OBJECT('page', 1, 'x', 100, 'y', 200, 'width', 120, 'height', 40),
            JSON_OBJECT('page', 1, 'x', 300, 'y', 200, 'width', 120, 'height', 40)
        )
    ),
    1, 
    '标准合同双方签署位置模板', 
    'system', 
    0
);

-- 插入危废转移合同模板
INSERT INTO esign_template (
    template_code, template_name, module_code, template_type,
    sign_positions, status, description, creator, tenant_id
) VALUES (
    'WASTE_TRANSPORT_TEMPLATE', 
    '危废运输合同模板', 
    'WASTE_TRANSFER', 
    2,
    JSON_OBJECT(
        'positions', JSON_ARRAY(
            JSON_OBJECT('page', 1, 'x', 80, 'y', 600, 'width', 100, 'height', 35, 'role', 'producer'),
            JSON_OBJECT('page', 1, 'x', 250, 'y', 600, 'width', 100, 'height', 35, 'role', 'recycler'),
            JSON_OBJECT('page', 1, 'x', 420, 'y', 600, 'width', 100, 'height', 35, 'role', 'transporter')
        )
    ),
    1, 
    '危废运输三方签署模板', 
    'system', 
    0
);
```

## 6. 数据维护脚本

### 6.1 清理过期数据

```sql
-- 清理30天前的回调日志
DELETE FROM esign_callback_log 
WHERE create_time < DATE_SUB(NOW(), INTERVAL 30 DAY)
  AND process_status = 1;

-- 清理90天前的操作日志
DELETE FROM esign_operation_log 
WHERE create_time < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 清理已删除的签署记录相关数据
DELETE s FROM esign_signer s
LEFT JOIN esign_record r ON s.record_id = r.id
WHERE r.id IS NULL OR r.deleted = 1;
```

### 6.2 数据统计查询

```sql
-- 签署成功率统计
SELECT 
    module_code,
    COUNT(*) as total_count,
    SUM(CASE WHEN sign_status = 3 THEN 1 ELSE 0 END) as success_count,
    ROUND(SUM(CASE WHEN sign_status = 3 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM esign_record 
WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
  AND deleted = 0
GROUP BY module_code;

-- 签署耗时统计
SELECT 
    module_code,
    AVG(TIMESTAMPDIFF(HOUR, sign_start_time, sign_end_time)) as avg_hours,
    MIN(TIMESTAMPDIFF(HOUR, sign_start_time, sign_end_time)) as min_hours,
    MAX(TIMESTAMPDIFF(HOUR, sign_start_time, sign_end_time)) as max_hours
FROM esign_record 
WHERE sign_status = 3 
  AND sign_start_time IS NOT NULL 
  AND sign_end_time IS NOT NULL
  AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY module_code;
```

## 7. 性能优化建议

### 7.1 分区策略

```sql
-- 按月分区回调日志表
ALTER TABLE esign_callback_log 
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    -- 继续添加分区...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 按月分区操作日志表
ALTER TABLE esign_operation_log 
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    -- 继续添加分区...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 7.2 读写分离

```sql
-- 创建只读视图用于查询统计
CREATE VIEW v_esign_record_summary AS
SELECT 
    id, module_code, business_id, esign_contract_id,
    contract_title, sign_type, sign_status, signer_count, signed_count,
    sign_start_time, sign_end_time, create_time
FROM esign_record 
WHERE deleted = 0;

-- 创建签署进度视图
CREATE VIEW v_esign_progress AS
SELECT 
    r.id, r.module_code, r.business_id, r.contract_title,
    r.sign_status, r.signer_count, r.signed_count,
    ROUND(r.signed_count * 100.0 / r.signer_count, 2) as progress_rate,
    r.sign_start_time, r.sign_end_time
FROM esign_record r
WHERE r.deleted = 0 AND r.sign_status IN (2, 3);
```

这个数据库设计文档提供了电子签章模块完整的数据库结构，包括表设计、索引优化、数据字典、初始化脚本等，确保系统的数据存储和查询性能。 