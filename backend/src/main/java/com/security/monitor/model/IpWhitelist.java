package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * IP白名单实体
 */
@Entity
@Table(name = "ip_whitelists")
public class IpWhitelist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress; // 支持IPv4和IPv6
    
    @Column(name = "cidr_range", length = 50)
    private String cidrRange; // CIDR格式，如 192.168.1.0/24
    
    @Column(name = "ip_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private IpType ipType = IpType.IPV4;
    
    @Column(name = "whitelist_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WhitelistType whitelistType = WhitelistType.LOGIN;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 优先级，数字越小优先级越高
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 过期时间
    
    @Column(name = "allowed_services", length = 500)
    private String allowedServices; // 允许的服务，JSON格式
    
    @Column(name = "access_count", nullable = false)
    private Long accessCount = 0L; // 访问次数
    
    @Column(name = "last_access_at")
    private LocalDateTime lastAccessAt; // 最后访问时间
    
    @Column(name = "created_by", length = 100)
    private String createdBy; // 创建者
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // IP类型枚举
    public enum IpType {
        IPV4("IPv4"),
        IPV6("IPv6"),
        RANGE("IP范围"),
        CIDR("CIDR网段");
        
        private final String description;
        
        IpType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 白名单类型枚举
    public enum WhitelistType {
        LOGIN("登录访问"),
        SMTP("SMTP服务"),
        IMAP("IMAP服务"),
        POP3("POP3服务"),
        WEBMAIL("网页邮箱"),
        API("API访问"),
        ADMIN("管理访问"),
        ALL("全部服务");
        
        private final String description;
        
        WhitelistType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public IpWhitelist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public IpWhitelist(String ipAddress, WhitelistType whitelistType) {
        this();
        this.ipAddress = ipAddress;
        this.whitelistType = whitelistType;
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
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getCidrRange() {
        return cidrRange;
    }
    
    public void setCidrRange(String cidrRange) {
        this.cidrRange = cidrRange;
    }
    
    public IpType getIpType() {
        return ipType;
    }
    
    public void setIpType(IpType ipType) {
        this.ipType = ipType;
    }
    
    public WhitelistType getWhitelistType() {
        return whitelistType;
    }
    
    public void setWhitelistType(WhitelistType whitelistType) {
        this.whitelistType = whitelistType;
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
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getAllowedServices() {
        return allowedServices;
    }
    
    public void setAllowedServices(String allowedServices) {
        this.allowedServices = allowedServices;
    }
    
    public Long getAccessCount() {
        return accessCount;
    }
    
    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }
    
    public LocalDateTime getLastAccessAt() {
        return lastAccessAt;
    }
    
    public void setLastAccessAt(LocalDateTime lastAccessAt) {
        this.lastAccessAt = lastAccessAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 增加访问计数
     */
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessAt = LocalDateTime.now();
    }
    
    /**
     * 获取实际用于匹配的IP地址
     */
    public String getMatchableAddress() {
        if (cidrRange != null && !cidrRange.isEmpty()) {
            return cidrRange;
        }
        return ipAddress;
    }
}