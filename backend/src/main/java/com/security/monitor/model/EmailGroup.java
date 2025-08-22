package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * 邮箱群组实体
 */
@Entity
@Table(name = "email_groups")
public class EmailGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private EmailDomain domain;
    
    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName; // 群组名称，如 "support"
    
    @Column(name = "group_email", nullable = false, length = 320)
    private String groupEmail; // 完整群组邮箱，如 "support@example.com"
    
    @Column(name = "display_name", length = 100)
    private String displayName; // 显示名称
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "group_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GroupType groupType = GroupType.DISTRIBUTION;
    
    @Column(name = "max_members")
    private Integer maxMembers; // 最大成员数限制
    
    @Column(name = "allow_external_senders", nullable = false)
    private Boolean allowExternalSenders = false; // 是否允许外部发件人
    
    @Column(name = "require_moderation", nullable = false)
    private Boolean requireModeration = false; // 是否需要审核
    
    @Column(name = "auto_subscribe", nullable = false)
    private Boolean autoSubscribe = false; // 新用户是否自动订阅
    
    @Column(name = "message_count", nullable = false)
    private Long messageCount = 0L; // 消息计数
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt; // 最后消息时间
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 群组成员关系
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<EmailGroupMember> members = new HashSet<>();
    
    // 群组类型枚举
    public enum GroupType {
        DISTRIBUTION("分发列表"),
        MAILING_LIST("邮件列表"),
        ANNOUNCEMENT("公告列表"),
        DEPARTMENT("部门群组"),
        PROJECT("项目群组"),
        SECURITY("安全群组");
        
        private final String description;
        
        GroupType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public EmailGroup() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public EmailGroup(EmailDomain domain, String groupName) {
        this();
        this.domain = domain;
        this.groupName = groupName;
        this.groupEmail = groupName + "@" + domain.getDomainName();
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
        if (domain != null && groupName != null) {
            this.groupEmail = groupName + "@" + domain.getDomainName();
        }
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
        if (domain != null && groupName != null) {
            this.groupEmail = groupName + "@" + domain.getDomainName();
        }
    }
    
    public String getGroupEmail() {
        return groupEmail;
    }
    
    public void setGroupEmail(String groupEmail) {
        this.groupEmail = groupEmail;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public GroupType getGroupType() {
        return groupType;
    }
    
    public void setGroupType(GroupType groupType) {
        this.groupType = groupType;
    }
    
    public Integer getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public Boolean getAllowExternalSenders() {
        return allowExternalSenders;
    }
    
    public void setAllowExternalSenders(Boolean allowExternalSenders) {
        this.allowExternalSenders = allowExternalSenders;
    }
    
    public Boolean getRequireModeration() {
        return requireModeration;
    }
    
    public void setRequireModeration(Boolean requireModeration) {
        this.requireModeration = requireModeration;
    }
    
    public Boolean getAutoSubscribe() {
        return autoSubscribe;
    }
    
    public void setAutoSubscribe(Boolean autoSubscribe) {
        this.autoSubscribe = autoSubscribe;
    }
    
    public Long getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Long messageCount) {
        this.messageCount = messageCount;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
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
    
    public Set<EmailGroupMember> getMembers() {
        return members;
    }
    
    public void setMembers(Set<EmailGroupMember> members) {
        this.members = members;
    }
    
    /**
     * 增加消息计数
     */
    public void incrementMessageCount() {
        this.messageCount++;
        this.lastMessageAt = LocalDateTime.now();
    }
    
    /**
     * 获取活跃成员数量
     */
    public long getActiveMemberCount() {
        return members.stream()
            .filter(member -> member.getIsActive())
            .count();
    }
    
    /**
     * 检查是否达到最大成员数
     */
    public boolean isAtMaxCapacity() {
        if (maxMembers == null) return false;
        return getActiveMemberCount() >= maxMembers;
    }
}