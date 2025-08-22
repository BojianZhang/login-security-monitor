package com.security.monitor.repository;

import com.security.monitor.model.DmarcReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DMARC报告Repository
 */
@Repository
public interface DmarcReportRepository extends JpaRepository<DmarcReport, Long> {
    
    /**
     * 根据报告ID查找报告
     */
    Optional<DmarcReport> findByReportId(String reportId);
    
    /**
     * 根据域名查找报告
     */
    List<DmarcReport> findByDomainOrderByCreatedAtDesc(String domain);
    
    /**
     * 根据域名和时间范围查找报告
     */
    Optional<DmarcReport> findByDomainAndBeginTimeAndEndTime(String domain, 
                                                           LocalDateTime beginTime, 
                                                           LocalDateTime endTime);
    
    /**
     * 查找未发送的报告
     */
    List<DmarcReport> findByIsSentFalseOrderByCreatedAtDesc();
    
    /**
     * 查找需要重试的报告
     */
    @Query("SELECT r FROM DmarcReport r WHERE r.isSent = false " +
           "AND r.retryCount < 5 " +
           "AND (r.nextRetryAt IS NULL OR r.nextRetryAt <= :currentTime)")
    List<DmarcReport> findReportsNeedingRetry(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 默认的重试查询
     */
    @Query("SELECT r FROM DmarcReport r WHERE r.isSent = false " +
           "AND r.retryCount < 5 " +
           "AND (r.nextRetryAt IS NULL OR r.nextRetryAt <= CURRENT_TIMESTAMP)")
    List<DmarcReport> findReportsNeedingRetry();
    
    /**
     * 查找指定时间之前创建的报告
     */
    List<DmarcReport> findByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * 根据组织名称查找报告
     */
    List<DmarcReport> findByOrgNameOrderByCreatedAtDesc(String orgName);
    
    /**
     * 查找指定时间范围内的报告
     */
    List<DmarcReport> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, 
                                                               LocalDateTime endTime);
    
    /**
     * 查找发送失败的报告
     */
    @Query("SELECT r FROM DmarcReport r WHERE r.isSent = false " +
           "AND r.errorMessage IS NOT NULL " +
           "AND r.retryCount > 0")
    List<DmarcReport> findFailedReports();
    
    /**
     * 统计报告数量按域名
     */
    @Query("SELECT r.domain, COUNT(r) FROM DmarcReport r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.domain ORDER BY COUNT(r) DESC")
    List<Object[]> countReportsByDomain(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计报告数量按发送状态
     */
    @Query("SELECT r.isSent, COUNT(r) FROM DmarcReport r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.isSent")
    List<Object[]> countReportsBySentStatus(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找合规率最低的报告
     */
    @Query("SELECT r FROM DmarcReport r WHERE r.totalMessages > 0 " +
           "ORDER BY (r.compliantMessages * 100.0 / r.totalMessages) ASC")
    List<DmarcReport> findReportsWithLowestComplianceRate();
    
    /**
     * 查找消息数量最多的报告
     */
    List<DmarcReport> findTop10ByOrderByTotalMessagesDesc();
    
    /**
     * 统计总体合规情况
     */
    @Query("SELECT SUM(r.totalMessages), SUM(r.compliantMessages), SUM(r.failedMessages) " +
           "FROM DmarcReport r WHERE r.createdAt BETWEEN :startTime AND :endTime")
    Object[] getTotalComplianceStats(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找特定策略的报告
     */
    List<DmarcReport> findByPolicyPOrderByCreatedAtDesc(DmarcReport.DispositionType policyP);
    
    /**
     * 查找压缩格式的报告
     */
    List<DmarcReport> findByCompressionTypeOrderByCreatedAtDesc(DmarcReport.CompressionType compressionType);
    
    /**
     * 查找大尺寸报告
     */
    @Query("SELECT r FROM DmarcReport r WHERE r.reportSize > :sizeThreshold " +
           "ORDER BY r.reportSize DESC")
    List<DmarcReport> findLargeReports(@Param("sizeThreshold") long sizeThreshold);
}