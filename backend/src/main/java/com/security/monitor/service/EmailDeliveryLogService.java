package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 邮件投递日志服务
 */
@Service
@Transactional
public class EmailDeliveryLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailDeliveryLogService.class);
    
    @Autowired
    private EmailDeliveryLogRepository deliveryLogRepository;
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private EmailQueueRepository queueRepository;
    
    @Value("${mail.delivery.max-attempts:3}")
    private int maxDeliveryAttempts;
    
    @Value("${mail.delivery.retry-delay-minutes:15}")
    private int retryDelayMinutes;
    
    @Value("${mail.delivery.max-retry-hours:72}")
    private int maxRetryHours;
    
    /**
     * 记录邮件投递日志
     */
    public EmailDeliveryLog logDelivery(EmailMessage message, String toAddress, 
                                       EmailDeliveryLog.DeliveryStatus status) {
        
        EmailDeliveryLog log = new EmailDeliveryLog();
        log.setMessage(message);
        log.setMessageIdString(message.getMessageId());
        log.setFromAddress(message.getFromAddress());
        log.setToAddress(toAddress);
        log.setDeliveryStatus(status);
        log.setMessageSize(message.getMessageSize());
        
        // 提取收件人域名
        log.extractRecipientDomain();
        
        return deliveryLogRepository.save(log);
    }
    
    /**
     * 记录SMTP投递结果
     */
    public EmailDeliveryLog logSmtpDelivery(EmailMessage message, String toAddress,
                                           String queueId, Integer smtpResponseCode, 
                                           String smtpResponseMessage, String deliveryServer) {
        
        EmailDeliveryLog log = logDelivery(message, toAddress, 
            determineStatusFromSmtpCode(smtpResponseCode));
        
        log.setQueueId(queueId);
        log.setSmtpResponseCode(smtpResponseCode);
        log.setSmtpResponseMessage(smtpResponseMessage);
        log.setDeliveryServer(deliveryServer);
        log.setDeliveryMethod(EmailDeliveryLog.DeliveryMethod.SMTP);
        
        // 根据SMTP响应码设置具体状态
        updateDeliveryStatusFromSmtp(log, smtpResponseCode, smtpResponseMessage);
        
        return deliveryLogRepository.save(log);
    }
    
    /**
     * 记录投递成功
     */
    public void logSuccessfulDelivery(Long logId, String finalServer, 
                                    EmailDeliveryLog.TransportSecurity security) {
        
        EmailDeliveryLog log = deliveryLogRepository.findById(logId)
            .orElseThrow(() -> new RuntimeException("投递日志不存在"));
        
        log.markAsDelivered();
        log.setDeliveryServer(finalServer);
        log.setTransportSecurity(security);
        
        deliveryLogRepository.save(log);
        
        logger.info("邮件投递成功: messageId={}, to={}, server={}", 
            log.getMessageIdString(), log.getToAddress(), finalServer);
    }
    
    /**
     * 记录投递失败
     */
    public void logFailedDelivery(Long logId, String reason, boolean isTemporary) {
        
        EmailDeliveryLog log = deliveryLogRepository.findById(logId)
            .orElseThrow(() -> new RuntimeException("投递日志不存在"));
        
        log.incrementDeliveryAttempts();
        
        if (isTemporary && log.getDeliveryAttempts() < maxDeliveryAttempts) {
            // 临时失败，安排重试
            LocalDateTime retryTime = LocalDateTime.now()
                .plusMinutes(retryDelayMinutes * log.getDeliveryAttempts());
            log.markAsDeferred(retryTime);
            
            // 添加到重试队列
            scheduleRetry(log);
            
        } else {
            // 永久失败或重试次数超限
            if (reason.toLowerCase().contains("bounce") || 
                reason.toLowerCase().contains("not found")) {
                log.markAsBounced(reason);
            } else {
                log.setDeliveryStatus(EmailDeliveryLog.DeliveryStatus.FAILED);
                log.setBounceReason(reason);
            }
        }
        
        deliveryLogRepository.save(log);
        
        logger.warn("邮件投递失败: messageId={}, to={}, reason={}, attempts={}", 
            log.getMessageIdString(), log.getToAddress(), reason, log.getDeliveryAttempts());
    }
    
    /**
     * 获取投递统计信息
     */
    public DeliveryStatistics getDeliveryStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                                   String domain) {
        
        DeliveryStatistics stats = new DeliveryStatistics();
        stats.setStartTime(startTime);
        stats.setEndTime(endTime);
        stats.setDomain(domain);
        
        List<EmailDeliveryLog> logs;
        if (domain != null && !domain.isEmpty()) {
            logs = deliveryLogRepository.findByRecipientDomainAndCreatedAtBetween(
                domain, startTime, endTime);
        } else {
            logs = deliveryLogRepository.findByCreatedAtBetween(startTime, endTime);
        }
        
        // 统计各种投递状态
        Map<EmailDeliveryLog.DeliveryStatus, Long> statusCounts = logs.stream()
            .collect(Collectors.groupingBy(
                EmailDeliveryLog::getDeliveryStatus, 
                Collectors.counting()));
        
        stats.setTotalMessages(logs.size());
        stats.setDeliveredMessages(statusCounts.getOrDefault(
            EmailDeliveryLog.DeliveryStatus.DELIVERED, 0L));
        stats.setBouncedMessages(statusCounts.getOrDefault(
            EmailDeliveryLog.DeliveryStatus.BOUNCED, 0L));
        stats.setDeferredMessages(statusCounts.getOrDefault(
            EmailDeliveryLog.DeliveryStatus.DEFERRED, 0L));
        stats.setFailedMessages(statusCounts.getOrDefault(
            EmailDeliveryLog.DeliveryStatus.FAILED, 0L));
        
        // 计算平均投递时间
        OptionalDouble avgDeliveryTime = logs.stream()
            .filter(log -> log.getDeliveredAt() != null)
            .mapToLong(log -> ChronoUnit.SECONDS.between(log.getCreatedAt(), log.getDeliveredAt()))
            .average();
        
        stats.setAverageDeliveryTime(avgDeliveryTime.orElse(0.0));
        
        // 统计重试次数
        OptionalDouble avgRetries = logs.stream()
            .mapToInt(EmailDeliveryLog::getDeliveryAttempts)
            .average();
        
        stats.setAverageRetries(avgRetries.orElse(0.0));
        
        // 按域名统计
        Map<String, Long> domainStats = logs.stream()
            .filter(log -> log.getRecipientDomain() != null)
            .collect(Collectors.groupingBy(
                EmailDeliveryLog::getRecipientDomain,
                Collectors.counting()));
        
        stats.setDomainStatistics(domainStats);
        
        return stats;
    }
    
    /**
     * 获取需要重试的投递任务
     */
    public List<EmailDeliveryLog> getPendingRetries() {
        return deliveryLogRepository.findPendingRetries(LocalDateTime.now());
    }
    
    /**
     * 清理过期的投递日志
     */
    @Transactional
    public int cleanupExpiredLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        return deliveryLogRepository.deleteByCreatedAtBefore(cutoffDate);
    }
    
    /**
     * 获取投递失败的邮件列表
     */
    public List<EmailDeliveryLog> getFailedDeliveries(LocalDateTime startTime, LocalDateTime endTime) {
        return deliveryLogRepository.findFailedDeliveries(startTime, endTime);
    }
    
    /**
     * 重新安排投递
     */
    public void rescheduleDelivery(Long logId) {
        EmailDeliveryLog log = deliveryLogRepository.findById(logId)
            .orElseThrow(() -> new RuntimeException("投递日志不存在"));
        
        if (log.getDeliveryAttempts() >= maxDeliveryAttempts) {
            throw new RuntimeException("已达到最大重试次数");
        }
        
        log.setDeliveryStatus(EmailDeliveryLog.DeliveryStatus.QUEUED);
        log.setRetryUntil(LocalDateTime.now().plusHours(maxRetryHours));
        
        scheduleRetry(log);
        
        deliveryLogRepository.save(log);
    }
    
    // 私有方法
    
    /**
     * 根据SMTP响应码确定投递状态
     */
    private EmailDeliveryLog.DeliveryStatus determineStatusFromSmtpCode(Integer code) {
        if (code == null) {
            return EmailDeliveryLog.DeliveryStatus.QUEUED;
        }
        
        if (code >= 200 && code < 300) {
            return EmailDeliveryLog.DeliveryStatus.DELIVERED;
        } else if (code >= 400 && code < 500) {
            return EmailDeliveryLog.DeliveryStatus.DEFERRED;
        } else if (code >= 500 && code < 600) {
            return EmailDeliveryLog.DeliveryStatus.BOUNCED;
        } else {
            return EmailDeliveryLog.DeliveryStatus.FAILED;
        }
    }
    
    /**
     * 根据SMTP响应更新投递状态
     */
    private void updateDeliveryStatusFromSmtp(EmailDeliveryLog log, Integer code, String message) {
        if (code == null) return;
        
        if (code == 250) {
            log.markAsDelivered();
        } else if (code >= 400 && code < 500) {
            // 临时失败
            LocalDateTime retryTime = LocalDateTime.now().plusMinutes(retryDelayMinutes);
            log.markAsDeferred(retryTime);
        } else if (code >= 500 && code < 600) {
            // 永久失败
            if (code == 550 || code == 551 || code == 553) {
                log.markAsBounced(message);
            } else {
                log.setDeliveryStatus(EmailDeliveryLog.DeliveryStatus.REJECTED);
                log.setBounceReason(message);
            }
        }
    }
    
    /**
     * 安排重试投递
     */
    private void scheduleRetry(EmailDeliveryLog log) {
        try {
            // 创建重试队列条目
            EmailQueue retryQueue = new EmailQueue();
            retryQueue.setFromAddress(log.getFromAddress());
            retryQueue.setToAddress(log.getToAddress());
            retryQueue.setSubject("Retry delivery for: " + log.getMessageIdString());
            retryQueue.setPriority((byte) 2); // 重试优先级稍低
            retryQueue.setScheduledAt(log.getRetryUntil());
            retryQueue.setMaxAttempts(maxDeliveryAttempts - log.getDeliveryAttempts());
            
            queueRepository.save(retryQueue);
            
        } catch (Exception e) {
            logger.error("安排重试投递失败", e);
        }
    }
    
    // 投递统计结果类
    public static class DeliveryStatistics {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String domain;
        private long totalMessages;
        private long deliveredMessages;
        private long bouncedMessages;
        private long deferredMessages;
        private long failedMessages;
        private double averageDeliveryTime; // 秒
        private double averageRetries;
        private Map<String, Long> domainStatistics = new HashMap<>();
        
        // Getters and Setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        
        public long getDeliveredMessages() { return deliveredMessages; }
        public void setDeliveredMessages(long deliveredMessages) { this.deliveredMessages = deliveredMessages; }
        
        public long getBouncedMessages() { return bouncedMessages; }
        public void setBouncedMessages(long bouncedMessages) { this.bouncedMessages = bouncedMessages; }
        
        public long getDeferredMessages() { return deferredMessages; }
        public void setDeferredMessages(long deferredMessages) { this.deferredMessages = deferredMessages; }
        
        public long getFailedMessages() { return failedMessages; }
        public void setFailedMessages(long failedMessages) { this.failedMessages = failedMessages; }
        
        public double getAverageDeliveryTime() { return averageDeliveryTime; }
        public void setAverageDeliveryTime(double averageDeliveryTime) { this.averageDeliveryTime = averageDeliveryTime; }
        
        public double getAverageRetries() { return averageRetries; }
        public void setAverageRetries(double averageRetries) { this.averageRetries = averageRetries; }
        
        public Map<String, Long> getDomainStatistics() { return domainStatistics; }
        public void setDomainStatistics(Map<String, Long> domainStatistics) { this.domainStatistics = domainStatistics; }
        
        public double getDeliveryRate() {
            return totalMessages > 0 ? (double) deliveredMessages / totalMessages * 100.0 : 0.0;
        }
        
        public double getBounceRate() {
            return totalMessages > 0 ? (double) bouncedMessages / totalMessages * 100.0 : 0.0;
        }
    }
}