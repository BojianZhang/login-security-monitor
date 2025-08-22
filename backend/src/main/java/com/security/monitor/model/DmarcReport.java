package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DMARC聚合报告实体
 */
@Entity
@Table(name = "dmarc_reports")
public class DmarcReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "report_id", unique = true, nullable = false, length = 255)
    private String reportId; // 报告唯一标识
    
    @Column(name = "domain", nullable = false, length = 255)
    private String domain; // 报告的域名
    
    @Column(name = "org_name", nullable = false, length = 255)
    private String orgName; // 报告组织名称
    
    @Column(name = "email", length = 320)
    private String email; // 联系邮箱
    
    @Column(name = "begin_time", nullable = false)
    private LocalDateTime beginTime; // 报告开始时间
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime; // 报告结束时间
    
    @Column(name = "policy_domain", nullable = false, length = 255)
    private String policyDomain; // 策略域名
    
    @Column(name = "policy_adkim", length = 10)
    @Enumerated(EnumType.STRING)
    private AlignmentMode policyAdkim = AlignmentMode.RELAXED; // DKIM对齐模式
    
    @Column(name = "policy_aspf", length = 10)
    @Enumerated(EnumType.STRING)
    private AlignmentMode policyAspf = AlignmentMode.RELAXED; // SPF对齐模式
    
    @Column(name = "policy_p", length = 20)
    @Enumerated(EnumType.STRING)
    private DispositionType policyP = DispositionType.NONE; // 域策略
    
    @Column(name = "policy_sp", length = 20)
    @Enumerated(EnumType.STRING)
    private DispositionType policySp; // 子域策略
    
    @Column(name = "policy_pct", nullable = false)
    private Integer policyPct = 100; // 策略百分比
    
    @Column(name = "total_messages", nullable = false)
    private Long totalMessages = 0L; // 总消息数
    
    @Column(name = "compliant_messages", nullable = false)
    private Long compliantMessages = 0L; // 合规消息数
    
    @Column(name = "failed_messages", nullable = false)
    private Long failedMessages = 0L; // 失败消息数
    
    @Column(name = "report_format", length = 20)
    @Enumerated(EnumType.STRING)
    private ReportFormat reportFormat = ReportFormat.XML;
    
    @Column(name = "report_size", nullable = false)
    private Long reportSize = 0L; // 报告大小（字节）
    
    @Column(name = "compression_type", length = 10)
    @Enumerated(EnumType.STRING)
    private CompressionType compressionType = CompressionType.GZIP;
    
    @Column(name = "report_path", length = 500)
    private String reportPath; // 报告文件路径
    
    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false; // 是否已发送
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt; // 发送时间
    
    @Column(name = "recipient_uri", length = 500)
    private String recipientUri; // 接收方URI
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage; // 错误信息
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0; // 重试次数
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt; // 下次重试时间
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "dmarcReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DmarcReportRecord> records; // 报告记录
    
    // 对齐模式枚举
    public enum AlignmentMode {
        STRICT("s"),
        RELAXED("r");
        
        private final String value;
        
        AlignmentMode(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 处置类型枚举
    public enum DispositionType {
        NONE("none"),
        QUARANTINE("quarantine"),
        REJECT("reject");
        
        private final String value;
        
        DispositionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 报告格式枚举
    public enum ReportFormat {
        XML("xml"),
        JSON("json");
        
        private final String value;
        
        ReportFormat(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 压缩类型枚举
    public enum CompressionType {
        NONE("none"),
        GZIP("gzip"),
        ZIP("zip");
        
        private final String value;
        
        CompressionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 构造函数
    public DmarcReport() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public DmarcReport(String domain, String orgName, LocalDateTime beginTime, LocalDateTime endTime) {
        this();
        this.domain = domain;
        this.orgName = orgName;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.policyDomain = domain;
        this.reportId = generateReportId();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getReportId() {
        return reportId;
    }
    
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getOrgName() {
        return orgName;
    }
    
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDateTime getBeginTime() {
        return beginTime;
    }
    
    public void setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getPolicyDomain() {
        return policyDomain;
    }
    
    public void setPolicyDomain(String policyDomain) {
        this.policyDomain = policyDomain;
    }
    
    public AlignmentMode getPolicyAdkim() {
        return policyAdkim;
    }
    
    public void setPolicyAdkim(AlignmentMode policyAdkim) {
        this.policyAdkim = policyAdkim;
    }
    
    public AlignmentMode getPolicyAspf() {
        return policyAspf;
    }
    
    public void setPolicyAspf(AlignmentMode policyAspf) {
        this.policyAspf = policyAspf;
    }
    
    public DispositionType getPolicyP() {
        return policyP;
    }
    
    public void setPolicyP(DispositionType policyP) {
        this.policyP = policyP;
    }
    
    public DispositionType getPolicySp() {
        return policySp;
    }
    
    public void setPolicySp(DispositionType policySp) {
        this.policySp = policySp;
    }
    
    public Integer getPolicyPct() {
        return policyPct;
    }
    
    public void setPolicyPct(Integer policyPct) {
        this.policyPct = policyPct;
    }
    
    public Long getTotalMessages() {
        return totalMessages;
    }
    
    public void setTotalMessages(Long totalMessages) {
        this.totalMessages = totalMessages;
    }
    
    public Long getCompliantMessages() {
        return compliantMessages;
    }
    
    public void setCompliantMessages(Long compliantMessages) {
        this.compliantMessages = compliantMessages;
    }
    
    public Long getFailedMessages() {
        return failedMessages;
    }
    
    public void setFailedMessages(Long failedMessages) {
        this.failedMessages = failedMessages;
    }
    
    public ReportFormat getReportFormat() {
        return reportFormat;
    }
    
    public void setReportFormat(ReportFormat reportFormat) {
        this.reportFormat = reportFormat;
    }
    
    public Long getReportSize() {
        return reportSize;
    }
    
    public void setReportSize(Long reportSize) {
        this.reportSize = reportSize;
    }
    
    public CompressionType getCompressionType() {
        return compressionType;
    }
    
    public void setCompressionType(CompressionType compressionType) {
        this.compressionType = compressionType;
    }
    
    public String getReportPath() {
        return reportPath;
    }
    
    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }
    
    public Boolean getIsSent() {
        return isSent;
    }
    
    public void setIsSent(Boolean isSent) {
        this.isSent = isSent;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public String getRecipientUri() {
        return recipientUri;
    }
    
    public void setRecipientUri(String recipientUri) {
        this.recipientUri = recipientUri;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }
    
    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<DmarcReportRecord> getRecords() {
        return records;
    }
    
    public void setRecords(List<DmarcReportRecord> records) {
        this.records = records;
    }
    
    /**
     * 生成报告ID
     */
    private String generateReportId() {
        return String.format("%s_%s_%d_%d", 
            orgName.replaceAll("\\s+", "_").toLowerCase(),
            domain.replaceAll("\\.", "_"),
            beginTime.toEpochSecond(java.time.ZoneOffset.UTC),
            endTime.toEpochSecond(java.time.ZoneOffset.UTC)
        );
    }
    
    /**
     * 增加消息统计
     */
    public void addMessageStats(long total, long compliant, long failed) {
        this.totalMessages += total;
        this.compliantMessages += compliant;
        this.failedMessages += failed;
    }
    
    /**
     * 标记为已发送
     */
    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }
    
    /**
     * 标记发送失败并设置重试
     */
    public void markSendFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.retryCount++;
        // 指数退避重试策略
        long retryDelayMinutes = (long) Math.pow(2, Math.min(retryCount, 6)) * 30; // 最多64*30=1920分钟
        this.nextRetryAt = LocalDateTime.now().plusMinutes(retryDelayMinutes);
    }
    
    /**
     * 计算合规率
     */
    public double getComplianceRate() {
        if (totalMessages == 0) {
            return 0.0;
        }
        return (double) compliantMessages / totalMessages * 100.0;
    }
    
    /**
     * 检查是否需要重试
     */
    public boolean needsRetry() {
        return !isSent && retryCount < 5 && 
               (nextRetryAt == null || LocalDateTime.now().isAfter(nextRetryAt));
    }
}