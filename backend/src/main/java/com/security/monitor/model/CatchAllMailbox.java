package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Catch-All邮箱实体
 */
@Entity
@Table(name = "catch_all_mailboxes")
public class CatchAllMailbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private EmailDomain domain;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser; // 目标用户
    
    @Column(name = "target_email", length = 320)
    private String targetEmail; // 目标邮箱（如果是外部邮箱）
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "catch_all_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CatchAllType catchAllType = CatchAllType.DELIVER;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 优先级，数字越小优先级越高
    
    @Column(name = "max_daily_messages")
    private Integer maxDailyMessages; // 每日最大消息数限制
    
    @Column(name = "message_count", nullable = false)
    private Long messageCount = 0L; // 消息计数
    
    @Column(name = "daily_message_count", nullable = false)
    private Integer dailyMessageCount = 0; // 今日消息计数
    
    @Column(name = "last_message_date")
    private LocalDateTime lastMessageDate; // 最后消息日期
    
    @Column(name = "filter_spam", nullable = false)
    private Boolean filterSpam = true; // 是否过滤垃圾邮件
    
    @Column(name = "filter_virus", nullable = false)
    private Boolean filterVirus = true; // 是否过滤病毒邮件
    
    @Column(name = "auto_reply_enabled", nullable = false)
    private Boolean autoReplyEnabled = false; // 是否启用自动回复
    
    @Column(name = "auto_reply_message", length = 1000)
    private String autoReplyMessage; // 自动回复消息
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Catch-All类型枚举
    public enum CatchAllType {
        DELIVER("投递"),
        FORWARD("转发"),
        DISCARD("丢弃"),
        BOUNCE("退回"),
        QUARANTINE("隔离");
        
        private final String description;
        
        CatchAllType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public CatchAllMailbox() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public CatchAllMailbox(EmailDomain domain, User targetUser) {
        this();
        this.domain = domain;
        this.targetUser = targetUser;
    }
    
    public CatchAllMailbox(EmailDomain domain, String targetEmail) {
        this();
        this.domain = domain;
        this.targetEmail = targetEmail;
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
    
    public EmailDomain getDomain() {
        return domain;
    }
    
    public void setDomain(EmailDomain domain) {
        this.domain = domain;
    }
    
    public User getTargetUser() {
        return targetUser;
    }
    
    public void setTargetUser(User targetUser) {
        this.targetUser = targetUser;
    }
    
    public String getTargetEmail() {
        return targetEmail;
    }
    
    public void setTargetEmail(String targetEmail) {
        this.targetEmail = targetEmail;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public CatchAllType getCatchAllType() {
        return catchAllType;
    }
    
    public void setCatchAllType(CatchAllType catchAllType) {
        this.catchAllType = catchAllType;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Integer getMaxDailyMessages() {
        return maxDailyMessages;
    }
    
    public void setMaxDailyMessages(Integer maxDailyMessages) {
        this.maxDailyMessages = maxDailyMessages;
    }
    
    public Long getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Long messageCount) {
        this.messageCount = messageCount;
    }
    
    public Integer getDailyMessageCount() {
        return dailyMessageCount;
    }
    
    public void setDailyMessageCount(Integer dailyMessageCount) {
        this.dailyMessageCount = dailyMessageCount;
    }
    
    public LocalDateTime getLastMessageDate() {
        return lastMessageDate;
    }
    
    public void setLastMessageDate(LocalDateTime lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }
    
    public Boolean getFilterSpam() {
        return filterSpam;
    }
    
    public void setFilterSpam(Boolean filterSpam) {
        this.filterSpam = filterSpam;
    }
    
    public Boolean getFilterVirus() {
        return filterVirus;
    }
    
    public void setFilterVirus(Boolean filterVirus) {
        this.filterVirus = filterVirus;
    }
    
    public Boolean getAutoReplyEnabled() {
        return autoReplyEnabled;
    }
    
    public void setAutoReplyEnabled(Boolean autoReplyEnabled) {
        this.autoReplyEnabled = autoReplyEnabled;
    }
    
    public String getAutoReplyMessage() {
        return autoReplyMessage;
    }
    
    public void setAutoReplyMessage(String autoReplyMessage) {
        this.autoReplyMessage = autoReplyMessage;
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
     * 获取目标邮箱地址
     */
    public String getTargetEmailAddress() {
        if (targetUser != null) {
            return targetUser.getEmail();
        }
        return targetEmail;
    }
    
    /**
     * 增加消息计数
     */
    public void incrementMessageCount() {
        this.messageCount++;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        
        // 检查是否是新的一天
        if (lastMessageDate == null || lastMessageDate.isBefore(today)) {
            this.dailyMessageCount = 1;
        } else {
            this.dailyMessageCount++;
        }
        
        this.lastMessageDate = now;
    }
    
    /**
     * 检查是否达到每日限制
     */
    public boolean isAtDailyLimit() {
        if (maxDailyMessages == null) return false;
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        // 如果最后消息不是今天的，重置计数
        if (lastMessageDate == null || lastMessageDate.isBefore(today)) {
            return false;
        }
        
        return dailyMessageCount >= maxDailyMessages;
    }
    
    /**
     * 重置每日计数
     */
    public void resetDailyCount() {
        this.dailyMessageCount = 0;
    }
}