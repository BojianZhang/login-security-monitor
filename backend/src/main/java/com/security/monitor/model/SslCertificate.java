package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SSL/TLS 证书管理实体
 */
@Entity
@Table(name = "ssl_certificates")
public class SslCertificate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private EmailDomain domain;
    
    @NotBlank(message = "证书名称不能为空")
    @Column(name = "certificate_name", nullable = false, length = 100)
    private String certificateName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false)
    private CertificateType certificateType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificateStatus status;
    
    @Column(name = "domain_name", nullable = false, length = 255)
    private String domainName; // 证书绑定的域名
    
    @Column(name = "subject_alternative_names", columnDefinition = "TEXT")
    private String subjectAlternativeNames; // SAN，多域名支持
    
    @Column(name = "issuer", length = 255)
    private String issuer; // 证书颁发者
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber; // 证书序列号
    
    @Column(name = "issued_at")
    private LocalDateTime issuedAt; // 证书颁发时间
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 证书过期时间
    
    @Column(name = "certificate_path", length = 500)
    private String certificatePath; // 证书文件路径
    
    @Column(name = "private_key_path", length = 500)
    private String privateKeyPath; // 私钥文件路径
    
    @Column(name = "certificate_chain_path", length = 500)
    private String certificateChainPath; // 证书链文件路径
    
    @Column(name = "auto_renew")
    private Boolean autoRenew = true; // 是否自动续期
    
    @Column(name = "renewal_days_before")
    private Integer renewalDaysBefore = 30; // 过期前多少天续期
    
    @Column(name = "last_renewal_attempt")
    private LocalDateTime lastRenewalAttempt; // 最后续期尝试时间
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError; // 最后一次错误信息
    
    @Column(name = "challenge_type", length = 50)
    private String challengeType; // ACME挑战类型 (http-01, dns-01, tls-alpn-01)
    
    @Column(name = "acme_account_email", length = 255)
    private String acmeAccountEmail; // ACME账户邮箱
    
    @Column(name = "is_active")
    private Boolean isActive = true; // 是否启用
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 证书类型枚举
    public enum CertificateType {
        FREE_LETSENCRYPT("Let's Encrypt 免费证书"),
        FREE_ZEROSSL("ZeroSSL 免费证书"),
        USER_UPLOADED("用户上传证书"),
        SELF_SIGNED("自签名证书");
        
        private final String description;
        
        CertificateType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 证书状态枚举
    public enum CertificateStatus {
        PENDING("待处理"),
        ACTIVE("活跃"),
        EXPIRED("已过期"),
        REVOKED("已吊销"),
        ERROR("错误"),
        RENEWAL_NEEDED("需要续期");
        
        private final String description;
        
        CertificateStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Constructors
    public SslCertificate() {}
    
    public SslCertificate(EmailDomain domain, String certificateName, CertificateType certificateType) {
        this.domain = domain;
        this.certificateName = certificateName;
        this.certificateType = certificateType;
        this.domainName = domain.getDomainName();
        this.status = CertificateStatus.PENDING;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public EmailDomain getDomain() { return domain; }
    public void setDomain(EmailDomain domain) { this.domain = domain; }
    
    public String getCertificateName() { return certificateName; }
    public void setCertificateName(String certificateName) { this.certificateName = certificateName; }
    
    public CertificateType getCertificateType() { return certificateType; }
    public void setCertificateType(CertificateType certificateType) { this.certificateType = certificateType; }
    
    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }
    
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    
    public String getSubjectAlternativeNames() { return subjectAlternativeNames; }
    public void setSubjectAlternativeNames(String subjectAlternativeNames) { 
        this.subjectAlternativeNames = subjectAlternativeNames; 
    }
    
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
    
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    
    public String getCertificateChainPath() { return certificateChainPath; }
    public void setCertificateChainPath(String certificateChainPath) { 
        this.certificateChainPath = certificateChainPath; 
    }
    
    public Boolean getAutoRenew() { return autoRenew; }
    public void setAutoRenew(Boolean autoRenew) { this.autoRenew = autoRenew; }
    
    public Integer getRenewalDaysBefore() { return renewalDaysBefore; }
    public void setRenewalDaysBefore(Integer renewalDaysBefore) { 
        this.renewalDaysBefore = renewalDaysBefore; 
    }
    
    public LocalDateTime getLastRenewalAttempt() { return lastRenewalAttempt; }
    public void setLastRenewalAttempt(LocalDateTime lastRenewalAttempt) { 
        this.lastRenewalAttempt = lastRenewalAttempt; 
    }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    
    public String getChallengeType() { return challengeType; }
    public void setChallengeType(String challengeType) { this.challengeType = challengeType; }
    
    public String getAcmeAccountEmail() { return acmeAccountEmail; }
    public void setAcmeAccountEmail(String acmeAccountEmail) { this.acmeAccountEmail = acmeAccountEmail; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // 业务方法
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean needsRenewal() {
        if (expiresAt == null || !autoRenew) {
            return false;
        }
        return expiresAt.isBefore(LocalDateTime.now().plusDays(renewalDaysBefore));
    }
    
    public long getDaysUntilExpiry() {
        if (expiresAt == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }
    
    public boolean isFreeType() {
        return certificateType == CertificateType.FREE_LETSENCRYPT || 
               certificateType == CertificateType.FREE_ZEROSSL;
    }
    
    @Override
    public String toString() {
        return String.format("SslCertificate{id=%d, domain='%s', type=%s, status=%s}", 
            id, domainName, certificateType, status);
    }
}