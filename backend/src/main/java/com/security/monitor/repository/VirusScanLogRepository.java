package com.security.monitor.repository;

import com.security.monitor.model.VirusScanLog;
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
 * 病毒扫描日志仓库接口
 */
@Repository
public interface VirusScanLogRepository extends JpaRepository<VirusScanLog, Long> {
    
    /**
     * 根据邮件查找扫描日志
     */
    List<VirusScanLog> findByMessage(EmailMessage message);
    
    /**
     * 查找指定时间段的扫描日志
     */
    Page<VirusScanLog> findByScannedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * 统计指定时间段的扫描次数
     */
    long countByScannedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计指定时间段发现威胁的扫描次数
     */
    long countByThreatFoundTrueAndScannedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计指定时间段扫描的文件总数
     */
    @Query("SELECT COALESCE(SUM(l.filesScanned), 0) FROM VirusScanLog l WHERE l.scannedAt BETWEEN :start AND :end")
    long sumFilesScannedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计指定时间段隔离的文件总数
     */
    @Query("SELECT COALESCE(SUM(l.quarantinedFiles), 0) FROM VirusScanLog l WHERE l.scannedAt BETWEEN :start AND :end")
    long sumQuarantinedFilesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找发现威胁的扫描日志
     */
    Page<VirusScanLog> findByThreatFound(boolean threatFound, Pageable pageable);
    
    /**
     * 查找指定状态的扫描日志
     */
    Page<VirusScanLog> findByScanStatus(String scanStatus, Pageable pageable);
    
    /**
     * 获取扫描状态分布统计
     */
    @Query("SELECT l.scanStatus, COUNT(l) FROM VirusScanLog l WHERE l.scannedAt BETWEEN :start AND :end GROUP BY l.scanStatus")
    List<Object[]> getScanStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取平均扫描时间
     */
    @Query("SELECT AVG(l.processingTimeMs) FROM VirusScanLog l WHERE l.processingTimeMs IS NOT NULL AND l.scannedAt BETWEEN :start AND :end")
    Double getAverageProcessingTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取最长扫描时间
     */
    @Query("SELECT MAX(l.processingTimeMs) FROM VirusScanLog l WHERE l.processingTimeMs IS NOT NULL AND l.scannedAt BETWEEN :start AND :end")
    Long getMaxProcessingTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找扫描时间最长的记录
     */
    List<VirusScanLog> findTop10ByProcessingTimeMsIsNotNullOrderByProcessingTimeMsDesc();
    
    /**
     * 查找发现威胁最多的扫描记录
     */
    List<VirusScanLog> findTop10ByThreatsFoundGreaterThanOrderByThreatsFoundDesc(int minThreats);
    
    /**
     * 获取每日扫描统计
     */
    @Query("SELECT DATE(l.scannedAt) as scanDate, " +
           "COUNT(l) as totalScans, " +
           "SUM(CASE WHEN l.threatFound = true THEN 1 ELSE 0 END) as threatsFound, " +
           "SUM(l.filesScanned) as filesScanned, " +
           "SUM(l.quarantinedFiles) as quarantinedFiles " +
           "FROM VirusScanLog l " +
           "WHERE l.scannedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.scannedAt) " +
           "ORDER BY scanDate DESC")
    List<Object[]> getDailyScanStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取扫描引擎版本使用统计
     */
    @Query("SELECT l.scanEngineVersion, COUNT(l) FROM VirusScanLog l WHERE l.scanEngineVersion IS NOT NULL AND l.scannedAt BETWEEN :start AND :end GROUP BY l.scanEngineVersion ORDER BY COUNT(l) DESC")
    List<Object[]> getScanEngineVersionStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最近的扫描日志
     */
    List<VirusScanLog> findTop10ByOrderByScannedAtDesc();
    
    /**
     * 查找指定邮件ID的最新扫描记录
     */
    @Query("SELECT l FROM VirusScanLog l WHERE l.message.id = :messageId ORDER BY l.scannedAt DESC")
    List<VirusScanLog> findLatestScanByMessageId(@Param("messageId") Long messageId, Pageable pageable);
    
    /**
     * 删除过期的扫描日志
     */
    void deleteByScannedAtBefore(LocalDateTime expiredDate);
    
    /**
     * 获取威胁检测率趋势
     */
    @Query("SELECT DATE(l.scannedAt) as scanDate, " +
           "COUNT(l) as totalScans, " +
           "SUM(CASE WHEN l.threatFound = true THEN 1 ELSE 0 END) as threatsFound, " +
           "(SUM(CASE WHEN l.threatFound = true THEN 1 ELSE 0 END) * 100.0 / COUNT(l)) as threatRate " +
           "FROM VirusScanLog l " +
           "WHERE l.scannedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.scannedAt) " +
           "ORDER BY scanDate ASC")
    List<Object[]> getThreatDetectionTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}