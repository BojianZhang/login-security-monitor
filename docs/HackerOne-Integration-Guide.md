# HackerOne 平台集成使用指南 - 完整邮箱地址格式

## 概述

本系统已集成HackerOne平台支持，可以自动识别和管理来自`hackerone.com`指向的`@wearehackerone.com`邮箱别名，**保持显示完整的邮箱地址格式**，确保用户可以看到和复制如 `alice+bug123@wearehackerone.com` 这样的完整邮箱地址。

## 核心特性

### 🎯 完整邮箱地址显示
- **显示格式**: 保持完整的 `alice+bug123@wearehackerone.com` 格式
- **可选择文本**: 用户可以点击选择整个邮箱地址
- **一键复制**: 提供复制按钮，支持多种复制格式
- **自动识别**: 系统自动识别所有HackerOne格式的邮箱

### 📋 支持的邮箱格式

#### 标准格式
```
alice@wearehackerone.com
security.researcher@wearehackerone.com
john.doe@wearehackerone.com
```

#### 带后缀格式
```
alice+bug123@wearehackerone.com
security.expert+critical@wearehackerone.com  
researcher+web.vuln@wearehackerone.com
test_user+mobile@wearehackerone.com
```

## API 接口说明

### 基础路径
```
/api/integrations/hackerone
```

### 核心接口

#### 1. 获取可复制的邮箱地址格式
```http
GET /api/integrations/hackerone/aliases/copyable-formats
```

**响应示例:**
```json
[
  {
    "aliasId": 123,
    "fullEmail": "alice+bug123@wearehackerone.com",
    "displayFormat": "alice+bug123@wearehackerone.com",
    "copyableText": "alice+bug123@wearehackerone.com",
    "username": "alice",
    "isActive": true,
    "description": "HackerOne platform alias: alice+bug123@wearehackerone.com"
  }
]
```

#### 2. 获取单个别名的可复制格式
```http
GET /api/integrations/hackerone/aliases/{aliasId}/copyable-format
```

**响应示例:**
```json
{
  "success": true,
  "aliasId": 123,
  "fullEmail": "alice+bug123@wearehackerone.com",
  "displayFormat": "alice+bug123@wearehackerone.com",
  "isHackerOne": true,
  "username": "alice",
  "copyInstruction": "点击复制完整邮箱地址: alice+bug123@wearehackerone.com"
}
```

#### 3. 同步HackerOne别名为完整格式
```http
POST /api/integrations/hackerone/sync-aliases
```

**响应示例:**
```json
{
  "success": true,
  "message": "HackerOne 别名同步完成",
  "syncedCount": 5
}
```

#### 4. 验证邮箱格式
```http
GET /api/integrations/hackerone/validate-email?email=alice%2Bbug123%40wearehackerone.com
```

**响应示例:**
```json
{
  "isHackerOne": true,
  "username": "alice",
  "suggestedDisplayName": "alice+bug123@wearehackerone.com",
  "email": "alice+bug123@wearehackerone.com"
}
```

### 5. 构建HackerOne邮箱地址
```http
POST /api/integrations/hackerone/build-email
Content-Type: application/json

{
    "username": "johndoe",
    "suffix": "vuln123"
}
```

**响应示例:**
```json
{
    "success": true,
    "email": "johndoe+vuln123@wearehackerone.com",
    "displayName": "HackerOne: johndoe",
    "username": "johndoe",
    "suffix": "vuln123"
}
```

### 6. 批量同步HackerOne用户名
```http
POST /api/integrations/hackerone/sync-usernames
Content-Type: application/json

[
    {
        "username": "alice", 
        "suffix": "bug",
        "customDisplayName": "Alice - Bug Hunter"
    },
    {
        "username": "bob",
        "suffix": "vuln",
        "customDisplayName": "Bob - Vulnerability Researcher"  
    }
]
```

## 使用场景

### 场景1: 自动同步现有HackerOne别名

当您的系统中已有HackerOne格式的别名，但显示名称不正确时：

```bash
# 调用同步API
curl -X POST http://localhost:8080/api/integrations/hackerone/sync-aliases \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

系统会自动：
- 识别所有 `@wearehackerone.com` 格式的别名
- 提取用户名（如：`alice+bug@wearehackerone.com` → `alice`）
- 设置显示名称为：`HackerOne: alice`
- 添加描述信息和外部ID

### 场景2: 创建新的HackerOne别名

创建新别名时，系统会自动检测HackerOne格式并设置正确的显示名称：

```http
POST /api/email/aliases
{
    "aliasEmail": "newhacker",
    "domainId": 1  // 对应 wearehackerone.com 域名
}
```

创建成功后，别名会自动显示为：`HackerOne: newhacker`

### 场景3: 批量导入HackerOne用户

如果您有多个HackerOne用户需要批量设置：

```http
POST /api/integrations/hackerone/sync-usernames
[
    {
        "username": "securityresearcher1",
        "suffix": "critical", 
        "customDisplayName": "SR1 - Critical Issues"
    },
    {
        "username": "bugbountyexpert",
        "suffix": "web",
        "customDisplayName": "BBE - Web Vulnerabilities"
    }
]
```

## 自动化功能

### 1. 实体监听器
系统会自动监听别名的创建和更新，对HackerOne格式的邮箱自动设置显示名称。

### 2. 定时同步任务
- **每小时同步**: 自动检查并同步所有用户的HackerOne别名
- **每日报告**: 生成HackerOne别名使用统计报告

### 3. 配置选项
在 `application.properties` 中可以配置：

```properties
# 启用HackerOne自动同步
app.integrations.hackerone.auto-sync.enabled=true

# 自动设置显示名称
app.integrations.hackerone.auto-set-display-name=true

# 自定义显示名称模板
app.integrations.hackerone.display-name.template=HackerOne: {username}
```

## 支持的邮箱格式

### 标准格式
```
username@wearehackerone.com
```

### 带后缀格式  
```
username+suffix@wearehackerone.com
```

### 实际示例
```
alice@wearehackerone.com          → HackerOne: alice
bob+bug123@wearehackerone.com     → HackerOne: bob  
security.expert@wearehackerone.com → HackerOne: security.expert
test_user+vuln@wearehackerone.com → HackerOne: test_user
```

## 前端显示效果

集成后，用户在界面上看到的别名显示会是：

**之前:**
- `alice+bug123@wearehackerone.com`
- `security.researcher@wearehackerone.com`

**之后:**  
- `HackerOne: alice`
- `HackerOne: security.researcher`

这样就实现了与HackerOne平台显示名称的一致性！

## 故障排除

### 问题1: 别名未自动识别
- 检查邮箱格式是否为 `@wearehackerone.com`
- 确认域名 `wearehackerone.com` 已在系统中配置
- 查看日志中的错误信息

### 问题2: 显示名称未更新
- 手动调用同步API: `POST /api/integrations/hackerone/sync-aliases`
- 检查是否有权限问题
- 确认配置项 `auto-set-display-name` 为 true

### 问题3: 批量同步失败
- 验证请求数据格式
- 检查用户名是否符合HackerOne格式要求
- 查看服务器日志获取详细错误信息

## 总结

通过HackerOne集成，您的邮件系统现在可以：

1. **自动识别** HackerOne格式的邮箱别名
2. **智能显示** 与HackerOne平台一致的用户名
3. **批量管理** 多个HackerOne用户的别名
4. **实时同步** 确保显示名称始终准确

这样就完美解决了您提出的需求：让主账户下显示的别名名称与hackerone.com指向的别名保持一致！