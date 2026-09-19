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
    natural_person_id BIGINT,
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
    real_name_status TINYINT DEFAULT 0,
    real_name_msg VARCHAR(500),
    real_name_time DATETIME,
    audit_result VARCHAR(10),
    reject_reason VARCHAR(500),
    onboarding_state VARCHAR(20),
    id_sign_date VARCHAR(10),
    id_validity_period VARCHAR(10),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_payee_id_card_no UNIQUE (tenant_id, id_card_no),
    CONSTRAINT uk_payee_mobile UNIQUE (tenant_id, mobile)
);

CREATE INDEX IF NOT EXISTS idx_payee_no ON icbc_payee_info(payee_no);
CREATE INDEX IF NOT EXISTS idx_partner_payee_id ON icbc_payee_info(partner_payee_id);
CREATE INDEX IF NOT EXISTS idx_payee_natural_person ON icbc_payee_info(natural_person_id);

-- icbc_invoice_order table (Invoice Order Information)
CREATE TABLE IF NOT EXISTS icbc_invoice_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    partner_order_id VARCHAR(64),
    acquisition_id BIGINT,
    payee_id BIGINT,
    payee_no VARCHAR(64),
    payer_id BIGINT,
    payer_no VARCHAR(64),
    total_amount DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,4),
    invoice_type INTEGER,
    business_type VARCHAR(50),
    order_status INTEGER NOT NULL DEFAULT 0,
    invoice_status INTEGER NOT NULL DEFAULT 0,
    payment_status INTEGER NOT NULL DEFAULT 0,
    tax_status INTEGER NOT NULL DEFAULT 0,
    upload_status INTEGER,
    tax_real_amount DECIMAL(10,2),
    tax_time DATETIME,
    tax_payment_method VARCHAR(4),
    tax_voucher_no VARCHAR(64),
    confirm_status INTEGER NOT NULL DEFAULT 0,
    pre_invoice_status INTEGER NOT NULL DEFAULT 0,
    pre_order_time DATETIME,
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
    CONSTRAINT uk_order_no UNIQUE (order_no),
    CONSTRAINT uk_invoice_partner_order_id UNIQUE (tenant_id, partner_order_id)
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
    quantity_note VARCHAR(200),
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
    acquisition_id BIGINT,
    invoice_order_id BIGINT,
    icbc_order_no VARCHAR(64),
    payee_no VARCHAR(40),
    payer_no VARCHAR(20),
    payment_amount DECIMAL(10,2) DEFAULT 0.00,
    payment_status TINYINT DEFAULT 0,
    pay_status VARCHAR(8),
    payment_time DATETIME,
    payment_serial_no VARCHAR(64),
    actually_received_amount DECIMAL(14,2),
    receipt_no VARCHAR(64),
    receipt_time DATETIME,
    seller_received_confirmed_at DATETIME,
    seller_received_confirm_ip VARCHAR(64),
    receipt_file_url VARCHAR(500),
    retry_count INTEGER DEFAULT 0,
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
    notify_id VARCHAR(128) NOT NULL,
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

-- icbc_qualification table (租户三层资质)
CREATE TABLE IF NOT EXISTS icbc_qualification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    issuing_authority VARCHAR(200),
    cert_no VARCHAR(100),
    valid_from DATE,
    valid_to DATE,
    file_url VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 0,
    audit_remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_goods_config table (品类与税收分类编码配置)
CREATE TABLE IF NOT EXISTS icbc_goods_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(20),
    tax_rate DECIMAL(5,4),
    tax_method VARCHAR(20),
    merged_code VARCHAR(19),
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

-- icbc_enterprise_auth table (工行企业授权)
CREATE TABLE IF NOT EXISTS icbc_enterprise_auth (
    id BIGINT NOT NULL AUTO_INCREMENT,
    out_vendor_id VARCHAR(40),
    site_type VARCHAR(10),
    user_type VARCHAR(10),
    auth_status TINYINT NOT NULL DEFAULT 0,
    auth_time DATETIME,
    expire_time DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_scrap_code table (平台级报废产品税收分类编码表，全局无租户)
CREATE TABLE IF NOT EXISTS icbc_scrap_code (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    merged_code VARCHAR(19) NOT NULL,
    unit VARCHAR(20),
    tax_rate DECIMAL(5,4),
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_expiry_warning table (资质到期预警)
CREATE TABLE IF NOT EXISTS icbc_expiry_warning (
    id BIGINT NOT NULL AUTO_INCREMENT,
    qualification_id BIGINT NOT NULL,
    type VARCHAR(20),
    name VARCHAR(100),
    valid_to DATE,
    status TINYINT NOT NULL DEFAULT 0,
    warned_at DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_evidence table (一票一档证据)
CREATE TABLE IF NOT EXISTS icbc_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_order_id BIGINT NOT NULL,
    partner_order_id VARCHAR(64) NOT NULL,
    flow VARCHAR(20) NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    title VARCHAR(200),
    file_url VARCHAR(500),
    file_name VARCHAR(200),
    occurred_time DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_evidence_partner_order_id ON icbc_evidence(partner_order_id);

-- icbc_public_token table (公开令牌，全局无租户过滤)
CREATE TABLE IF NOT EXISTS icbc_public_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    jti VARCHAR(64) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    tenant_id BIGINT NOT NULL,
    business_key VARCHAR(64) NOT NULL,
    max_uses INTEGER NOT NULL DEFAULT 1,
    used_count INTEGER NOT NULL DEFAULT 0,
    expires_time DATETIME NOT NULL,
    last_used_time DATETIME,
    remark VARCHAR(500),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_public_token_jti UNIQUE (jti)
);

-- icbc_contact_lead table (收方入驻失败留联系方式)
CREATE TABLE IF NOT EXISTS icbc_contact_lead (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payee_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    mobile VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_contact_lead_payee_id ON icbc_contact_lead(payee_id);

-- icbc_framework_agreement table (框架收购协议)
CREATE TABLE IF NOT EXISTS icbc_framework_agreement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payee_id BIGINT NOT NULL,
    agreement_no VARCHAR(64) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity VARCHAR(100) NOT NULL,
    specification VARCHAR(100) NOT NULL,
    recycle_period VARCHAR(100) NOT NULL,
    settlement_method VARCHAR(200) NOT NULL,
    sign_method VARCHAR(20),
    signed_at DATETIME,
    file_url VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_framework_agreement_no UNIQUE (agreement_no)
);
CREATE INDEX IF NOT EXISTS idx_framework_agreement_payee_id ON icbc_framework_agreement(payee_id);

-- icbc_seller_authorization table (出售者首次授权)
CREATE TABLE IF NOT EXISTS icbc_seller_authorization (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payee_id BIGINT NOT NULL,
    reverse_invoice_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    tax_agency_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    authorized_at DATETIME,
    revoked_at DATETIME,
    revoke_reason VARCHAR(500),
    channel VARCHAR(20),
    operator VARCHAR(64),
    evidence_url VARCHAR(500),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_seller_authorization_payee_id ON icbc_seller_authorization(payee_id);

-- icbc_acquisition table (收购登记单，合同流 / 货物流 / 信息流骨架)
CREATE TABLE IF NOT EXISTS icbc_acquisition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    acquisition_no VARCHAR(64) NOT NULL,
    client_request_id VARCHAR(64),
    payee_id BIGINT NOT NULL,
    partner_payee_id VARCHAR(64),
    seller_name VARCHAR(100),
    seller_mobile VARCHAR(32),
    goods_config_id BIGINT,
    category_name VARCHAR(100),
    unit VARCHAR(20),
    tax_rate DECIMAL(5,4),
    tax_method VARCHAR(20),
    merged_code VARCHAR(19),
    specification VARCHAR(100),
    quantity DECIMAL(14,4),
    unit_price DECIMAL(14,2),
    amount DECIMAL(14,2),
    gross_weight DECIMAL(14,4),
    tare_weight DECIMAL(14,4),
    net_weight DECIMAL(14,4),
    deduction DECIMAL(14,4),
    deduction_method VARCHAR(10),
    settlement_weight DECIMAL(14,4),
    adjustment_amount DECIMAL(14,2),
    adjustment_reason VARCHAR(200),
    quantity_note VARCHAR(200),
    settlement_id BIGINT,
    batch_key VARCHAR(64),
    cancel_reason VARCHAR(500),
    driver_name VARCHAR(50),
    driver_mobile VARCHAR(32),
    weight_ticket_no VARCHAR(64),
    weight_ticket_image_url VARCHAR(500),
    weight_ticket_plate_no VARCHAR(32),
    vehicle_plate_no VARCHAR(32),
    plate_matched BOOLEAN,
    vehicle_front_image_url VARCHAR(500),
    vehicle_rear_image_url VARCHAR(500),
    trade_address VARCHAR(255),
    trade_time DATETIME,
    settlement_method VARCHAR(200),
    status TINYINT NOT NULL DEFAULT 0,
    invoice_partner_order_id VARCHAR(64),
    source VARCHAR(20),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_acquisition_no UNIQUE (acquisition_no),
    CONSTRAINT uk_acquisition_client_request UNIQUE (tenant_id, client_request_id)
);

CREATE INDEX IF NOT EXISTS idx_acquisition_payee_id ON icbc_acquisition(payee_id);
CREATE INDEX IF NOT EXISTS idx_acquisition_invoice_order ON icbc_acquisition(invoice_partner_order_id);
 
-- icbc_red_invoice table (Red Invoice / red offset, #14)
CREATE TABLE IF NOT EXISTS icbc_red_invoice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    red_offset_no VARCHAR(35) NOT NULL,
    invoice_order_id BIGINT,
    partner_order_id VARCHAR(35) NOT NULL,
    acquisition_id BIGINT,
    reason VARCHAR(2) NOT NULL,
    amount DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    red_offset_status INTEGER NOT NULL DEFAULT 0,
    red_offset_status_code VARCHAR(2),
    red_invoice_no VARCHAR(20),
    red_invoice_date DATETIME,
    revoke_status VARCHAR(2),
    revoke_time DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_red_offset_no UNIQUE (red_offset_no)
);

CREATE INDEX IF NOT EXISTS idx_red_partner_order_id ON icbc_red_invoice(partner_order_id);

-- icbc_seller_quota_guidance table (出售者额度超限的经营主体登记引导, #12)
CREATE TABLE IF NOT EXISTS icbc_seller_quota_guidance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payee_id BIGINT NOT NULL,
    seller_name VARCHAR(100),
    id_card_no VARCHAR(32),
    trigger_scene VARCHAR(32),
    trigger_biz_no VARCHAR(64),
    used_amount DECIMAL(14,2),
    cap_amount DECIMAL(14,2),
    status TINYINT NOT NULL DEFAULT 0,
    triggered_at DATETIME,
    last_triggered_at DATETIME,
    handled_at DATETIME,
    handle_remark VARCHAR(500),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_quota_guidance_payee_id ON icbc_seller_quota_guidance(payee_id);

-- icbc_tax_declaration table (代办税费申报单, #13)
CREATE TABLE IF NOT EXISTS icbc_tax_declaration (
    id BIGINT NOT NULL AUTO_INCREMENT,
    declaration_no VARCHAR(64),
    period_month VARCHAR(7) NOT NULL,
    declaration_deadline DATE,
    status TINYINT NOT NULL DEFAULT 0,
    seller_count INTEGER,
    over_exempt_seller_count INTEGER,
    total_sales_amount DECIMAL(14,2),
    amount_at_one_percent DECIMAL(14,2),
    amount_at_three_percent DECIMAL(14,2),
    other_amount DECIMAL(14,2),
    vat_amount DECIMAL(14,2),
    surcharge_amount DECIMAL(14,2),
    iit_amount DECIMAL(14,2),
    total_tax_amount DECIMAL(14,2),
    paid_amount DECIMAL(14,2),
    declared_at DATETIME,
    declared_by VARCHAR(64),
    declared_remark VARCHAR(500),
    paid_at DATETIME,
    payment_method VARCHAR(64),
    voucher_no VARCHAR(64),
    voucher_file_url VARCHAR(500),
    data_ready BOOLEAN DEFAULT TRUE,
    missing_data_count INTEGER,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_tax_declaration_period UNIQUE (tenant_id, period_month)
);

-- icbc_tax_declaration_item table (代办税费申报明细, #13)
CREATE TABLE IF NOT EXISTS icbc_tax_declaration_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    declaration_id BIGINT,
    period_month VARCHAR(7),
    payee_id BIGINT,
    seller_name VARCHAR(100),
    id_card_no VARCHAR(32),
    invoice_count INTEGER,
    sales_amount DECIMAL(14,2),
    amount_at_one_percent DECIMAL(14,2),
    amount_at_three_percent DECIMAL(14,2),
    other_amount DECIMAL(14,2),
    cross_tenant_month_amount DECIMAL(14,2),
    vat_exempt BOOLEAN,
    over_exempt BOOLEAN,
    vat_amount DECIMAL(14,2),
    surcharge_amount DECIMAL(14,2),
    iit_amount DECIMAL(14,2),
    total_tax_amount DECIMAL(14,2),
    paid_amount DECIMAL(14,2),
    status TINYINT NOT NULL DEFAULT 0,
    paid_at DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_tax_item_declaration ON icbc_tax_declaration_item(declaration_id);

-- icbc_tax_declaration_invoice table (申报单与发票关联, #13)
CREATE TABLE IF NOT EXISTS icbc_tax_declaration_invoice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    declaration_id BIGINT,
    period_month VARCHAR(7),
    invoice_order_id BIGINT,
    partner_order_id VARCHAR(64),
    invoice_no VARCHAR(64),
    payee_id BIGINT,
    direction VARCHAR(8),
    amount DECIMAL(14,2),
    tax_rate DECIMAL(5,4),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_tax_supplement table (需补缴税费, #13)
CREATE TABLE IF NOT EXISTS icbc_tax_supplement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supplement_no VARCHAR(64),
    declaration_id BIGINT,
    period_month VARCHAR(7),
    payee_id BIGINT,
    seller_name VARCHAR(100),
    reason VARCHAR(500),
    amount_at_one_percent DECIMAL(14,2),
    amount_at_three_percent DECIMAL(14,2),
    amount DECIMAL(14,2),
    status TINYINT NOT NULL DEFAULT 0,
    paid_amount DECIMAL(14,2),
    paid_at DATETIME,
    voucher_no VARCHAR(64),
    voucher_file_url VARCHAR(500),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_settlement_reminder table (汇算清缴提醒, #13)
CREATE TABLE IF NOT EXISTS icbc_settlement_reminder (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payee_id BIGINT,
    seller_name VARCHAR(100),
    id_card_no VARCHAR(32),
    tax_year INTEGER,
    deadline DATE,
    invoice_count INTEGER,
    invoiced_amount DECIMAL(14,2),
    paid_tax_amount DECIMAL(14,2),
    iit_amount DECIMAL(14,2),
    status TINYINT NOT NULL DEFAULT 0,
    reminded_at DATETIME,
    handle_remark VARCHAR(500),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

-- icbc_billing_ledger table (平台计费计量台账, #16；全局表，tenant_id 是被计费租户)
CREATE TABLE IF NOT EXISTS icbc_billing_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    period_month VARCHAR(7) NOT NULL,
    issued_count INTEGER NOT NULL DEFAULT 0,
    reversed_count INTEGER NOT NULL DEFAULT 0,
    billable_count INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    generated_time DATETIME,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_tenant_period ON icbc_billing_ledger(tenant_id, period_month);

-- icbc_natural_person table（平台级自然人主体，#31；全局表，无租户维度）
CREATE TABLE IF NOT EXISTS icbc_natural_person (
    id BIGINT NOT NULL AUTO_INCREMENT,
    out_user_id VARCHAR(64) NOT NULL,
    name VARCHAR(100),
    id_card_no VARCHAR(32) NOT NULL,
    mobile VARCHAR(32),
    real_name_status TINYINT DEFAULT 0,
    real_name_msg VARCHAR(500),
    real_name_time DATETIME,
    status TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_natural_person_id_card_no UNIQUE (id_card_no),
    CONSTRAINT uk_natural_person_out_user_id UNIQUE (out_user_id)
);

-- icbc_natural_person_login table（自然人主体 ↔ 登录凭证绑定，#31；全局表，无租户维度）
CREATE TABLE IF NOT EXISTS icbc_natural_person_login (
    id BIGINT NOT NULL AUTO_INCREMENT,
    natural_person_id BIGINT NOT NULL,
    member_user_id BIGINT NOT NULL,
    bound_at DATETIME,
    bind_source VARCHAR(20),
    remark VARCHAR(500),
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_natural_person_login UNIQUE (natural_person_id, member_user_id)
);

-- icbc_settlement table（结算单，#33；租户表）
CREATE TABLE IF NOT EXISTS icbc_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    settlement_no VARCHAR(64) NOT NULL,
    payee_id BIGINT NOT NULL,
    natural_person_id BIGINT,
    seller_name VARCHAR(100),
    seller_mobile VARCHAR(32),
    batch_key VARCHAR(64),
    generate_time DATETIME,
    generated_by BIGINT,
    current_version_id BIGINT,
    current_version_no INT,
    confirm_status TINYINT NOT NULL DEFAULT 0,
    confirm_time DATETIME,
    confirm_ip VARCHAR(64),
    confirm_device VARCHAR(255),
    confirm_hash VARCHAR(64),
    confirm_member_user_id BIGINT,
    dispute_reason VARCHAR(10),
    dispute_note VARCHAR(500),
    dispute_time DATETIME,
    dispute_count INT NOT NULL DEFAULT 0,
    enterprise_reply_note VARCHAR(500),
    enterprise_reply_time DATETIME,
    deadline_time DATETIME,
    offline_sign_file_url VARCHAR(500),
    offline_sign_handler VARCHAR(100),
    offline_sign_time DATETIME,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_settlement_no UNIQUE (settlement_no)
);

-- icbc_settlement_version table（结算单版本，#33；租户表）
CREATE TABLE IF NOT EXISTS icbc_settlement_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    settlement_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    snapshot_json TEXT,
    snapshot_hash VARCHAR(64),
    total_settlement_weight DECIMAL(14,4),
    total_amount DECIMAL(14,2),
    acquisition_count INT,
    change_reason VARCHAR(500),
    changed_by VARCHAR(64),
    source VARCHAR(32),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_settlement_version UNIQUE (tenant_id, settlement_id, version_no)
);

-- icbc_station table（场站与场站二维码，#34；租户表）
CREATE TABLE IF NOT EXISTS icbc_station (
    id BIGINT NOT NULL AUTO_INCREMENT,
    station_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    contact_mobile VARCHAR(32),
    open_status INT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_station_code UNIQUE (station_code)
);

-- icbc_appointment table（到站预约，#35；租户表；不是订单）
CREATE TABLE IF NOT EXISTS icbc_appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_no VARCHAR(64) NOT NULL,
    natural_person_id BIGINT NOT NULL,
    payee_id BIGINT,
    seller_name VARCHAR(100),
    station_id BIGINT NOT NULL,
    station_code VARCHAR(64),
    station_name VARCHAR(100),
    goods_config_id BIGINT NOT NULL,
    category_name VARCHAR(100),
    unit VARCHAR(32),
    expected_quantity DECIMAL(14,4),
    plate_no VARCHAR(32),
    expected_arrival_time DATETIME,
    status INT NOT NULL DEFAULT 0,
    arrived_at DATETIME,
    acquisition_id BIGINT,
    cancelled_at DATETIME,
    cancel_reason VARCHAR(255),
    no_show_reason VARCHAR(255),
    remark VARCHAR(500),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_appointment_no UNIQUE (appointment_no)
);

-- icbc_seller_notify table（出售者触达记录，#36；租户表）
CREATE TABLE IF NOT EXISTS icbc_seller_notify (
    id BIGINT NOT NULL AUTO_INCREMENT,
    biz_type VARCHAR(32) NOT NULL,
    biz_key VARCHAR(128) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    natural_person_id BIGINT,
    payee_id BIGINT,
    seller_name VARCHAR(100),
    mobile VARCHAR(32),
    status INT NOT NULL DEFAULT 0,
    sms_log_id BIGINT,
    content VARCHAR(500),
    link VARCHAR(500),
    error_msg VARCHAR(500),
    send_time DATETIME,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_seller_notify_dedup UNIQUE (tenant_id, biz_type, biz_key)
);

-- icbc_notify_setting table（租户级触达设置，#36；租户表）
CREATE TABLE IF NOT EXISTS icbc_notify_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sms_enabled TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    tenant_id BIGINT NOT NULL DEFAULT 0,
    creator VARCHAR(64) DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_notify_setting_tenant UNIQUE (tenant_id)
);
