package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮箱群组成员实体
 */
@Entity
@Table(name = "email_group_members")
public class EmailGroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private EmailGroup group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 内部用户
    
    @Column(name = "external_email", length = 320)
    private String externalEmail; // 外部邮箱地址
    
    @Column(name = "member_name", length = 100)
    private String memberName; // 成员名称
    
    @Column(name = "member_role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MemberRole memberRole = MemberRole.MEMBER;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "can_send", nullable = false)
    private Boolean canSend = true; // 是否可以发送邮件到群组
    
    @Column(name = "can_receive", nullable = false)
    private Boolean canReceive = true; // 是否可以接收群组邮件
    
    @Column(name = "is_moderator", nullable = false)
    private Boolean isModerator = false; // 是否为审核员
    
    @Column(name = "subscription_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SubscriptionType subscriptionType = SubscriptionType.NORMAL;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    // 成员角色枚举
    public enum MemberRole {
        OWNER("所有者"),
        ADMIN("管理员"),
        MODERATOR("审核员"),
        MEMBER("成员"),
        READONLY("只读");
        
        private final String description;
        
        MemberRole(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 订阅类型枚举
    public enum SubscriptionType {
        NORMAL("正常"),
        DIGEST("摘要"),
        NOMAIL("不接收邮件"),
        SUSPENDED("暂停");
        
        private final String description;
        
        SubscriptionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public EmailGroupMember() {
        this.joinedAt = LocalDateTime.now();
    }
    
    public EmailGroupMember(EmailGroup group, User user) {
        this();
        this.group = group;
        this.user = user;
        this.memberName = user.getFullName();
    }
    
    public EmailGroupMember(EmailGroup group, String externalEmail, String memberName) {
        this();
        this.group = group;
        this.externalEmail = externalEmail;
        this.memberName = memberName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailGroup getGroup() {
        return group;
    }
    
    public void setGroup(EmailGroup group) {
        this.group = group;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.memberName = user.getFullName();
        }
    }
    
    public String getExternalEmail() {
        return externalEmail;
    }
    
    public void setExternalEmail(String externalEmail) {
        this.externalEmail = externalEmail;
    }
    
    public String getMemberName() {
        return memberName;
    }
    
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
    
    public MemberRole getMemberRole() {
        return memberRole;
    }
    
    public void setMemberRole(MemberRole memberRole) {
        this.memberRole = memberRole;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getCanSend() {
        return canSend;
    }
    
    public void setCanSend(Boolean canSend) {
        this.canSend = canSend;
    }
    
    public Boolean getCanReceive() {
        return canReceive;
    }
    
    public void setCanReceive(Boolean canReceive) {
        this.canReceive = canReceive;
    }
    
    public Boolean getIsModerator() {
        return isModerator;
    }
    
    public void setIsModerator(Boolean isModerator) {
        this.isModerator = isModerator;
    }
    
    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }
    
    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    /**
     * 获取成员的邮箱地址
     */
    public String getMemberEmail() {
        if (user != null) {
            return user.getEmail();
        }
        return externalEmail;
    }
    
    /**
     * 检查是否为内部成员
     */
    public boolean isInternalMember() {
        return user != null;
    }
    
    /**
     * 检查是否为外部成员
     */
    public boolean isExternalMember() {
        return externalEmail != null && !externalEmail.isEmpty();
    }
    
    /**
     * 更新活动时间
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}