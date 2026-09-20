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
