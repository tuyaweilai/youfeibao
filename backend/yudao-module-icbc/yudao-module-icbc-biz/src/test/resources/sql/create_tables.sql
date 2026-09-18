-- icbc_payer_info table (Payer Information)
CREATE TABLE IF NOT EXISTS icbc_payer_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payer_no VARCHAR(64),
    partner_payer_id VARCHAR(64),
    name VARCHAR(100) NOT NULL,
    credit_code VARCHAR(32),
    tax_no VARCHAR(32),
    bank_account VARCHAR(32),
    bank_name VARCHAR(100),
    address VARCHAR(500),
    telephone VARCHAR(32),
    contact_name VARCHAR(50),
    contact_mobile VARCHAR(32),
    taxpayer_type VARCHAR(10),
    business_type VARCHAR(50),
    status TINYINT NOT NULL DEFAULT 0,
    audit_msg VARCHAR(500),
    icbc_payer_status VARCHAR(32),
    icbc_medium_id VARCHAR(32),
    icbc_openacct_status VARCHAR(32),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_credit_code UNIQUE (credit_code),
    CONSTRAINT uk_tax_no UNIQUE (tax_no)
);

CREATE INDEX IF NOT EXISTS idx_payer_no ON icbc_payer_info(payer_no);
CREATE INDEX IF NOT EXISTS idx_partner_payer_id ON icbc_payer_info(partner_payer_id);

-- icbc_payee_info table (Payee Information)
CREATE TABLE IF NOT EXISTS icbc_payee_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payee_no VARCHAR(64),
    partner_payee_id VARCHAR(64),
    name VARCHAR(100) NOT NULL,
    id_card_no VARCHAR(32),
    mobile VARCHAR(32),
    bank_card_no VARCHAR(32),
    bank_name VARCHAR(100),
    bank_branch VARCHAR(100),
    address VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 0,
    audit_msg VARCHAR(500),
    business_type VARCHAR(50),
    icbc_receiver_status VARCHAR(32),
    icbc_medium_id VARCHAR(32),
    icbc_openacct_status VARCHAR(32),
    occupation VARCHAR(50),
    company_name VARCHAR(100),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_payee_id_card_no UNIQUE (id_card_no),
    CONSTRAINT uk_payee_mobile UNIQUE (mobile)
);

CREATE INDEX IF NOT EXISTS idx_payee_no ON icbc_payee_info(payee_no);
CREATE INDEX IF NOT EXISTS idx_partner_payee_id ON icbc_payee_info(partner_payee_id);

-- icbc_invoice_order table (Invoice Order Information)
CREATE TABLE IF NOT EXISTS icbc_invoice_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    partner_order_id VARCHAR(64),
    payee_id BIGINT,
    payee_no VARCHAR(64),
    payer_id BIGINT,
    payer_no VARCHAR(64),
    total_amount DECIMAL(10,2) NOT NULL,
    invoice_type INTEGER,
    business_type VARCHAR(50),
    order_status INTEGER NOT NULL DEFAULT 0,
    invoice_status INTEGER NOT NULL DEFAULT 0,
    payment_status INTEGER NOT NULL DEFAULT 0,
    tax_status INTEGER NOT NULL DEFAULT 0,
    invoice_no VARCHAR(64),
    invoice_code VARCHAR(64),
    invoice_date DATETIME,
    invoice_amount DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    invoice_file_url VARCHAR(500),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_no UNIQUE (order_no)
);

CREATE INDEX IF NOT EXISTS idx_invoice_order_no ON icbc_invoice_order(order_no);
CREATE INDEX IF NOT EXISTS idx_partner_order_id ON icbc_invoice_order(partner_order_id);

-- icbc_order_item table (Order Item Information)
CREATE TABLE IF NOT EXISTS icbc_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64),
    item_name VARCHAR(200) NOT NULL,
    item_code VARCHAR(100),
    specification VARCHAR(200),
    unit VARCHAR(20),
    quantity DECIMAL(10,3) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,4),
    tax_amount DECIMAL(10,2),
    category VARCHAR(100),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_order_id ON icbc_order_item(order_id);

-- icbc_payment_order table (Payment Order Information)
CREATE TABLE IF NOT EXISTS icbc_payment_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    partner_order_id VARCHAR(35) NOT NULL,
    icbc_order_no VARCHAR(64),
    payee_no VARCHAR(40),
    payer_no VARCHAR(20),
    payment_amount DECIMAL(10,2) DEFAULT 0.00,
    payment_status TINYINT DEFAULT 0,
    payment_time DATETIME,
    payment_serial_no VARCHAR(64),
    verified_code VARCHAR(30),
    ukey_id VARCHAR(24),
    redirect_url TEXT,
    msg_id VARCHAR(32),
    error_code VARCHAR(20),
    error_msg VARCHAR(500),
    remark VARCHAR(500),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_partner_order_id UNIQUE (partner_order_id),
    CONSTRAINT uk_payment_order_no UNIQUE (order_no)
);

CREATE INDEX IF NOT EXISTS idx_payment_icbc_order_no ON icbc_payment_order(icbc_order_no);
CREATE INDEX IF NOT EXISTS idx_payment_status ON icbc_payment_order(payment_status);

-- icbc_invoice_download table (Invoice Download Information)
CREATE TABLE IF NOT EXISTS icbc_invoice_download (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_order_id BIGINT NOT NULL,
    partner_order_id VARCHAR(64) NOT NULL,
    order_number VARCHAR(64),
    invoice_number VARCHAR(64),
    download_url VARCHAR(500),
    file_path VARCHAR(500),
    file_name VARCHAR(200),
    file_size BIGINT,
    download_status INTEGER NOT NULL DEFAULT 0,
    download_time DATETIME,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_msg VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_download_partner_order_id ON icbc_invoice_download(partner_order_id);
CREATE INDEX IF NOT EXISTS idx_download_status ON icbc_invoice_download(download_status);

-- icbc_invoice_file table (Invoice File Information)
CREATE TABLE IF NOT EXISTS icbc_invoice_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    download_id BIGINT NOT NULL,
    invoice_number VARCHAR(64) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT,
    file_hash VARCHAR(64),
    file_md5 VARCHAR(32),
    upload_time DATETIME,
    access_count INTEGER NOT NULL DEFAULT 0,
    last_access_time DATETIME,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_file_download_id ON icbc_invoice_file(download_id);
CREATE INDEX IF NOT EXISTS idx_file_invoice_number ON icbc_invoice_file(invoice_number);

-- icbc_api_log table (API Call Log)
CREATE TABLE IF NOT EXISTS icbc_api_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    msg_id VARCHAR(64) NOT NULL,
    api_name VARCHAR(100) NOT NULL,
    api_url VARCHAR(200) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_params TEXT,
    response_data TEXT,
    return_code VARCHAR(20),
    return_msg VARCHAR(500),
    status INTEGER NOT NULL,
    cost_time INTEGER,
    business_id VARCHAR(64),
    business_type VARCHAR(50),
    error_msg VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_icbc_api_log_msg_id ON icbc_api_log(msg_id);
CREATE INDEX IF NOT EXISTS idx_icbc_api_log_business_id ON icbc_api_log(business_id);
CREATE INDEX IF NOT EXISTS idx_icbc_api_log_create_time ON icbc_api_log(create_time);

-- icbc_callback_notify table (Callback Notification)
CREATE TABLE IF NOT EXISTS icbc_callback_notify (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notify_id VARCHAR(64) NOT NULL,
    notify_type VARCHAR(50) NOT NULL,
    business_id VARCHAR(64) NOT NULL,
    notify_data TEXT NOT NULL,
    sign VARCHAR(500),
    process_status INTEGER NOT NULL DEFAULT 0,
    process_msg VARCHAR(500),
    process_time DATETIME,
    retry_count INTEGER NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_icbc_callback_notify_notify_id ON icbc_callback_notify(notify_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_icbc_callback_notify_notify_id ON icbc_callback_notify(notify_id);
CREATE INDEX IF NOT EXISTS idx_icbc_callback_notify_business_id ON icbc_callback_notify(business_id);
CREATE INDEX IF NOT EXISTS idx_icbc_callback_notify_process_status ON icbc_callback_notify(process_status);
CREATE INDEX IF NOT EXISTS idx_icbc_callback_notify_create_time ON icbc_callback_notify(create_time); 