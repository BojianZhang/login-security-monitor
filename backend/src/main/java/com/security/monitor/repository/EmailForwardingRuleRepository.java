package com.security.monitor.repository;

import com.security.monitor.model.EmailAlias;
import com.security.monitor.model.EmailForwardingRule;
import com.security.monitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 邮件转发规则仓库接口
 */
@Repository
public interface EmailForwardingRuleRepository extends JpaRepository<EmailForwardingRule, Long> {
    
    /**
     * 根据别名查找转发规则，按优先级降序排列
     */
    List<EmailForwardingRule> findByAliasOrderByPriorityDesc(EmailAlias alias);
    
    /**
     * 查找别名的活跃转发规则，按优先级降序排列
     */
    List<EmailForwardingRule> findByAliasAndIsActiveTrueOrderByPriorityDesc(EmailAlias alias);
    
    /**
     * 查找用户的所有转发规则
     */
    @Query("SELECT r FROM EmailForwardingRule r WHERE r.alias.user = :user ORDER BY r.alias.id ASC, r.priority DESC")
    List<EmailForwardingRule> findByAliasUserOrderByAliasIdAscPriorityDesc(@Param("user") User user);
    
    /**
     * 查找指定转发地址的规则
     */
    List<EmailForwardingRule> findByForwardToContaining(String forwardTo);
    
    /**
     * 统计用户的转发规则数量
     */
    @Query("SELECT COUNT(r) FROM EmailForwardingRule r WHERE r.alias.user = :user")
    long countByAliasUser(@Param("user") User user);
    
    /**
     * 统计用户的活跃转发规则数量
     */
    @Query("SELECT COUNT(r) FROM EmailForwardingRule r WHERE r.alias.user = :user AND r.isActive = true")
    long countByAliasUserAndIsActiveTrue(@Param("user") User user);
    
    /**
     * 查找指定别名和规则名的规则（用于检查重复）
     */
    List<EmailForwardingRule> findByAliasAndRuleName(EmailAlias alias, String ruleName);
}