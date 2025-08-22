package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Sieve过滤器执行日志实体
 */
@Entity
@Table(name = "sieve_filter_logs")
public class SieveFilterLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private EmailMessage message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_id", nullable = false)
    private SieveFilter filter;
    
    @Column(name = "filter_matched", nullable = false)
    private Boolean filterMatched = false;
    
    @Column(name = "executed_action", length = 50)
    private String executedAction;
    
    @Column(name = "action_parameters", length = 1000)
    private String actionParameters;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "error_occurred", nullable = false)
    private Boolean errorOccurred = false;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "script_version", length = 10)
    private String scriptVersion;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    // 构造函数
    public SieveFilterLog() {
        this.executedAt = LocalDateTime.now();
    }
    
    public SieveFilterLog(EmailMessage message, SieveFilter filter, boolean filterMatched) {
        this();
        this.message = message;
        this.filter = filter;
        this.filterMatched = filterMatched;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailMessage getMessage() {
        return message;
    }
    
    public void setMessage(EmailMessage message) {
        this.message = message;
    }
    
    public SieveFilter getFilter() {
        return filter;
    }
    
    public void setFilter(SieveFilter filter) {
        this.filter = filter;
    }
    
    public Boolean getFilterMatched() {
        return filterMatched;
    }
    
    public void setFilterMatched(Boolean filterMatched) {
        this.filterMatched = filterMatched;
    }
    
    public String getExecutedAction() {
        return executedAction;
    }
    
    public void setExecutedAction(String executedAction) {
        this.executedAction = executedAction;
    }
    
    public String getActionParameters() {
        return actionParameters;
    }
    
    public void setActionParameters(String actionParameters) {
        this.actionParameters = actionParameters;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public Boolean getErrorOccurred() {
        return errorOccurred;
    }
    
    public void setErrorOccurred(Boolean errorOccurred) {
        this.errorOccurred = errorOccurred;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.errorOccurred = (errorMessage != null && !errorMessage.isEmpty());
    }
    
    public String getScriptVersion() {
        return scriptVersion;
    }
    
    public void setScriptVersion(String scriptVersion) {
        this.scriptVersion = scriptVersion;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}