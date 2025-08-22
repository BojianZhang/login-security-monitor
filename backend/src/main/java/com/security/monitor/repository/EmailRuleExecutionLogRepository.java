package com.security.monitor.repository;

import com.security.monitor.model.EmailRule;
import com.security.monitor.model.EmailRuleExecutionLog;
import com.security.monitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件规则执行日志仓库接口
 */
@Repository
public interface EmailRuleExecutionLogRepository extends JpaRepository<EmailRuleExecutionLog, Long> {
    
    /**
     * 查找规则的执行日志
     */
    Page<EmailRuleExecutionLog> findByRule(EmailRule rule, Pageable pageable);
    
    /**
     * 查找规则的执行日志（按时间倒序）
     */
    List<EmailRuleExecutionLog> findByRuleOrderByExecutedAtDesc(EmailRule rule);
    
    /**
     * 查找用户所有规则的执行日志
     */
    @Query("SELECT l FROM EmailRuleExecutionLog l WHERE l.rule.user = :user ORDER BY l.executedAt DESC")
    Page<EmailRuleExecutionLog> findByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找指定时间段的执行日志
     */
    @Query("SELECT l FROM EmailRuleExecutionLog l WHERE l.rule = :rule AND l.executedAt BETWEEN :startDate AND :endDate ORDER BY l.executedAt DESC")
    List<EmailRuleExecutionLog> findByRuleAndExecutedAtBetween(@Param("rule") EmailRule rule,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * 查找失败的执行日志
     */
    @Query("SELECT l FROM EmailRuleExecutionLog l WHERE l.rule.user = :user AND l.success = false ORDER BY l.executedAt DESC")
    Page<EmailRuleExecutionLog> findFailedExecutions(@Param("user") User user, Pageable pageable);
    
    /**
     * 统计规则执行次数
     */
    long countByRule(EmailRule rule);
    
    /**
     * 统计规则成功执行次数
     */
    long countByRuleAndSuccess(EmailRule rule, boolean success);
    
    /**
     * 统计用户规则执行统计
     */
    @Query("SELECT " +
           "COUNT(l) as totalExecutions, " +
           "SUM(CASE WHEN l.success = true THEN 1 ELSE 0 END) as successfulExecutions, " +
           "SUM(CASE WHEN l.success = false THEN 1 ELSE 0 END) as failedExecutions, " +
           "AVG(l.executionTimeMs) as avgExecutionTime " +
           "FROM EmailRuleExecutionLog l " +
           "WHERE l.rule.user = :user")
    Object[] getExecutionStatistics(@Param("user") User user);
    
    /**
     * 获取最近执行的规则统计
     */
    @Query("SELECT l.rule.id, l.rule.ruleName, COUNT(l), " +
           "SUM(CASE WHEN l.success = true THEN 1 ELSE 0 END) as successCount, " +
           "MAX(l.executedAt) as lastExecution " +
           "FROM EmailRuleExecutionLog l " +
           "WHERE l.rule.user = :user " +
           "AND l.executedAt >= :since " +
           "GROUP BY l.rule.id, l.rule.ruleName " +
           "ORDER BY COUNT(l) DESC")
    List<Object[]> getRecentRuleExecutionStats(@Param("user") User user, 
                                              @Param("since") LocalDateTime since,
                                              Pageable pageable);
    
    /**
     * 删除过期的执行日志
     */
    @Modifying
    @Query("DELETE FROM EmailRuleExecutionLog l WHERE l.executedAt < :expiredDate")
    void deleteExpiredLogs(@Param("expiredDate") LocalDateTime expiredDate);
    
    /**
     * 删除规则的所有执行日志
     */
    void deleteByRule(EmailRule rule);
}