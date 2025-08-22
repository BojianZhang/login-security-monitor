package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Sieve邮件过滤器实体
 */
@Entity
@Table(name = "sieve_filters")
public class SieveFilter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "filter_name", nullable = false, length = 100)
    private String filterName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "filter_script", nullable = false, columnDefinition = "TEXT")
    private String filterScript; // Sieve脚本内容
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 优先级，数字越小优先级越高
    
    @Column(name = "filter_type", length = 20)
    @Enumerated(EnumType.STRING)
    private FilterType filterType = FilterType.USER_DEFINED;
    
    @Column(name = "hit_count", nullable = false)
    private Long hitCount = 0L; // 命中次数
    
    @Column(name = "last_hit_at")
    private LocalDateTime lastHitAt; // 最后命中时间
    
    @Column(name = "syntax_version", length = 10)
    private String syntaxVersion = "1.0"; // Sieve语法版本
    
    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0; // 错误次数
    
    @Column(name = "last_error")
    private String lastError; // 最后错误信息
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 过滤器类型枚举
    public enum FilterType {
        USER_DEFINED("用户自定义"),
        SYSTEM_DEFAULT("系统默认"),
        SPAM_FILTER("垃圾邮件过滤"),
        VIRUS_FILTER("病毒过滤"),
        VACATION("假期自动回复"),
        FORWARD("邮件转发");
        
        private final String description;
        
        FilterType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public SieveFilter() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public SieveFilter(User user, String filterName, String filterScript) {
        this();
        this.user = user;
        this.filterName = filterName;
        this.filterScript = filterScript;
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
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getFilterName() {
        return filterName;
    }
    
    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFilterScript() {
        return filterScript;
    }
    
    public void setFilterScript(String filterScript) {
        this.filterScript = filterScript;
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
    
    public FilterType getFilterType() {
        return filterType;
    }
    
    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }
    
    public Long getHitCount() {
        return hitCount;
    }
    
    public void setHitCount(Long hitCount) {
        this.hitCount = hitCount;
    }
    
    public LocalDateTime getLastHitAt() {
        return lastHitAt;
    }
    
    public void setLastHitAt(LocalDateTime lastHitAt) {
        this.lastHitAt = lastHitAt;
    }
    
    public String getSyntaxVersion() {
        return syntaxVersion;
    }
    
    public void setSyntaxVersion(String syntaxVersion) {
        this.syntaxVersion = syntaxVersion;
    }
    
    public Integer getErrorCount() {
        return errorCount;
    }
    
    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
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
     * 增加命中计数
     */
    public void incrementHitCount() {
        this.hitCount++;
        this.lastHitAt = LocalDateTime.now();
    }
    
    /**
     * 增加错误计数
     */
    public void incrementErrorCount(String errorMessage) {
        this.errorCount++;
        this.lastError = errorMessage;
    }
    
    /**
     * 重置错误计数
     */
    public void resetErrorCount() {
        this.errorCount = 0;
        this.lastError = null;
    }
}