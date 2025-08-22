-- 初始化数据脚本
-- 插入默认管理员用户和测试数据

-- 插入默认管理员用户
-- 密码：admin123 (BCrypt加密)
INSERT INTO users (username, password, email, full_name, is_admin, is_active, created_at, updated_at) 
VALUES (
    'admin', 
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqyc3Vo6STKa2MaNu.VQF.G', 
    'admin@example.com', 
    'System Administrator', 
    true, 
    true, 
    NOW(), 
    NOW()
);

-- 插入测试普通用户
-- 密码：user123 (BCrypt加密)
INSERT INTO users (username, password, email, full_name, is_admin, is_active, created_at, updated_at) 
VALUES (
    'testuser', 
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 
    'testuser@example.com', 
    'Test User', 
    false, 
    true, 
    NOW(), 
    NOW()
);

-- 插入另一个测试用户
-- 密码：demo123 (BCrypt加密)
INSERT INTO users (username, password, email, full_name, is_admin, is_active, created_at, updated_at) 
VALUES (
    'demo', 
    '$2a$12$xLd67LVQ1LBWKjq9yYVtIu6VZ.L1UyQoKGcmjQvQZT6sA4xVZj3yC', 
    'demo@example.com', 
    'Demo User', 
    false, 
    true, 
    NOW(), 
    NOW()
);

-- 插入测试用户常用地点数据
INSERT INTO user_locations (user_id, latitude, longitude, country, region, city, trust_score, visit_count, first_seen, last_seen, created_at, updated_at)
VALUES 
    (1, 39.9042, 116.4074, 'China', 'Beijing', 'Beijing', 95.0, 50, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW(), NOW(), NOW()),
    (2, 31.2304, 121.4737, 'China', 'Shanghai', 'Shanghai', 85.0, 25, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), NOW()),
    (3, 22.3193, 114.1694, 'China', 'Hong Kong', 'Hong Kong', 75.0, 10, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), NOW());

-- 插入一些示例登录记录
INSERT INTO login_records (user_id, ip_address, user_agent, login_time, login_status, browser, os, device_type, country, region, city, latitude, longitude, timezone, isp, risk_score, is_suspicious, session_id, created_at, updated_at)
VALUES 
    -- 管理员的正常登录
    (1, '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', NOW(), 'SUCCESS', 'Chrome', 'Windows 10', 'Desktop', 'China', 'Beijing', 'Beijing', 39.9042, 116.4074, 'Asia/Shanghai', 'China Telecom', 15, false, UUID(), NOW(), NOW()),
    
    -- 测试用户的正常登录
    (2, '192.168.1.101', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'SUCCESS', 'Safari', 'macOS', 'Desktop', 'China', 'Shanghai', 'Shanghai', 31.2304, 121.4737, 'Asia/Shanghai', 'China Unicom', 20, false, UUID(), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    
    -- 异地可疑登录
    (2, '203.0.113.1', 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'SUCCESS', 'Firefox', 'Linux', 'Desktop', 'United States', 'California', 'Los Angeles', 34.0522, -118.2437, 'America/Los_Angeles', 'Unknown ISP', 75, true, UUID(), DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    
    -- 失败的登录尝试
    (3, '198.51.100.1', 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'FAILED', 'Safari', 'iOS', 'Mobile', 'United States', 'New York', 'New York', 40.7128, -74.0060, 'America/New_York', 'Unknown ISP', 85, true, UUID(), DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- 插入一些示例安全警报
INSERT INTO security_alerts (user_id, login_record_id, alert_type, severity, status, message, details, created_at, updated_at)
VALUES 
    -- 异地登录警报
    (2, 3, 'LOCATION_ANOMALY', 'HIGH', 'OPEN', 'User testuser logged in from an unusual location', 'Login from Los Angeles, CA while user typically logs in from Shanghai', NOW(), NOW()),
    
    -- 高风险登录警报
    (3, 4, 'HIGH_RISK_SCORE', 'MEDIUM', 'OPEN', 'High risk score detected for user demo', 'Login attempt from New York with risk score of 85', DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- 创建索引以提高查询性能
CREATE INDEX idx_login_records_user_time ON login_records (user_id, login_time DESC);
CREATE INDEX idx_login_records_ip ON login_records (ip_address);
CREATE INDEX idx_login_records_suspicious ON login_records (is_suspicious, login_time DESC);
CREATE INDEX idx_security_alerts_user_status ON security_alerts (user_id, status);
CREATE INDEX idx_security_alerts_created ON security_alerts (created_at DESC);
CREATE INDEX idx_user_locations_user ON user_locations (user_id);

-- 创建视图以简化常用查询
CREATE VIEW v_recent_suspicious_logins AS
SELECT 
    lr.id,
    u.username,
    lr.ip_address,
    lr.country,
    lr.city,
    lr.login_time,
    lr.risk_score,
    lr.is_suspicious
FROM login_records lr
JOIN users u ON lr.user_id = u.id
WHERE lr.is_suspicious = true
    AND lr.login_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY lr.login_time DESC;

CREATE VIEW v_user_login_summary AS
SELECT 
    u.id,
    u.username,
    u.full_name,
    COUNT(lr.id) as total_logins,
    COUNT(CASE WHEN lr.login_status = 'SUCCESS' THEN 1 END) as successful_logins,
    COUNT(CASE WHEN lr.login_status = 'FAILED' THEN 1 END) as failed_logins,
    COUNT(CASE WHEN lr.is_suspicious = true THEN 1 END) as suspicious_logins,
    MAX(lr.login_time) as last_login,
    AVG(lr.risk_score) as avg_risk_score
FROM users u
LEFT JOIN login_records lr ON u.id = lr.user_id
GROUP BY u.id, u.username, u.full_name;