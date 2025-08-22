package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 垃圾邮件检测日志实体
 */
@Entity
@Table(name = "spam_detection_logs")
public class SpamDetectionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private EmailMessage message;
    
    @Column(name = "spam_score", nullable = false)
    private Double spamScore;
    
    @Column(name = "is_spam", nullable = false)
    private Boolean isSpam;
    
    @Column(name = "detection_details", columnDefinition = "TEXT")
    private String detectionDetails; // 检测详情，包括触发的规则
    
    @Column(name = "detection_method", length = 50)
    private String detectionMethod; // AUTO, MANUAL, LEARNING
    
    @Column(name = "is_user_feedback", nullable = false)
    private Boolean isUserFeedback = false; // 是否为用户反馈
    
    @Column(name = "user_marked_as_spam")
    private Boolean userMarkedAsSpam; // 用户标记为垃圾邮件
    
    @Column(name = "action_taken", length = 50)
    private String actionTaken; // QUARANTINE, DELETE, ALLOW, MARK
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs; // 处理时间（毫秒）
    
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;
    
    // 构造函数
    public SpamDetectionLog() {
        this.detectedAt = LocalDateTime.now();
    }
    
    public SpamDetectionLog(EmailMessage message, Double spamScore, Boolean isSpam) {
        this();
        this.message = message;
        this.spamScore = spamScore;
        this.isSpam = isSpam;
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
    
    public Double getSpamScore() {
        return spamScore;
    }
    
    public void setSpamScore(Double spamScore) {
        this.spamScore = spamScore;
    }
    
    public Boolean getIsSpam() {
        return isSpam;
    }
    
    public void setIsSpam(Boolean isSpam) {
        this.isSpam = isSpam;
    }
    
    public String getDetectionDetails() {
        return detectionDetails;
    }
    
    public void setDetectionDetails(String detectionDetails) {
        this.detectionDetails = detectionDetails;
    }
    
    public String getDetectionMethod() {
        return detectionMethod;
    }
    
    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }
    
    public Boolean getIsUserFeedback() {
        return isUserFeedback;
    }
    
    public void setIsUserFeedback(Boolean isUserFeedback) {
        this.isUserFeedback = isUserFeedback;
    }
    
    public Boolean getUserMarkedAsSpam() {
        return userMarkedAsSpam;
    }
    
    public void setUserMarkedAsSpam(Boolean userMarkedAsSpam) {
        this.userMarkedAsSpam = userMarkedAsSpam;
    }
    
    public String getActionTaken() {
        return actionTaken;
    }
    
    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
    
    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}