package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DNS黑名单实体
 */
@Entity
@Table(name = "dns_blacklists")
public class DnsBlacklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "blacklist_name", nullable = false, length = 100)
    private String blacklistName;
    
    @Column(name = "hostname", nullable = false, length = 255)
    private String hostname; // 黑名单服务器地址
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "query_timeout_ms", nullable = false)
    private Integer queryTimeoutMs = 5000; // 查询超时时间
    
    @Column(name = "weight", nullable = false)
    private Double weight = 1.0; // 权重
    
    @Column(name = "total_queries", nullable = false)
    private Long totalQueries = 0L;
    
    @Column(name = "positive_hits", nullable = false)
    private Long positiveHits = 0L;
    
    @Column(name = "last_query_at")
    private LocalDateTime lastQueryAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 构造函数
    public DnsBlacklist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public DnsBlacklist(String blacklistName, String hostname) {
        this();
        this.blacklistName = blacklistName;
        this.hostname = hostname;
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
    
    public String getBlacklistName() {
        return blacklistName;
    }
    
    public void setBlacklistName(String blacklistName) {
        this.blacklistName = blacklistName;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
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
    
    public Integer getQueryTimeoutMs() {
        return queryTimeoutMs;
    }
    
    public void setQueryTimeoutMs(Integer queryTimeoutMs) {
        this.queryTimeoutMs = queryTimeoutMs;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public Long getTotalQueries() {
        return totalQueries;
    }
    
    public void setTotalQueries(Long totalQueries) {
        this.totalQueries = totalQueries;
    }
    
    public Long getPositiveHits() {
        return positiveHits;
    }
    
    public void setPositiveHits(Long positiveHits) {
        this.positiveHits = positiveHits;
    }
    
    public LocalDateTime getLastQueryAt() {
        return lastQueryAt;
    }
    
    public void setLastQueryAt(LocalDateTime lastQueryAt) {
        this.lastQueryAt = lastQueryAt;
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
}