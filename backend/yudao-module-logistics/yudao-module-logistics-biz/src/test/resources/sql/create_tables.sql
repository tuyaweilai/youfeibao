-- 物流模块的测试建表脚本（H2，MODE=MYSQL）。
--
-- 与 backend/sql/mysql/logistics-vehicle-driver.sql 保持一致（先有测试表，再有真实表）。
-- 新增表的票别忘了同时更新本文件与 clean.sql，否则测试之间会互相污染。
--
-- 注意：脚本里必须有真实语句，Spring 会拒绝只含注释的文件
--（'script' must not be null or empty）。

-- 车辆档案（V2a #77）
CREATE TABLE IF NOT EXISTS logistics_vehicle (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plate_no VARCHAR(32) NOT NULL,
    vehicle_type VARCHAR(64),
    capacity_ton DECIMAL(10,3),
    driving_license_expiry_date DATE,
    insurance_expiry_date DATE,
    photos TEXT,
    gps_device_id VARCHAR(64),
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 司机档案（V2a #77）
CREATE TABLE IF NOT EXISTS logistics_driver (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    mobile VARCHAR(32),
    source TINYINT NOT NULL DEFAULT 1,
    carrier_id BIGINT,
    driving_license_no VARCHAR(64),
    driving_license_type VARCHAR(32),
    driving_license_expiry_date DATE,
    qualification_cert_no VARCHAR(64),
    qualification_cert_expiry_date DATE,
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 运输任务（V2b #78）
CREATE TABLE IF NOT EXISTS logistics_transport_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    vehicle_id BIGINT,
    plate_no VARCHAR(32),
    driver_id BIGINT,
    driver_name VARCHAR(64),
    driver_mobile VARCHAR(32),
    departure_address VARCHAR(255),
    pickup_address VARCHAR(255) NOT NULL,
    pickup_contact_name VARCHAR(64),
    pickup_contact_phone VARCHAR(32),
    expected_start_time DATETIME,
    expected_end_time DATETIME,
    purchase_order_id BIGINT,
    purchase_order_no VARCHAR(64),
    cargo_name VARCHAR(100),
    estimated_quantity DECIMAL(16,4),
    quantity_unit VARCHAR(16),
    assign_time DATETIME,
    override_reason VARCHAR(500),
    override_by BIGINT,
    override_time DATETIME,
    accept_time DATETIME,
    start_time DATETIME,
    complete_time DATETIME,
    cancel_time DATETIME,
    cancel_reason VARCHAR(500),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 运输节点（V2b #78；异常字段 V4 #71；stop_id V5 #72）
-- node_type 可空：异常事件没有「走到哪一步」，它就是一条独立事实（#71 的「异常是独立标记」）
-- stop_id 可空：到达场站/卸货完成为整趟收尾，不属于任何单个停靠点
CREATE TABLE IF NOT EXISTS logistics_transport_node (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    stop_id BIGINT,
    node_type TINYINT,
    node_time DATETIME NOT NULL,
    report_time DATETIME NOT NULL,
    location VARCHAR(500),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    photos TEXT,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    abnormal_type TINYINT,
    abnormal_reason VARCHAR(500),
    abnormal_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    abnormal_resolved_at DATETIME,
    abnormal_resolved_by BIGINT,
    abnormal_resolved_name VARCHAR(64),
    abnormal_resolved_remark VARCHAR(500),
    client_request_id VARCHAR(64) NOT NULL,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 运输任务改派承接记录（V4 #71）：换车换人不覆盖原记录，前后关系留在这里
CREATE TABLE IF NOT EXISTS logistics_transport_task_reassign (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    prev_vehicle_id BIGINT,
    prev_plate_no VARCHAR(32),
    prev_driver_id BIGINT,
    prev_driver_name VARCHAR(64),
    prev_driver_mobile VARCHAR(32),
    vehicle_id BIGINT NOT NULL,
    plate_no VARCHAR(32),
    driver_id BIGINT NOT NULL,
    driver_name VARCHAR(64),
    driver_mobile VARCHAR(32),
    reason VARCHAR(500) NOT NULL,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    reassign_time DATETIME NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 承运商档案（V3 #70）
CREATE TABLE IF NOT EXISTS logistics_carrier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(64),
    contact_mobile VARCHAR(32),
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 运输停靠点（V5 #72）：一车可提多家，各自交接、各自推进、各自凭证
CREATE TABLE IF NOT EXISTS logistics_transport_stop (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    stop_no INT NOT NULL,
    stop_type TINYINT NOT NULL DEFAULT 1,
    payee_id BIGINT,
    payee_name VARCHAR(64),
    payee_mobile VARCHAR(32),
    address VARCHAR(255) NOT NULL,
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    cargo_name VARCHAR(100),
    estimated_quantity DECIMAL(16,4),
    quantity_unit VARCHAR(16),
    expected_arrival_time DATETIME,
    status TINYINT NOT NULL DEFAULT 0,
    cancel_reason VARCHAR(500),
    cancel_time DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- 交接登记（V6 #73）：司机在提货点登记的现场交接事实，不是收购单
CREATE TABLE IF NOT EXISTS logistics_transport_handover (
    id BIGINT NOT NULL AUTO_INCREMENT,
    handover_no VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    task_no VARCHAR(64),
    stop_id BIGINT,
    address VARCHAR(255),
    payee_id BIGINT,
    payee_name VARCHAR(64),
    payee_mobile VARCHAR(32),
    goods_config_id BIGINT,
    category_name VARCHAR(64),
    unit VARCHAR(16),
    reference_quantity DECIMAL(16,4),
    reference_unit_price DECIMAL(16,4),
    photos TEXT,
    driver_id BIGINT,
    driver_name VARCHAR(64),
    driver_mobile VARCHAR(32),
    vehicle_id BIGINT,
    plate_no VARCHAR(32),
    occur_time DATETIME,
    document_status VARCHAR(16) NOT NULL DEFAULT 'COMPLETE',
    document_gap VARCHAR(128),
    client_request_id VARCHAR(64),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);
