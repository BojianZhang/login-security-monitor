#!/bin/bash

# 管理脚本 - Login Security Monitor
# 提供常用的管理操作命令

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
    echo "Login Security Monitor 管理脚本"
    echo ""
    echo "使用方法:"
    echo "  ./manage.sh <命令> [参数]"
    echo ""
    echo "可用命令:"
    echo "  status          - 显示服务状态"
    echo "  logs <service>  - 查看服务日志"
    echo "  restart         - 重启所有服务"
    echo "  stop            - 停止所有服务"
    echo "  start           - 启动所有服务"
    echo "  backup          - 备份数据库"
    echo "  restore <file>  - 恢复数据库"
    echo "  shell <service> - 进入服务容器"
    echo "  update          - 更新应用"
    echo "  clean           - 清理系统"
    echo "  monitor         - 实时监控"
    echo ""
    echo "示例:"
    echo "  ./manage.sh status"
    echo "  ./manage.sh logs backend"
    echo "  ./manage.sh backup"
    echo "  ./manage.sh shell mysql"
}

# 显示服务状态
show_status() {
    echo ""
    echo "========================================"
    echo "  Login Security Monitor 服务状态"
    echo "========================================"
    docker-compose ps
    echo ""
    
    # 显示资源使用情况
    echo "========================================"
    echo "  系统资源使用情况"
    echo "========================================"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"
    echo ""
}

# 查看服务日志
view_logs() {
    local service=$1
    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        echo "可用服务: mysql, redis, backend, frontend, nginx-proxy"
        exit 1
    fi
    
    log_info "查看 $service 服务日志..."
    docker-compose logs -f --tail=100 "$service"
}

# 重启服务
restart_services() {
    log_info "重启所有服务..."
    docker-compose restart
    log_success "服务重启完成"
    show_status
}

# 停止服务
stop_services() {
    log_info "停止所有服务..."
    docker-compose down
    log_success "服务已停止"
}

# 启动服务
start_services() {
    log_info "启动所有服务..."
    docker-compose up -d
    log_success "服务启动完成"
    show_status
}

# 备份数据库
backup_database() {
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_file="backup_${timestamp}.sql"
    
    log_info "开始备份数据库..."
    
    # 创建备份目录
    mkdir -p backups
    
    # 执行备份
    docker-compose exec mysql mysqldump -u root -p${DB_ROOT_PASSWORD:-rootpassword} ${DB_NAME:-login_security_monitor} > "backups/$backup_file"
    
    if [ $? -eq 0 ]; then
        log_success "数据库备份完成: backups/$backup_file"
    else
        log_error "数据库备份失败"
        exit 1
    fi
}

# 恢复数据库
restore_database() {
    local backup_file=$1
    
    if [ -z "$backup_file" ]; then
        log_error "请指定备份文件"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        exit 1
    fi
    
    log_warning "即将恢复数据库，这将覆盖现有数据！"
    read -p "确认继续吗？(y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "操作已取消"
        exit 0
    fi
    
    log_info "开始恢复数据库..."
    
    # 执行恢复
    docker-compose exec -T mysql mysql -u root -p${DB_ROOT_PASSWORD:-rootpassword} ${DB_NAME:-login_security_monitor} < "$backup_file"
    
    if [ $? -eq 0 ]; then
        log_success "数据库恢复完成"
    else
        log_error "数据库恢复失败"
        exit 1
    fi
}

# 进入容器Shell
enter_shell() {
    local service=$1
    
    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        echo "可用服务: mysql, redis, backend, frontend"
        exit 1
    fi
    
    log_info "进入 $service 容器..."
    
    case $service in
        mysql)
            docker-compose exec mysql mysql -u root -p${DB_ROOT_PASSWORD:-rootpassword} ${DB_NAME:-login_security_monitor}
            ;;
        redis)
            docker-compose exec redis redis-cli -a ${REDIS_PASSWORD:-redis123}
            ;;
        backend)
            docker-compose exec backend bash
            ;;
        frontend)
            docker-compose exec frontend sh
            ;;
        *)
            docker-compose exec "$service" sh
            ;;
    esac
}

# 更新应用
update_application() {
    log_info "开始更新应用..."
    
    # 停止服务
    log_info "停止当前服务..."
    docker-compose down
    
    # 拉取最新代码（如果是git仓库）
    if [ -d ".git" ]; then
        log_info "拉取最新代码..."
        git pull origin main
    fi
    
    # 重新构建并启动
    log_info "重新构建并启动服务..."
    docker-compose up -d --build
    
    log_success "应用更新完成"
    show_status
}

# 清理系统
clean_system() {
    log_warning "即将清理Docker系统资源，包括未使用的镜像、容器和卷"
    read -p "确认继续吗？(y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "操作已取消"
        exit 0
    fi
    
    log_info "清理Docker系统资源..."
    
    # 清理未使用的容器
    docker container prune -f
    
    # 清理未使用的镜像
    docker image prune -f
    
    # 清理未使用的卷
    docker volume prune -f
    
    # 清理未使用的网络
    docker network prune -f
    
    log_success "系统清理完成"
}

# 实时监控
monitor_system() {
    log_info "启动实时监控模式 (按 Ctrl+C 退出)..."
    echo ""
    
    while true; do
        clear
        echo "========================================"
        echo "  Login Security Monitor 实时监控"
        echo "  $(date)"
        echo "========================================"
        echo ""
        
        # 显示容器状态
        echo "容器状态:"
        docker-compose ps
        echo ""
        
        # 显示资源使用情况
        echo "资源使用情况:"
        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
        echo ""
        
        # 显示最近的日志
        echo "最近日志 (最后10行):"
        docker-compose logs --tail=10 backend | tail -10
        echo ""
        
        sleep 10
    done
}

# 主函数
main() {
    local command=$1
    shift
    
    case $command in
        status)
            show_status
            ;;
        logs)
            view_logs "$1"
            ;;
        restart)
            restart_services
            ;;
        stop)
            stop_services
            ;;
        start)
            start_services
            ;;
        backup)
            backup_database
            ;;
        restore)
            restore_database "$1"
            ;;
        shell)
            enter_shell "$1"
            ;;
        update)
            update_application
            ;;
        clean)
            clean_system
            ;;
        monitor)
            monitor_system
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

# 检查参数
if [ $# -eq 0 ]; then
    show_help
    exit 1
fi

# 运行主函数
main "$@"