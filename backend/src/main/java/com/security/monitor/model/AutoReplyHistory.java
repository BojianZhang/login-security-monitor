package com.security.monitor.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 自动回复历史记录实体
 */
@Entity
@Table(name = "auto_reply_history")
public class AutoReplyHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settings_id", nullable = false)
    private AutoReplySettings settings;
    
    @Column(name = "from_address", nullable = false)
    private String fromAddress;
    
    @Column(name = "to_address", nullable = false)
    private String toAddress;
    
    @Column(name = "original_subject")
    private String originalSubject;
    
    @Column(name = "reply_subject", nullable = false)
    private String replySubject;
    
    @Column(name = "reply_sent")
    private Boolean replySent = false;
    
    @Column(name = "reply_error")
    private String replyError;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public AutoReplyHistory() {}
    
    public AutoReplyHistory(AutoReplySettings settings, String fromAddress, String toAddress, String originalSubject, String replySubject) {
        this.settings = settings;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.originalSubject = originalSubject;
        this.replySubject = replySubject;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public AutoReplySettings getSettings() {
        return settings;
    }
    
    public void setSettings(AutoReplySettings settings) {
        this.settings = settings;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    public String getToAddress() {
        return toAddress;
    }
    
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
    
    public String getOriginalSubject() {
        return originalSubject;
    }
    
    public void setOriginalSubject(String originalSubject) {
        this.originalSubject = originalSubject;
    }
    
    public String getReplySubject() {
        return replySubject;
    }
    
    public void setReplySubject(String replySubject) {
        this.replySubject = replySubject;
    }
    
    public Boolean getReplySent() {
        return replySent;
    }
    
    public void setReplySent(Boolean replySent) {
        this.replySent = replySent;
    }
    
    public String getReplyError() {
        return replyError;
    }
    
    public void setReplyError(String replyError) {
        this.replyError = replyError;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}