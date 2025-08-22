package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 邮件转发规则实体
 */
@Entity
@Table(name = "email_forwarding_rules")
public class EmailForwardingRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alias_id", nullable = false)
    private EmailAlias alias;
    
    @NotBlank(message = "规则名称不能为空")
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    @NotBlank(message = "转发地址不能为空")
    @Column(name = "forward_to", nullable = false, length = 320)
    private String forwardTo;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "priority")
    private Integer priority = 0; // 优先级，数值越大优先级越高
    
    @Column(name = "forward_subject", length = 200)
    private String forwardSubject; // 转发主题模板，支持变量替换
    
    @Column(name = "keep_original")
    private Boolean keepOriginal = true; // 是否保留原邮件在收件箱
    
    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions; // 转发条件，简单字符串格式
    
    @Column(name = "continue_processing")
    private Boolean continueProcessing = false; // 匹配后是否继续处理后续规则
    
    @Column(name = "forward_count")
    private Long forwardCount = 0L; // 转发次数统计
    
    @Column(name = "last_forward_at")
    private LocalDateTime lastForwardAt; // 最后转发时间
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EmailForwardingRule() {}
    
    public EmailForwardingRule(EmailAlias alias) {
        this.alias = alias;
    }
    
    public EmailForwardingRule(EmailAlias alias, String ruleName, String forwardTo) {
        this.alias = alias;
        this.ruleName = ruleName;
        this.forwardTo = forwardTo;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailAlias getAlias() {
        return alias;
    }
    
    public void setAlias(EmailAlias alias) {
        this.alias = alias;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getForwardTo() {
        return forwardTo;
    }
    
    public void setForwardTo(String forwardTo) {
        this.forwardTo = forwardTo;
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
    
    public String getForwardSubject() {
        return forwardSubject;
    }
    
    public void setForwardSubject(String forwardSubject) {
        this.forwardSubject = forwardSubject;
    }
    
    public Boolean getKeepOriginal() {
        return keepOriginal;
    }
    
    public void setKeepOriginal(Boolean keepOriginal) {
        this.keepOriginal = keepOriginal;
    }
    
    public String getConditions() {
        return conditions;
    }
    
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }
    
    public Boolean getContinueProcessing() {
        return continueProcessing;
    }
    
    public void setContinueProcessing(Boolean continueProcessing) {
        this.continueProcessing = continueProcessing;
    }
    
    public Long getForwardCount() {
        return forwardCount;
    }
    
    public void setForwardCount(Long forwardCount) {
        this.forwardCount = forwardCount;
    }
    
    public void incrementForwardCount() {
        this.forwardCount = this.forwardCount == null ? 1L : this.forwardCount + 1L;
        this.lastForwardAt = LocalDateTime.now();
    }
    
    public LocalDateTime getLastForwardAt() {
        return lastForwardAt;
    }
    
    public void setLastForwardAt(LocalDateTime lastForwardAt) {
        this.lastForwardAt = lastForwardAt;
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
     * 检查转发条件是否匹配
     */
    public boolean matchesConditions(EmailMessage message) {
        if (conditions == null || conditions.trim().isEmpty()) {
            return true; // 无条件则匹配所有
        }
        
        // 这里可以实现更复杂的条件匹配逻辑
        // 当前简化版本在服务层实现
        return true;
    }
    
    @Override
    public String toString() {
        return "EmailForwardingRule{" +
                "id=" + id +
                ", ruleName='" + ruleName + '\'' +
                ", forwardTo='" + forwardTo + '\'' +
                ", isActive=" + isActive +
                ", priority=" + priority +
                '}';
    }
}