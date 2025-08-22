package com.security.monitor.repository;

import com.security.monitor.model.ActiveSyncDevice;
import com.security.monitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ActiveSync设备Repository
 */
@Repository
public interface ActiveSyncDeviceRepository extends JpaRepository<ActiveSyncDevice, Long> {
    
    /**
     * 根据设备ID查找设备
     */
    Optional<ActiveSyncDevice> findByDeviceId(String deviceId);
    
    /**
     * 根据用户查找设备列表
     */
    List<ActiveSyncDevice> findByUserOrderByLastSyncTimeDesc(User user);
    
    /**
     * 根据用户和设备状态查找设备
     */
    List<ActiveSyncDevice> findByUserAndStatus(User user, ActiveSyncDevice.DeviceStatus status);
    
    /**
     * 统计用户的非阻止设备数量
     */
    long countByUserAndIsBlockedFalse(User user);
    
    /**
     * 查找待批准的设备
     */
    List<ActiveSyncDevice> findByStatusOrderByCreatedAtDesc(ActiveSyncDevice.DeviceStatus status);
    
    /**
     * 查找被阻止的设备
     */
    List<ActiveSyncDevice> findByIsBlockedTrueOrderByUpdatedAtDesc();
    
    /**
     * 查找需要远程擦除的设备
     */
    List<ActiveSyncDevice> findByRemoteWipeRequestedTrueAndRemoteWipeAcknowledgedFalse();
    
    /**
     * 根据设备类型查找设备
     */
    List<ActiveSyncDevice> findByDeviceTypeOrderByLastSyncTimeDesc(String deviceType);
    
    /**
     * 查找最近同步的设备
     */
    @Query("SELECT d FROM ActiveSyncDevice d WHERE d.lastSyncTime >= :sinceTime " +
           "ORDER BY d.lastSyncTime DESC")
    List<ActiveSyncDevice> findRecentlyActivated(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * 查找长时间未同步的设备
     */
    @Query("SELECT d FROM ActiveSyncDevice d WHERE d.lastSyncTime < :beforeTime " +
           "OR d.lastSyncTime IS NULL ORDER BY d.lastSyncTime ASC")
    List<ActiveSyncDevice> findInactiveDevices(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 统计设备数量按状态
     */
    @Query("SELECT d.status, COUNT(d) FROM ActiveSyncDevice d GROUP BY d.status")
    List<Object[]> countDevicesByStatus();
    
    /**
     * 统计设备数量按类型
     */
    @Query("SELECT d.deviceType, COUNT(d) FROM ActiveSyncDevice d " +
           "WHERE d.deviceType IS NOT NULL GROUP BY d.deviceType ORDER BY COUNT(d) DESC")
    List<Object[]> countDevicesByType();
    
    /**
     * 查找高失败率的设备
     */
    @Query("SELECT d FROM ActiveSyncDevice d WHERE d.failedSyncCount >= :failureThreshold " +
           "ORDER BY d.failedSyncCount DESC")
    List<ActiveSyncDevice> findDevicesWithHighFailureRate(@Param("failureThreshold") int failureThreshold);
    
    /**
     * 查找指定时间范围内创建的设备
     */
    List<ActiveSyncDevice> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据IP地址查找设备
     */
    List<ActiveSyncDevice> findByLastSyncIPOrderByLastSyncTimeDesc(String ipAddress);
    
    /**
     * 查找需要策略确认的设备
     */
    @Query("SELECT d FROM ActiveSyncDevice d WHERE d.policyAcknowledged = false " +
           "OR d.status = 'PROVISION_PENDING' ORDER BY d.updatedAt DESC")
    List<ActiveSyncDevice> findDevicesNeedingPolicyAcknowledgment();
    
    /**
     * 根据协议版本查找设备
     */
    List<ActiveSyncDevice> findByProtocolVersionOrderByLastSyncTimeDesc(String protocolVersion);
    
    /**
     * 查找活跃设备（指定时间内有同步活动）
     */
    @Query("SELECT d FROM ActiveSyncDevice d WHERE d.lastSyncTime >= :sinceTime " +
           "AND d.isBlocked = false AND d.status = 'ALLOWED' " +
           "ORDER BY d.totalSyncCount DESC")
    List<ActiveSyncDevice> findActiveDevices(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * 查找同步最频繁的设备
     */
    List<ActiveSyncDevice> findTop10ByOrderByTotalSyncCountDesc();
    
    /**
     * 根据用户名查找设备
     */
    @Query("SELECT d FROM ActiveSyncDevice d WHERE d.user.username = :username " +
           "ORDER BY d.lastSyncTime DESC")
    List<ActiveSyncDevice> findByUsername(@Param("username") String username);
    
    /**
     * 查找特定时间段内活跃的设备数量
     */
    @Query("SELECT COUNT(DISTINCT d) FROM ActiveSyncDevice d WHERE d.lastSyncTime BETWEEN :startTime AND :endTime")
    long countActiveDevicesBetween(@Param("startTime") LocalDateTime startTime, 
                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户的设备数量
     */
    @Query("SELECT d.user.username, COUNT(d) FROM ActiveSyncDevice d " +
           "GROUP BY d.user.username ORDER BY COUNT(d) DESC")
    List<Object[]> countDevicesByUser();
    
    /**
     * 查找使用特定操作系统的设备
     */
    List<ActiveSyncDevice> findByDeviceOSContainingIgnoreCaseOrderByLastSyncTimeDesc(String osName);
    
    /**
     * 查找合作关系ID不为空的设备
     */
    List<ActiveSyncDevice> findByPartnershipIdIsNotNullOrderByCreatedAtDesc();
}