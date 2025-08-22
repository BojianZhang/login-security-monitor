package com.security.monitor.scheduler;

import com.security.monitor.model.SslCertificate;
import com.security.monitor.service.SslCertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SSL证书自动续期调度器
 * 负责定期检查证书过期情况并自动续期
 */
@Component
public class SslCertificateRenewalScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SslCertificateRenewalScheduler.class);
    
    @Autowired
    private SslCertificateService certificateService;
    
    @Value("${app.ssl.scheduler.enabled:true}")
    private boolean schedulerEnabled;
    
    @Value("${app.ssl.scheduler.batch-size:10}")
    private int batchSize;
    
    @Value("${app.ssl.scheduler.max-concurrent:3}")
    private int maxConcurrentRenewals;
    
    /**
     * 每天凌晨2点检查需要续期的证书
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkCertificateRenewal() {
        if (!schedulerEnabled) {
            logger.debug("SSL证书自动续期调度器已禁用");
            return;
        }
        
        logger.info("开始执行SSL证书续期检查任务");
        
        try {
            List<SslCertificate> certificatesNeedingRenewal = certificateService.getCertificatesNeedingRenewal();
            
            if (certificatesNeedingRenewal.isEmpty()) {
                logger.info("当前没有需要续期的证书");
                return;
            }
            
            logger.info("发现 {} 个证书需要续期", certificatesNeedingRenewal.size());
            
            // 分批处理证书续期
            processCertificateRenewalBatch(certificatesNeedingRenewal);
            
        } catch (Exception e) {
            logger.error("SSL证书续期检查任务执行失败", e);
        }
    }
    
    /**
     * 每4小时检查即将过期的证书（用于监控告警）
     */
    @Scheduled(cron = "0 0 */4 * * ?")
    public void checkExpiringCertificates() {
        if (!schedulerEnabled) {
            return;
        }
        
        logger.debug("开始检查即将过期的证书");
        
        try {
            // 检查7天内过期的证书
            List<SslCertificate> expiringCertificates = certificateService.getExpiringCertificates(7);
            
            if (!expiringCertificates.isEmpty()) {
                logger.warn("发现 {} 个证书将在7天内过期", expiringCertificates.size());
                
                for (SslCertificate cert : expiringCertificates) {
                    logger.warn("证书即将过期: domain={}, expires={}, days_left={}", 
                        cert.getDomainName(), cert.getExpiresAt(), cert.getDaysUntilExpiry());
                }
                
                // 这里可以发送告警通知
                sendExpirationWarnings(expiringCertificates);
            }
            
        } catch (Exception e) {
            logger.error("检查即将过期证书失败", e);
        }
    }
    
    /**
     * 每小时检查证书状态和健康度
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void monitorCertificateHealth() {
        if (!schedulerEnabled) {
            return;
        }
        
        logger.debug("开始执行证书健康度检查");
        
        try {
            // 获取证书统计信息
            SslCertificateService.CertificateStatistics stats = certificateService.getCertificateStatistics();
            
            logger.info("证书统计 - 总数: {}, 活跃: {}, 已过期: {}, 即将过期: {}", 
                stats.getTotalCertificates(), 
                stats.getActiveCertificates(),
                stats.getExpiredCertificates(),
                stats.getExpiringSoonCertificates());
            
            // 如果有已过期的证书，记录警告
            if (stats.getExpiredCertificates() > 0) {
                logger.warn("发现 {} 个已过期的证书，请及时处理", stats.getExpiredCertificates());
            }
            
        } catch (Exception e) {
            logger.error("证书健康度检查失败", e);
        }
    }
    
    /**
     * 分批处理证书续期
     */
    private void processCertificateRenewalBatch(List<SslCertificate> certificates) {
        int totalCertificates = certificates.size();
        int processedCount = 0;
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < totalCertificates; i += batchSize) {
            int endIndex = Math.min(i + batchSize, totalCertificates);
            List<SslCertificate> batch = certificates.subList(i, endIndex);
            
            logger.info("处理第 {} 批证书续期，数量: {}", (i / batchSize) + 1, batch.size());
            
            // 并发处理当前批次的证书
            CompletableFuture<Void>[] renewalFutures = batch.stream()
                .map(this::renewCertificateAsync)
                .toArray(CompletableFuture[]::new);
            
            try {
                // 等待当前批次完成
                CompletableFuture.allOf(renewalFutures).join();
                
                // 统计结果
                for (SslCertificate cert : batch) {
                    processedCount++;
                    if (cert.getStatus() == SslCertificate.CertificateStatus.ACTIVE) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }
                
                logger.info("第 {} 批续期完成，处理: {}, 成功: {}, 失败: {}", 
                    (i / batchSize) + 1, batch.size(), 
                    batch.stream().mapToInt(c -> c.getStatus() == SslCertificate.CertificateStatus.ACTIVE ? 1 : 0).sum(),
                    batch.stream().mapToInt(c -> c.getStatus() != SslCertificate.CertificateStatus.ACTIVE ? 1 : 0).sum());
                
            } catch (Exception e) {
                logger.error("第 {} 批证书续期处理失败", (i / batchSize) + 1, e);
                failureCount += batch.size();
            }
            
            // 批次间间隔，避免过度负载
            if (i + batchSize < totalCertificates) {
                try {
                    Thread.sleep(5000); // 5秒间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.info("证书续期任务完成 - 总数: {}, 处理: {}, 成功: {}, 失败: {}", 
            totalCertificates, processedCount, successCount, failureCount);
    }
    
    /**
     * 异步续期单个证书
     */
    private CompletableFuture<Void> renewCertificateAsync(SslCertificate certificate) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始续期证书: domain={}, type={}", 
                    certificate.getDomainName(), certificate.getCertificateType());
                
                certificateService.renewCertificate(certificate.getId());
                
                logger.info("证书续期完成: domain={}", certificate.getDomainName());
                
            } catch (Exception e) {
                logger.error("证书续期失败: domain=" + certificate.getDomainName(), e);
            }
        });
    }
    
    /**
     * 发送证书过期警告
     */
    private void sendExpirationWarnings(List<SslCertificate> expiringCertificates) {
        // 这里可以集成邮件通知、短信或其他告警方式
        logger.info("需要发送过期警告的证书数量: {}", expiringCertificates.size());
        
        for (SslCertificate cert : expiringCertificates) {
            // 记录告警信息
            logger.warn("证书过期警告: 域名={}, 过期时间={}, 剩余天数={}", 
                cert.getDomainName(), cert.getExpiresAt(), cert.getDaysUntilExpiry());
            
            // TODO: 实现具体的通知逻辑
            // - 发送邮件给管理员
            // - 调用webhook
            // - 发送短信等
        }
    }
    
    /**
     * 手动触发证书续期检查（用于测试或紧急情况）
     */
    public void triggerManualRenewalCheck() {
        logger.info("手动触发证书续期检查");
        checkCertificateRenewal();
    }
    
    /**
     * 获取调度器状态
     */
    public SchedulerStatus getSchedulerStatus() {
        return new SchedulerStatus(
            schedulerEnabled,
            batchSize,
            maxConcurrentRenewals,
            LocalDateTime.now()
        );
    }
    
    /**
     * 启用/禁用调度器
     */
    public void setSchedulerEnabled(boolean enabled) {
        this.schedulerEnabled = enabled;
        logger.info("SSL证书续期调度器已{}", enabled ? "启用" : "禁用");
    }
    
    // 内部数据类
    
    /**
     * 调度器状态信息
     */
    public static class SchedulerStatus {
        private final boolean enabled;
        private final int batchSize;
        private final int maxConcurrentRenewals;
        private final LocalDateTime lastCheckTime;
        
        public SchedulerStatus(boolean enabled, int batchSize, int maxConcurrentRenewals, LocalDateTime lastCheckTime) {
            this.enabled = enabled;
            this.batchSize = batchSize;
            this.maxConcurrentRenewals = maxConcurrentRenewals;
            this.lastCheckTime = lastCheckTime;
        }
        
        public boolean isEnabled() { return enabled; }
        public int getBatchSize() { return batchSize; }
        public int getMaxConcurrentRenewals() { return maxConcurrentRenewals; }
        public LocalDateTime getLastCheckTime() { return lastCheckTime; }
    }
}