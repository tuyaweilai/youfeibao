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

-- 运输节点（V2b #78）
CREATE TABLE IF NOT EXISTS logistics_transport_node (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    node_type TINYINT NOT NULL,
    node_time DATETIME NOT NULL,
    report_time DATETIME NOT NULL,
    location VARCHAR(500),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    photos TEXT,
    operator_id BIGINT,
    operator_name VARCHAR(64),
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
