package com.security.monitor.repository;

import com.security.monitor.model.DmarcReportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DMARC报告记录Repository
 */
@Repository
public interface DmarcReportRecordRepository extends JpaRepository<DmarcReportRecord, Long> {
    
    /**
     * 根据DMARC报告ID查找记录
     */
    List<DmarcReportRecord> findByDmarcReportIdOrderBySourceIp(Long dmarcReportId);
    
    /**
     * 根据源IP查找记录
     */
    List<DmarcReportRecord> findBySourceIpOrderByCreatedAtDesc(String sourceIp);
    
    /**
     * 根据From域查找记录
     */
    List<DmarcReportRecord> findByHeaderFromOrderByCreatedAtDesc(String headerFrom);
    
    /**
     * 查找DMARC通过的记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.dmarcResult = 'PASS'")
    List<DmarcReportRecord> findDmarcPassRecords();
    
    /**
     * 查找DMARC失败的记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.dmarcResult = 'FAIL'")
    List<DmarcReportRecord> findDmarcFailRecords();
    
    /**
     * 查找SPF通过的记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.spfResult = 'PASS'")
    List<DmarcReportRecord> findSpfPassRecords();
    
    /**
     * 查找DKIM通过的记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.dkimResult = 'PASS'")
    List<DmarcReportRecord> findDkimPassRecords();
    
    /**
     * 根据处置类型查找记录
     */
    List<DmarcReportRecord> findByDispositionOrderByCreatedAtDesc(DmarcReportRecord.DispositionType disposition);
    
    /**
     * 统计源IP的记录数量
     */
    @Query("SELECT r.sourceIp, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.sourceIp ORDER BY SUM(r.count) DESC")
    List<Object[]> getSourceIpStats(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计From域的记录数量
     */
    @Query("SELECT r.headerFrom, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.headerFrom ORDER BY SUM(r.count) DESC")
    List<Object[]> getHeaderFromStats(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计DMARC结果分布
     */
    @Query("SELECT r.dmarcResult, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.dmarcResult")
    List<Object[]> getDmarcResultStats(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计SPF结果分布
     */
    @Query("SELECT r.spfResult, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.spfResult")
    List<Object[]> getSpfResultStats(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计DKIM结果分布
     */
    @Query("SELECT r.dkimResult, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.dkimResult")
    List<Object[]> getDkimResultStats(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计处置类型分布
     */
    @Query("SELECT r.disposition, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.disposition")
    List<Object[]> getDispositionStats(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找高计数的记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.count >= :minCount " +
           "ORDER BY r.count DESC")
    List<DmarcReportRecord> findHighCountRecords(@Param("minCount") long minCount);
    
    /**
     * 查找认证失败的记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.authFailureType IS NOT NULL " +
           "ORDER BY r.createdAt DESC")
    List<DmarcReportRecord> findAuthFailureRecords();
    
    /**
     * 根据认证失败类型统计
     */
    @Query("SELECT r.authFailureType, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.authFailureType IS NOT NULL " +
           "AND r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.authFailureType ORDER BY SUM(r.count) DESC")
    List<Object[]> getAuthFailureTypeStats(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找DKIM域名统计
     */
    @Query("SELECT r.dkimDomain, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.dkimDomain IS NOT NULL " +
           "AND r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.dkimDomain ORDER BY SUM(r.count) DESC")
    List<Object[]> getDkimDomainStats(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找SPF域名统计
     */
    @Query("SELECT r.spfDomain, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.spfDomain IS NOT NULL " +
           "AND r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.spfDomain ORDER BY SUM(r.count) DESC")
    List<Object[]> getSpfDomainStats(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找策略覆盖原因统计
     */
    @Query("SELECT r.reasonType, COUNT(r), SUM(r.count) FROM DmarcReportRecord r " +
           "WHERE r.reasonType IS NOT NULL " +
           "AND r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.reasonType ORDER BY SUM(r.count) DESC")
    List<Object[]> getPolicyOverrideReasonStats(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找特定报告的合规记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.dmarcReport.id = :reportId " +
           "AND r.dmarcResult = 'PASS' ORDER BY r.count DESC")
    List<DmarcReportRecord> findCompliantRecordsByReport(@Param("reportId") Long reportId);
    
    /**
     * 查找特定报告的失败记录
     */
    @Query("SELECT r FROM DmarcReportRecord r WHERE r.dmarcReport.id = :reportId " +
           "AND r.dmarcResult = 'FAIL' ORDER BY r.count DESC")
    List<DmarcReportRecord> findFailedRecordsByReport(@Param("reportId") Long reportId);
    
    /**
     * 计算指定域名的总体合规率
     */
    @Query("SELECT SUM(CASE WHEN r.dmarcResult = 'PASS' THEN r.count ELSE 0 END) * 100.0 / SUM(r.count) " +
           "FROM DmarcReportRecord r " +
           "WHERE r.headerFrom = :domain AND r.createdAt BETWEEN :startTime AND :endTime")
    Double getComplianceRateByDomain(@Param("domain") String domain,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找可疑的发送源
     */
    @Query("SELECT r.sourceIp, COUNT(DISTINCT r.headerFrom), SUM(r.count) " +
           "FROM DmarcReportRecord r " +
           "WHERE r.dmarcResult = 'FAIL' " +
           "AND r.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY r.sourceIp " +
           "HAVING COUNT(DISTINCT r.headerFrom) > :domainThreshold " +
           "ORDER BY SUM(r.count) DESC")
    List<Object[]> findSuspiciousSources(@Param("domainThreshold") int domainThreshold,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}