package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 自动回复设置实体
 */
@Entity
@Table(name = "auto_reply_settings")
public class AutoReplySettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alias_id", nullable = false)
    private EmailAlias alias;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = false;
    
    @NotBlank(message = "回复主题不能为空")
    @Column(name = "reply_subject", nullable = false)
    private String replySubject;
    
    @NotBlank(message = "回复内容不能为空")
    @Column(name = "reply_content", columnDefinition = "TEXT", nullable = false)
    private String replyContent;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "only_external")
    private Boolean onlyExternal = false; // 是否只对外部邮件回复
    
    @Column(name = "max_replies_per_sender")
    private Integer maxRepliesPerSender = 1; // 每个发件人最大回复次数
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public AutoReplySettings() {}
    
    public AutoReplySettings(EmailAlias alias) {
        this.alias = alias;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailAlias getAlias() {
        return alias;
    }
    
    public void setAlias(EmailAlias alias) {
        this.alias = alias;
    }
    
    public Boolean getIsEnabled() {
        return isEnabled;
    }
    
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
    
    public String getReplySubject() {
        return replySubject;
    }
    
    public void setReplySubject(String replySubject) {
        this.replySubject = replySubject;
    }
    
    public String getReplyContent() {
        return replyContent;
    }
    
    public void setReplyContent(String replyContent) {
        this.replyContent = replyContent;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getOnlyExternal() {
        return onlyExternal;
    }
    
    public void setOnlyExternal(Boolean onlyExternal) {
        this.onlyExternal = onlyExternal;
    }
    
    public Integer getMaxRepliesPerSender() {
        return maxRepliesPerSender;
    }
    
    public void setMaxRepliesPerSender(Integer maxRepliesPerSender) {
        this.maxRepliesPerSender = maxRepliesPerSender;
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
    
    /**
     * 检查当前时间是否在自动回复时间范围内
     */
    public boolean isInActivePeriod() {
        LocalDateTime now = LocalDateTime.now();
        
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查是否应该对指定发件人自动回复
     */
    public boolean shouldReplyTo(String fromAddress) {
        if (!isEnabled || !isInActivePeriod()) {
            return false;
        }
        
        // 如果设置了只对外部邮件回复，检查是否为内部邮件
        if (onlyExternal && isInternalEmail(fromAddress)) {
            return false;
        }
        
        return true;
    }
    
    private boolean isInternalEmail(String fromAddress) {
        // 简单判断是否为内部邮件（基于域名）
        String aliasDomain = alias.getDomain().getDomainName();
        return fromAddress.toLowerCase().endsWith("@" + aliasDomain.toLowerCase());
    }
}