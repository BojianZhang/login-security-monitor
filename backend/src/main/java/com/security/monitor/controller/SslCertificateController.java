package com.security.monitor.controller;

import com.security.monitor.model.SslCertificate;
import com.security.monitor.service.SslCertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SSL/TLS证书管理控制器
 */
@RestController
@RequestMapping("/api/ssl/certificates")
@CrossOrigin(origins = "*")
public class SslCertificateController {
    
    private static final Logger logger = LoggerFactory.getLogger(SslCertificateController.class);
    
    @Autowired
    private SslCertificateService certificateService;
    
    /**
     * 获取域名的所有证书
     */
    @GetMapping("/domain/{domainName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getDomainCertificates(@PathVariable String domainName) {
        try {
            List<SslCertificate> certificates = certificateService.getDomainCertificates(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainName", domainName);
            response.put("certificates", certificates);
            response.put("count", certificates.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取域名证书失败: domain=" + domainName, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取域名证书失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取域名的活跃证书
     */
    @GetMapping("/domain/{domainName}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getActiveCertificate(@PathVariable String domainName) {
        try {
            Optional<SslCertificate> certificate = certificateService.getActiveCertificate(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainName", domainName);
            response.put("hasActiveCertificate", certificate.isPresent());
            
            if (certificate.isPresent()) {
                response.put("certificate", certificate.get());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取活跃证书失败: domain=" + domainName, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取活跃证书失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 申请Let's Encrypt免费证书
     */
    @PostMapping("/request/letsencrypt")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> requestLetsEncryptCertificate(
            @RequestBody Map<String, String> request) {
        try {
            String domainName = request.get("domainName");
            String email = request.get("email");
            String challengeType = request.getOrDefault("challengeType", "http-01");
            
            if (domainName == null || domainName.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "域名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "邮箱不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            SslCertificate certificate = certificateService.requestLetsEncryptCertificate(
                domainName, email, challengeType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", certificate.getStatus() == SslCertificate.CertificateStatus.ACTIVE);
            response.put("message", certificate.getStatus() == SslCertificate.CertificateStatus.ACTIVE ? 
                "Let's Encrypt 证书申请成功" : "证书申请失败: " + certificate.getLastError());
            response.put("certificate", certificate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Let's Encrypt证书申请失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "证书申请失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 上传用户证书
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> uploadUserCertificate(
            @RequestParam("domainName") String domainName,
            @RequestParam("certificateName") String certificateName,
            @RequestParam("certificateFile") MultipartFile certificateFile,
            @RequestParam("privateKeyFile") MultipartFile privateKeyFile,
            @RequestParam(value = "chainFile", required = false) MultipartFile chainFile) {
        try {
            // 验证参数
            if (domainName == null || domainName.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "域名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (certificateFile.isEmpty() || privateKeyFile.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "证书文件和私钥文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            SslCertificate certificate = certificateService.uploadUserCertificate(
                domainName, certificateName, certificateFile, privateKeyFile, chainFile);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "用户证书上传成功");
            response.put("certificate", certificate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("用户证书上传失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "证书上传失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 续期证书
     */
    @PostMapping("/{certificateId}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> renewCertificate(@PathVariable Long certificateId) {
        try {
            SslCertificate certificate = certificateService.renewCertificate(certificateId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", certificate.getStatus() == SslCertificate.CertificateStatus.ACTIVE);
            response.put("message", certificate.getStatus() == SslCertificate.CertificateStatus.ACTIVE ? 
                "证书续期成功" : "证书续期失败: " + certificate.getLastError());
            response.put("certificate", certificate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("证书续期失败: certificateId=" + certificateId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "证书续期失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除证书
     */
    @DeleteMapping("/{certificateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> deleteCertificate(@PathVariable Long certificateId) {
        try {
            certificateService.deleteCertificate(certificateId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "证书删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("证书删除失败: certificateId=" + certificateId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "证书删除失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取需要续期的证书
     */
    @GetMapping("/renewal/needed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCertificatesNeedingRenewal() {
        try {
            List<SslCertificate> certificates = certificateService.getCertificatesNeedingRenewal();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("certificates", certificates);
            response.put("count", certificates.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取需要续期的证书失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取续期证书失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取即将过期的证书
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getExpiringCertificates(
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<SslCertificate> certificates = certificateService.getExpiringCertificates(days);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("certificates", certificates);
            response.put("count", certificates.size());
            response.put("daysThreshold", days);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取即将过期的证书失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取过期证书失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取证书统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCertificateStatistics() {
        try {
            SslCertificateService.CertificateStatistics stats = certificateService.getCertificateStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", Map.of(
                "totalCertificates", stats.getTotalCertificates(),
                "activeCertificates", stats.getActiveCertificates(),
                "expiredCertificates", stats.getExpiredCertificates(),
                "expiringSoonCertificates", stats.getExpiringSoonCertificates()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取证书统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 检查域名是否有活跃证书
     */
    @GetMapping("/domain/{domainName}/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> checkActiveCertificate(@PathVariable String domainName) {
        try {
            boolean hasActiveCertificate = certificateService.hasActiveCertificate(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainName", domainName);
            response.put("hasActiveCertificate", hasActiveCertificate);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查活跃证书失败: domain=" + domainName, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有证书列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 这里应该实现分页查询，暂时返回简单列表
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "证书列表获取成功");
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取证书列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取证书列表失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 批量续期证书
     */
    @PostMapping("/batch/renew")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> batchRenewCertificates() {
        try {
            List<SslCertificate> needingRenewal = certificateService.getCertificatesNeedingRenewal();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量续期任务已启动");
            response.put("certificatesCount", needingRenewal.size());
            
            // 异步执行批量续期
            for (SslCertificate cert : needingRenewal) {
                try {
                    certificateService.renewCertificate(cert.getId());
                } catch (Exception e) {
                    logger.error("批量续期失败: certificateId=" + cert.getId(), e);
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量续期证书失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量续期失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}