package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件搜索历史实体
 */
@Entity
@Table(name = "email_search_history")
public class EmailSearchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "search_query", nullable = false, length = 500)
    private String query;
    
    @Column(name = "search_type", length = 20)
    private String searchType; // FULLTEXT, ADVANCED
    
    @Column(name = "result_count")
    private Long resultCount;
    
    @Column(name = "search_filters", columnDefinition = "TEXT")
    private String searchFilters; // JSON格式的搜索过滤条件
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 构造函数
    public EmailSearchHistory() {
        this.createdAt = LocalDateTime.now();
    }
    
    public EmailSearchHistory(User user, String query, String searchType) {
        this();
        this.user = user;
        this.query = query;
        this.searchType = searchType;
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
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getSearchType() {
        return searchType;
    }
    
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
    
    public Long getResultCount() {
        return resultCount;
    }
    
    public void setResultCount(Long resultCount) {
        this.resultCount = resultCount;
    }
    
    public String getSearchFilters() {
        return searchFilters;
    }
    
    public void setSearchFilters(String searchFilters) {
        this.searchFilters = searchFilters;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}