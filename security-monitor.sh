#!/bin/bash

# 攻击防护和数据保护监控脚本
# 实时监控系统安全状态并在检测到攻击时自动响应

set -e

# 配置参数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
MONITOR_INTERVAL=30  # 监控间隔（秒）
LOG_FILE="security-monitor.log"
PID_FILE="/tmp/security-monitor.pid"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 阈值配置
MAX_FAILED_LOGINS=20          # 最大失败登录次数
MAX_REQUESTS_PER_MINUTE=300   # 每分钟最大请求数
DISK_USAGE_THRESHOLD=85       # 磁盘使用率阈值
MEMORY_USAGE_THRESHOLD=90     # 内存使用率阈值

# 日志函数
log_security() {
    echo -e "${RED}[SECURITY]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_emergency() {
    echo -e "${PURPLE}[EMERGENCY]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# 检查进程是否正在运行
check_running() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            return 0
        else
            rm -f "$PID_FILE"
        fi
    fi
    return 1
}

# 启动监控
start_monitor() {
    if check_running; then
        log_warning "安全监控已经在运行中 (PID: $(cat $PID_FILE))"
        return 1
    fi
    
    echo $$ > "$PID_FILE"
    log_success "🛡️ 安全监控系统启动 (PID: $$)"
    
    # 信号处理
    trap 'stop_monitor' SIGTERM SIGINT
    
    # 主监控循环
    while true; do
        monitor_cycle
        sleep "$MONITOR_INTERVAL"
    done
}

# 停止监控
stop_monitor() {
    log_info "正在停止安全监控系统..."
    
    if [ -f "$PID_FILE" ]; then
        rm -f "$PID_FILE"
    fi
    
    log_success "安全监控系统已停止"
    exit 0
}

# 监控周期
monitor_cycle() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # 1. 检查系统资源
    check_system_resources
    
    # 2. 检查服务状态
    check_service_status
    
    # 3. 分析安全日志
    analyze_security_logs
    
    # 4. 检查网络连接
    check_network_connections
    
    # 5. 验证数据完整性
    check_data_integrity
    
    # 6. 检查攻击模式
    detect_attack_patterns
}

# 检查系统资源
check_system_resources() {
    # 检查磁盘使用率
    local disk_usage=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ "$disk_usage" -gt "$DISK_USAGE_THRESHOLD" ]; then
        log_warning "⚠️  磁盘使用率过高: ${disk_usage}%"
        
        # 自动清理日志文件
        find logs/ -name "*.log" -mtime +7 -delete 2>/dev/null || true
        
        if [ "$disk_usage" -gt 95 ]; then
            log_emergency "🚨 磁盘空间严重不足，触发紧急备份"
            ./disaster-recovery.sh backup-now &
        fi
    fi
    
    # 检查内存使用率
    local memory_usage=$(free | awk 'NR==2{printf "%.0f", $3*100/$2}')
    if [ "$memory_usage" -gt "$MEMORY_USAGE_THRESHOLD" ]; then
        log_warning "⚠️  内存使用率过高: ${memory_usage}%"
        
        # 记录内存使用情况
        {
            echo "=== 内存使用详情 $(date) ==="
            ps aux --sort=-%mem | head -10
            echo ""
        } >> memory-usage.log
    fi
}

# 检查服务状态
check_service_status() {
    local services=("mysql" "redis" "backend" "frontend")
    
    for service in "${services[@]}"; do
        if ! docker-compose ps "$service" | grep -q "Up"; then
            log_security "🚨 服务异常停止: $service"
            
            # 尝试重启服务
            log_info "尝试重启服务: $service"
            docker-compose up -d "$service"
            
            sleep 10
            
            # 验证重启是否成功
            if docker-compose ps "$service" | grep -q "Up"; then
                log_success "✅ 服务重启成功: $service"
            else
                log_emergency "❌ 服务重启失败: $service - 可能遭受攻击"
                
                # 触发紧急响应
                emergency_response "service_failure" "$service"
            fi
        fi
    done
}

# 分析安全日志
analyze_security_logs() {
    local app_log="logs/security-monitor.log"
    
    if [ ! -f "$app_log" ]; then
        return
    fi
    
    # 检查最近5分钟的攻击事件
    local recent_attacks=$(grep "$(date -d '5 minutes ago' '+%Y-%m-%d %H:%M')" "$app_log" 2>/dev/null | grep -c "攻击检测" || echo 0)
    
    if [ "$recent_attacks" -gt 5 ]; then
        log_security "🚨 检测到密集攻击活动: ${recent_attacks} 次攻击"
        
        # 分析攻击来源IP
        local top_attackers=$(grep "攻击检测" "$app_log" | tail -20 | grep -o "IP: [0-9.]*" | sort | uniq -c | sort -rn | head -3)
        
        log_security "主要攻击来源IP:"
        echo "$top_attackers" | while read count ip; do
            log_security "  $ip - $count 次攻击"
        done
        
        # 触发攻击响应
        emergency_response "multiple_attacks" "$recent_attacks"
    fi
    
    # 检查系统错误
    local system_errors=$(grep "ERROR" "$app_log" | grep "$(date '+%Y-%m-%d')" | wc -l)
    if [ "$system_errors" -gt 10 ]; then
        log_warning "⚠️  今日系统错误较多: $system_errors 个"
    fi
}

# 检查网络连接
check_network_connections() {
    # 检查异常网络连接
    local suspicious_connections=$(netstat -tuln 2>/dev/null | grep -E ":22|:80|:443|:8080" | wc -l)
    
    # 检查是否有过多的连接
    local total_connections=$(netstat -tun 2>/dev/null | wc -l)
    if [ "$total_connections" -gt 1000 ]; then
        log_warning "⚠️  网络连接数异常: $total_connections"
        
        # 记录连接详情
        {
            echo "=== 网络连接详情 $(date) ==="
            netstat -tuln | head -50
            echo ""
        } >> network-connections.log
    fi
    
    # 检查端口扫描攻击
    local port_scan_attempts=$(grep "connection refused\|connection reset" /var/log/syslog 2>/dev/null | grep "$(date '+%b %d %H')" | wc -l || echo 0)
    if [ "$port_scan_attempts" -gt 50 ]; then
        log_security "🚨 检测到端口扫描攻击: $port_scan_attempts 次尝试"
    fi
}

# 验证数据完整性
check_data_integrity() {
    # 检查备份文件是否存在
    if [ ! -d "backups" ] || [ $(find backups -name "*.sql*" | wc -l) -eq 0 ]; then
        log_warning "⚠️  未找到数据备份文件"
        
        # 自动执行备份
        log_info "执行自动数据备份..."
        ./disaster-recovery.sh backup-now &
    fi
    
    # 检查数据库连接
    if ! docker-compose exec -T mysql mysqladmin ping -u root -p${DB_ROOT_PASSWORD:-rootpassword} >/dev/null 2>&1; then
        log_security "🚨 数据库连接失败 - 可能遭受攻击"
        emergency_response "database_failure" "mysql"
    fi
}

# 检测攻击模式
detect_attack_patterns() {
    local app_log="logs/security-monitor.log"
    
    if [ ! -f "$app_log" ]; then
        return
    fi
    
    # 检测SQL注入攻击模式
    local sql_injection_count=$(grep -i "sql.*injection\|union.*select\|drop.*table" "$app_log" | grep "$(date '+%Y-%m-%d %H')" | wc -l || echo 0)
    if [ "$sql_injection_count" -gt 5 ]; then
        log_security "🚨 检测到SQL注入攻击模式: $sql_injection_count 次"
    fi
    
    # 检测XSS攻击模式
    local xss_attack_count=$(grep -i "xss\|script.*alert\|javascript:" "$app_log" | grep "$(date '+%Y-%m-%d %H')" | wc -l || echo 0)
    if [ "$xss_attack_count" -gt 5 ]; then
        log_security "🚨 检测到XSS攻击模式: $xss_attack_count 次"
    fi
    
    # 检测暴力破解模式
    local brute_force_count=$(grep "暴力破解攻击检测" "$app_log" | grep "$(date '+%Y-%m-%d %H')" | wc -l || echo 0)
    if [ "$brute_force_count" -gt 3 ]; then
        log_security "🚨 检测到暴力破解攻击: $brute_force_count 次"
    fi
    
    # 检测DDoS攻击模式
    local request_count=$(docker-compose logs nginx 2>/dev/null | grep "$(date '+%d/%b/%Y:%H')" | wc -l || echo 0)
    if [ "$request_count" -gt "$MAX_REQUESTS_PER_MINUTE" ]; then
        log_security "🚨 检测到可能的DDoS攻击: $request_count 个请求/分钟"
        emergency_response "ddos_attack" "$request_count"
    fi
}

# 紧急响应处理
emergency_response() {
    local attack_type=$1
    local details=$2
    
    log_emergency "🚨🚨🚨 触发紧急响应 - 攻击类型: $attack_type, 详情: $details"
    
    case $attack_type in
        "multiple_attacks")
            # 多重攻击 - 激活紧急模式
            log_emergency "激活系统紧急模式"
            ./disaster-recovery.sh backup-now &
            
            # 通知管理员
            curl -X POST http://localhost:8080/api/admin/security/emergency-mode/activate \
                -H "Content-Type: application/json" \
                -d "{\"reason\":\"自动检测到多重攻击: $details 次攻击\"}" 2>/dev/null || true
            ;;
            
        "service_failure")
            # 服务失败 - 可能遭受攻击
            log_emergency "关键服务失败，可能遭受攻击"
            
            # 隔离网络
            ./disaster-recovery.sh isolate-network &
            ;;
            
        "database_failure")
            # 数据库失败 - 立即备份
            log_emergency "数据库连接失败，立即执行紧急备份"
            ./disaster-recovery.sh backup-now &
            ;;
            
        "ddos_attack")
            # DDoS攻击 - 限制访问
            log_emergency "检测到DDoS攻击，限制网络访问"
            
            # 可以添加iptables规则限制连接
            # iptables -A INPUT -p tcp --dport 80 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT
            ;;
    esac
    
    # 记录紧急响应事件
    {
        echo "=== 紧急响应事件 $(date) ==="
        echo "攻击类型: $attack_type"
        echo "详情: $details"
        echo "响应措施: 已触发相应的紧急处理程序"
        echo ""
    } >> emergency-response.log
}

# 生成安全报告
generate_security_report() {
    local report_file="security-report-$(date +%Y%m%d_%H%M%S).html"
    
    {
        cat << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>安全监控报告</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 20px; border-radius: 8px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .critical { border-color: #dc3545; background: #f8d7da; }
        .warning { border-color: #ffc107; background: #fff3cd; }
        .success { border-color: #28a745; background: #d4edda; }
        .metric { display: inline-block; margin: 10px; padding: 10px; background: #f8f9fa; border-radius: 5px; }
    </style>
</head>
<body>
EOF
        
        echo "    <div class=\"header\">"
        echo "        <h1>🛡️ 登录安全监控系统 - 安全报告</h1>"
        echo "        <p>生成时间: $(date)</p>"
        echo "    </div>"
        
        # 系统状态概览
        echo "    <div class=\"section\">"
        echo "        <h2>📊 系统状态概览</h2>"
        echo "        <div class=\"metric\">服务运行状态: $(docker-compose ps | grep -c "Up")/4</div>"
        echo "        <div class=\"metric\">磁盘使用率: $(df / | awk 'NR==2 {print $5}')</div>"
        echo "        <div class=\"metric\">内存使用率: $(free | awk 'NR==2{printf "%.0f%%", $3*100/$2}')</div>"
        echo "    </div>"
        
        # 安全事件统计
        if [ -f "logs/security-monitor.log" ]; then
            echo "    <div class=\"section\">"
            echo "        <h2>🚨 安全事件统计（今日）</h2>"
            
            local today=$(date '+%Y-%m-%d')
            local attacks=$(grep "攻击检测" logs/security-monitor.log | grep "$today" | wc -l || echo 0)
            local blocked_ips=$(grep "IP已自动拉黑" logs/security-monitor.log | grep "$today" | wc -l || echo 0)
            local emergency_events=$(grep "EMERGENCY" logs/security-monitor.log | grep "$today" | wc -l || echo 0)
            
            echo "        <div class=\"metric\">攻击事件: $attacks 次</div>"
            echo "        <div class=\"metric\">拉黑IP: $blocked_ips 个</div>"
            echo "        <div class=\"metric\">紧急事件: $emergency_events 次</div>"
            
            if [ "$attacks" -gt 10 ]; then
                echo "        <div class=\"critical\">"
                echo "            <h3>⚠️ 高危告警</h3>"
                echo "            <p>今日攻击事件较多 ($attacks 次)，建议立即检查系统安全状态</p>"
                echo "        </div>"
            fi
        fi
        
        echo "    </div>"
        
        # 备份状态
        echo "    <div class=\"section\">"
        echo "        <h2>📦 备份状态</h2>"
        if [ -d "backups" ]; then
            local backup_count=$(find backups -name "*.sql*" | wc -l)
            local latest_backup=$(find backups -name "*.sql*" -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)
            
            echo "        <div class=\"metric\">总备份数: $backup_count</div>"
            echo "        <div class=\"metric\">最新备份: $(basename "$latest_backup")</div>"
            
            if [ "$backup_count" -gt 0 ]; then
                echo "        <div class=\"success\">"
                echo "            <p>✅ 数据备份正常</p>"
                echo "        </div>"
            fi
        else
            echo "        <div class=\"critical\">"
            echo "            <p>❌ 未找到备份文件，数据安全风险极高</p>"
            echo "        </div>"
        fi
        echo "    </div>"
        
        echo "</body>"
        echo "</html>"
        
    } > "$report_file"
    
    log_success "安全报告已生成: $report_file"
}

# 显示使用帮助
show_help() {
    echo "安全监控脚本"
    echo ""
    echo "使用方法:"
    echo "  $0 <操作>"
    echo ""
    echo "操作:"
    echo "  start     - 启动安全监控"
    echo "  stop      - 停止安全监控"
    echo "  status    - 查看监控状态"
    echo "  report    - 生成安全报告"
    echo "  test      - 执行测试检查"
    echo ""
    echo "示例:"
    echo "  $0 start"
    echo "  $0 report"
}

# 主函数
main() {
    case "${1:-start}" in
        start)
            start_monitor
            ;;
        stop)
            if check_running; then
                kill "$(cat $PID_FILE)"
                rm -f "$PID_FILE"
                log_success "安全监控已停止"
            else
                log_info "安全监控未在运行"
            fi
            ;;
        status)
            if check_running; then
                log_info "安全监控正在运行 (PID: $(cat $PID_FILE))"
            else
                log_info "安全监控未运行"
            fi
            ;;
        report)
            generate_security_report
            ;;
        test)
            log_info "执行测试检查..."
            monitor_cycle
            log_success "测试检查完成"
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            echo "未知操作: $1"
            show_help
            exit 1
            ;;
    esac
}

# 运行主函数
main "$@"