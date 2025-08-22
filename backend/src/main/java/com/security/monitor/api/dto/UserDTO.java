package com.security.monitor.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户DTO
 */
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Boolean isActive;
    private Boolean isAdmin;
    private Boolean isEmailAdmin;
    private Long storageQuota;
    private Long storageUsed;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLogin;
    
    private Boolean emailEnabled;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private List<EmailAliasDTO> aliases;
    private List<EmailFolderDTO> folders;
    
    // 构造函数
    public UserDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public Boolean getIsEmailAdmin() {
        return isEmailAdmin;
    }
    
    public void setIsEmailAdmin(Boolean isEmailAdmin) {
        this.isEmailAdmin = isEmailAdmin;
    }
    
    public Long getStorageQuota() {
        return storageQuota;
    }
    
    public void setStorageQuota(Long storageQuota) {
        this.storageQuota = storageQuota;
    }
    
    public Long getStorageUsed() {
        return storageUsed;
    }
    
    public void setStorageUsed(Long storageUsed) {
        this.storageUsed = storageUsed;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Boolean getEmailEnabled() {
        return emailEnabled;
    }
    
    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
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
    
    public List<EmailAliasDTO> getAliases() {
        return aliases;
    }
    
    public void setAliases(List<EmailAliasDTO> aliases) {
        this.aliases = aliases;
    }
    
    public List<EmailFolderDTO> getFolders() {
        return folders;
    }
    
    public void setFolders(List<EmailFolderDTO> folders) {
        this.folders = folders;
    }
    
    /**
     * 计算存储使用率
     */
    public double getStorageUsagePercentage() {
        if (storageQuota == null || storageQuota == 0) {
            return 0.0;
        }
        return (double) (storageUsed != null ? storageUsed : 0) / storageQuota * 100.0;
    }
    
    /**
     * 获取剩余存储空间
     */
    public long getRemainingStorage() {
        return (storageQuota != null ? storageQuota : 0) - (storageUsed != null ? storageUsed : 0);
    }
}

/**
 * 邮件别名DTO
 */
class EmailAliasDTO {
    private Long id;
    private String aliasEmail;
    private String domainName;
    private Boolean isActive;
    private String forwardTo;
    private String displayName;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAliasEmail() { return aliasEmail; }
    public void setAliasEmail(String aliasEmail) { this.aliasEmail = aliasEmail; }
    
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getForwardTo() { return forwardTo; }
    public void setForwardTo(String forwardTo) { this.forwardTo = forwardTo; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * 获取完整邮箱地址
     */
    public String getFullEmailAddress() {
        return aliasEmail + "@" + domainName;
    }
}

/**
 * 邮件文件夹DTO
 */
class EmailFolderDTO {
    private Long id;
    private String folderName;
    private String folderType;
    private Integer messageCount;
    private Integer unreadCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    
    public String getFolderType() { return folderType; }
    public void setFolderType(String folderType) { this.folderType = folderType; }
    
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    
    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}