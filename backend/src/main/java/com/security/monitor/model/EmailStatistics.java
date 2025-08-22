package com.security.monitor.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * 邮件统计实体
 */
@Entity
@Table(name = "email_statistics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stat_date"}))
public class EmailStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private EmailDomain domain;
    
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    
    @Column(name = "messages_received")
    private Integer messagesReceived = 0;
    
    @Column(name = "messages_sent")
    private Integer messagesSent = 0;
    
    @Column(name = "messages_blocked")
    private Integer messagesBlocked = 0;
    
    @Column(name = "storage_used")
    private Long storageUsed = 0L;
    
    @Column(name = "unique_senders")
    private Integer uniqueSenders = 0;
    
    @Column(name = "spam_count")
    private Integer spamCount = 0;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    
    // Constructors
    public EmailStatistics() {}
    
    public EmailStatistics(User user, LocalDate statDate) {
        this.user = user;
        this.statDate = statDate;
    }
    
    public EmailStatistics(EmailDomain domain, LocalDate statDate) {
        this.domain = domain;
        this.statDate = statDate;
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
    
    public EmailDomain getDomain() {
        return domain;
    }
    
    public void setDomain(EmailDomain domain) {
        this.domain = domain;
    }
    
    public LocalDate getStatDate() {
        return statDate;
    }
    
    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }
    
    public Integer getMessagesReceived() {
        return messagesReceived;
    }
    
    public void setMessagesReceived(Integer messagesReceived) {
        this.messagesReceived = messagesReceived;
    }
    
    public Integer getMessagesSent() {
        return messagesSent;
    }
    
    public void setMessagesSent(Integer messagesSent) {
        this.messagesSent = messagesSent;
    }
    
    public Integer getMessagesBlocked() {
        return messagesBlocked;
    }
    
    public void setMessagesBlocked(Integer messagesBlocked) {
        this.messagesBlocked = messagesBlocked;
    }
    
    public Long getStorageUsed() {
        return storageUsed;
    }
    
    public void setStorageUsed(Long storageUsed) {
        this.storageUsed = storageUsed;
    }
    
    public Integer getUniqueSenders() {
        return uniqueSenders;
    }
    
    public void setUniqueSenders(Integer uniqueSenders) {
        this.uniqueSenders = uniqueSenders;
    }
    
    public Integer getSpamCount() {
        return spamCount;
    }
    
    public void setSpamCount(Integer spamCount) {
        this.spamCount = spamCount;
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 增加接收消息计数
     */
    public void incrementMessagesReceived() {
        this.messagesReceived++;
    }
    
    /**
     * 增加发送消息计数
     */
    public void incrementMessagesSent() {
        this.messagesSent++;
    }
    
    /**
     * 增加拦截消息计数
     */
    public void incrementMessagesBlocked() {
        this.messagesBlocked++;
    }
    
    /**
     * 增加垃圾邮件计数
     */
    public void incrementSpamCount() {
        this.spamCount++;
    }
    
    /**
     * 计算垃圾邮件比例
     */
    public double getSpamRatio() {
        if (messagesReceived == 0) {
            return 0.0;
        }
        return (double) spamCount / messagesReceived;
    }
}