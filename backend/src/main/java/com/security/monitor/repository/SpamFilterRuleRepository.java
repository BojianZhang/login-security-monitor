package com.security.monitor.repository;

import com.security.monitor.model.SpamFilterRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 垃圾邮件过滤规则仓库接口
 */
@Repository
public interface SpamFilterRuleRepository extends JpaRepository<SpamFilterRule, Long> {
    
    /**
     * 查找活跃的规则（按优先级排序）
     */
    @Query("SELECT r FROM SpamFilterRule r WHERE r.isActive = true ORDER BY r.priority ASC")
    List<SpamFilterRule> findActiveRules();
    
    /**
     * 根据规则类型查找规则
     */
    List<SpamFilterRule> findByRuleTypeAndIsActive(SpamFilterRule.RuleType ruleType, boolean isActive);
    
    /**
     * 根据规则名称查找
     */
    SpamFilterRule findByRuleName(String ruleName);
    
    /**
     * 分页查询规则
     */
    Page<SpamFilterRule> findByIsActive(boolean isActive, Pageable pageable);
    
    /**
     * 查找最常用的规则
     */
    @Query("SELECT r FROM SpamFilterRule r WHERE r.isActive = true ORDER BY r.hitCount DESC")
    List<SpamFilterRule> findMostUsedRules(Pageable pageable);
    
    /**
     * 查找最近更新的规则
     */
    List<SpamFilterRule> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime after);
    
    /**
     * 统计规则数量
     */
    long countByIsActive(boolean isActive);
    
    /**
     * 根据字段名查找规则
     */
    List<SpamFilterRule> findByFieldNameAndIsActive(String fieldName, boolean isActive);
    
    /**
     * 更新规则命中统计
     */
    @Query("UPDATE SpamFilterRule r SET r.hitCount = r.hitCount + 1, r.lastHitAt = :hitTime WHERE r.id = :ruleId")
    void updateHitStatistics(@Param("ruleId") Long ruleId, @Param("hitTime") LocalDateTime hitTime);
}