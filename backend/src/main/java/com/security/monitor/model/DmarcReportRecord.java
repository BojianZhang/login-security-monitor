package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DMARC报告记录实体
 */
@Entity
@Table(name = "dmarc_report_records")
public class DmarcReportRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dmarc_report_id", nullable = false)
    private DmarcReport dmarcReport;
    
    @Column(name = "source_ip", nullable = false, length = 45)
    private String sourceIp; // 发送IP地址
    
    @Column(name = "count", nullable = false)
    private Long count = 1L; // 消息数量
    
    @Column(name = "disposition", length = 20)
    @Enumerated(EnumType.STRING)
    private DispositionType disposition; // 处置结果
    
    @Column(name = "header_from", nullable = false, length = 255)
    private String headerFrom; // From头域名
    
    @Column(name = "envelope_from", length = 255)
    private String envelopeFrom; // 信封发件人域名
    
    // SPF验证结果
    @Column(name = "spf_domain", length = 255)
    private String spfDomain;
    
    @Column(name = "spf_result", length = 20)
    @Enumerated(EnumType.STRING)
    private AuthResult spfResult;
    
    @Column(name = "spf_scope", length = 20)
    @Enumerated(EnumType.STRING)
    private SPFScope spfScope = SPFScope.MFROM; // SPF检查范围
    
    // DKIM验证结果
    @Column(name = "dkim_domain", length = 255)
    private String dkimDomain;
    
    @Column(name = "dkim_result", length = 20)
    @Enumerated(EnumType.STRING)
    private AuthResult dkimResult;
    
    @Column(name = "dkim_selector", length = 100)
    private String dkimSelector;
    
    @Column(name = "dkim_human_result", length = 500)
    private String dkimHumanResult; // 人类可读的DKIM结果
    
    // 对齐检查结果
    @Column(name = "dmarc_result", length = 20)
    @Enumerated(EnumType.STRING)
    private DMARCResult dmarcResult;
    
    @Column(name = "reason_type", length = 50)
    @Enumerated(EnumType.STRING)
    private PolicyOverrideReason reasonType; // 策略覆盖原因
    
    @Column(name = "reason_comment", length = 500)
    private String reasonComment; // 原因说明
    
    // 身份标识符
    @Column(name = "identities_header_from", length = 255)
    private String identitiesHeaderFrom;
    
    @Column(name = "identities_envelope_from", length = 255)
    private String identitiesEnvelopeFrom;
    
    @Column(name = "identities_envelope_to", length = 255)
    private String identitiesEnvelopeTo;
    
    // 额外信息
    @Column(name = "feedback_source", length = 100)
    private String feedbackSource; // 反馈来源
    
    @Column(name = "delivery_result", length = 20)
    @Enumerated(EnumType.STRING)
    private DeliveryResult deliveryResult; // 投递结果
    
    @Column(name = "auth_failure_type", length = 50)
    @Enumerated(EnumType.STRING)
    private AuthFailureType authFailureType; // 认证失败类型
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
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
    
    // 认证结果枚举
    public enum AuthResult {
        PASS("pass"),
        FAIL("fail"),
        SOFT_FAIL("softfail"),
        NEUTRAL("neutral"),
        TEMP_ERROR("temperror"),
        PERM_ERROR("permerror"),
        NONE("none");
        
        private final String value;
        
        AuthResult(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // SPF检查范围枚举
    public enum SPFScope {
        HELO("helo"),
        MFROM("mfrom");
        
        private final String value;
        
        SPFScope(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // DMARC结果枚举
    public enum DMARCResult {
        PASS("pass"),
        FAIL("fail");
        
        private final String value;
        
        DMARCResult(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 策略覆盖原因枚举
    public enum PolicyOverrideReason {
        FORWARDED("forwarded"),
        SAMPLED_OUT("sampled_out"),
        TRUSTED_FORWARDER("trusted_forwarder"),
        MAILING_LIST("mailing_list"),
        LOCAL_POLICY("local_policy"),
        OTHER("other");
        
        private final String value;
        
        PolicyOverrideReason(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 投递结果枚举
    public enum DeliveryResult {
        DELIVERED("delivered"),
        BOUNCE("bounce"),
        REJECT("reject"),
        UNKNOWN("unknown");
        
        private final String value;
        
        DeliveryResult(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 认证失败类型枚举
    public enum AuthFailureType {
        SPF_ALIGNMENT("spf_alignment"),
        DKIM_ALIGNMENT("dkim_alignment"),
        SPF_VALIDATION("spf_validation"),
        DKIM_VALIDATION("dkim_validation"),
        POLICY_OTHER("policy_other");
        
        private final String value;
        
        AuthFailureType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    // 构造函数
    public DmarcReportRecord() {
        this.createdAt = LocalDateTime.now();
    }
    
    public DmarcReportRecord(DmarcReport dmarcReport, String sourceIp, String headerFrom) {
        this();
        this.dmarcReport = dmarcReport;
        this.sourceIp = sourceIp;
        this.headerFrom = headerFrom;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public DmarcReport getDmarcReport() {
        return dmarcReport;
    }
    
    public void setDmarcReport(DmarcReport dmarcReport) {
        this.dmarcReport = dmarcReport;
    }
    
    public String getSourceIp() {
        return sourceIp;
    }
    
    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }
    
    public Long getCount() {
        return count;
    }
    
    public void setCount(Long count) {
        this.count = count;
    }
    
    public DispositionType getDisposition() {
        return disposition;
    }
    
    public void setDisposition(DispositionType disposition) {
        this.disposition = disposition;
    }
    
    public String getHeaderFrom() {
        return headerFrom;
    }
    
    public void setHeaderFrom(String headerFrom) {
        this.headerFrom = headerFrom;
    }
    
    public String getEnvelopeFrom() {
        return envelopeFrom;
    }
    
    public void setEnvelopeFrom(String envelopeFrom) {
        this.envelopeFrom = envelopeFrom;
    }
    
    public String getSpfDomain() {
        return spfDomain;
    }
    
    public void setSpfDomain(String spfDomain) {
        this.spfDomain = spfDomain;
    }
    
    public AuthResult getSpfResult() {
        return spfResult;
    }
    
    public void setSpfResult(AuthResult spfResult) {
        this.spfResult = spfResult;
    }
    
    public SPFScope getSpfScope() {
        return spfScope;
    }
    
    public void setSpfScope(SPFScope spfScope) {
        this.spfScope = spfScope;
    }
    
    public String getDkimDomain() {
        return dkimDomain;
    }
    
    public void setDkimDomain(String dkimDomain) {
        this.dkimDomain = dkimDomain;
    }
    
    public AuthResult getDkimResult() {
        return dkimResult;
    }
    
    public void setDkimResult(AuthResult dkimResult) {
        this.dkimResult = dkimResult;
    }
    
    public String getDkimSelector() {
        return dkimSelector;
    }
    
    public void setDkimSelector(String dkimSelector) {
        this.dkimSelector = dkimSelector;
    }
    
    public String getDkimHumanResult() {
        return dkimHumanResult;
    }
    
    public void setDkimHumanResult(String dkimHumanResult) {
        this.dkimHumanResult = dkimHumanResult;
    }
    
    public DMARCResult getDmarcResult() {
        return dmarcResult;
    }
    
    public void setDmarcResult(DMARCResult dmarcResult) {
        this.dmarcResult = dmarcResult;
    }
    
    public PolicyOverrideReason getReasonType() {
        return reasonType;
    }
    
    public void setReasonType(PolicyOverrideReason reasonType) {
        this.reasonType = reasonType;
    }
    
    public String getReasonComment() {
        return reasonComment;
    }
    
    public void setReasonComment(String reasonComment) {
        this.reasonComment = reasonComment;
    }
    
    public String getIdentitiesHeaderFrom() {
        return identitiesHeaderFrom;
    }
    
    public void setIdentitiesHeaderFrom(String identitiesHeaderFrom) {
        this.identitiesHeaderFrom = identitiesHeaderFrom;
    }
    
    public String getIdentitiesEnvelopeFrom() {
        return identitiesEnvelopeFrom;
    }
    
    public void setIdentitiesEnvelopeFrom(String identitiesEnvelopeFrom) {
        this.identitiesEnvelopeFrom = identitiesEnvelopeFrom;
    }
    
    public String getIdentitiesEnvelopeTo() {
        return identitiesEnvelopeTo;
    }
    
    public void setIdentitiesEnvelopeTo(String identitiesEnvelopeTo) {
        this.identitiesEnvelopeTo = identitiesEnvelopeTo;
    }
    
    public String getFeedbackSource() {
        return feedbackSource;
    }
    
    public void setFeedbackSource(String feedbackSource) {
        this.feedbackSource = feedbackSource;
    }
    
    public DeliveryResult getDeliveryResult() {
        return deliveryResult;
    }
    
    public void setDeliveryResult(DeliveryResult deliveryResult) {
        this.deliveryResult = deliveryResult;
    }
    
    public AuthFailureType getAuthFailureType() {
        return authFailureType;
    }
    
    public void setAuthFailureType(AuthFailureType authFailureType) {
        this.authFailureType = authFailureType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 检查DMARC是否通过
     */
    public boolean isDmarcCompliant() {
        return dmarcResult == DMARCResult.PASS;
    }
    
    /**
     * 检查SPF是否通过
     */
    public boolean isSpfPass() {
        return spfResult == AuthResult.PASS;
    }
    
    /**
     * 检查DKIM是否通过
     */
    public boolean isDkimPass() {
        return dkimResult == AuthResult.PASS;
    }
    
    /**
     * 获取认证失败的详细信息
     */
    public String getAuthFailureDetails() {
        StringBuilder details = new StringBuilder();
        
        if (!isSpfPass() && spfResult != null) {
            details.append("SPF: ").append(spfResult.getValue());
        }
        
        if (!isDkimPass() && dkimResult != null) {
            if (details.length() > 0) details.append(", ");
            details.append("DKIM: ").append(dkimResult.getValue());
        }
        
        if (authFailureType != null) {
            if (details.length() > 0) details.append(", ");
            details.append("Type: ").append(authFailureType.getValue());
        }
        
        return details.toString();
    }
}