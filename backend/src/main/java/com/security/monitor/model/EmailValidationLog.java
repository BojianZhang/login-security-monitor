package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件验证日志实体
 */
@Entity
@Table(name = "email_validation_logs")
public class EmailValidationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private EmailMessage message;
    
    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;
    
    @Column(name = "spf_status", length = 20)
    private String spfStatus;
    
    @Column(name = "spf_record", length = 1000)
    private String spfRecord;
    
    @Column(name = "dkim_status", length = 20)
    private String dkimStatus;
    
    @Column(name = "dkim_domain", length = 255)
    private String dkimDomain;
    
    @Column(name = "dkim_selector", length = 100)
    private String dkimSelector;
    
    @Column(name = "dmarc_status", length = 20)
    private String dmarcStatus;
    
    @Column(name = "dmarc_record", length = 1000)
    private String dmarcRecord;
    
    @Column(name = "dmarc_policy", length = 20)
    private String dmarcPolicy;
    
    @Column(name = "sender_ip", length = 45)
    private String senderIp;
    
    @Column(name = "validation_details", length = 2000)
    private String validationDetails;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "validated_at", nullable = false)
    private LocalDateTime validatedAt;
    
    // 构造函数
    public EmailValidationLog() {
        this.validatedAt = LocalDateTime.now();
    }
    
    public EmailValidationLog(EmailMessage message, String validationStatus) {
        this();
        this.message = message;
        this.validationStatus = validationStatus;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailMessage getMessage() {
        return message;
    }
    
    public void setMessage(EmailMessage message) {
        this.message = message;
    }
    
    public String getValidationStatus() {
        return validationStatus;
    }
    
    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
    
    public String getSpfStatus() {
        return spfStatus;
    }
    
    public void setSpfStatus(String spfStatus) {
        this.spfStatus = spfStatus;
    }
    
    public String getSpfRecord() {
        return spfRecord;
    }
    
    public void setSpfRecord(String spfRecord) {
        this.spfRecord = spfRecord;
    }
    
    public String getDkimStatus() {
        return dkimStatus;
    }
    
    public void setDkimStatus(String dkimStatus) {
        this.dkimStatus = dkimStatus;
    }
    
    public String getDkimDomain() {
        return dkimDomain;
    }
    
    public void setDkimDomain(String dkimDomain) {
        this.dkimDomain = dkimDomain;
    }
    
    public String getDkimSelector() {
        return dkimSelector;
    }
    
    public void setDkimSelector(String dkimSelector) {
        this.dkimSelector = dkimSelector;
    }
    
    public String getDmarcStatus() {
        return dmarcStatus;
    }
    
    public void setDmarcStatus(String dmarcStatus) {
        this.dmarcStatus = dmarcStatus;
    }
    
    public String getDmarcRecord() {
        return dmarcRecord;
    }
    
    public void setDmarcRecord(String dmarcRecord) {
        this.dmarcRecord = dmarcRecord;
    }
    
    public String getDmarcPolicy() {
        return dmarcPolicy;
    }
    
    public void setDmarcPolicy(String dmarcPolicy) {
        this.dmarcPolicy = dmarcPolicy;
    }
    
    public String getSenderIp() {
        return senderIp;
    }
    
    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }
    
    public String getValidationDetails() {
        return validationDetails;
    }
    
    public void setValidationDetails(String validationDetails) {
        this.validationDetails = validationDetails;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }
    
    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }
}