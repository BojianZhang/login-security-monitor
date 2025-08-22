package com.security.monitor.repository;

import com.security.monitor.model.SpamDetectionLog;
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
 * 垃圾邮件检测日志仓库接口
 */
@Repository
public interface SpamDetectionLogRepository extends JpaRepository<SpamDetectionLog, Long> {
    
    /**
     * 根据邮件查找检测日志
     */
    List<SpamDetectionLog> findByMessage(EmailMessage message);
    
    /**
     * 查找指定时间段的检测日志
     */
    Page<SpamDetectionLog> findByDetectedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * 统计指定时间段的总消息数
     */
    long countByDetectedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计指定时间段的垃圾邮件数
     */
    long countByIsSpamTrueAndDetectedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找垃圾邮件检测日志
     */
    Page<SpamDetectionLog> findByIsSpam(boolean isSpam, Pageable pageable);
    
    /**
     * 查找用户反馈日志
     */
    Page<SpamDetectionLog> findByIsUserFeedback(boolean isUserFeedback, Pageable pageable);
    
    /**
     * 查找误报（系统判断为垃圾邮件，用户标记为正常）
     */
    @Query("SELECT l FROM SpamDetectionLog l WHERE l.isSpam = true AND l.userMarkedAsSpam = false AND l.isUserFeedback = true AND l.detectedAt BETWEEN :start AND :end")
    List<SpamDetectionLog> findFalsePositivesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找漏报（系统判断为正常，用户标记为垃圾邮件）
     */
    @Query("SELECT l FROM SpamDetectionLog l WHERE l.isSpam = false AND l.userMarkedAsSpam = true AND l.isUserFeedback = true AND l.detectedAt BETWEEN :start AND :end")
    List<SpamDetectionLog> findFalseNegativesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计误报数量
     */
    @Query("SELECT COUNT(l) FROM SpamDetectionLog l WHERE l.isSpam = true AND l.userMarkedAsSpam = false AND l.isUserFeedback = true AND l.detectedAt BETWEEN :start AND :end")
    long countFalsePositives(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计漏报数量
     */
    @Query("SELECT COUNT(l) FROM SpamDetectionLog l WHERE l.isSpam = false AND l.userMarkedAsSpam = true AND l.isUserFeedback = true AND l.detectedAt BETWEEN :start AND :end")
    long countFalseNegatives(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取垃圾邮件分数分布统计
     */
    @Query("SELECT FLOOR(l.spamScore) as scoreRange, COUNT(l) as count FROM SpamDetectionLog l WHERE l.detectedAt BETWEEN :start AND :end GROUP BY FLOOR(l.spamScore) ORDER BY scoreRange")
    List<Object[]> getSpamScoreDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取平均处理时间
     */
    @Query("SELECT AVG(l.processingTimeMs) FROM SpamDetectionLog l WHERE l.processingTimeMs IS NOT NULL AND l.detectedAt BETWEEN :start AND :end")
    Double getAverageProcessingTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取最高分数的垃圾邮件
     */
    List<SpamDetectionLog> findTop10ByIsSpamTrueOrderBySpamScoreDesc();
    
    /**
     * 删除过期的检测日志
     */
    void deleteByDetectedAtBefore(LocalDateTime expiredDate);
}