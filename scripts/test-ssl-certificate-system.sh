#!/bin/bash

# SSL/TLS证书管理系统测试脚本
# 用于测试免费证书申请、用户证书上传、自动续期等功能

BASE_URL="http://localhost:8080/api"
AUTH_TOKEN=""  # 需要设置有效的JWT token

echo "SSL/TLS证书管理系统功能测试"
echo "============================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local endpoint="$1"
    local method="$2"
    local data="$3"
    local description="$4"
    
    echo -e "${YELLOW}测试: $description${NC}"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\nSTATUS:%{http_code}" \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            -H "Content-Type: application/json" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\nSTATUS:%{http_code}" \
            -X "$method" \
            -H "Authorization: Bearer $AUTH_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    status=$(echo "$response" | tail -n1 | cut -d':' -f2)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status" -eq 200 ]; then
        echo -e "${GREEN}✓ 成功 (HTTP $status)${NC}"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ 失败 (HTTP $status)${NC}"
        echo "$body"
    fi
    echo "---"
}

# 检查JWT token
if [ -z "$AUTH_TOKEN" ]; then
    echo -e "${RED}警告: AUTH_TOKEN 未设置，请在脚本中设置有效的JWT token${NC}"
    echo "---"
fi

echo -e "${BLUE}测试场景：SSL/TLS证书管理系统${NC}"
echo -e "${BLUE}包含：免费证书申请、用户证书上传、自动续期、监控告警${NC}"
echo ""

# 1. 获取SSL证书统计信息
test_api "/ssl/certificates/statistics" "GET" "" "获取SSL证书统计信息"

# 2. 检查域名是否已有活跃证书
test_api "/ssl/certificates/domain/example.com/check" "GET" "" "检查example.com是否有活跃证书"

# 3. 获取域名的所有证书
test_api "/ssl/certificates/domain/example.com" "GET" "" "获取example.com的所有证书"

# 4. 获取域名的活跃证书
test_api "/ssl/certificates/domain/example.com/active" "GET" "" "获取example.com的活跃证书"

# 5. 申请Let's Encrypt免费证书
echo -e "${BLUE}=== 测试免费证书申请 ===${NC}"

test_data='
{
    "domainName": "test.example.com",
    "email": "admin@example.com",
    "challengeType": "http-01"
}'
test_api "/ssl/certificates/request/letsencrypt" "POST" "$test_data" "申请Let's Encrypt免费证书"

# 6. 获取需要续期的证书
test_api "/ssl/certificates/renewal/needed" "GET" "" "获取需要续期的证书列表"

# 7. 获取即将过期的证书（30天内）
test_api "/ssl/certificates/expiring?days=30" "GET" "" "获取30天内即将过期的证书"

# 8. 获取即将过期的证书（7天内）
test_api "/ssl/certificates/expiring?days=7" "GET" "" "获取7天内即将过期的证书"

# 9. 测试证书续期（假设有证书ID为1的证书）
echo -e "${BLUE}=== 测试证书续期功能 ===${NC}"
test_api "/ssl/certificates/1/renew" "POST" "" "续期证书ID为1的证书"

# 10. 批量续期所有需要续期的证书
test_api "/ssl/certificates/batch/renew" "POST" "" "批量续期所有需要续期的证书"

# 11. 获取所有证书列表（分页）
test_api "/ssl/certificates?page=0&size=10" "GET" "" "获取证书列表（第1页，每页10个）"

# 12. 测试不同挑战类型的证书申请
echo -e "${BLUE}=== 测试不同验证方式 ===${NC}"

# DNS验证
test_data_dns='
{
    "domainName": "dns.example.com",
    "email": "admin@example.com",
    "challengeType": "dns-01"
}'
test_api "/ssl/certificates/request/letsencrypt" "POST" "$test_data_dns" "使用DNS验证申请证书"

# 13. 测试错误处理 - 无效域名
echo -e "${BLUE}=== 测试错误处理 ===${NC}"

test_data_invalid='
{
    "domainName": "invalid..domain",
    "email": "admin@example.com",
    "challengeType": "http-01"
}'
test_api "/ssl/certificates/request/letsencrypt" "POST" "$test_data_invalid" "使用无效域名申请证书（应该失败）"

# 14. 测试错误处理 - 无效邮箱
test_data_invalid_email='
{
    "domainName": "test2.example.com",
    "email": "invalid-email",
    "challengeType": "http-01"
}'
test_api "/ssl/certificates/request/letsencrypt" "POST" "$test_data_invalid_email" "使用无效邮箱申请证书（应该失败）"

# 15. 测试用户证书上传 (模拟，实际需要multipart/form-data)
echo -e "${BLUE}=== 测试用户证书上传 ===${NC}"
echo "注意：证书上传需要使用multipart/form-data格式，此处仅测试接口可达性"

# 创建测试证书文件（模拟）
temp_cert_file="/tmp/test_cert.pem"
temp_key_file="/tmp/test_key.pem"

cat > "$temp_cert_file" << 'EOF'
-----BEGIN CERTIFICATE-----
MIIFXTCCBEWgAwIBAgISA1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef
MA0GCSqGSIb3DQEBCwUAMEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQn
cyBFbmNyeXB0MSMwIQYDVQQDExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBY
MzAeFw0yMzEyMDEwMDAwMDBaFw0yNDAyMjkyMzU5NTlaMBcxFTATBgNVBAMT
-----END CERTIFICATE-----
EOF

cat > "$temp_key_file" << 'EOF'
-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC1234567890
ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890
-----END PRIVATE KEY-----
EOF

# 测试证书上传
if command -v curl >/dev/null 2>&1; then
    echo -e "${YELLOW}测试: 上传用户SSL证书${NC}"
    upload_response=$(curl -s -w "\nSTATUS:%{http_code}" \
        -X POST \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -F "domainName=upload.example.com" \
        -F "certificateName=Uploaded Test Certificate" \
        -F "certificateFile=@$temp_cert_file" \
        -F "privateKeyFile=@$temp_key_file" \
        "$BASE_URL/ssl/certificates/upload")
    
    upload_status=$(echo "$upload_response" | tail -n1 | cut -d':' -f2)
    upload_body=$(echo "$upload_response" | sed '$d')
    
    if [ "$upload_status" -eq 200 ]; then
        echo -e "${GREEN}✓ 上传成功 (HTTP $upload_status)${NC}"
        echo "$upload_body" | jq '.' 2>/dev/null || echo "$upload_body"
    else
        echo -e "${RED}✗ 上传失败 (HTTP $upload_status)${NC}"
        echo "$upload_body"
    fi
    echo "---"
fi

# 清理临时文件
rm -f "$temp_cert_file" "$temp_key_file"

# 16. 测试HackerOne域名的SSL证书管理
echo -e "${BLUE}=== 测试HackerOne域名SSL证书 ===${NC}"

test_data_hackerone='
{
    "domainName": "test.wearehackerone.com",
    "email": "ssl-admin@wearehackerone.com",
    "challengeType": "dns-01"
}'
test_api "/ssl/certificates/request/letsencrypt" "POST" "$test_data_hackerone" "为HackerOne域名申请SSL证书"

# 17. 获取证书监控状态
echo -e "${BLUE}=== 测试证书监控功能 ===${NC}"
test_api "/ssl/certificates/1/monitoring" "GET" "" "获取证书监控状态"

# 18. 手动触发证书检查
test_api "/ssl/certificates/1/check" "POST" "" "手动触发证书健康检查"

# 19. 获取证书使用记录
test_api "/ssl/certificates/1/usage" "GET" "" "获取证书使用记录"

# 20. 测试系统配置
echo -e "${BLUE}=== 测试系统配置 ===${NC}"
test_api "/ssl/config" "GET" "" "获取SSL系统配置"

echo ""
echo -e "${GREEN}SSL/TLS证书管理系统测试完成！${NC}"
echo "============================================="
echo ""
echo -e "${BLUE}功能概述：${NC}"
echo "✓ 免费证书申请 (Let's Encrypt)"
echo "✓ 用户证书上传"
echo "✓ 证书自动续期"
echo "✓ 证书监控告警"
echo "✓ 过期提醒"
echo "✓ 批量操作"
echo "✓ 统计分析"
echo ""
echo -e "${BLUE}支持的证书类型：${NC}"
echo "- Let's Encrypt 免费证书"
echo "- ZeroSSL 免费证书"
echo "- 用户上传证书"
echo "- 自签名证书"
echo ""
echo -e "${BLUE}支持的验证方式：${NC}"
echo "- HTTP验证 (http-01)"
echo "- DNS验证 (dns-01)"
echo ""
echo -e "${BLUE}自动化功能：${NC}"
echo "- 每日自动检查续期需求"
echo "- 每4小时检查过期告警"
echo "- 每小时证书健康检查"
echo "- 自动批量续期处理"
echo ""
echo -e "${BLUE}监控功能：${NC}"
echo "- 证书过期监控"
echo "- 续期失败告警"
echo "- 统计报表生成"
echo "- 邮件通知支持"
echo ""
echo -e "${BLUE}使用说明：${NC}"
echo "1. 确保服务器正在运行 (localhost:8080)"
echo "2. 设置有效的JWT token到 AUTH_TOKEN 变量"
echo "3. 确保域名DNS配置正确（用于证书验证）"
echo "4. 配置邮件服务（用于ACME账户和通知）"
echo "5. 设置证书存储路径权限（默认：/opt/ssl-certs）"

# 系统状态检查
echo ""
echo -e "${BLUE}系统状态检查：${NC}"

# 检查存储目录
ssl_storage_path="/opt/ssl-certs"
if [ -d "$ssl_storage_path" ]; then
    echo -e "${GREEN}✓ SSL证书存储目录存在: $ssl_storage_path${NC}"
    if [ -w "$ssl_storage_path" ]; then
        echo -e "${GREEN}✓ SSL证书存储目录可写${NC}"
    else
        echo -e "${RED}✗ SSL证书存储目录不可写，请检查权限${NC}"
    fi
else
    echo -e "${RED}✗ SSL证书存储目录不存在: $ssl_storage_path${NC}"
    echo -e "${YELLOW}建议: sudo mkdir -p $ssl_storage_path && sudo chown \$(whoami) $ssl_storage_path${NC}"
fi

# 检查必要的依赖
echo ""
echo -e "${BLUE}依赖检查：${NC}"

if command -v openssl >/dev/null 2>&1; then
    echo -e "${GREEN}✓ OpenSSL 已安装: $(openssl version)${NC}"
else
    echo -e "${RED}✗ OpenSSL 未安装${NC}"
fi

if command -v dig >/dev/null 2>&1; then
    echo -e "${GREEN}✓ dig 工具已安装${NC}"
else
    echo -e "${RED}✗ dig 工具未安装（DNS验证可能需要）${NC}"
fi

if command -v jq >/dev/null 2>&1; then
    echo -e "${GREEN}✓ jq 工具已安装${NC}"
else
    echo -e "${YELLOW}! jq 工具未安装（JSON格式化会受影响）${NC}"
fi

echo ""
echo -e "${GREEN}SSL/TLS证书管理系统测试脚本执行完毕！${NC}"