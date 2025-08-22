package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统度量指标实体
 * 用于收集和存储各种系统性能和使用指标
 */
@Entity
@Table(name = "system_metrics")
public class SystemMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metric_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MetricType metricType;
    
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;
    
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;
    
    @Column(name = "metric_unit", length = 20)
    private String metricUnit;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "domain_name", length = 255)
    private String domainName;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "time_period", length = 20)
    @Enumerated(EnumType.STRING)
    private TimePeriod timePeriod;
    
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;
    
    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;
    
    @Column(name = "labels", columnDefinition = "JSON")
    private String labels; // 额外的标签信息
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 指标类型枚举
    public enum MetricType {
        // 邮件相关指标
        EMAIL_THROUGHPUT("邮件吞吐量"),
        EMAIL_LATENCY("邮件延迟"),
        EMAIL_QUEUE_SIZE("邮件队列大小"),
        EMAIL_ERROR_RATE("邮件错误率"),
        
        // 系统性能指标
        CPU_USAGE("CPU使用率"),
        MEMORY_USAGE("内存使用率"),
        DISK_USAGE("磁盘使用率"),
        NETWORK_IO("网络IO"),
        
        // 协议相关指标
        SMTP_CONNECTIONS("SMTP连接数"),
        IMAP_CONNECTIONS("IMAP连接数"),
        POP3_CONNECTIONS("POP3连接数"),
        ACTIVE_SESSIONS("活跃会话数"),
        
        // 安全相关指标
        FAILED_LOGINS("登录失败次数"),
        BLOCKED_IPS("被阻止的IP数"),
        SPAM_BLOCKED("拦截的垃圾邮件"),
        VIRUS_DETECTED("检测到的病毒"),
        
        // 存储相关指标
        STORAGE_USAGE("存储使用量"),
        ATTACHMENT_SIZE("附件大小"),
        DATABASE_SIZE("数据库大小"),
        BACKUP_SIZE("备份大小"),
        
        // 用户相关指标
        ACTIVE_USERS("活跃用户数"),
        NEW_USERS("新用户数"),
        USER_SESSIONS("用户会话数"),
        API_REQUESTS("API请求数");
        
        private final String description;
        
        MetricType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 时间周期枚举
    public enum TimePeriod {
        REAL_TIME("实时"),
        MINUTE("分钟"),
        HOUR("小时"),
        DAY("天"),
        WEEK("周"),
        MONTH("月"),
        YEAR("年");
        
        private final String description;
        
        TimePeriod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public SystemMetrics() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SystemMetrics(MetricType metricType, String metricName, Double metricValue, String metricUnit) {
        this();
        this.metricType = metricType;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.metricUnit = metricUnit;
        this.timePeriod = TimePeriod.REAL_TIME;
        this.periodStart = LocalDateTime.now();
        this.periodEnd = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MetricType getMetricType() { return metricType; }
    public void setMetricType(MetricType metricType) { this.metricType = metricType; }
    
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    
    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }
    
    public String getMetricUnit() { return metricUnit; }
    public void setMetricUnit(String metricUnit) { this.metricUnit = metricUnit; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public TimePeriod getTimePeriod() { return timePeriod; }
    public void setTimePeriod(TimePeriod timePeriod) { this.timePeriod = timePeriod; }
    
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    
    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * 格式化显示值
     */
    public String getFormattedValue() {
        if (metricUnit != null) {
            switch (metricUnit) {
                case "bytes":
                    return formatBytes(metricValue.longValue());
                case "percentage":
                    return String.format("%.2f%%", metricValue);
                case "milliseconds":
                    return String.format("%.2f ms", metricValue);
                case "seconds":
                    return String.format("%.2f s", metricValue);
                case "requests_per_second":
                    return String.format("%.2f req/s", metricValue);
                case "emails_per_minute":
                    return String.format("%.0f emails/min", metricValue);
                case "connections":
                    return String.format("%.0f", metricValue);
                default:
                    return String.format("%.2f %s", metricValue, metricUnit);
            }
        }
        
        return String.format("%.2f", metricValue);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * 获取趋势指示
     */
    public TrendIndicator getTrendIndicator(Double previousValue) {
        if (previousValue == null) return TrendIndicator.STABLE;
        
        double changePercent = ((metricValue - previousValue) / previousValue) * 100;
        
        if (changePercent > 5) return TrendIndicator.INCREASING;
        if (changePercent < -5) return TrendIndicator.DECREASING;
        return TrendIndicator.STABLE;
    }
    
    public enum TrendIndicator {
        INCREASING, DECREASING, STABLE
    }
}