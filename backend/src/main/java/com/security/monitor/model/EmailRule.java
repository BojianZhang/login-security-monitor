package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件规则实体
 */
@Entity
@Table(name = "email_rules")
public class EmailRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "conditions", columnDefinition = "TEXT", nullable = false)
    private String conditions; // JSON格式的条件
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 优先级，数字越小优先级越高
    
    @Column(name = "continue_processing", nullable = false)
    private Boolean continueProcessing = true; // 是否继续处理后续规则
    
    @Column(name = "applied_count", nullable = false)
    private Long appliedCount = 0L; // 规则应用次数
    
    @Column(name = "last_applied_at")
    private LocalDateTime lastAppliedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailRuleAction> actions;
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailRuleExecutionLog> executionLogs;
    
    // 构造函数
    public EmailRule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public EmailRule(User user, String ruleName, String conditions) {
        this();
        this.user = user;
        this.ruleName = ruleName;
        this.conditions = conditions;
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
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getConditions() {
        return conditions;
    }
    
    public void setConditions(String conditions) {
        this.conditions = conditions;
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
    
    public Boolean getContinueProcessing() {
        return continueProcessing;
    }
    
    public void setContinueProcessing(Boolean continueProcessing) {
        this.continueProcessing = continueProcessing;
    }
    
    public Long getAppliedCount() {
        return appliedCount;
    }
    
    public void setAppliedCount(Long appliedCount) {
        this.appliedCount = appliedCount;
    }
    
    public LocalDateTime getLastAppliedAt() {
        return lastAppliedAt;
    }
    
    public void setLastAppliedAt(LocalDateTime lastAppliedAt) {
        this.lastAppliedAt = lastAppliedAt;
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
    
    public List<EmailRuleAction> getActions() {
        return actions;
    }
    
    public void setActions(List<EmailRuleAction> actions) {
        this.actions = actions;
    }
    
    public List<EmailRuleExecutionLog> getExecutionLogs() {
        return executionLogs;
    }
    
    public void setExecutionLogs(List<EmailRuleExecutionLog> executionLogs) {
        this.executionLogs = executionLogs;
    }
}