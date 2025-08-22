package com.security.monitor.repository;

import com.security.monitor.model.EmailRule;
import com.security.monitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件规则仓库接口
 */
@Repository
public interface EmailRuleRepository extends JpaRepository<EmailRule, Long> {
    
    /**
     * 查找用户的所有规则
     */
    Page<EmailRule> findByUser(User user, Pageable pageable);
    
    /**
     * 查找用户的所有规则（按优先级排序）
     */
    List<EmailRule> findByUserOrderByPriorityAsc(User user);
    
    /**
     * 查找用户的活跃规则（按优先级排序）
     */
    @Query("SELECT r FROM EmailRule r WHERE r.user = :user AND r.isActive = true ORDER BY r.priority ASC")
    List<EmailRule> findActiveRulesByUser(@Param("user") User user);
    
    /**
     * 根据ID和用户查找规则
     */
    Optional<EmailRule> findByIdAndUser(Long id, User user);
    
    /**
     * 查找用户的规则按名称
     */
    Optional<EmailRule> findByUserAndRuleName(User user, String ruleName);
    
    /**
     * 统计用户规则数量
     */
    long countByUser(User user);
    
    /**
     * 统计用户活跃规则数量
     */
    long countByUserAndIsActive(User user, boolean isActive);
    
    /**
     * 查找最近应用的规则
     */
    @Query("SELECT r FROM EmailRule r WHERE r.user = :user AND r.lastAppliedAt IS NOT NULL ORDER BY r.lastAppliedAt DESC")
    List<EmailRule> findRecentlyAppliedRules(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找使用频率最高的规则
     */
    @Query("SELECT r FROM EmailRule r WHERE r.user = :user ORDER BY r.appliedCount DESC")
    List<EmailRule> findMostUsedRules(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找指定时间段内应用过的规则
     */
    @Query("SELECT r FROM EmailRule r WHERE r.user = :user AND r.lastAppliedAt BETWEEN :startDate AND :endDate ORDER BY r.lastAppliedAt DESC")
    List<EmailRule> findRulesAppliedBetween(@Param("user") User user, 
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * 获取规则统计信息
     */
    @Query("SELECT " +
           "COUNT(r) as totalRules, " +
           "SUM(CASE WHEN r.isActive = true THEN 1 ELSE 0 END) as activeRules, " +
           "SUM(r.appliedCount) as totalApplications, " +
           "AVG(r.appliedCount) as avgApplications " +
           "FROM EmailRule r " +
           "WHERE r.user = :user")
    Object[] getRuleStatistics(@Param("user") User user);
    
    /**
     * 删除用户的所有规则
     */
    void deleteByUser(User user);
}