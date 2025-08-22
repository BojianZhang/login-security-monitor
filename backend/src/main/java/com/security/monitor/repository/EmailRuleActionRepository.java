package com.security.monitor.repository;

import com.security.monitor.model.EmailRule;
import com.security.monitor.model.EmailRuleAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 邮件规则动作仓库接口
 */
@Repository
public interface EmailRuleActionRepository extends JpaRepository<EmailRuleAction, Long> {
    
    /**
     * 查找规则的所有动作（按优先级排序）
     */
    List<EmailRuleAction> findByRuleOrderByPriority(EmailRule rule);
    
    /**
     * 查找规则的活跃动作（按优先级排序）
     */
    @Query("SELECT a FROM EmailRuleAction a WHERE a.rule = :rule AND a.isActive = true ORDER BY a.priority ASC")
    List<EmailRuleAction> findActiveActionsByRule(@Param("rule") EmailRule rule);
    
    /**
     * 根据动作类型查找动作
     */
    List<EmailRuleAction> findByRuleAndActionType(EmailRule rule, EmailRuleAction.ActionType actionType);
    
    /**
     * 统计规则的动作数量
     */
    long countByRule(EmailRule rule);
    
    /**
     * 统计规则的活跃动作数量
     */
    long countByRuleAndIsActive(EmailRule rule, boolean isActive);
    
    /**
     * 删除规则的所有动作
     */
    void deleteByRule(EmailRule rule);
}