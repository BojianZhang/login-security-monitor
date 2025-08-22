package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件规则动作实体
 */
@Entity
@Table(name = "email_rule_actions")
public class EmailRuleAction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private EmailRule rule;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;
    
    @Column(name = "action_value", length = 500)
    private String actionValue; // 动作参数（如文件夹名、标签名等）
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0; // 动作执行优先级
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public enum ActionType {
        MOVE_TO_FOLDER("移动到文件夹"),
        MARK_AS_READ("标记为已读"),
        MARK_AS_UNREAD("标记为未读"),
        ADD_STAR("添加星标"),
        REMOVE_STAR("移除星标"),
        DELETE("删除"),
        MARK_AS_SPAM("标记为垃圾邮件"),
        ADD_LABEL("添加标签"),
        FORWARD_TO("转发到"),
        AUTO_REPLY("自动回复");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public EmailRuleAction() {
        this.createdAt = LocalDateTime.now();
    }
    
    public EmailRuleAction(EmailRule rule, ActionType actionType, String actionValue) {
        this();
        this.rule = rule;
        this.actionType = actionType;
        this.actionValue = actionValue;
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
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public String getActionValue() {
        return actionValue;
    }
    
    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}