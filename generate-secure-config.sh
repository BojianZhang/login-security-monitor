# 生成安全的JWT密钥脚本
#!/bin/bash

# 生成256位随机密钥
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
echo "生成的JWT密钥: $JWT_SECRET"

# 创建更安全的环境配置
cat > .env.secure << EOF
# 数据库配置
DB_ROOT_PASSWORD=$(openssl rand -base64 16 | tr -d '\n')
DB_NAME=secure_email_system  
DB_USERNAME=secure_user
DB_PASSWORD=$(openssl rand -base64 12 | tr -d '\n')

# Redis配置
REDIS_PASSWORD=$(openssl rand -base64 12 | tr -d '\n')

# JWT安全配置
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION=86400000

# 邮件配置 (请修改为实际配置)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=Secure Email System <noreply@yourcompany.com>

# 管理员邮箱
ADMIN_EMAILS=admin@yourcompany.com,security@yourcompany.com

# Grafana配置
GRAFANA_PASSWORD=$(openssl rand -base64 12 | tr -d '\n')
EOF

echo "安全配置已保存到 .env.secure"
echo "请将 .env.secure 重命名为 .env 并根据需要调整邮件配置"