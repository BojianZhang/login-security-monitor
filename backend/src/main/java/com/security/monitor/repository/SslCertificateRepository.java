package com.security.monitor.repository;

import com.security.monitor.model.EmailDomain;
import com.security.monitor.model.SslCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SSL证书仓库接口
 */
@Repository
public interface SslCertificateRepository extends JpaRepository<SslCertificate, Long> {
    
    /**
     * 根据域名查找证书
     */
    List<SslCertificate> findByDomainOrderByCreatedAtDesc(EmailDomain domain);
    
    /**
     * 根据域名名称查找证书
     */
    List<SslCertificate> findByDomainNameOrderByCreatedAtDesc(String domainName);
    
    /**
     * 查找域名的活跃证书
     */
    Optional<SslCertificate> findByDomainAndIsActiveTrueAndStatus(
        EmailDomain domain, SslCertificate.CertificateStatus status);
    
    /**
     * 根据域名名称查找活跃证书
     */
    Optional<SslCertificate> findByDomainNameAndIsActiveTrueAndStatus(
        String domainName, SslCertificate.CertificateStatus status);
    
    /**
     * 查找需要续期的证书
     */
    @Query("SELECT c FROM SslCertificate c WHERE c.autoRenew = true " +
           "AND c.isActive = true " +
           "AND c.expiresAt <= :renewalDate " +
           "AND c.status = :status")
    List<SslCertificate> findCertificatesNeedingRenewal(
        @Param("renewalDate") LocalDateTime renewalDate,
        @Param("status") SslCertificate.CertificateStatus status);
    
    /**
     * 查找即将过期的证书
     */
    @Query("SELECT c FROM SslCertificate c WHERE c.isActive = true " +
           "AND c.expiresAt BETWEEN :now AND :expiryDate " +
           "ORDER BY c.expiresAt ASC")
    List<SslCertificate> findExpiringCertificates(
        @Param("now") LocalDateTime now,
        @Param("expiryDate") LocalDateTime expiryDate);
    
    /**
     * 查找已过期的证书
     */
    @Query("SELECT c FROM SslCertificate c WHERE c.isActive = true " +
           "AND c.expiresAt < :now " +
           "ORDER BY c.expiresAt DESC")
    List<SslCertificate> findExpiredCertificates(@Param("now") LocalDateTime now);
    
    /**
     * 根据证书类型查找证书
     */
    List<SslCertificate> findByCertificateTypeOrderByCreatedAtDesc(
        SslCertificate.CertificateType certificateType);
    
    /**
     * 根据状态查找证书
     */
    List<SslCertificate> findByStatusOrderByCreatedAtDesc(SslCertificate.CertificateStatus status);
    
    /**
     * 统计各类型证书数量
     */
    @Query("SELECT c.certificateType, COUNT(c) FROM SslCertificate c " +
           "WHERE c.isActive = true GROUP BY c.certificateType")
    List<Object[]> countCertificatesByType();
    
    /**
     * 统计各状态证书数量
     */
    @Query("SELECT c.status, COUNT(c) FROM SslCertificate c " +
           "WHERE c.isActive = true GROUP BY c.status")
    List<Object[]> countCertificatesByStatus();
    
    /**
     * 查找ACME账户邮箱的证书
     */
    List<SslCertificate> findByAcmeAccountEmailOrderByCreatedAtDesc(String acmeAccountEmail);
    
    /**
     * 查找免费证书
     */
    @Query("SELECT c FROM SslCertificate c WHERE c.certificateType IN :freeTypes " +
           "AND c.isActive = true ORDER BY c.createdAt DESC")
    List<SslCertificate> findFreeCertificates(@Param("freeTypes") List<SslCertificate.CertificateType> freeTypes);
    
    /**
     * 查找用户上传的证书
     */
    List<SslCertificate> findByCertificateTypeAndIsActiveTrueOrderByCreatedAtDesc(
        SslCertificate.CertificateType certificateType);
    
    /**
     * 检查域名是否已有活跃证书
     */
    @Query("SELECT COUNT(c) > 0 FROM SslCertificate c WHERE c.domainName = :domainName " +
           "AND c.isActive = true AND c.status = :status")
    boolean hasActiveCertificate(@Param("domainName") String domainName, 
                                @Param("status") SslCertificate.CertificateStatus status);
    
    /**
     * 获取证书统计信息
     */
    @Query("SELECT " +
           "COUNT(c) as total, " +
           "SUM(CASE WHEN c.status = 'ACTIVE' THEN 1 ELSE 0 END) as active, " +
           "SUM(CASE WHEN c.expiresAt < :now THEN 1 ELSE 0 END) as expired, " +
           "SUM(CASE WHEN c.expiresAt BETWEEN :now AND :thirtyDaysLater THEN 1 ELSE 0 END) as expiringSoon " +
           "FROM SslCertificate c WHERE c.isActive = true")
    List<Object[]> getCertificateStatistics(@Param("now") LocalDateTime now, 
                                           @Param("thirtyDaysLater") LocalDateTime thirtyDaysLater);
}