package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件规则执行日志实体
 */
@Entity
@Table(name = "email_rule_execution_logs")
public class EmailRuleExecutionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private EmailRule rule;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private EmailMessage message;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "actions_executed", columnDefinition = "TEXT")
    private String actionsExecuted; // JSON格式的已执行动作列表
    
    // 构造函数
    public EmailRuleExecutionLog() {
        this.executedAt = LocalDateTime.now();
    }
    
    public EmailRuleExecutionLog(EmailRule rule, EmailMessage message, Boolean success) {
        this();
        this.rule = rule;
        this.message = message;
        this.success = success;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailRule getRule() {
        return rule;
    }
    
    public void setRule(EmailRule rule) {
        this.rule = rule;
    }
    
    public EmailMessage getMessage() {
        return message;
    }
    
    public void setMessage(EmailMessage message) {
        this.message = message;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getActionsExecuted() {
        return actionsExecuted;
    }
    
    public void setActionsExecuted(String actionsExecuted) {
        this.actionsExecuted = actionsExecuted;
    }
}