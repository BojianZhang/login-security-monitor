-- 企业级安全邮件管理系统数据库结构
-- 集成登录安全监控和邮件服务管理

CREATE DATABASE IF NOT EXISTS secure_email_system;
USE secure_email_system;

-- 用户表（扩展支持邮件系统）
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    is_admin BOOLEAN DEFAULT FALSE,
    is_email_admin BOOLEAN DEFAULT FALSE,  -- 邮件管理员权限
    storage_quota BIGINT DEFAULT 1073741824, -- 存储配额（字节，默认1GB）
    storage_used BIGINT DEFAULT 0,        -- 已使用存储
    last_login TIMESTAMP NULL,
    email_password_hash VARCHAR(255),     -- 邮件系统密码（可与系统密码不同）
    email_enabled BOOLEAN DEFAULT TRUE,   -- 邮件功能启用状态
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_email_enabled (email_enabled)
);

-- 登录记录表
CREATE TABLE login_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    country VARCHAR(100),
    region VARCHAR(100),
    city VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    timezone VARCHAR(50),
    isp VARCHAR(100),
    user_agent TEXT,
    device_type VARCHAR(50),
    browser VARCHAR(50),
    os VARCHAR(50),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    login_status ENUM('SUCCESS', 'FAILED', 'BLOCKED') DEFAULT 'SUCCESS',
    risk_score INT DEFAULT 0,
    is_suspicious BOOLEAN DEFAULT FALSE,
    session_id VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_login_time (login_time),
    INDEX idx_risk_score (risk_score),
    INDEX idx_suspicious (is_suspicious)
);

-- IP地理位置缓存表
CREATE TABLE ip_geo_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip_address VARCHAR(45) UNIQUE NOT NULL,
    country VARCHAR(100),
    region VARCHAR(100),
    city VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    timezone VARCHAR(50),
    isp VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ip (ip_address)
);

-- 用户常用位置表
CREATE TABLE user_locations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    country VARCHAR(100),
    region VARCHAR(100),
    city VARCHAR(100),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    login_count INT DEFAULT 1,
    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_trusted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    UNIQUE KEY unique_user_location (user_id, country, region, city)
);

-- 安全警报表
CREATE TABLE security_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    login_record_id BIGINT,
    alert_type ENUM('ANOMALOUS_LOCATION', 'MULTIPLE_LOCATIONS', 'SUSPICIOUS_DEVICE', 'BRUTE_FORCE', 'HIGH_RISK_IP') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    risk_score INT DEFAULT 0,
    status ENUM('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE') DEFAULT 'OPEN',
    handled_by BIGINT NULL,
    handled_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (login_record_id) REFERENCES login_records(id) ON DELETE SET NULL,
    FOREIGN KEY (handled_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 通知记录表
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_id BIGINT NOT NULL,
    recipient_email VARCHAR(100) NOT NULL,
    notification_type ENUM('EMAIL', 'SMS', 'WEBHOOK') DEFAULT 'EMAIL',
    subject VARCHAR(200),
    content TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'SENT', 'FAILED') DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    FOREIGN KEY (alert_id) REFERENCES security_alerts(id) ON DELETE CASCADE,
    INDEX idx_alert_id (alert_id),
    INDEX idx_status (status)
);

-- 系统配置表
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description VARCHAR(500),
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 用户设备指纹表
CREATE TABLE device_fingerprints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    fingerprint_hash VARCHAR(255) NOT NULL,
    user_agent TEXT,
    screen_resolution VARCHAR(20),
    timezone VARCHAR(50),
    language VARCHAR(10),
    plugins TEXT,
    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    trust_score INT DEFAULT 50,
    is_trusted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_fingerprint (fingerprint_hash),
    UNIQUE KEY unique_user_fingerprint (user_id, fingerprint_hash)
);

-- 插入默认配置
INSERT INTO system_config (config_key, config_value, description) VALUES
-- 安全监控配置
('max_distance_km', '500', '触发异地登录警报的最大距离（公里）'),
('time_window_minutes', '60', '检测多地登录的时间窗口（分钟）'),
('risk_threshold', '70', '触发高风险警报的分数阈值'),
('email_notifications_enabled', 'true', '是否启用邮件通知'),
('admin_emails', 'admin@example.com', '管理员邮箱地址（逗号分隔）'),
('geo_api_key', '', 'IP地理位置API密钥'),

-- 邮件系统配置
('smtp_host', 'localhost', 'SMTP服务器地址'),
('smtp_port', '587', 'SMTP端口'),
('smtp_username', '', 'SMTP用户名'),
('smtp_password', '', 'SMTP密码'),
('smtp_encryption', 'STARTTLS', 'SMTP加密方式'),
('imap_host', 'localhost', 'IMAP服务器地址'),
('imap_port', '993', 'IMAP端口'),
('imap_encryption', 'SSL', 'IMAP加密方式'),
('pop3_host', 'localhost', 'POP3服务器地址'),
('pop3_port', '995', 'POP3端口'),
('pop3_encryption', 'SSL', 'POP3加密方式'),
('default_storage_quota', '1073741824', '默认存储配额（字节）'),
('max_attachment_size', '52428800', '最大附件大小（字节，50MB）'),
('spam_threshold', '5.0', '垃圾邮件阈值分数'),
('enable_antivirus', 'true', '启用病毒扫描'),
('enable_dkim', 'true', '启用DKIM签名'),
('enable_spf', 'true', '启用SPF验证'),
('enable_dmarc', 'true', '启用DMARC策略'),
('mail_retention_days', '365', '邮件保留天数（0表示永久保留）');

-- 插入示例管理员用户 (密码: admin123)
INSERT INTO users (username, email, password_hash, full_name, is_admin, is_email_admin, storage_quota) VALUES 
('admin', 'admin@example.com', '$2a$10$rQ8QwPzKpEWIjsH8K9v.dOYxqb5L5H9H5v5v5v5v5v5v5v5v5v5v5v', '系统管理员', TRUE, TRUE, 10737418240);

-- 插入默认域名
INSERT INTO email_domains (domain_name, is_active, is_primary) VALUES 
('example.com', TRUE, TRUE);

-- 插入默认邮件文件夹
INSERT INTO email_folders (user_id, folder_name, folder_type) VALUES 
(1, 'INBOX', 'INBOX'),
(1, 'SENT', 'SENT'),
(1, 'DRAFTS', 'DRAFT'),
(1, 'TRASH', 'TRASH'),
(1, 'SPAM', 'SPAM');

-- 插入管理员邮箱别名
INSERT INTO email_aliases (user_id, alias_email, domain_id, is_active) VALUES 
(1, 'admin', 1, TRUE),
(1, 'postmaster', 1, TRUE),
(1, 'support', 1, TRUE);

-- 邮件域名表
CREATE TABLE email_domains (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_name VARCHAR(255) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_primary BOOLEAN DEFAULT FALSE,
    mx_record VARCHAR(255),
    dkim_selector VARCHAR(100),
    dkim_private_key TEXT,
    dkim_public_key TEXT,
    spf_record TEXT,
    dmarc_policy VARCHAR(50) DEFAULT 'none',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain_active (domain_name, is_active)
);

-- 邮箱别名表
CREATE TABLE email_aliases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    alias_email VARCHAR(255) NOT NULL,
    domain_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_catch_all BOOLEAN DEFAULT FALSE,
    forward_to VARCHAR(255), -- 转发地址（可选）
    display_name VARCHAR(100), -- 自定义显示名称，用于与外部平台保持一致
    external_alias_id VARCHAR(100), -- 外部平台的别名ID，用于同步
    description TEXT, -- 别名描述
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (domain_id) REFERENCES email_domains(id) ON DELETE CASCADE,
    UNIQUE KEY unique_alias (alias_email, domain_id),
    UNIQUE KEY unique_external_alias (external_alias_id),
    INDEX idx_user_id (user_id),
    INDEX idx_domain_id (domain_id),
    INDEX idx_active (is_active),
    INDEX idx_display_name (display_name),
    INDEX idx_external_alias_id (external_alias_id)
);

-- 邮件文件夹表
CREATE TABLE email_folders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    folder_name VARCHAR(100) NOT NULL,
    folder_type ENUM('INBOX', 'SENT', 'DRAFT', 'TRASH', 'SPAM', 'CUSTOM') DEFAULT 'CUSTOM',
    parent_id BIGINT NULL,
    message_count INT DEFAULT 0,
    unread_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES email_folders(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_folder_type (folder_type),
    UNIQUE KEY unique_user_folder (user_id, folder_name)
);

-- 邮件消息表
CREATE TABLE email_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    message_id VARCHAR(255) UNIQUE NOT NULL, -- RFC 2822 Message-ID
    thread_id VARCHAR(255),
    subject VARCHAR(998), -- RFC 2822 最大主题长度
    from_address VARCHAR(320) NOT NULL,
    to_addresses TEXT, -- JSON格式存储多个收件人
    cc_addresses TEXT, -- 抄送地址
    bcc_addresses TEXT, -- 密送地址
    reply_to VARCHAR(320),
    body_text LONGTEXT,
    body_html LONGTEXT,
    message_size BIGINT DEFAULT 0,
    is_read BOOLEAN DEFAULT FALSE,
    is_starred BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_spam BOOLEAN DEFAULT FALSE,
    priority_level TINYINT DEFAULT 3, -- 1=高, 3=正常, 5=低
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES email_folders(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_folder_id (folder_id),
    INDEX idx_message_id (message_id),
    INDEX idx_thread_id (thread_id),
    INDEX idx_received_at (received_at DESC),
    INDEX idx_read_status (is_read),
    INDEX idx_from_address (from_address),
    FULLTEXT idx_subject_body (subject, body_text)
);

-- 邮件附件表
CREATE TABLE email_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT DEFAULT 0,
    file_hash VARCHAR(64), -- SHA-256 hash
    storage_path VARCHAR(500), -- 文件存储路径
    is_inline BOOLEAN DEFAULT FALSE,
    content_id VARCHAR(255), -- 用于内嵌图片
    is_quarantined BOOLEAN DEFAULT FALSE, -- 是否已隔离
    quarantine_reason VARCHAR(500), -- 隔离原因
    virus_scan_status VARCHAR(20), -- 病毒扫描状态
    last_scanned_at TIMESTAMP NULL, -- 最后扫描时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES email_messages(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_filename (filename),
    INDEX idx_file_hash (file_hash),
    INDEX idx_quarantined (is_quarantined),
    INDEX idx_scan_status (virus_scan_status),
    INDEX idx_last_scanned (last_scanned_at DESC)
);

-- 邮件规则表（过滤器）
CREATE TABLE email_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    priority_order INT DEFAULT 0,
    conditions JSON, -- 规则条件（JSON格式）
    actions JSON,    -- 执行动作（JSON格式）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_active_priority (is_active, priority_order)
);

-- 邮件统计表
CREATE TABLE email_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    domain_id BIGINT,
    stat_date DATE NOT NULL,
    messages_received INT DEFAULT 0,
    messages_sent INT DEFAULT 0,
    messages_blocked INT DEFAULT 0,
    storage_used BIGINT DEFAULT 0,
    unique_senders INT DEFAULT 0,
    spam_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (domain_id) REFERENCES email_domains(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, stat_date),
    INDEX idx_stat_date (stat_date),
    INDEX idx_domain_date (domain_id, stat_date)
);

-- 系统邮件队列表
CREATE TABLE email_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_address VARCHAR(320) NOT NULL,
    to_address VARCHAR(320) NOT NULL,
    subject VARCHAR(998),
    body_text LONGTEXT,
    body_html LONGTEXT,
    priority TINYINT DEFAULT 3,
    max_attempts INT DEFAULT 3,
    attempt_count INT DEFAULT 0,
    status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
    scheduled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_priority (status, priority),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_to_address (to_address)
);

-- 创建索引以优化查询性能
CREATE INDEX idx_login_user_time ON login_records(user_id, login_time DESC);
CREATE INDEX idx_alert_severity_status ON security_alerts(severity, status);
CREATE INDEX idx_location_user_trusted ON user_locations(user_id, is_trusted);
CREATE INDEX idx_messages_user_folder_received ON email_messages(user_id, folder_id, received_at DESC);
CREATE INDEX idx_messages_unread ON email_messages(user_id, is_read, received_at DESC);
CREATE INDEX idx_aliases_user_active ON email_aliases(user_id, is_active);

-- 自动回复设置表
CREATE TABLE auto_reply_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_id BIGINT NOT NULL,
    is_enabled BOOLEAN DEFAULT FALSE,
    reply_subject VARCHAR(200) NOT NULL,
    reply_content TEXT NOT NULL,
    start_date TIMESTAMP NULL,
    end_date TIMESTAMP NULL,
    only_external BOOLEAN DEFAULT FALSE,
    max_replies_per_sender INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (alias_id) REFERENCES email_aliases(id) ON DELETE CASCADE,
    INDEX idx_alias_id (alias_id),
    INDEX idx_enabled (is_enabled)
);

-- 自动回复历史表
CREATE TABLE auto_reply_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    settings_id BIGINT NOT NULL,
    from_address VARCHAR(320) NOT NULL,
    to_address VARCHAR(320) NOT NULL,
    original_subject VARCHAR(998),
    reply_subject VARCHAR(200) NOT NULL,
    reply_sent BOOLEAN DEFAULT FALSE,
    reply_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (settings_id) REFERENCES auto_reply_settings(id) ON DELETE CASCADE,
    INDEX idx_settings_id (settings_id),
    INDEX idx_from_address (from_address),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_settings_from (settings_id, from_address)
);

-- 邮件转发规则表
CREATE TABLE email_forwarding_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alias_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    forward_to VARCHAR(320) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    forward_subject VARCHAR(200),
    keep_original BOOLEAN DEFAULT TRUE,
    conditions TEXT,
    continue_processing BOOLEAN DEFAULT FALSE,
    forward_count BIGINT DEFAULT 0,
    last_forward_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (alias_id) REFERENCES email_aliases(id) ON DELETE CASCADE,
    INDEX idx_alias_id (alias_id),
    INDEX idx_active_priority (is_active, priority DESC),
    INDEX idx_forward_to (forward_to),
    UNIQUE KEY unique_alias_rule (alias_id, rule_name)
);

-- SSL/TLS证书管理表
CREATE TABLE ssl_certificates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_id BIGINT NOT NULL,
    certificate_name VARCHAR(100) NOT NULL,
    certificate_type ENUM('FREE_LETSENCRYPT', 'FREE_ZEROSSL', 'USER_UPLOADED', 'SELF_SIGNED') NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'EXPIRED', 'REVOKED', 'ERROR', 'RENEWAL_NEEDED') DEFAULT 'PENDING',
    domain_name VARCHAR(255) NOT NULL,
    subject_alternative_names TEXT,
    issuer VARCHAR(255),
    serial_number VARCHAR(100),
    issued_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    certificate_path VARCHAR(500),
    private_key_path VARCHAR(500),
    certificate_chain_path VARCHAR(500),
    auto_renew BOOLEAN DEFAULT TRUE,
    renewal_days_before INT DEFAULT 30,
    last_renewal_attempt TIMESTAMP NULL,
    last_error TEXT,
    challenge_type VARCHAR(50),
    acme_account_email VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (domain_id) REFERENCES email_domains(id) ON DELETE CASCADE,
    INDEX idx_domain_id (domain_id),
    INDEX idx_domain_name (domain_name),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    INDEX idx_auto_renew (auto_renew),
    INDEX idx_certificate_type (certificate_type),
    INDEX idx_active_status (is_active, status),
    INDEX idx_renewal_needed (auto_renew, expires_at, status)
);

-- SSL证书续期日志表
CREATE TABLE ssl_renewal_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certificate_id BIGINT NOT NULL,
    renewal_type ENUM('MANUAL', 'AUTOMATIC', 'FORCED') DEFAULT 'AUTOMATIC',
    status ENUM('SUCCESS', 'FAILED', 'PENDING') DEFAULT 'PENDING',
    old_expires_at TIMESTAMP NULL,
    new_expires_at TIMESTAMP NULL,
    error_message TEXT,
    attempt_duration_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (certificate_id) REFERENCES ssl_certificates(id) ON DELETE CASCADE,
    INDEX idx_certificate_id (certificate_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_renewal_type (renewal_type)
);

-- SSL证书监控表
CREATE TABLE ssl_certificate_monitoring (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certificate_id BIGINT NOT NULL,
    monitoring_enabled BOOLEAN DEFAULT TRUE,
    check_interval_hours INT DEFAULT 24,
    last_check_at TIMESTAMP NULL,
    next_check_at TIMESTAMP NULL,
    check_status ENUM('OK', 'WARNING', 'ERROR') DEFAULT 'OK',
    days_until_expiry INT DEFAULT -1,
    notification_sent BOOLEAN DEFAULT FALSE,
    warning_threshold_days INT DEFAULT 30,
    critical_threshold_days INT DEFAULT 7,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (certificate_id) REFERENCES ssl_certificates(id) ON DELETE CASCADE,
    INDEX idx_certificate_id (certificate_id),
    INDEX idx_monitoring_enabled (monitoring_enabled),
    INDEX idx_next_check (next_check_at),
    INDEX idx_check_status (check_status),
    INDEX idx_days_until_expiry (days_until_expiry)
);

-- SSL证书使用记录表（用于统计和审计）
CREATE TABLE ssl_certificate_usage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certificate_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL, -- 如：nginx, apache, postfix等
    service_config_path VARCHAR(500),
    usage_start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usage_end_date TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (certificate_id) REFERENCES ssl_certificates(id) ON DELETE CASCADE,
    INDEX idx_certificate_id (certificate_id),
    INDEX idx_service_name (service_name),
    INDEX idx_is_active (is_active),
    INDEX idx_usage_dates (usage_start_date, usage_end_date)
);

-- 邮件搜索历史表
CREATE TABLE email_search_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    search_query VARCHAR(500) NOT NULL,
    search_type ENUM('FULLTEXT', 'ADVANCED') DEFAULT 'FULLTEXT',
    result_count BIGINT DEFAULT 0,
    search_filters TEXT, -- JSON格式的搜索过滤条件
    execution_time_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_search_query (search_query),
    INDEX idx_search_type (search_type),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_user_created (user_id, created_at DESC)
);

-- 病毒定义表
CREATE TABLE virus_definitions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    virus_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    signature_type ENUM('HASH', 'PATTERN', 'BYTE_SEQUENCE', 'FILE_EXTENSION', 'MIME_TYPE') NOT NULL,
    signature_value VARCHAR(1000) NOT NULL,
    hash_signature VARCHAR(64),
    pattern_signature VARCHAR(1000),
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    is_active BOOLEAN DEFAULT TRUE,
    detection_count BIGINT DEFAULT 0,
    last_detected_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_virus_name (virus_name),
    INDEX idx_signature_type (signature_type),
    INDEX idx_hash_signature (hash_signature),
    INDEX idx_severity (severity),
    INDEX idx_is_active (is_active),
    INDEX idx_detection_count (detection_count DESC),
    INDEX idx_last_detected (last_detected_at DESC),
    INDEX idx_active_type (is_active, signature_type),
    UNIQUE KEY unique_virus_name (virus_name)
);

-- 病毒扫描日志表
CREATE TABLE virus_scan_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    scan_status VARCHAR(20) NOT NULL,
    threat_found BOOLEAN DEFAULT FALSE,
    threats_found INT DEFAULT 0,
    files_scanned INT DEFAULT 0,
    scan_details VARCHAR(1000),
    processing_time_ms BIGINT,
    quarantined_files INT DEFAULT 0,
    scan_engine_version VARCHAR(50),
    virus_definitions_version VARCHAR(50),
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES email_messages(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_scan_status (scan_status),
    INDEX idx_threat_found (threat_found),
    INDEX idx_scanned_at (scanned_at DESC),
    INDEX idx_threats_found (threats_found DESC),
    INDEX idx_processing_time (processing_time_ms DESC),
    INDEX idx_message_scan_date (message_id, scanned_at DESC)
);

-- 邮件验证日志表
CREATE TABLE email_validation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    validation_status VARCHAR(20) NOT NULL,
    spf_status VARCHAR(20),
    spf_record VARCHAR(1000),
    dkim_status VARCHAR(20),
    dkim_domain VARCHAR(255),
    dkim_selector VARCHAR(100),
    dmarc_status VARCHAR(20),
    dmarc_record VARCHAR(1000),
    dmarc_policy VARCHAR(20),
    sender_ip VARCHAR(45),
    validation_details VARCHAR(2000),
    processing_time_ms BIGINT,
    validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES email_messages(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_validation_status (validation_status),
    INDEX idx_spf_status (spf_status),
    INDEX idx_dkim_status (dkim_status),
    INDEX idx_dmarc_status (dmarc_status),
    INDEX idx_sender_ip (sender_ip),
    INDEX idx_validated_at (validated_at DESC),
    INDEX idx_dkim_domain (dkim_domain),
    INDEX idx_dmarc_policy (dmarc_policy),
    INDEX idx_validation_date (validated_at, validation_status)
);

-- DNS黑名单检查日志表
CREATE TABLE dns_blacklist_check_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT,
    ip_address VARCHAR(45) NOT NULL,
    check_status VARCHAR(20) NOT NULL,
    hit_count INT DEFAULT 0,
    total_weight DOUBLE DEFAULT 0.0,
    risk_level VARCHAR(20),
    blacklists_checked INT DEFAULT 0,
    check_details VARCHAR(2000),
    processing_time_ms BIGINT,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES email_messages(id) ON DELETE SET NULL,
    INDEX idx_message_id (message_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_check_status (check_status),
    INDEX idx_risk_level (risk_level),
    INDEX idx_checked_at (checked_at DESC),
    INDEX idx_hit_count (hit_count DESC),
    INDEX idx_total_weight (total_weight DESC),
    INDEX idx_ip_status (ip_address, check_status),
    INDEX idx_check_date (checked_at, check_status)
);

-- Sieve过滤器表
CREATE TABLE sieve_filters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    filter_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    filter_script TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    filter_type ENUM('USER_DEFINED', 'SYSTEM_DEFAULT', 'SPAM_FILTER', 'VIRUS_FILTER', 'VACATION', 'FORWARD') DEFAULT 'USER_DEFINED',
    hit_count BIGINT DEFAULT 0,
    last_hit_at TIMESTAMP NULL,
    syntax_version VARCHAR(10) DEFAULT '1.0',
    error_count INT DEFAULT 0,
    last_error VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_filter_name (filter_name),
    INDEX idx_is_active (is_active),
    INDEX idx_priority (priority),
    INDEX idx_filter_type (filter_type),
    INDEX idx_hit_count (hit_count DESC),
    INDEX idx_user_priority (user_id, priority),
    UNIQUE KEY unique_user_filter (user_id, filter_name)
);

-- Sieve过滤器执行日志表
CREATE TABLE sieve_filter_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    filter_id BIGINT NOT NULL,
    filter_matched BOOLEAN DEFAULT FALSE,
    executed_action VARCHAR(50),
    action_parameters VARCHAR(1000),
    execution_time_ms BIGINT,
    error_occurred BOOLEAN DEFAULT FALSE,
    error_message VARCHAR(1000),
    script_version VARCHAR(10),
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES email_messages(id) ON DELETE CASCADE,
    FOREIGN KEY (filter_id) REFERENCES sieve_filters(id) ON DELETE CASCADE,
    INDEX idx_message_id (message_id),
    INDEX idx_filter_id (filter_id),
    INDEX idx_filter_matched (filter_matched),
    INDEX idx_executed_action (executed_action),
    INDEX idx_executed_at (executed_at DESC),
    INDEX idx_error_occurred (error_occurred),
    INDEX idx_execution_time (execution_time_ms DESC),
    INDEX idx_filter_execution (filter_id, executed_at DESC)
);

-- 邮件群组表
CREATE TABLE email_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_id BIGINT NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    group_email VARCHAR(320) NOT NULL,
    display_name VARCHAR(100),
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    group_type ENUM('DISTRIBUTION', 'MAILING_LIST', 'ANNOUNCEMENT', 'DEPARTMENT', 'PROJECT', 'SECURITY') DEFAULT 'DISTRIBUTION',
    max_members INT,
    allow_external_senders BOOLEAN DEFAULT FALSE,
    require_moderation BOOLEAN DEFAULT FALSE,
    auto_subscribe BOOLEAN DEFAULT FALSE,
    message_count BIGINT DEFAULT 0,
    last_message_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (domain_id) REFERENCES email_domains(id) ON DELETE CASCADE,
    INDEX idx_domain_id (domain_id),
    INDEX idx_group_email (group_email),
    INDEX idx_group_name (group_name),
    INDEX idx_is_active (is_active),
    INDEX idx_group_type (group_type),
    INDEX idx_message_count (message_count DESC),
    INDEX idx_last_message (last_message_at DESC),
    UNIQUE KEY unique_domain_group (domain_id, group_name),
    UNIQUE KEY unique_group_email (group_email)
);

-- 邮件群组成员表
CREATE TABLE email_group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT,
    external_email VARCHAR(320),
    member_name VARCHAR(100),
    member_role ENUM('OWNER', 'ADMIN', 'MODERATOR', 'MEMBER', 'READONLY') DEFAULT 'MEMBER',
    is_active BOOLEAN DEFAULT TRUE,
    can_send BOOLEAN DEFAULT TRUE,
    can_receive BOOLEAN DEFAULT TRUE,
    is_moderator BOOLEAN DEFAULT FALSE,
    subscription_type ENUM('NORMAL', 'DIGEST', 'NOMAIL', 'SUSPENDED') DEFAULT 'NORMAL',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NULL,
    FOREIGN KEY (group_id) REFERENCES email_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_external_email (external_email),
    INDEX idx_member_role (member_role),
    INDEX idx_is_active (is_active),
    INDEX idx_is_moderator (is_moderator),
    INDEX idx_subscription_type (subscription_type),
    INDEX idx_joined_at (joined_at DESC),
    INDEX idx_last_activity (last_activity_at DESC),
    INDEX idx_group_user (group_id, user_id),
    UNIQUE KEY unique_group_user (group_id, user_id),
    UNIQUE KEY unique_group_external (group_id, external_email)
);

-- Catch-All邮箱表
CREATE TABLE catch_all_mailboxes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain_id BIGINT NOT NULL,
    target_user_id BIGINT,
    target_email VARCHAR(320),
    is_active BOOLEAN DEFAULT TRUE,
    catch_all_type ENUM('DELIVER', 'FORWARD', 'DISCARD', 'BOUNCE', 'QUARANTINE') DEFAULT 'DELIVER',
    priority INT DEFAULT 0,
    max_daily_messages INT,
    message_count BIGINT DEFAULT 0,
    daily_message_count INT DEFAULT 0,
    last_message_date TIMESTAMP NULL,
    filter_spam BOOLEAN DEFAULT TRUE,
    filter_virus BOOLEAN DEFAULT TRUE,
    auto_reply_enabled BOOLEAN DEFAULT FALSE,
    auto_reply_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (domain_id) REFERENCES email_domains(id) ON DELETE CASCADE,
    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_domain_id (domain_id),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_target_email (target_email),
    INDEX idx_is_active (is_active),
    INDEX idx_catch_all_type (catch_all_type),
    INDEX idx_priority (priority),
    INDEX idx_message_count (message_count DESC),
    INDEX idx_last_message (last_message_date DESC),
    INDEX idx_domain_priority (domain_id, priority)
);