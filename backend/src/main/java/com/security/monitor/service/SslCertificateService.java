package com.security.monitor.service;

import com.security.monitor.model.EmailDomain;
import com.security.monitor.model.SslCertificate;
import com.security.monitor.repository.EmailDomainRepository;
import com.security.monitor.repository.SslCertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SSL证书管理服务
 */
@Service
@Transactional
public class SslCertificateService {
    
    private static final Logger logger = LoggerFactory.getLogger(SslCertificateService.class);
    
    @Autowired
    private SslCertificateRepository certificateRepository;
    
    @Autowired
    private EmailDomainRepository domainRepository;
    
    @Autowired
    private AcmeCertificateService acmeService;
    
    @Value("${app.ssl.storage.path:/opt/ssl-certs}")
    private String sslStoragePath;
    
    @Value("${app.ssl.auto-renew.enabled:true}")
    private boolean autoRenewEnabled;
    
    @Value("${app.ssl.renewal.days-before:30}")
    private int renewalDaysBefore;
    
    /**
     * 获取域名的所有证书
     */
    @Transactional(readOnly = true)
    public List<SslCertificate> getDomainCertificates(String domainName) {
        return certificateRepository.findByDomainNameOrderByCreatedAtDesc(domainName);
    }
    
    /**
     * 获取域名的活跃证书
     */
    @Transactional(readOnly = true)
    public Optional<SslCertificate> getActiveCertificate(String domainName) {
        return certificateRepository.findByDomainNameAndIsActiveTrueAndStatus(
            domainName, SslCertificate.CertificateStatus.ACTIVE);
    }
    
    /**
     * 申请Let's Encrypt免费证书
     */
    public SslCertificate requestLetsEncryptCertificate(String domainName, String email, String challengeType) {
        logger.info("开始申请 Let's Encrypt 免费证书: domain={}, email={}", domainName, email);
        
        Optional<EmailDomain> domainOpt = domainRepository.findByDomainNameAndIsActiveTrue(domainName);
        if (domainOpt.isEmpty()) {
            throw new RuntimeException("域名不存在或未激活: " + domainName);
        }
        
        EmailDomain domain = domainOpt.get();
        
        // 检查是否已有活跃证书
        if (hasActiveCertificate(domainName)) {
            throw new RuntimeException("域名已有活跃证书: " + domainName);
        }
        
        // 创建证书记录
        SslCertificate certificate = new SslCertificate(domain, 
            "Let's Encrypt - " + domainName, 
            SslCertificate.CertificateType.FREE_LETSENCRYPT);
        
        certificate.setAcmeAccountEmail(email);
        certificate.setChallengeType(challengeType);
        certificate.setAutoRenew(autoRenewEnabled);
        certificate.setRenewalDaysBefore(renewalDaysBefore);
        
        try {
            // 调用ACME服务申请证书
            AcmeCertificateService.CertificateResult result = acmeService.requestCertificate(
                domainName, email, challengeType);
            
            if (result.isSuccess()) {
                // 保存证书文件
                String certDir = createCertificateDirectory(domainName);
                String certPath = saveCertificateFile(certDir, "cert.pem", result.getCertificate());
                String keyPath = saveCertificateFile(certDir, "privkey.pem", result.getPrivateKey());
                String chainPath = saveCertificateFile(certDir, "chain.pem", result.getCertificateChain());
                
                // 解析证书信息
                X509Certificate x509Cert = parseCertificate(result.getCertificate());
                
                // 更新证书信息
                certificate.setCertificatePath(certPath);
                certificate.setPrivateKeyPath(keyPath);
                certificate.setCertificateChainPath(chainPath);
                certificate.setIssuer(x509Cert.getIssuerDN().toString());
                certificate.setSerialNumber(x509Cert.getSerialNumber().toString());
                certificate.setIssuedAt(LocalDateTime.ofInstant(
                    x509Cert.getNotBefore().toInstant(), ZoneId.systemDefault()));
                certificate.setExpiresAt(LocalDateTime.ofInstant(
                    x509Cert.getNotAfter().toInstant(), ZoneId.systemDefault()));
                certificate.setStatus(SslCertificate.CertificateStatus.ACTIVE);
                
                logger.info("Let's Encrypt 证书申请成功: domain={}, expires={}", 
                    domainName, certificate.getExpiresAt());
                
            } else {
                certificate.setStatus(SslCertificate.CertificateStatus.ERROR);
                certificate.setLastError("ACME证书申请失败: " + result.getErrorMessage());
                
                logger.error("Let's Encrypt 证书申请失败: domain={}, error={}", 
                    domainName, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            certificate.setStatus(SslCertificate.CertificateStatus.ERROR);
            certificate.setLastError("证书申请异常: " + e.getMessage());
            
            logger.error("Let's Encrypt 证书申请异常: domain=" + domainName, e);
        }
        
        return certificateRepository.save(certificate);
    }
    
    /**
     * 上传用户证书
     */
    public SslCertificate uploadUserCertificate(String domainName, String certificateName,
                                              MultipartFile certificateFile, 
                                              MultipartFile privateKeyFile,
                                              MultipartFile chainFile) throws IOException {
        
        logger.info("开始上传用户证书: domain={}, name={}", domainName, certificateName);
        
        Optional<EmailDomain> domainOpt = domainRepository.findByDomainNameAndIsActiveTrue(domainName);
        if (domainOpt.isEmpty()) {
            throw new RuntimeException("域名不存在或未激活: " + domainName);
        }
        
        EmailDomain domain = domainOpt.get();
        
        // 创建证书记录
        SslCertificate certificate = new SslCertificate(domain, certificateName, 
            SslCertificate.CertificateType.USER_UPLOADED);
        
        try {
            // 验证证书文件
            byte[] certData = certificateFile.getBytes();
            X509Certificate x509Cert = parseCertificate(new String(certData));
            
            // 验证域名匹配
            if (!validateCertificateForDomain(x509Cert, domainName)) {
                throw new RuntimeException("证书与域名不匹配: " + domainName);
            }
            
            // 创建存储目录
            String certDir = createCertificateDirectory(domainName + "-uploaded");
            
            // 保存证书文件
            String certPath = saveUploadedFile(certDir, "cert.pem", certificateFile);
            String keyPath = saveUploadedFile(certDir, "privkey.pem", privateKeyFile);
            String chainPath = null;
            if (chainFile != null && !chainFile.isEmpty()) {
                chainPath = saveUploadedFile(certDir, "chain.pem", chainFile);
            }
            
            // 更新证书信息
            certificate.setCertificatePath(certPath);
            certificate.setPrivateKeyPath(keyPath);
            certificate.setCertificateChainPath(chainPath);
            certificate.setIssuer(x509Cert.getIssuerDN().toString());
            certificate.setSerialNumber(x509Cert.getSerialNumber().toString());
            certificate.setIssuedAt(LocalDateTime.ofInstant(
                x509Cert.getNotBefore().toInstant(), ZoneId.systemDefault()));
            certificate.setExpiresAt(LocalDateTime.ofInstant(
                x509Cert.getNotAfter().toInstant(), ZoneId.systemDefault()));
            certificate.setStatus(SslCertificate.CertificateStatus.ACTIVE);
            certificate.setAutoRenew(false); // 用户证书不自动续期
            
            logger.info("用户证书上传成功: domain={}, expires={}", 
                domainName, certificate.getExpiresAt());
            
        } catch (Exception e) {
            certificate.setStatus(SslCertificate.CertificateStatus.ERROR);
            certificate.setLastError("证书上传失败: " + e.getMessage());
            
            logger.error("用户证书上传失败: domain=" + domainName, e);
            throw new RuntimeException("证书上传失败: " + e.getMessage(), e);
        }
        
        return certificateRepository.save(certificate);
    }
    
    /**
     * 续期证书
     */
    public SslCertificate renewCertificate(Long certificateId) {
        Optional<SslCertificate> certOpt = certificateRepository.findById(certificateId);
        if (certOpt.isEmpty()) {
            throw new RuntimeException("证书不存在: " + certificateId);
        }
        
        SslCertificate certificate = certOpt.get();
        
        if (!certificate.isFreeType()) {
            throw new RuntimeException("只有免费证书支持自动续期");
        }
        
        logger.info("开始续期证书: domain={}, type={}", 
            certificate.getDomainName(), certificate.getCertificateType());
        
        certificate.setLastRenewalAttempt(LocalDateTime.now());
        
        try {
            // 调用ACME服务续期证书
            AcmeCertificateService.CertificateResult result = acmeService.renewCertificate(
                certificate.getDomainName(), 
                certificate.getAcmeAccountEmail(),
                certificate.getChallengeType());
            
            if (result.isSuccess()) {
                // 更新证书文件
                Files.write(Paths.get(certificate.getCertificatePath()), 
                    result.getCertificate().getBytes(), StandardCopyOption.REPLACE_EXISTING);
                Files.write(Paths.get(certificate.getPrivateKeyPath()), 
                    result.getPrivateKey().getBytes(), StandardCopyOption.REPLACE_EXISTING);
                if (certificate.getCertificateChainPath() != null) {
                    Files.write(Paths.get(certificate.getCertificateChainPath()), 
                        result.getCertificateChain().getBytes(), StandardCopyOption.REPLACE_EXISTING);
                }
                
                // 解析新证书信息
                X509Certificate x509Cert = parseCertificate(result.getCertificate());
                certificate.setExpiresAt(LocalDateTime.ofInstant(
                    x509Cert.getNotAfter().toInstant(), ZoneId.systemDefault()));
                certificate.setStatus(SslCertificate.CertificateStatus.ACTIVE);
                certificate.setLastError(null);
                
                logger.info("证书续期成功: domain={}, new_expires={}", 
                    certificate.getDomainName(), certificate.getExpiresAt());
                
            } else {
                certificate.setStatus(SslCertificate.CertificateStatus.ERROR);
                certificate.setLastError("续期失败: " + result.getErrorMessage());
                
                logger.error("证书续期失败: domain={}, error={}", 
                    certificate.getDomainName(), result.getErrorMessage());
            }
            
        } catch (Exception e) {
            certificate.setLastError("续期异常: " + e.getMessage());
            logger.error("证书续期异常: domain=" + certificate.getDomainName(), e);
        }
        
        return certificateRepository.save(certificate);
    }
    
    /**
     * 删除证书
     */
    public void deleteCertificate(Long certificateId) {
        Optional<SslCertificate> certOpt = certificateRepository.findById(certificateId);
        if (certOpt.isEmpty()) {
            throw new RuntimeException("证书不存在: " + certificateId);
        }
        
        SslCertificate certificate = certOpt.get();
        
        try {
            // 删除证书文件
            if (certificate.getCertificatePath() != null) {
                Files.deleteIfExists(Paths.get(certificate.getCertificatePath()));
            }
            if (certificate.getPrivateKeyPath() != null) {
                Files.deleteIfExists(Paths.get(certificate.getPrivateKeyPath()));
            }
            if (certificate.getCertificateChainPath() != null) {
                Files.deleteIfExists(Paths.get(certificate.getCertificateChainPath()));
            }
            
            // 删除证书目录（如果为空）
            Path certDir = Paths.get(certificate.getCertificatePath()).getParent();
            if (Files.exists(certDir) && isDirEmpty(certDir)) {
                Files.delete(certDir);
            }
            
        } catch (IOException e) {
            logger.warn("删除证书文件失败: " + certificate.getDomainName(), e);
        }
        
        certificateRepository.delete(certificate);
        logger.info("证书已删除: domain={}, type={}", 
            certificate.getDomainName(), certificate.getCertificateType());
    }
    
    /**
     * 获取需要续期的证书
     */
    @Transactional(readOnly = true)
    public List<SslCertificate> getCertificatesNeedingRenewal() {
        LocalDateTime renewalDate = LocalDateTime.now().plusDays(renewalDaysBefore);
        return certificateRepository.findCertificatesNeedingRenewal(
            renewalDate, SslCertificate.CertificateStatus.ACTIVE);
    }
    
    /**
     * 获取即将过期的证书
     */
    @Transactional(readOnly = true)
    public List<SslCertificate> getExpiringCertificates(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(days);
        return certificateRepository.findExpiringCertificates(now, expiryDate);
    }
    
    /**
     * 获取证书统计信息
     */
    @Transactional(readOnly = true)
    public CertificateStatistics getCertificateStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysLater = now.plusDays(30);
        
        List<Object[]> results = certificateRepository.getCertificateStatistics(now, thirtyDaysLater);
        
        if (results.isEmpty()) {
            return new CertificateStatistics(0, 0, 0, 0);
        }
        
        Object[] result = results.get(0);
        return new CertificateStatistics(
            ((Number) result[0]).longValue(), // total
            ((Number) result[1]).longValue(), // active
            ((Number) result[2]).longValue(), // expired
            ((Number) result[3]).longValue()  // expiringSoon
        );
    }
    
    /**
     * 检查域名是否有活跃证书
     */
    @Transactional(readOnly = true)
    public boolean hasActiveCertificate(String domainName) {
        return certificateRepository.hasActiveCertificate(
            domainName, SslCertificate.CertificateStatus.ACTIVE);
    }
    
    // 私有辅助方法
    
    private String createCertificateDirectory(String domainName) throws IOException {
        Path certDir = Paths.get(sslStoragePath, domainName, 
            String.valueOf(System.currentTimeMillis()));
        Files.createDirectories(certDir);
        return certDir.toString();
    }
    
    private String saveCertificateFile(String certDir, String filename, String content) throws IOException {
        Path filePath = Paths.get(certDir, filename);
        Files.write(filePath, content.getBytes());
        return filePath.toString();
    }
    
    private String saveUploadedFile(String certDir, String filename, MultipartFile file) throws IOException {
        Path filePath = Paths.get(certDir, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }
    
    private X509Certificate parseCertificate(String certificateContent) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
            new java.io.ByteArrayInputStream(certificateContent.getBytes()));
    }
    
    private boolean validateCertificateForDomain(X509Certificate certificate, String domainName) {
        try {
            // 检查CN
            String subjectDN = certificate.getSubjectDN().getName();
            if (subjectDN.contains("CN=" + domainName)) {
                return true;
            }
            
            // 检查SAN
            Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
            if (altNames != null) {
                for (List<?> altName : altNames) {
                    if (altName.size() >= 2 && altName.get(1).equals(domainName)) {
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("验证证书域名失败", e);
            return false;
        }
    }
    
    private boolean isDirEmpty(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        }
    }
    
    // 内部数据类
    public static class CertificateStatistics {
        private final long totalCertificates;
        private final long activeCertificates;
        private final long expiredCertificates;
        private final long expiringSoonCertificates;
        
        public CertificateStatistics(long total, long active, long expired, long expiringSoon) {
            this.totalCertificates = total;
            this.activeCertificates = active;
            this.expiredCertificates = expired;
            this.expiringSoonCertificates = expiringSoon;
        }
        
        // Getters
        public long getTotalCertificates() { return totalCertificates; }
        public long getActiveCertificates() { return activeCertificates; }
        public long getExpiredCertificates() { return expiredCertificates; }
        public long getExpiringSoonCertificates() { return expiringSoonCertificates; }
    }
}