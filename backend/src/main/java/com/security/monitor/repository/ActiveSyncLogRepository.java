package com.security.monitor.repository;

import com.security.monitor.model.ActiveSyncLog;
import com.security.monitor.model.ActiveSyncDevice;
import com.security.monitor.model.ActiveSyncFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ActiveSync日志Repository
 */
@Repository
public interface ActiveSyncLogRepository extends JpaRepository<ActiveSyncLog, Long> {
    
    /**
     * 根据设备查找日志
     */
    List<ActiveSyncLog> findByDeviceOrderByCreatedAtDesc(ActiveSyncDevice device);
    
    /**
     * 根据设备和时间范围查找日志
     */
    List<ActiveSyncLog> findByDeviceAndCreatedAtBetween(ActiveSyncDevice device, 
                                                       LocalDateTime startTime, 
                                                       LocalDateTime endTime);
    
    /**
     * 根据文件夹查找日志
     */
    List<ActiveSyncLog> findByFolderOrderByCreatedAtDesc(ActiveSyncFolder folder);
    
    /**
     * 根据同步类型查找日志
     */
    List<ActiveSyncLog> findBySyncTypeOrderByCreatedAtDesc(ActiveSyncLog.SyncType syncType);
    
    /**
     * 根据同步状态查找日志
     */
    List<ActiveSyncLog> findByStatusOrderByCreatedAtDesc(ActiveSyncLog.SyncStatus status);
    
    /**
     * 查找失败的同步日志
     */
    @Query("SELECT l FROM ActiveSyncLog l WHERE l.status IN ('FAILED', 'PROTOCOL_ERROR', 'AUTHENTICATION_ERROR', 'POLICY_ERROR', 'FOLDER_ERROR', 'SYNC_ERROR', 'SERVER_ERROR', 'CLIENT_ERROR') " +
           "ORDER BY l.createdAt DESC")
    List<ActiveSyncLog> findFailedSyncs();
    
    /**
     * 根据命令查找日志
     */
    List<ActiveSyncLog> findByCommandOrderByCreatedAtDesc(String command);
    
    /**
     * 根据客户端IP查找日志
     */
    List<ActiveSyncLog> findByClientIPOrderByCreatedAtDesc(String clientIP);
    
    /**
     * 查找指定时间范围内的日志
     */
    List<ActiveSyncLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计同步类型分布
     */
    @Query("SELECT l.syncType, COUNT(l) FROM ActiveSyncLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY l.syncType ORDER BY COUNT(l) DESC")
    List<Object[]> countSyncTypesBetween(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计同步状态分布
     */
    @Query("SELECT l.status, COUNT(l) FROM ActiveSyncLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY l.status")
    List<Object[]> countSyncStatusBetween(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计命令使用情况
     */
    @Query("SELECT l.command, COUNT(l) FROM ActiveSyncLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY l.command ORDER BY COUNT(l) DESC")
    List<Object[]> countCommandsBetween(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找处理时间较长的日志
     */
    @Query("SELECT l FROM ActiveSyncLog l WHERE l.processingTimeMs > :thresholdMs " +
           "ORDER BY l.processingTimeMs DESC")
    List<ActiveSyncLog> findSlowSyncs(@Param("thresholdMs") long thresholdMs);
    
    /**
     * 查找数据传输量大的日志
     */
    @Query("SELECT l FROM ActiveSyncLog l WHERE (l.dataSentBytes + l.dataReceivedBytes) > :thresholdBytes " +
           "ORDER BY (l.dataSentBytes + l.dataReceivedBytes) DESC")
    List<ActiveSyncLog> findHighDataTransferSyncs(@Param("thresholdBytes") long thresholdBytes);
    
    /**
     * 统计指定设备的同步次数
     */
    long countByDevice(ActiveSyncDevice device);
    
    /**
     * 统计指定设备的成功同步次数
     */
    @Query("SELECT COUNT(l) FROM ActiveSyncLog l WHERE l.device = :device " +
           "AND l.status IN ('SUCCESS', 'PARTIAL_SUCCESS')")
    long countSuccessfulSyncsByDevice(@Param("device") ActiveSyncDevice device);
    
    /**
     * 统计指定时间范围内的同步次数
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找错误代码不为空的日志
     */
    List<ActiveSyncLog> findByErrorCodeIsNotNullOrderByCreatedAtDesc();
    
    /**
     * 根据错误代码查找日志
     */
    List<ActiveSyncLog> findByErrorCodeOrderByCreatedAtDesc(String errorCode);
    
    /**
     * 查找应用了策略的日志
     */
    List<ActiveSyncLog> findByPolicyAppliedTrueOrderByCreatedAtDesc();
    
    /**
     * 查找请求了擦除的日志
     */
    List<ActiveSyncLog> findByWipeRequestedTrueOrderByCreatedAtDesc();
    
    /**
     * 根据协议版本查找日志
     */
    List<ActiveSyncLog> findByProtocolVersionOrderByCreatedAtDesc(String protocolVersion);
    
    /**
     * 查找有会话ID的日志
     */
    List<ActiveSyncLog> findBySessionIdIsNotNullOrderByCreatedAtDesc();
    
    /**
     * 根据会话ID查找日志
     */
    List<ActiveSyncLog> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    /**
     * 查找有心跳间隔设置的日志
     */
    List<ActiveSyncLog> findByHeartbeatIntervalIsNotNullOrderByCreatedAtDesc();
    
    /**
     * 查找最近的同步日志
     */
    List<ActiveSyncLog> findTop100ByOrderByCreatedAtDesc();
    
    /**
     * 统计平均处理时间
     */
    @Query("SELECT AVG(l.processingTimeMs) FROM ActiveSyncLog l " +
           "WHERE l.processingTimeMs IS NOT NULL " +
           "AND l.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageProcessingTime(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计总数据传输量
     */
    @Query("SELECT SUM(l.dataSentBytes + l.dataReceivedBytes) FROM ActiveSyncLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime")
    Long getTotalDataTransfer(@Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计总同步项目数
     */
    @Query("SELECT SUM(l.itemsAdded + l.itemsChanged + l.itemsDeleted + l.itemsFetched) FROM ActiveSyncLog l " +
           "WHERE l.createdAt BETWEEN :startTime AND :endTime")
    Long getTotalSyncItems(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找特定HTTP状态码的日志
     */
    List<ActiveSyncLog> findByHttpStatusCodeOrderByCreatedAtDesc(Integer httpStatusCode);
    
    /**
     * 统计IP地址的同步活动
     */
    @Query("SELECT l.clientIP, COUNT(l) FROM ActiveSyncLog l " +
           "WHERE l.clientIP IS NOT NULL " +
           "AND l.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY l.clientIP ORDER BY COUNT(l) DESC")
    List<Object[]> countSyncsByIP(@Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找有请求体或响应体的日志（调试用）
     */
    @Query("SELECT l FROM ActiveSyncLog l WHERE l.requestBody IS NOT NULL " +
           "OR l.responseBody IS NOT NULL ORDER BY l.createdAt DESC")
    List<ActiveSyncLog> findLogsWithBody();
    
    /**
     * 删除指定时间之前的日志
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}