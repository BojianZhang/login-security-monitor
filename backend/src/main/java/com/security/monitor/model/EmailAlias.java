package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 邮箱别名实体
 */
@Entity
@Table(name = "email_aliases", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"alias_email", "domain_id"}))
@EntityListeners(com.security.monitor.config.EmailAliasEntityListener.class)
public class EmailAlias {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "别名邮箱不能为空")
    @Column(name = "alias_email", nullable = false)
    private String aliasEmail;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private EmailDomain domain;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_catch_all")
    private Boolean isCatchAll = false;
    
    @Email(message = "转发地址格式不正确")
    @Column(name = "forward_to")
    private String forwardTo;
    
    @Column(name = "display_name", length = 100)
    private String displayName; // 自定义显示名称，用于与外部平台保持一致
    
    @Column(name = "external_alias_id", length = 100)
    private String externalAliasId; // 外部平台的别名ID，用于同步
    
    @Column(name = "description")
    private String description; // 别名描述
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EmailAlias() {}
    
    public EmailAlias(User user, String aliasEmail, EmailDomain domain) {
        this.user = user;
        this.aliasEmail = aliasEmail;
        this.domain = domain;
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
    
    public String getAliasEmail() {
        return aliasEmail;
    }
    
    public void setAliasEmail(String aliasEmail) {
        this.aliasEmail = aliasEmail;
    }
    
    public EmailDomain getDomain() {
        return domain;
    }
    
    public void setDomain(EmailDomain domain) {
        this.domain = domain;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsCatchAll() {
        return isCatchAll;
    }
    
    public void setIsCatchAll(Boolean isCatchAll) {
        this.isCatchAll = isCatchAll;
    }
    
    public String getForwardTo() {
        return forwardTo;
    }
    
    public void setForwardTo(String forwardTo) {
        this.forwardTo = forwardTo;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getExternalAliasId() {
        return externalAliasId;
    }
    
    public void setExternalAliasId(String externalAliasId) {
        this.externalAliasId = externalAliasId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
     * 获取完整的邮箱地址
     */
    public String getFullEmail() {
        return aliasEmail + "@" + domain.getDomainName();
    }
    
    /**
     * 获取应该显示的名称
     * 优先使用自定义显示名称，如果没有则使用完整邮箱地址
     */
    public String getDisplayNameOrEmail() {
        return displayName != null && !displayName.trim().isEmpty() 
            ? displayName 
            : getFullEmail();
    }
    
    /**
     * 获取简短的显示名称
     * 优先使用自定义显示名称，如果没有则使用别名前缀
     */
    public String getShortDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() 
            ? displayName 
            : aliasEmail;
    }
}