package com.security.monitor.repository;

import com.security.monitor.model.SieveFilterLog;
import com.security.monitor.model.SieveFilter;
import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sieve过滤器执行日志仓库接口
 */
@Repository
public interface SieveFilterLogRepository extends JpaRepository<SieveFilterLog, Long> {
    
    /**
     * 根据邮件查找执行日志
     */
    List<SieveFilterLog> findByMessage(EmailMessage message);
    
    /**
     * 根据过滤器查找执行日志
     */
    Page<SieveFilterLog> findByFilter(SieveFilter filter, Pageable pageable);
    
    /**
     * 根据用户查找执行日志
     */
    @Query("SELECT l FROM SieveFilterLog l WHERE l.filter.user = :user ORDER BY l.executedAt DESC")
    Page<SieveFilterLog> findByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找指定时间段的执行日志
     */
    Page<SieveFilterLog> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * 统计指定时间段的执行次数
     */
    long countByExecutedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计指定时间段匹配成功的次数
     */
    long countByFilterMatchedTrueAndExecutedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找匹配成功的执行日志
     */
    Page<SieveFilterLog> findByFilterMatched(boolean filterMatched, Pageable pageable);
    
    /**
     * 查找有错误的执行日志
     */
    Page<SieveFilterLog> findByErrorOccurred(boolean errorOccurred, Pageable pageable);
    
    /**
     * 根据执行动作查找日志
     */
    Page<SieveFilterLog> findByExecutedAction(String executedAction, Pageable pageable);
    
    /**
     * 获取过滤器执行统计
     */
    @Query("SELECT " +
           "COUNT(l) as totalExecutions, " +
           "SUM(CASE WHEN l.filterMatched = true THEN 1 ELSE 0 END) as matchedExecutions, " +
           "SUM(CASE WHEN l.errorOccurred = true THEN 1 ELSE 0 END) as errorExecutions, " +
           "AVG(l.executionTimeMs) as avgExecutionTime " +
           "FROM SieveFilterLog l WHERE l.filter.user = :user AND l.executedAt BETWEEN :start AND :end")
    Object[] getUserExecutionStatistics(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取执行动作分布统计
     */
    @Query("SELECT l.executedAction, COUNT(l) FROM SieveFilterLog l WHERE l.filter.user = :user AND l.filterMatched = true AND l.executedAt BETWEEN :start AND :end GROUP BY l.executedAction")
    List<Object[]> getActionDistribution(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取每日执行统计
     */
    @Query("SELECT DATE(l.executedAt) as executionDate, " +
           "COUNT(l) as totalExecutions, " +
           "SUM(CASE WHEN l.filterMatched = true THEN 1 ELSE 0 END) as matchedExecutions, " +
           "SUM(CASE WHEN l.errorOccurred = true THEN 1 ELSE 0 END) as errorExecutions, " +
           "AVG(l.executionTimeMs) as avgExecutionTime " +
           "FROM SieveFilterLog l " +
           "WHERE l.filter.user = :user AND l.executedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.executedAt) " +
           "ORDER BY executionDate DESC")
    List<Object[]> getDailyExecutionStatistics(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取过滤器性能统计
     */
    @Query("SELECT l.filter.id, l.filter.filterName, " +
           "COUNT(l) as executionCount, " +
           "SUM(CASE WHEN l.filterMatched = true THEN 1 ELSE 0 END) as matchCount, " +
           "AVG(l.executionTimeMs) as avgExecutionTime, " +
           "MAX(l.executionTimeMs) as maxExecutionTime " +
           "FROM SieveFilterLog l " +
           "WHERE l.filter.user = :user AND l.executedAt BETWEEN :start AND :end " +
           "GROUP BY l.filter.id, l.filter.filterName " +
           "ORDER BY executionCount DESC")
    List<Object[]> getFilterPerformanceStatistics(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找执行时间最长的日志
     */
    List<SieveFilterLog> findTop10ByExecutionTimeMsIsNotNullOrderByExecutionTimeMsDesc();
    
    /**
     * 查找最近的执行日志
     */
    List<SieveFilterLog> findTop10ByOrderByExecutedAtDesc();
    
    /**
     * 根据过滤器ID统计执行情况
     */
    @Query("SELECT " +
           "COUNT(l) as totalExecutions, " +
           "SUM(CASE WHEN l.filterMatched = true THEN 1 ELSE 0 END) as matchedExecutions, " +
           "SUM(CASE WHEN l.errorOccurred = true THEN 1 ELSE 0 END) as errorExecutions, " +
           "AVG(l.executionTimeMs) as avgExecutionTime, " +
           "MAX(l.executedAt) as lastExecution " +
           "FROM SieveFilterLog l WHERE l.filter.id = :filterId")
    Object[] getFilterExecutionStatistics(@Param("filterId") Long filterId);
    
    /**
     * 查找特定邮件的所有过滤器执行记录
     */
    @Query("SELECT l FROM SieveFilterLog l WHERE l.message.id = :messageId ORDER BY l.filter.priority ASC, l.executedAt ASC")
    List<SieveFilterLog> findByMessageIdOrderByExecution(@Param("messageId") Long messageId);
    
    /**
     * 获取错误率最高的过滤器
     */
    @Query("SELECT l.filter.id, l.filter.filterName, " +
           "COUNT(l) as totalExecutions, " +
           "SUM(CASE WHEN l.errorOccurred = true THEN 1 ELSE 0 END) as errorExecutions, " +
           "(SUM(CASE WHEN l.errorOccurred = true THEN 1 ELSE 0 END) * 100.0 / COUNT(l)) as errorRate " +
           "FROM SieveFilterLog l " +
           "WHERE l.filter.user = :user AND l.executedAt BETWEEN :start AND :end " +
           "GROUP BY l.filter.id, l.filter.filterName " +
           "HAVING COUNT(l) >= :minExecutions " +
           "ORDER BY errorRate DESC")
    List<Object[]> getFiltersWithHighestErrorRate(@Param("user") User user, 
                                                   @Param("start") LocalDateTime start, 
                                                   @Param("end") LocalDateTime end, 
                                                   @Param("minExecutions") int minExecutions);
    
    /**
     * 获取匹配率趋势
     */
    @Query("SELECT DATE(l.executedAt) as executionDate, " +
           "COUNT(l) as totalExecutions, " +
           "SUM(CASE WHEN l.filterMatched = true THEN 1 ELSE 0 END) as matchedExecutions, " +
           "(SUM(CASE WHEN l.filterMatched = true THEN 1 ELSE 0 END) * 100.0 / COUNT(l)) as matchRate " +
           "FROM SieveFilterLog l " +
           "WHERE l.filter.user = :user AND l.executedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.executedAt) " +
           "ORDER BY executionDate ASC")
    List<Object[]> getMatchRateTrend(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找从未匹配过的过滤器
     */
    @Query("SELECT f FROM SieveFilter f WHERE f.user = :user AND f.id NOT IN (SELECT DISTINCT l.filter.id FROM SieveFilterLog l WHERE l.filterMatched = true)")
    List<SieveFilter> findNeverMatchedFilters(@Param("user") User user);
    
    /**
     * 删除过期的执行日志
     */
    void deleteByExecutedAtBefore(LocalDateTime expiredDate);
    
    /**
     * 根据过滤器删除执行日志
     */
    void deleteByFilter(SieveFilter filter);
    
    /**
     * 获取小时级执行活动统计
     */
    @Query("SELECT HOUR(l.executedAt) as hour, COUNT(l) as executionCount FROM SieveFilterLog l WHERE l.filter.user = :user AND l.executedAt BETWEEN :start AND :end GROUP BY HOUR(l.executedAt) ORDER BY hour")
    List<Object[]> getHourlyExecutionActivity(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}