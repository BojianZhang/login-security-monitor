#!/bin/bash

# 部署脚本 - 企业级安全邮件管理系统
# Enterprise Secure Email Management System
# 使用方法: ./deploy.sh [dev|prod] [--rebuild]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    echo "企业级安全邮件管理系统 部署脚本"
    echo "Enterprise Secure Email Management System"
    echo ""
    echo "使用方法:"
    echo "  ./deploy.sh [环境] [选项]"
    echo ""
    echo "环境:"
    echo "  dev     - 开发环境"
    echo "  prod    - 生产环境"
    echo ""
    echo "选项:"
    echo "  --rebuild    - 重新构建Docker镜像"
    echo "  --no-cache   - 构建时不使用缓存"
    echo "  --help       - 显示帮助信息"
    echo ""
    echo "功能:"
    echo "  ✉️  邮件管理: 多域名支持，别名管理，邮件收发"
    echo "  🛡️  安全监控: 登录监控，异地检测，风险评估"
    echo "  📊  数据保护: 自动备份，攻击防护，灾难恢复"
    echo ""
    echo "示例:"
    echo "  ./deploy.sh dev"
    echo "  ./deploy.sh prod --rebuild"
    echo "  ./deploy.sh dev --rebuild --no-cache"
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_success "系统依赖检查通过"
}

# 检查环境文件
check_env_file() {
    local env_type=$1
    
    if [ "$env_type" = "prod" ]; then
        if [ ! -f ".env" ]; then
            log_warning ".env 文件不存在，正在从模板创建..."
            cp .env.example .env
            log_warning "请编辑 .env 文件，配置生产环境参数后重新运行部署脚本"
            exit 1
        fi
    fi
}

# 创建必要目录
create_directories() {
    log_info "创建必要的目录..."
    
    mkdir -p logs/nginx
    mkdir -p logs/backend
    mkdir -p config
    mkdir -p mysql/conf.d
    mkdir -p nginx/ssl
    mkdir -p monitoring
    
    log_success "目录创建完成"
}

# 设置MySQL配置
setup_mysql_config() {
    log_info "设置MySQL配置..."
    
    cat > mysql/conf.d/mysql.cnf << 'EOF'
[mysqld]
# 字符集设置
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 连接设置
max_connections=200
max_connect_errors=1000
wait_timeout=28800
interactive_timeout=28800

# InnoDB设置
innodb_buffer_pool_size=256M
innodb_log_file_size=64M
innodb_flush_log_at_trx_commit=2
innodb_flush_method=O_DIRECT

# 日志设置
log_error=/var/log/mysql/mysql.err
slow_query_log=1
slow_query_log_file=/var/log/mysql/mysql-slow.log
long_query_time=2

# 时区设置
default-time-zone='+08:00'

[client]
default-character-set=utf8mb4
EOF
    
    log_success "MySQL配置设置完成"
}

# 部署开发环境
deploy_dev() {
    local rebuild=$1
    local no_cache=$2
    
    log_info "开始部署开发环境..."
    
    # 停止现有容器
    log_info "停止现有的开发环境容器..."
    docker-compose -f docker-compose.dev.yml down
    
    # 启动开发环境服务
    log_info "启动开发环境服务..."
    docker-compose -f docker-compose.dev.yml up -d
    
    log_success "开发环境部署完成！"
    echo ""
    echo "=========================================="
    echo "   企业级安全邮件管理系统 - 开发环境"
    echo "   Enterprise Secure Email System"
    echo "=========================================="
    echo ""
    echo "🌐 Web访问地址："
    echo "  - 主应用:     http://localhost:3000"
    echo "  - 后端API:    http://localhost:8080"
    echo "  - 数据库管理: http://localhost:8081 (Adminer)"
    echo "  - Redis管理:  http://localhost:8082 (Redis Commander)"
    echo "  - 邮件测试:   http://localhost:8025 (MailHog)"
    echo ""
    echo "📊 数据库连接信息："
    echo "  - 主机: localhost:3307"
    echo "  - 数据库: secure_email_system_dev"
    echo "  - 用户名: dev_user"
    echo "  - 密码: dev_pass"
    echo ""
    echo "✉️ 邮件系统功能："
    echo "  - 多域名邮箱别名管理"
    echo "  - 邮件收发和存储"
    echo "  - 文件夹分类管理"
    echo ""
    echo "🛡️ 安全监控功能："
    echo "  - 实时登录监控"
    echo "  - 异地登录检测"
    echo "  - 攻击防护机制"
    echo "  - 数据自动备份"
    echo ""
}

# 部署生产环境
deploy_prod() {
    local rebuild=$1
    local no_cache=$2
    
    log_info "开始部署生产环境..."
    
    # 构建选项
    build_opts=""
    if [ "$rebuild" = true ]; then
        build_opts="--build"
        if [ "$no_cache" = true ]; then
            build_opts="$build_opts --no-cache"
        fi
    fi
    
    # 停止现有容器
    log_info "停止现有的生产环境容器..."
    docker-compose down
    
    # 拉取最新镜像
    if [ "$rebuild" = false ]; then
        log_info "拉取最新的Docker镜像..."
        docker-compose pull
    fi
    
    # 启动生产环境服务
    log_info "启动生产环境服务..."
    docker-compose up -d $build_opts
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_services_health
    
    log_success "生产环境部署完成！"
    echo ""
    echo "服务访问地址："
    echo "  - 前端应用: http://localhost:80"
    echo "  - 后端API:  http://localhost:8080/api"
    echo ""
    echo "默认管理员账号："
    echo "  - 用户名: admin"
    echo "  - 密码: admin123"
}

# 检查服务健康状态
check_services_health() {
    log_info "检查服务健康状态..."
    
    services=("mysql" "redis" "backend" "frontend")
    
    for service in "${services[@]}"; do
        log_info "检查 $service 服务状态..."
        
        max_attempts=30
        attempt=1
        
        while [ $attempt -le $max_attempts ]; do
            if docker-compose ps | grep "$service" | grep -q "healthy\|Up"; then
                log_success "$service 服务运行正常"
                break
            fi
            
            if [ $attempt -eq $max_attempts ]; then
                log_error "$service 服务启动失败或不健康"
                docker-compose logs "$service"
                exit 1
            fi
            
            log_info "等待 $service 服务启动... (尝试 $attempt/$max_attempts)"
            sleep 5
            ((attempt++))
        done
    done
    
    log_success "所有服务健康检查通过"
}

# 清理函数
cleanup() {
    log_info "清理未使用的Docker资源..."
    docker system prune -f
    docker volume prune -f
    log_success "清理完成"
}

# 显示状态
show_status() {
    echo ""
    echo "========================================"
    echo "  Login Security Monitor 服务状态"
    echo "========================================"
    docker-compose ps
    echo ""
}

# 主函数
main() {
    local env_type="dev"
    local rebuild=false
    local no_cache=false
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            dev|prod)
                env_type="$1"
                shift
                ;;
            --rebuild)
                rebuild=true
                shift
                ;;
            --no-cache)
                no_cache=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 显示部署信息
    echo "========================================"
    echo "  Login Security Monitor 部署脚本"
    echo "========================================"
    echo "环境: $env_type"
    echo "重新构建: $rebuild"
    echo "不使用缓存: $no_cache"
    echo "========================================"
    echo ""
    
    # 检查依赖
    check_dependencies
    
    # 检查环境文件
    check_env_file "$env_type"
    
    # 创建必要目录
    create_directories
    
    # 设置MySQL配置
    setup_mysql_config
    
    # 根据环境类型部署
    if [ "$env_type" = "dev" ]; then
        deploy_dev "$rebuild" "$no_cache"
    else
        deploy_prod "$rebuild" "$no_cache"
    fi
    
    # 显示服务状态
    show_status
    
    # 清理
    cleanup
    
    log_success "部署完成！"
}

# 错误处理
trap 'log_error "部署过程中发生错误，请检查日志"; exit 1' ERR

# 运行主函数
main "$@"