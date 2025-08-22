#!/bin/bash

# HackerOne 集成测试脚本
# 用于测试HackerOne别名集成功能 - 完整邮箱地址格式

BASE_URL="http://localhost:8080/api"
AUTH_TOKEN=""  # 需要设置有效的JWT token

echo "HackerOne 集成功能测试 - 完整邮箱地址格式"
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

echo -e "${BLUE}测试场景：显示完整的HackerOne邮箱地址格式${NC}"
echo -e "${BLUE}期望结果：alice+bug123@wearehackerone.com（可复制）${NC}"
echo ""

# 1. 获取HackerOne统计信息
test_api "/integrations/hackerone/stats" "GET" "" "获取HackerOne别名统计"

# 2. 验证多种HackerOne邮箱格式
echo -e "${BLUE}=== 验证HackerOne邮箱格式识别 ===${NC}"

test_api "/integrations/hackerone/validate-email?email=alice%40wearehackerone.com" "GET" "" "验证标准格式: alice@wearehackerone.com"

test_api "/integrations/hackerone/validate-email?email=alice%2Bbug123%40wearehackerone.com" "GET" "" "验证带后缀格式: alice+bug123@wearehackerone.com"

test_api "/integrations/hackerone/validate-email?email=security.expert%2Bcritical%40wearehackerone.com" "GET" "" "验证复杂格式: security.expert+critical@wearehackerone.com"

# 3. 获取所有HackerOne别名的可复制格式
echo -e "${BLUE}=== 获取可复制的邮箱地址格式 ===${NC}"

test_api "/integrations/hackerone/aliases/copyable-formats" "GET" "" "获取所有HackerOne别名的完整格式"

# 4. 构建HackerOne邮箱地址
echo -e "${BLUE}=== 构建完整的HackerOne邮箱地址 ===${NC}"

test_data='{
    "username": "testuser",
    "suffix": "bug123"
}'
test_api "/integrations/hackerone/build-email" "POST" "$test_data" "构建: testuser+bug123@wearehackerone.com"

# 5. 同步HackerOne别名（保持完整格式）
test_api "/integrations/hackerone/sync-aliases" "POST" "" "同步所有HackerOne别名为完整格式"

# 6. 测试别名搜索
test_api "/email/aliases/search?keyword=wearehackerone" "GET" "" "搜索wearehackerone相关别名"

echo ""
echo -e "${GREEN}测试完成！${NC}"
echo "============================================="
echo ""
echo -e "${BLUE}显示格式说明：${NC}"
echo "✓ 标准格式: alice@wearehackerone.com"
echo "✓ 带后缀格式: alice+bug123@wearehackerone.com" 
echo "✓ 复杂格式: security.expert+critical@wearehackerone.com"
echo ""
echo -e "${BLUE}前端显示效果：${NC}"
echo "- 界面显示: alice+bug123@wearehackerone.com"
echo "- 可选择文本: 用户可以点击选择整个邮箱地址"
echo "- 复制按钮: 点击复制完整的邮箱地址到剪贴板"
echo "- 支持多种复制格式: 完整地址、用户名部分、纯文本"
echo ""
echo -e "${BLUE}使用说明：${NC}"
echo "1. 确保服务器正在运行 (localhost:8080)"
echo "2. 设置有效的JWT token到 AUTH_TOKEN 变量"
echo "3. 系统会自动识别所有 @wearehackerone.com 格式的邮箱"
echo "4. 显示名称将保持完整的邮箱地址格式"
echo "5. 用户可以轻松复制任何HackerOne邮箱地址"

# 示例响应格式预览
echo ""
echo -e "${BLUE}预期API响应格式：${NC}"
cat << 'EOF'
{
  "success": true,
  "aliasId": 123,
  "fullEmail": "alice+bug123@wearehackerone.com",
  "displayFormat": "alice+bug123@wearehackerone.com", 
  "copyableText": "alice+bug123@wearehackerone.com",
  "isHackerOne": true,
  "username": "alice",
  "copyInstruction": "点击复制完整邮箱地址: alice+bug123@wearehackerone.com"
}
EOF