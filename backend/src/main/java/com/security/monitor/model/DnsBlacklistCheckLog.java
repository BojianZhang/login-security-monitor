package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DNS黑名单检查日志实体
 */
@Entity
@Table(name = "dns_blacklist_check_logs")
public class DnsBlacklistCheckLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private EmailMessage message;
    
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    @Column(name = "check_status", nullable = false, length = 20)
    private String checkStatus;
    
    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;
    
    @Column(name = "total_weight", nullable = false)
    private Double totalWeight = 0.0;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel;
    
    @Column(name = "blacklists_checked", nullable = false)
    private Integer blacklistsChecked = 0;
    
    @Column(name = "check_details", length = 2000)
    private String checkDetails;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
    
    // 构造函数
    public DnsBlacklistCheckLog() {
        this.checkedAt = LocalDateTime.now();
    }
    
    public DnsBlacklistCheckLog(String ipAddress, String checkStatus) {
        this();
        this.ipAddress = ipAddress;
        this.checkStatus = checkStatus;
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
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getCheckStatus() {
        return checkStatus;
    }
    
    public void setCheckStatus(String checkStatus) {
        this.checkStatus = checkStatus;
    }
    
    public Integer getHitCount() {
        return hitCount;
    }
    
    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }
    
    public Double getTotalWeight() {
        return totalWeight;
    }
    
    public void setTotalWeight(Double totalWeight) {
        this.totalWeight = totalWeight;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public Integer getBlacklistsChecked() {
        return blacklistsChecked;
    }
    
    public void setBlacklistsChecked(Integer blacklistsChecked) {
        this.blacklistsChecked = blacklistsChecked;
    }
    
    public String getCheckDetails() {
        return checkDetails;
    }
    
    public void setCheckDetails(String checkDetails) {
        this.checkDetails = checkDetails;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
    
    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}