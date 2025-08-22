package com.security.monitor.repository;

import com.security.monitor.model.EmailDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件投递日志Repository
 */
@Repository
public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, Long> {
    
    /**
     * 根据消息ID查找投递日志
     */
    List<EmailDeliveryLog> findByMessageIdString(String messageId);
    
    /**
     * 根据收件人地址查找投递日志
     */
    List<EmailDeliveryLog> findByToAddress(String toAddress);
    
    /**
     * 根据发件人地址查找投递日志
     */
    List<EmailDeliveryLog> findByFromAddress(String fromAddress);
    
    /**
     * 根据投递状态查找投递日志
     */
    List<EmailDeliveryLog> findByDeliveryStatus(EmailDeliveryLog.DeliveryStatus deliveryStatus);
    
    /**
     * 查找指定时间范围内的投递日志
     */
    List<EmailDeliveryLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据收件人域名和时间范围查找投递日志
     */
    List<EmailDeliveryLog> findByRecipientDomainAndCreatedAtBetween(
        String recipientDomain, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找需要重试的投递任务
     */
    @Query("SELECT d FROM EmailDeliveryLog d WHERE d.deliveryStatus = 'DEFERRED' " +
           "AND d.retryUntil IS NOT NULL AND d.retryUntil <= :currentTime " +
           "AND d.deliveryAttempts < :maxAttempts")
    List<EmailDeliveryLog> findPendingRetries(@Param("currentTime") LocalDateTime currentTime,
                                             @Param("maxAttempts") int maxAttempts);
    
    /**
     * 查找需要重试的投递任务（重载方法）
     */
    @Query("SELECT d FROM EmailDeliveryLog d WHERE d.deliveryStatus = 'DEFERRED' " +
           "AND d.retryUntil IS NOT NULL AND d.retryUntil <= :currentTime")
    List<EmailDeliveryLog> findPendingRetries(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 查找投递失败的邮件
     */
    @Query("SELECT d FROM EmailDeliveryLog d WHERE d.deliveryStatus IN ('FAILED', 'BOUNCED', 'REJECTED', 'EXPIRED') " +
           "AND d.createdAt BETWEEN :startTime AND :endTime")
    List<EmailDeliveryLog> findFailedDeliveries(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的投递状态数量
     */
    @Query("SELECT d.deliveryStatus, COUNT(d) FROM EmailDeliveryLog d " +
           "WHERE d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.deliveryStatus")
    List<Object[]> countByDeliveryStatusAndCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定域名和时间范围内的投递状态数量
     */
    @Query("SELECT d.deliveryStatus, COUNT(d) FROM EmailDeliveryLog d " +
           "WHERE d.recipientDomain = :domain AND d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.deliveryStatus")
    List<Object[]> countByRecipientDomainAndDeliveryStatusAndCreatedAtBetween(
        @Param("domain") String domain,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的总投递次数
     */
    @Query("SELECT COUNT(d) FROM EmailDeliveryLog d WHERE d.createdAt BETWEEN :startTime AND :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定状态和时间范围内的投递次数
     */
    @Query("SELECT COUNT(d) FROM EmailDeliveryLog d WHERE d.deliveryStatus = :status " +
           "AND d.createdAt BETWEEN :startTime AND :endTime")
    long countByDeliveryStatusAndCreatedAtBetween(@Param("status") EmailDeliveryLog.DeliveryStatus status,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找指定域名的投递统计
     */
    @Query("SELECT d.recipientDomain, COUNT(d) FROM EmailDeliveryLog d " +
           "WHERE d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.recipientDomain ORDER BY COUNT(d) DESC")
    List<Object[]> getDeliveryStatsByDomain(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找指定SMTP服务器的投递统计
     */
    @Query("SELECT d.deliveryServer, COUNT(d), " +
           "SUM(CASE WHEN d.deliveryStatus = 'DELIVERED' THEN 1 ELSE 0 END) as delivered " +
           "FROM EmailDeliveryLog d " +
           "WHERE d.deliveryServer IS NOT NULL AND d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.deliveryServer ORDER BY COUNT(d) DESC")
    List<Object[]> getDeliveryStatsByServer(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找平均投递时间
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, d.createdAt, d.deliveredAt)) " +
           "FROM EmailDeliveryLog d " +
           "WHERE d.deliveryStatus = 'DELIVERED' AND d.deliveredAt IS NOT NULL " +
           "AND d.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageDeliveryTime(@Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找平均重试次数
     */
    @Query("SELECT AVG(d.deliveryAttempts) FROM EmailDeliveryLog d " +
           "WHERE d.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageRetryAttempts(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找队列ID对应的投递日志
     */
    List<EmailDeliveryLog> findByQueueId(String queueId);
    
    /**
     * 查找最近的投递日志
     */
    List<EmailDeliveryLog> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 查找指定邮件消息的投递日志
     */
    List<EmailDeliveryLog> findByMessageOrderByCreatedAtDesc(EmailMessage message);
    
    /**
     * 删除指定时间之前的投递日志
     */
    @Modifying
    @Query("DELETE FROM EmailDeliveryLog d WHERE d.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 查找投递延迟超过指定时间的日志
     */
    @Query("SELECT d FROM EmailDeliveryLog d WHERE d.deliveryDelaySeconds > :delaySeconds " +
           "AND d.createdAt BETWEEN :startTime AND :endTime")
    List<EmailDeliveryLog> findDelayedDeliveries(@Param("delaySeconds") long delaySeconds,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找高重试次数的投递日志
     */
    @Query("SELECT d FROM EmailDeliveryLog d WHERE d.deliveryAttempts >= :minAttempts " +
           "AND d.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY d.deliveryAttempts DESC")
    List<EmailDeliveryLog> findHighRetryDeliveries(@Param("minAttempts") int minAttempts,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计退信原因
     */
    @Query("SELECT d.bounceReason, COUNT(d) FROM EmailDeliveryLog d " +
           "WHERE d.deliveryStatus = 'BOUNCED' AND d.bounceReason IS NOT NULL " +
           "AND d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.bounceReason ORDER BY COUNT(d) DESC")
    List<Object[]> getBounceReasonStats(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计SMTP响应码
     */
    @Query("SELECT d.smtpResponseCode, COUNT(d) FROM EmailDeliveryLog d " +
           "WHERE d.smtpResponseCode IS NOT NULL " +
           "AND d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.smtpResponseCode ORDER BY COUNT(d) DESC")
    List<Object[]> getSmtpResponseCodeStats(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找特定SMTP响应码的投递日志
     */
    List<EmailDeliveryLog> findBySmtpResponseCodeAndCreatedAtBetween(
        Integer responseCode, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找使用特定传输安全的投递日志
     */
    List<EmailDeliveryLog> findByTransportSecurityAndCreatedAtBetween(
        EmailDeliveryLog.TransportSecurity transportSecurity, 
        LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找特定IP地址的投递日志
     */
    List<EmailDeliveryLog> findBySenderIpAndCreatedAtBetween(
        String senderIp, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计传输安全使用情况
     */
    @Query("SELECT d.transportSecurity, COUNT(d) FROM EmailDeliveryLog d " +
           "WHERE d.transportSecurity IS NOT NULL " +
           "AND d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.transportSecurity")
    List<Object[]> getTransportSecurityStats(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 检查是否存在重复的投递日志
     */
    @Query("SELECT COUNT(d) > 1 FROM EmailDeliveryLog d " +
           "WHERE d.messageIdString = :messageId AND d.toAddress = :toAddress")
    boolean existsDuplicateDelivery(@Param("messageId") String messageId,
                                   @Param("toAddress") String toAddress);
    
    /**
     * 查找最后一次投递记录
     */
    Optional<EmailDeliveryLog> findTopByMessageIdStringAndToAddressOrderByCreatedAtDesc(
        String messageId, String toAddress);
}