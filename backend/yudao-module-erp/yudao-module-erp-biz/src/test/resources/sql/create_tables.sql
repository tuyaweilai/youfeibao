-- ERP 库存域 H2 建表（单元测试用）。列名与 `backend/sql/mysql/erp-stock-location-batch.sql` 之后的状态一致：
-- 库存维度是 goods_config_id，明细表没有 product_unit_id，
-- erp_stock 对 (goods_config_id, warehouse_id, location_id, batch_id) 唯一。

CREATE TABLE IF NOT EXISTS erp_warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    address VARCHAR(50),
    sort BIGINT NOT NULL,
    remark VARCHAR(100),
    station_id BIGINT,
    principal VARCHAR(20),
    warehouse_price DECIMAL(24, 6),
    truckage_price DECIMAL(24, 6),
    status TINYINT NOT NULL,
    default_status BOOLEAN DEFAULT FALSE,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_location (
    id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort BIGINT NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_no VARCHAR(64) NOT NULL,
    goods_config_id BIGINT,
    in_time DATETIME,
    remark VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_batch_no UNIQUE (tenant_id, batch_no)
);

CREATE TABLE IF NOT EXISTS erp_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    goods_config_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL DEFAULT 0,
    batch_id BIGINT NOT NULL DEFAULT 0,
    count DECIMAL(24, 6) NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_goods_config_warehouse_location_batch UNIQUE (goods_config_id, warehouse_id, location_id, batch_id)
);

CREATE TABLE IF NOT EXISTS erp_stock_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    goods_config_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL DEFAULT 0,
    batch_id BIGINT NOT NULL DEFAULT 0,
    count DECIMAL(24, 6) NOT NULL,
    total_count DECIMAL(24, 6) NOT NULL,
    biz_type TINYINT NOT NULL,
    biz_id BIGINT NOT NULL,
    biz_item_id BIGINT NOT NULL,
    biz_no VARCHAR(255) NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_in (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    supplier_id BIGINT,
    in_time DATETIME,
    total_count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6) NOT NULL,
    status TINYINT NOT NULL,
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_in_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    in_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    goods_config_id BIGINT NOT NULL,
    product_price DECIMAL(24, 6),
    count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6),
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_out (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    customer_id BIGINT,
    out_time DATETIME,
    total_count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6) NOT NULL,
    status TINYINT NOT NULL,
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_out_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    out_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    goods_config_id BIGINT NOT NULL,
    product_price DECIMAL(24, 6),
    count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6),
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_move (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    move_time DATETIME,
    total_count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6) NOT NULL,
    status TINYINT NOT NULL,
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_move_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    move_id BIGINT NOT NULL,
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    goods_config_id BIGINT NOT NULL,
    product_price DECIMAL(24, 6),
    count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6),
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_check (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    check_time DATETIME,
    total_count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6) NOT NULL,
    status TINYINT NOT NULL,
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_stock_check_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    check_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    goods_config_id BIGINT NOT NULL,
    product_price DECIMAL(24, 6),
    stock_count DECIMAL(24, 6) NOT NULL,
    actual_count DECIMAL(24, 6) NOT NULL,
    count DECIMAL(24, 6) NOT NULL,
    total_price DECIMAL(24, 6),
    remark VARCHAR(255),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_supplier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(100),
    mobile VARCHAR(30),
    telephone VARCHAR(30),
    email VARCHAR(50),
    fax VARCHAR(50),
    remark VARCHAR(255),
    status TINYINT NOT NULL,
    sort INT NOT NULL,
    subject_type TINYINT,
    taxpayer_qualification TINYINT,
    address VARCHAR(255),
    tax_no VARCHAR(50),
    tax_percent DECIMAL(24, 6),
    bank_name VARCHAR(50),
    bank_account VARCHAR(50),
    bank_address VARCHAR(50),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 采购单据：单位供货方被历史单据引用时不可删的校验用到（T06 / #44）。
CREATE TABLE IF NOT EXISTS erp_purchase_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    account_id BIGINT,
    order_time DATETIME,
    total_count DECIMAL(24, 6),
    total_price DECIMAL(24, 6),
    total_product_price DECIMAL(24, 6),
    total_tax_price DECIMAL(24, 6),
    discount_percent DECIMAL(24, 6),
    discount_price DECIMAL(24, 6),
    deposit_price DECIMAL(24, 6) DEFAULT 0,
    file_url VARCHAR(512),
    remark VARCHAR(1024),
    in_count DECIMAL(24, 6) DEFAULT 0,
    return_count DECIMAL(24, 6) DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_purchase_in (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    account_id BIGINT,
    in_time DATETIME,
    order_id BIGINT,
    order_no VARCHAR(255),
    total_count DECIMAL(24, 6),
    total_price DECIMAL(24, 6),
    payment_price DECIMAL(24, 6) DEFAULT 0,
    total_product_price DECIMAL(24, 6),
    total_tax_price DECIMAL(24, 6),
    discount_percent DECIMAL(24, 6),
    discount_price DECIMAL(24, 6),
    other_price DECIMAL(24, 6) DEFAULT 0,
    file_url VARCHAR(512),
    remark VARCHAR(1024),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_purchase_return (
    id BIGINT NOT NULL AUTO_INCREMENT,
    no VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    account_id BIGINT,
    return_time DATETIME,
    order_id BIGINT,
    order_no VARCHAR(255),
    total_count DECIMAL(24, 6),
    total_price DECIMAL(24, 6),
    refund_price DECIMAL(24, 6) DEFAULT 0,
    total_product_price DECIMAL(24, 6),
    total_tax_price DECIMAL(24, 6),
    discount_percent DECIMAL(24, 6),
    discount_price DECIMAL(24, 6),
    other_price DECIMAL(24, 6) DEFAULT 0,
    file_url VARCHAR(512),
    remark VARCHAR(1024),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS erp_customer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    contact VARCHAR(100),
    mobile VARCHAR(30),
    telephone VARCHAR(30),
    email VARCHAR(50),
    fax VARCHAR(50),
    remark VARCHAR(255),
    status TINYINT NOT NULL,
    sort INT NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
