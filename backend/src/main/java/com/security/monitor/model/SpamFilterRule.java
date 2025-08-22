package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 垃圾邮件过滤规则实体
 */
@Entity
@Table(name = "spam_filter_rules")
public class SpamFilterRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName; // subject, from, to, body, body_html
    
    @Column(name = "operator", nullable = false, length = 20)
    private String operator; // contains, equals, regex, not_contains
    
    @Column(name = "pattern", nullable = false, length = 1000)
    private String pattern; // 匹配模式
    
    @Column(name = "score_modifier", nullable = false)
    private Double scoreModifier; // 分数修正值
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 优先级，数字越小优先级越高
    
    @Column(name = "hit_count", nullable = false)
    private Long hitCount = 0L; // 命中次数
    
    @Column(name = "last_hit_at")
    private LocalDateTime lastHitAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum RuleType {
        SPAM_DETECTION("垃圾邮件检测"),
        HAM_DETECTION("正常邮件检测"),
        WHITELIST("白名单"),
        BLACKLIST("黑名单");
        
        private final String description;
        
        RuleType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public SpamFilterRule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public SpamFilterRule(String ruleName, String fieldName, String operator, String pattern, Double scoreModifier) {
        this();
        this.ruleName = ruleName;
        this.fieldName = fieldName;
        this.operator = operator;
        this.pattern = pattern;
        this.scoreModifier = scoreModifier;
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
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public Double getScoreModifier() {
        return scoreModifier;
    }
    
    public void setScoreModifier(Double scoreModifier) {
        this.scoreModifier = scoreModifier;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public RuleType getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
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