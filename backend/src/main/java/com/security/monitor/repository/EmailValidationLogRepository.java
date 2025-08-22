package com.security.monitor.repository;

import com.security.monitor.model.EmailValidationLog;
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
 * 邮件验证日志仓库接口
 */
@Repository
public interface EmailValidationLogRepository extends JpaRepository<EmailValidationLog, Long> {
    
    /**
     * 根据邮件查找验证日志
     */
    List<EmailValidationLog> findByMessage(EmailMessage message);
    
    /**
     * 查找指定时间段的验证日志
     */
    Page<EmailValidationLog> findByValidatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * 统计指定时间段的验证次数
     */
    long countByValidatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计指定时间段通过验证的次数
     */
    long countByValidationStatusAndValidatedAtBetween(String validationStatus, LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找指定验证状态的日志
     */
    Page<EmailValidationLog> findByValidationStatus(String validationStatus, Pageable pageable);
    
    /**
     * 查找指定SPF状态的日志
     */
    Page<EmailValidationLog> findBySpfStatus(String spfStatus, Pageable pageable);
    
    /**
     * 查找指定DKIM状态的日志
     */
    Page<EmailValidationLog> findByDkimStatus(String dkimStatus, Pageable pageable);
    
    /**
     * 查找指定DMARC状态的日志
     */
    Page<EmailValidationLog> findByDmarcStatus(String dmarcStatus, Pageable pageable);
    
    /**
     * 根据发送方IP查找日志
     */
    Page<EmailValidationLog> findBySenderIp(String senderIp, Pageable pageable);
    
    /**
     * 获取验证状态分布统计
     */
    @Query("SELECT l.validationStatus, COUNT(l) FROM EmailValidationLog l WHERE l.validatedAt BETWEEN :start AND :end GROUP BY l.validationStatus")
    List<Object[]> getValidationStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取SPF状态分布统计
     */
    @Query("SELECT l.spfStatus, COUNT(l) FROM EmailValidationLog l WHERE l.validatedAt BETWEEN :start AND :end GROUP BY l.spfStatus")
    List<Object[]> getSpfStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取DKIM状态分布统计
     */
    @Query("SELECT l.dkimStatus, COUNT(l) FROM EmailValidationLog l WHERE l.validatedAt BETWEEN :start AND :end GROUP BY l.dkimStatus")
    List<Object[]> getDkimStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取DMARC状态分布统计
     */
    @Query("SELECT l.dmarcStatus, COUNT(l) FROM EmailValidationLog l WHERE l.validatedAt BETWEEN :start AND :end GROUP BY l.dmarcStatus")
    List<Object[]> getDmarcStatusDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取平均处理时间
     */
    @Query("SELECT AVG(l.processingTimeMs) FROM EmailValidationLog l WHERE l.processingTimeMs IS NOT NULL AND l.validatedAt BETWEEN :start AND :end")
    Double getAverageProcessingTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取最常失败的发送方IP
     */
    @Query("SELECT l.senderIp, COUNT(l) FROM EmailValidationLog l WHERE l.validationStatus = 'FAIL' AND l.validatedAt BETWEEN :start AND :end GROUP BY l.senderIp ORDER BY COUNT(l) DESC")
    List<Object[]> getTopFailingIPs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
    
    /**
     * 获取每日验证统计
     */
    @Query("SELECT DATE(l.validatedAt) as validationDate, " +
           "COUNT(l) as totalValidations, " +
           "SUM(CASE WHEN l.validationStatus = 'PASS' THEN 1 ELSE 0 END) as passedValidations, " +
           "SUM(CASE WHEN l.validationStatus = 'FAIL' THEN 1 ELSE 0 END) as failedValidations " +
           "FROM EmailValidationLog l " +
           "WHERE l.validatedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.validatedAt) " +
           "ORDER BY validationDate DESC")
    List<Object[]> getDailyValidationStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取DMARC策略使用统计
     */
    @Query("SELECT l.dmarcPolicy, COUNT(l) FROM EmailValidationLog l WHERE l.dmarcPolicy IS NOT NULL AND l.validatedAt BETWEEN :start AND :end GROUP BY l.dmarcPolicy ORDER BY COUNT(l) DESC")
    List<Object[]> getDmarcPolicyUsage(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找最近的验证日志
     */
    List<EmailValidationLog> findTop10ByOrderByValidatedAtDesc();
    
    /**
     * 查找处理时间最长的验证记录
     */
    List<EmailValidationLog> findTop10ByProcessingTimeMsIsNotNullOrderByProcessingTimeMsDesc();
    
    /**
     * 根据DKIM域名查找日志
     */
    Page<EmailValidationLog> findByDkimDomain(String dkimDomain, Pageable pageable);
    
    /**
     * 统计各个域名的验证情况
     */
    @Query("SELECT l.dkimDomain, " +
           "COUNT(l) as totalValidations, " +
           "SUM(CASE WHEN l.validationStatus = 'PASS' THEN 1 ELSE 0 END) as passedValidations, " +
           "SUM(CASE WHEN l.spfStatus = 'PASS' THEN 1 ELSE 0 END) as spfPassed, " +
           "SUM(CASE WHEN l.dkimStatus = 'PASS' THEN 1 ELSE 0 END) as dkimPassed, " +
           "SUM(CASE WHEN l.dmarcStatus = 'PASS' THEN 1 ELSE 0 END) as dmarcPassed " +
           "FROM EmailValidationLog l " +
           "WHERE l.dkimDomain IS NOT NULL AND l.validatedAt BETWEEN :start AND :end " +
           "GROUP BY l.dkimDomain " +
           "ORDER BY totalValidations DESC")
    List<Object[]> getDomainValidationStatistics(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 删除过期的验证日志
     */
    void deleteByValidatedAtBefore(LocalDateTime expiredDate);
    
    /**
     * 获取验证成功率趋势
     */
    @Query("SELECT DATE(l.validatedAt) as validationDate, " +
           "COUNT(l) as totalValidations, " +
           "SUM(CASE WHEN l.validationStatus = 'PASS' THEN 1 ELSE 0 END) as passedValidations, " +
           "(SUM(CASE WHEN l.validationStatus = 'PASS' THEN 1 ELSE 0 END) * 100.0 / COUNT(l)) as successRate " +
           "FROM EmailValidationLog l " +
           "WHERE l.validatedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(l.validatedAt) " +
           "ORDER BY validationDate ASC")
    List<Object[]> getValidationSuccessRateTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}