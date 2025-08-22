package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 安全警报实体类
 */
@Entity
@Table(name = "security_alerts")
public class SecurityAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_record_id")
    private LoginRecord loginRecord;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MEDIUM;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "risk_score")
    private Integer riskScore = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 枚举类型
    public enum AlertType {
        ANOMALOUS_LOCATION,
        MULTIPLE_LOCATIONS,
        SUSPICIOUS_DEVICE,
        BRUTE_FORCE,
        HIGH_RISK_IP
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    }

    // 构造函数
    public SecurityAlert() {}
    
    public SecurityAlert(User user, AlertType alertType, String title, String description) {
        this.user = user;
        this.alertType = alertType;
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // JPA生命周期回调
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 业务方法
    public void resolve(User handledBy) {
        this.status = Status.RESOLVED;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
    }

    public void markAsFalsePositive(User handledBy) {
        this.status = Status.FALSE_POSITIVE;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
    }

    public void startInvestigation(User handledBy) {
        this.status = Status.INVESTIGATING;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return this.status == Status.OPEN;
    }

    public boolean isCritical() {
        return this.severity == Severity.CRITICAL;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LoginRecord getLoginRecord() {
        return loginRecord;
    }

    public void setLoginRecord(LoginRecord loginRecord) {
        this.loginRecord = loginRecord;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(User handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
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
}