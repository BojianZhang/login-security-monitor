package com.security.monitor.repository;

import com.security.monitor.model.DnsBlacklistCheckLog;
import com.security.monitor.model.EmailMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DNS黑名单检查日志仓库接口
 */
@Repository
public interface DnsBlacklistCheckLogRepository extends JpaRepository<DnsBlacklistCheckLog, Long> {
    
    /**
     * 根据邮件查找检查日志
     */
    List<DnsBlacklistCheckLog> findByMessage(EmailMessage message);
    
    /**
     * 根据IP地址查找检查日志
     */
    Page<DnsBlacklistCheckLog> findByIpAddress(String ipAddress, Pageable pageable);
    
    /**
     * 查找指定时间段的检查日志
     */
    Page<DnsBlacklistCheckLog> findByCheckedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * 统计指定时间段的检查次数
     */
    long countByCheckedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计指定时间段命中黑名单的次数
     */
    long countByCheckStatusAndCheckedAtBetween(String checkStatus, LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找指定检查状态的日志
     */
    Page<DnsBlacklistCheckLog> findByCheckStatus(String checkStatus, Pageable pageable);
    
    /**
     * 查找指定风险级别的日志
     */
    Page<DnsBlacklistCheckLog> findByRiskLevel(String riskLevel, Pageable pageable);
    
    /**
     * 获取检查状态分布统计
     */
    @Query("SELECT l.checkStatus, COUNT(l) FROM DnsBlacklistCheckLog l WHERE l.checkedAt BETWEEN :start AND :end GROUP BY l.checkStatus")
    List<Object[]> getCheckStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取风险级别分布统计
     */
    @Query("SELECT l.riskLevel, COUNT(l) FROM DnsBlacklistCheckLog l WHERE l.checkedAt BETWEEN :start AND :end GROUP BY l.riskLevel")
    List<Object[]> getRiskLevelDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取平均处理时间
     */
    @Query("SELECT AVG(l.processingTimeMs) FROM DnsBlacklistCheckLog l WHERE l.processingTimeMs IS NOT NULL AND l.checkedAt BETWEEN :start AND :end")
    Double getAverageProcessingTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取最常被检查的IP地址
     */
    @Query("SELECT l.ipAddress, COUNT(l) FROM DnsBlacklistCheckLog l WHERE l.checkedAt BETWEEN :start AND :end GROUP BY l.ipAddress ORDER BY COUNT(l) DESC")
    List<Object[]> getMostCheckedIPs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
    
    /**
     * 获取最常命中黑名单的IP地址
     */
    @Query("SELECT l.ipAddress, COUNT(l), AVG(l.hitCount), AVG(l.totalWeight) FROM DnsBlacklistCheckLog l WHERE l.checkStatus = 'LISTED' AND l.checkedAt BETWEEN :start AND :end GROUP BY l.ipAddress ORDER BY COUNT(l) DESC")
    List<Object[]> getMostBlacklistedIPs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
    
    /**
     * 获取每日检查统计
     */
    @Query("SELECT DATE(l.checkedAt) as checkDate, " +
           "COUNT(l) as totalChecks, " +
           "SUM(CASE WHEN l.checkStatus = 'LISTED' THEN 1 ELSE 0 END) as blacklistedChecks, " +
           "SUM(CASE WHEN l.checkStatus = 'CLEAN' THEN 1 ELSE 0 END) as cleanChecks, " +
           "AVG(l.hitCount) as avgHitCount, " +
           "AVG(l.totalWeight) as avgTotalWeight " +
           "FROM DnsBlacklistCheckLog l " +
           "WHERE l.checkedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.checkedAt) " +
           "ORDER BY checkDate DESC")
    List<Object[]> getDailyCheckStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取黑名单命中率趋势
     */
    @Query("SELECT DATE(l.checkedAt) as checkDate, " +
           "COUNT(l) as totalChecks, " +
           "SUM(CASE WHEN l.checkStatus = 'LISTED' THEN 1 ELSE 0 END) as hitChecks, " +
           "(SUM(CASE WHEN l.checkStatus = 'LISTED' THEN 1 ELSE 0 END) * 100.0 / COUNT(l)) as hitRate " +
           "FROM DnsBlacklistCheckLog l " +
           "WHERE l.checkedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.checkedAt) " +
           "ORDER BY checkDate ASC")
    List<Object[]> getBlacklistHitRateTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最近的检查日志
     */
    List<DnsBlacklistCheckLog> findTop10ByOrderByCheckedAtDesc();
    
    /**
     * 查找处理时间最长的检查记录
     */
    List<DnsBlacklistCheckLog> findTop10ByProcessingTimeMsIsNotNullOrderByProcessingTimeMsDesc();
    
    /**
     * 查找命中数最多的检查记录
     */
    List<DnsBlacklistCheckLog> findTop10ByHitCountGreaterThanOrderByHitCountDesc(int minHits);
    
    /**
     * 查找权重最高的检查记录
     */
    List<DnsBlacklistCheckLog> findTop10ByTotalWeightGreaterThanOrderByTotalWeightDesc(double minWeight);
    
    /**
     * 根据IP地址统计检查情况
     */
    @Query("SELECT l.ipAddress, " +
           "COUNT(l) as totalChecks, " +
           "SUM(CASE WHEN l.checkStatus = 'LISTED' THEN 1 ELSE 0 END) as blacklistedChecks, " +
           "AVG(l.hitCount) as avgHitCount, " +
           "AVG(l.totalWeight) as avgTotalWeight, " +
           "MAX(l.checkedAt) as lastChecked " +
           "FROM DnsBlacklistCheckLog l " +
           "WHERE l.checkedAt BETWEEN :start AND :end " +
           "GROUP BY l.ipAddress " +
           "ORDER BY blacklistedChecks DESC, totalChecks DESC")
    List<Object[]> getIPCheckStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找特定IP的最新检查记录
     */
    @Query("SELECT l FROM DnsBlacklistCheckLog l WHERE l.ipAddress = :ipAddress ORDER BY l.checkedAt DESC")
    List<DnsBlacklistCheckLog> findLatestChecksByIP(@Param("ipAddress") String ipAddress, Pageable pageable);
    
    /**
     * 删除过期的检查日志
     */
    void deleteByCheckedAtBefore(LocalDateTime expiredDate);
    
    /**
     * 获取高风险IP列表
     */
    @Query("SELECT l.ipAddress FROM DnsBlacklistCheckLog l WHERE l.riskLevel = 'HIGH' AND l.checkedAt BETWEEN :start AND :end GROUP BY l.ipAddress HAVING COUNT(l) >= :minOccurrences ORDER BY COUNT(l) DESC")
    List<String> getHighRiskIPs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("minOccurrences") int minOccurrences);
    
    /**
     * 统计不同时间段的检查活动
     */
    @Query("SELECT HOUR(l.checkedAt) as hour, COUNT(l) as checkCount FROM DnsBlacklistCheckLog l WHERE l.checkedAt BETWEEN :start AND :end GROUP BY HOUR(l.checkedAt) ORDER BY hour")
    List<Object[]> getHourlyCheckActivity(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取命中权重分布
     */
    @Query("SELECT " +
           "CASE " +
           "WHEN l.totalWeight = 0 THEN '0' " +
           "WHEN l.totalWeight > 0 AND l.totalWeight <= 2 THEN '0-2' " +
           "WHEN l.totalWeight > 2 AND l.totalWeight <= 5 THEN '2-5' " +
           "WHEN l.totalWeight > 5 AND l.totalWeight <= 10 THEN '5-10' " +
           "ELSE '10+' " +
           "END as weightRange, " +
           "COUNT(l) as count " +
           "FROM DnsBlacklistCheckLog l " +
           "WHERE l.checkedAt BETWEEN :start AND :end " +
           "GROUP BY " +
           "CASE " +
           "WHEN l.totalWeight = 0 THEN '0' " +
           "WHEN l.totalWeight > 0 AND l.totalWeight <= 2 THEN '0-2' " +
           "WHEN l.totalWeight > 2 AND l.totalWeight <= 5 THEN '2-5' " +
           "WHEN l.totalWeight > 5 AND l.totalWeight <= 10 THEN '5-10' " +
           "ELSE '10+' " +
           "END " +
           "ORDER BY weightRange")
    List<Object[]> getWeightDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}