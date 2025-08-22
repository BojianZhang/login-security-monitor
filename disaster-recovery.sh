#!/bin/bash

# 数据恢复和灾难恢复脚本
# 在遭受攻击后快速恢复系统和数据

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_emergency() {
    echo -e "${PURPLE}[EMERGENCY]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 显示帮助信息
show_help() {
    echo "数据保护和灾难恢复脚本"
    echo ""
    echo "使用方法:"
    echo "  ./disaster-recovery.sh <操作> [参数]"
    echo ""
    echo "操作:"
    echo "  emergency-shutdown    - 紧急关闭系统"
    echo "  isolate-network      - 网络隔离模式"
    echo "  backup-now           - 立即执行紧急备份"
    echo "  restore <backup>     - 从备份恢复数据"
    echo "  analyze-attack       - 分析攻击日志"
    echo "  security-scan        - 系统安全扫描"
    echo "  clean-rebuild        - 清理重建系统"
    echo "  status              - 检查系统状态"
    echo ""
    echo "示例:"
    echo "  ./disaster-recovery.sh emergency-shutdown"
    echo "  ./disaster-recovery.sh backup-now"
    echo "  ./disaster-recovery.sh restore emergency_backup_20231201_120000.sql"
}

# 紧急关闭系统
emergency_shutdown() {
    log_emergency "🚨 执行紧急系统关闭程序"
    
    # 立即执行数据备份
    log_info "正在执行紧急数据备份..."
    backup_now "emergency_shutdown"
    
    # 停止所有服务
    log_warning "停止所有应用服务..."
    docker-compose down
    
    # 保存系统状态快照
    log_info "保存系统状态信息..."
    mkdir -p emergency-logs/$(date +%Y%m%d_%H%M%S)
    
    # 导出容器日志
    docker-compose logs > emergency-logs/$(date +%Y%m%d_%H%M%S)/container-logs.txt 2>/dev/null || true
    
    # 保存系统信息
    {
        echo "=== EMERGENCY SHUTDOWN REPORT ==="
        echo "Timestamp: $(date)"
        echo "System Status: EMERGENCY SHUTDOWN"
        echo ""
        echo "=== RUNNING PROCESSES ==="
        ps aux
        echo ""
        echo "=== NETWORK CONNECTIONS ==="
        netstat -tuln 2>/dev/null || ss -tuln
        echo ""
        echo "=== DISK USAGE ==="
        df -h
        echo ""
        echo "=== MEMORY USAGE ==="
        free -h
        echo ""
    } > emergency-logs/$(date +%Y%m%d_%H%M%S)/system-status.txt
    
    log_emergency "系统已紧急关闭，数据已备份到: emergency-logs/"
    log_warning "请分析 emergency-logs/ 中的信息以确定攻击原因"
}

# 网络隔离模式
isolate_network() {
    log_emergency "🔒 激活网络隔离模式"
    
    # 创建隔离网络
    docker network create --driver bridge isolated-network 2>/dev/null || true
    
    # 停止当前服务
    docker-compose down
    
    # 在隔离网络中重启关键服务
    log_info "在隔离环境中重启数据库和内部服务..."
    
    # 修改docker-compose配置以使用隔离网络
    cp docker-compose.yml docker-compose.isolated.yml
    sed -i 's/login-monitor-network/isolated-network/g' docker-compose.isolated.yml
    
    # 只启动数据库和内部服务，不暴露外部端口
    docker-compose -f docker-compose.isolated.yml up -d mysql redis
    
    log_success "系统已进入网络隔离模式"
    log_warning "外部访问已被阻止，仅内部服务运行"
}

# 立即备份数据
backup_now() {
    local reason=${1:-"manual_emergency_backup"}
    
    log_info "🔄 开始紧急数据备份..."
    
    # 创建备份目录
    mkdir -p backups/emergency
    
    local timestamp=$(date +"%Y%m%d_%H%M%S")
    local backup_file="backups/emergency/emergency_${timestamp}.sql"
    local metadata_file="backups/emergency/emergency_${timestamp}.meta"
    
    # 检查数据库是否运行
    if ! docker-compose ps mysql | grep -q "Up"; then
        log_warning "数据库未运行，尝试启动..."
        docker-compose up -d mysql
        sleep 10
    fi
    
    # 执行数据库备份
    log_info "正在备份数据库..."
    if docker-compose exec -T mysql mysqldump \
        -u root -p${DB_ROOT_PASSWORD:-rootpassword} \
        --single-transaction --routines --triggers \
        ${DB_NAME:-login_security_monitor} > "$backup_file"; then
        
        # 计算校验和
        local checksum=$(sha256sum "$backup_file" | cut -d' ' -f1)
        
        # 创建元数据
        cat > "$metadata_file" << EOF
backup_timestamp=$(date)
backup_reason=$reason
backup_size=$(stat -c%s "$backup_file")
backup_checksum=$checksum
system_version=Login Security Monitor v1.0.0
emergency_mode=true
EOF
        
        # 压缩备份文件
        gzip "$backup_file"
        
        log_success "✅ 紧急备份完成: ${backup_file}.gz"
        log_info "备份大小: $(stat -c%s "${backup_file}.gz" | numfmt --to=iec)"
        log_info "校验和: $checksum"
        
    else
        log_error "❌ 数据库备份失败"
        return 1
    fi
}

# 从备份恢复数据
restore_data() {
    local backup_file=$1
    
    if [ -z "$backup_file" ]; then
        log_error "请指定备份文件"
        echo "可用备份文件:"
        ls -la backups/ 2>/dev/null || echo "未找到备份文件"
        return 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    log_warning "⚠️  即将从备份恢复数据，这将覆盖当前数据库！"
    read -p "确认继续恢复操作吗？(输入 YES 确认): " confirm
    
    if [ "$confirm" != "YES" ]; then
        log_info "恢复操作已取消"
        return 0
    fi
    
    log_info "🔄 开始数据恢复..."
    
    # 检查文件是否压缩
    if [[ "$backup_file" == *.gz ]]; then
        log_info "解压备份文件..."
        gunzip -c "$backup_file" > /tmp/restore_temp.sql
        restore_file="/tmp/restore_temp.sql"
    else
        restore_file="$backup_file"
    fi
    
    # 验证备份文件完整性
    if [ -f "${backup_file%.gz}.meta" ]; then
        log_info "验证备份文件完整性..."
        expected_checksum=$(grep backup_checksum "${backup_file%.gz}.meta" | cut -d'=' -f2)
        if [ ! -z "$expected_checksum" ]; then
            actual_checksum=$(sha256sum "$restore_file" | cut -d' ' -f1)
            if [ "$expected_checksum" != "$actual_checksum" ]; then
                log_error "备份文件校验失败！可能已损坏"
                return 1
            fi
            log_success "备份文件校验通过"
        fi
    fi
    
    # 确保数据库运行
    log_info "启动数据库服务..."
    docker-compose up -d mysql
    sleep 15
    
    # 执行恢复
    log_info "正在恢复数据库..."
    if docker-compose exec -T mysql mysql \
        -u root -p${DB_ROOT_PASSWORD:-rootpassword} \
        ${DB_NAME:-login_security_monitor} < "$restore_file"; then
        
        log_success "✅ 数据恢复完成"
        
        # 清理临时文件
        [ "$restore_file" = "/tmp/restore_temp.sql" ] && rm -f /tmp/restore_temp.sql
        
        # 重启服务
        log_info "重启应用服务..."
        docker-compose restart backend
        
        log_success "系统恢复完成！"
        
    else
        log_error "❌ 数据恢复失败"
        return 1
    fi
}

# 攻击日志分析
analyze_attack() {
    log_info "🔍 开始攻击日志分析..."
    
    local analysis_dir="attack-analysis/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$analysis_dir"
    
    # 分析应用日志
    if [ -f "logs/security-monitor.log" ]; then
        log_info "分析应用安全日志..."
        
        # 提取攻击相关日志
        grep -i "attack\|threat\|suspicious\|blocked\|emergency" logs/security-monitor.log > "$analysis_dir/attack-events.log" 2>/dev/null || true
        
        # 统计攻击类型
        {
            echo "=== 攻击事件统计 ==="
            echo "SQL注入攻击: $(grep -c "SQL注入攻击检测" logs/security-monitor.log 2>/dev/null || echo 0)"
            echo "XSS攻击: $(grep -c "XSS攻击检测" logs/security-monitor.log 2>/dev/null || echo 0)"
            echo "路径遍历攻击: $(grep -c "路径遍历攻击检测" logs/security-monitor.log 2>/dev/null || echo 0)"
            echo "暴力破解攻击: $(grep -c "暴力破解攻击检测" logs/security-monitor.log 2>/dev/null || echo 0)"
            echo ""
            echo "=== 被拉黑IP统计 ==="
            grep "IP已自动拉黑" logs/security-monitor.log 2>/dev/null | sort | uniq -c | sort -rn || echo "无数据"
            echo ""
        } > "$analysis_dir/attack-summary.txt"
    fi
    
    # 分析容器日志
    if docker-compose ps | grep -q "Up"; then
        log_info "导出容器日志..."
        docker-compose logs --no-color > "$analysis_dir/container-logs.txt" 2>/dev/null || true
    fi
    
    # 分析网络连接
    {
        echo "=== 网络连接分析 ==="
        echo "当前连接:"
        netstat -tuln 2>/dev/null || ss -tuln
        echo ""
        echo "监听端口:"
        netstat -tln 2>/dev/null || ss -tln
    } > "$analysis_dir/network-analysis.txt"
    
    # 生成报告
    {
        echo "================================================"
        echo "  登录安全监控系统 - 攻击分析报告"
        echo "================================================"
        echo "生成时间: $(date)"
        echo "分析目录: $analysis_dir"
        echo ""
        
        if [ -f "$analysis_dir/attack-summary.txt" ]; then
            cat "$analysis_dir/attack-summary.txt"
        fi
        
        echo ""
        echo "=== 建议处理措施 ==="
        echo "1. 检查被拉黑IP是否为已知威胁"
        echo "2. 更新系统安全规则"
        echo "3. 加强防火墙配置"
        echo "4. 考虑启用更严格的安全策略"
        echo "5. 定期更换JWT密钥和数据库密码"
        
    } > "$analysis_dir/analysis-report.txt"
    
    log_success "攻击分析完成，报告保存在: $analysis_dir/"
    
    # 显示关键信息
    if [ -f "$analysis_dir/attack-summary.txt" ]; then
        echo ""
        echo "=== 攻击事件摘要 ==="
        cat "$analysis_dir/attack-summary.txt"
    fi
}

# 系统安全扫描
security_scan() {
    log_info "🔒 开始系统安全扫描..."
    
    local scan_dir="security-scan/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$scan_dir"
    
    # 检查Docker安全配置
    {
        echo "=== Docker 安全配置检查 ==="
        echo ""
        echo "1. 检查容器是否以root用户运行:"
        docker-compose exec backend whoami 2>/dev/null || echo "后端服务未运行"
        echo ""
        
        echo "2. 检查暴露端口:"
        docker-compose ps
        echo ""
        
        echo "3. 检查挂载卷:"
        docker-compose config | grep -A5 "volumes:" || echo "未找到卷配置"
        
    } > "$scan_dir/docker-security.txt"
    
    # 检查密码和密钥安全
    {
        echo "=== 密码和密钥安全检查 ==="
        echo ""
        
        if [ -f ".env" ]; then
            echo "检查 .env 文件中的敏感配置:"
            echo "- JWT密钥长度: $(grep JWT_SECRET .env | cut -d'=' -f2 | wc -c) 字符"
            echo "- 数据库密码设置: $(grep -q 'CHANGE_THIS' .env && echo '⚠️  使用默认密码' || echo '✅ 已设置自定义密码')"
            echo "- Redis密码设置: $(grep -q 'CHANGE_THIS' .env && echo '⚠️  使用默认密码' || echo '✅ 已设置自定义密码')"
        else
            echo "⚠️  未找到 .env 配置文件"
        fi
        
    } > "$scan_dir/password-security.txt"
    
    # 检查网络安全
    {
        echo "=== 网络安全检查 ==="
        echo ""
        echo "开放端口检查:"
        netstat -tuln 2>/dev/null || ss -tuln
        echo ""
        echo "防火墙状态:"
        if command -v ufw >/dev/null 2>&1; then
            ufw status
        elif command -v iptables >/dev/null 2>&1; then
            iptables -L -n | head -20
        else
            echo "未检测到防火墙工具"
        fi
        
    } > "$scan_dir/network-security.txt"
    
    # 生成安全报告
    {
        echo "================================================"
        echo "  登录安全监控系统 - 安全扫描报告"
        echo "================================================"
        echo "扫描时间: $(date)"
        echo "扫描目录: $scan_dir"
        echo ""
        
        echo "=== 安全检查结果 ==="
        
        # 检查常见安全问题
        local issues=0
        
        if grep -q "CHANGE_THIS" .env 2>/dev/null; then
            echo "❌ 发现默认密码未更改"
            issues=$((issues + 1))
        else
            echo "✅ 密码配置检查通过"
        fi
        
        if docker-compose ps | grep -q ":80->80"; then
            echo "⚠️  HTTP端口暴露（建议使用HTTPS）"
            issues=$((issues + 1))
        fi
        
        if [ ! -f "logs/security-monitor.log" ]; then
            echo "⚠️  未找到安全日志文件"
            issues=$((issues + 1))
        else
            echo "✅ 安全日志文件存在"
        fi
        
        echo ""
        echo "=== 安全评分 ==="
        local score=$((100 - issues * 20))
        echo "总体安全评分: $score/100"
        
        if [ $score -lt 60 ]; then
            echo "🔴 安全级别: 低 - 需要立即处理"
        elif [ $score -lt 80 ]; then
            echo "🟡 安全级别: 中 - 建议优化"
        else
            echo "🟢 安全级别: 高"
        fi
        
    } > "$scan_dir/security-report.txt"
    
    log_success "安全扫描完成，报告保存在: $scan_dir/"
    
    # 显示报告
    echo ""
    cat "$scan_dir/security-report.txt"
}

# 清理重建系统
clean_rebuild() {
    log_warning "⚠️  即将完全清理并重建系统"
    read -p "这将删除所有容器和镜像，确认继续吗？(输入 YES 确认): " confirm
    
    if [ "$confirm" != "YES" ]; then
        log_info "清理重建操作已取消"
        return 0
    fi
    
    log_info "🔄 开始清理重建系统..."
    
    # 先执行备份
    backup_now "before_clean_rebuild"
    
    # 停止所有服务
    log_info "停止所有服务..."
    docker-compose down -v
    
    # 清理Docker资源
    log_info "清理Docker容器和镜像..."
    docker system prune -a -f
    docker volume prune -f
    docker network prune -f
    
    # 重新构建和启动
    log_info "重新构建系统..."
    docker-compose build --no-cache
    docker-compose up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    if docker-compose ps | grep -q "Up"; then
        log_success "✅ 系统清理重建完成"
        docker-compose ps
    else
        log_error "❌ 系统重建失败，请检查日志"
        docker-compose logs
    fi
}

# 检查系统状态
check_status() {
    log_info "📊 检查系统状态..."
    
    echo ""
    echo "=== 服务状态 ==="
    docker-compose ps
    
    echo ""
    echo "=== 系统资源使用 ==="
    echo "CPU和内存使用:"
    docker stats --no-stream
    
    echo ""
    echo "磁盘使用情况:"
    df -h
    
    echo ""
    echo "=== 安全状态 ==="
    if [ -f "logs/security-monitor.log" ]; then
        echo "最近24小时攻击事件:"
        grep "攻击检测" logs/security-monitor.log | tail -10 2>/dev/null || echo "未发现攻击事件"
    else
        echo "安全日志文件不存在"
    fi
    
    echo ""
    echo "=== 备份状态 ==="
    if [ -d "backups" ]; then
        echo "备份文件统计:"
        find backups -name "*.sql*" -type f | wc -l | xargs echo "总备份文件数:"
        echo "最新备份:"
        find backups -name "*.sql*" -type f -printf '%T@ %p\n' | sort -n | tail -3 | cut -d' ' -f2-
    else
        echo "未找到备份目录"
    fi
}

# 主函数
main() {
    local action=${1:-"status"}
    
    case $action in
        emergency-shutdown)
            emergency_shutdown
            ;;
        isolate-network)
            isolate_network
            ;;
        backup-now)
            backup_now
            ;;
        restore)
            restore_data "$2"
            ;;
        analyze-attack)
            analyze_attack
            ;;
        security-scan)
            security_scan
            ;;
        clean-rebuild)
            clean_rebuild
            ;;
        status)
            check_status
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知操作: $action"
            show_help
            exit 1
            ;;
    esac
}

# 检查是否以root权限运行（某些操作需要）
check_permissions() {
    if [[ $EUID -ne 0 ]] && [[ "$1" == "emergency-shutdown" || "$1" == "isolate-network" ]]; then
        log_warning "某些操作可能需要管理员权限才能完全执行"
    fi
}

# 错误处理
trap 'log_error "脚本执行过程中发生错误"; exit 1' ERR

# 检查权限
check_permissions "$1"

# 运行主函数
main "$@"