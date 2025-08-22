package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ActiveSync设备实体
 */
@Entity
@Table(name = "activesync_devices")
public class ActiveSyncDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "device_id", unique = true, nullable = false, length = 100)
    private String deviceId; // 设备唯一标识
    
    @Column(name = "device_type", length = 50)
    private String deviceType; // 设备类型：iPhone, Android, Windows等
    
    @Column(name = "device_model", length = 100)
    private String deviceModel; // 设备型号
    
    @Column(name = "device_os", length = 50)
    private String deviceOS; // 操作系统版本
    
    @Column(name = "device_language", length = 10)
    private String deviceLanguage; // 设备语言
    
    @Column(name = "device_user_agent", length = 500)
    private String deviceUserAgent; // 设备User-Agent
    
    @Column(name = "device_friendly_name", length = 100)
    private String deviceFriendlyName; // 设备友好名称
    
    @Column(name = "device_imei", length = 20)
    private String deviceIMEI; // 设备IMEI号
    
    @Column(name = "device_mobile_operator", length = 100)
    private String deviceMobileOperator; // 移动运营商
    
    @Column(name = "protocol_version", length = 10)
    private String protocolVersion = "14.1"; // ActiveSync协议版本
    
    @Column(name = "partnership_id", length = 100)
    private String partnershipId; // 合作关系ID
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private DeviceStatus status = DeviceStatus.PENDING;
    
    @Column(name = "sync_key", length = 100)
    private String syncKey; // 同步密钥
    
    @Column(name = "folder_sync_key", length = 100)
    private String folderSyncKey; // 文件夹同步密钥
    
    @Column(name = "policy_key", length = 100)
    private String policyKey; // 安全策略密钥
    
    @Column(name = "policy_acknowledged", nullable = false)
    private Boolean policyAcknowledged = false; // 是否确认安全策略
    
    @Column(name = "remote_wipe_requested", nullable = false)
    private Boolean remoteWipeRequested = false; // 是否请求远程擦除
    
    @Column(name = "remote_wipe_acknowledged", nullable = false)
    private Boolean remoteWipeAcknowledged = false; // 是否确认远程擦除
    
    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime; // 最后同步时间
    
    @Column(name = "last_sync_ip", length = 45)
    private String lastSyncIP; // 最后同步IP
    
    @Column(name = "heartbeat_interval")
    private Integer heartbeatInterval = 300; // 心跳间隔（秒）
    
    @Column(name = "max_items_to_sync")
    private Integer maxItemsToSync = 100; // 单次同步最大项目数
    
    @Column(name = "sync_conflicts_resolution", length = 20)
    @Enumerated(EnumType.STRING)
    private ConflictResolution syncConflictsResolution = ConflictResolution.SERVER_WINS;
    
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false; // 是否被阻止
    
    @Column(name = "block_reason", length = 500)
    private String blockReason; // 阻止原因
    
    @Column(name = "first_sync_time")
    private LocalDateTime firstSyncTime; // 首次同步时间
    
    @Column(name = "total_sync_count", nullable = false)
    private Long totalSyncCount = 0L; // 总同步次数
    
    @Column(name = "failed_sync_count", nullable = false)
    private Integer failedSyncCount = 0; // 失败同步次数
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActiveSyncFolder> folders; // 同步文件夹
    
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActiveSyncLog> syncLogs; // 同步日志
    
    // 设备状态枚举
    public enum DeviceStatus {
        PENDING("待批准"),
        ALLOWED("已允许"),
        BLOCKED("已阻止"),
        QUARANTINED("已隔离"),
        WIPED("已擦除"),
        PROVISION_PENDING("策略待确认");
        
        private final String description;
        
        DeviceStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 冲突解决策略枚举
    public enum ConflictResolution {
        SERVER_WINS("服务器优先"),
        CLIENT_WINS("客户端优先"),
        DUPLICATE("创建副本");
        
        private final String description;
        
        ConflictResolution(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public ActiveSyncDevice() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ActiveSyncDevice(User user, String deviceId, String deviceType) {
        this();
        this.user = user;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.firstSyncTime = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getDeviceModel() {
        return deviceModel;
    }
    
    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }
    
    public String getDeviceOS() {
        return deviceOS;
    }
    
    public void setDeviceOS(String deviceOS) {
        this.deviceOS = deviceOS;
    }
    
    public String getDeviceLanguage() {
        return deviceLanguage;
    }
    
    public void setDeviceLanguage(String deviceLanguage) {
        this.deviceLanguage = deviceLanguage;
    }
    
    public String getDeviceUserAgent() {
        return deviceUserAgent;
    }
    
    public void setDeviceUserAgent(String deviceUserAgent) {
        this.deviceUserAgent = deviceUserAgent;
    }
    
    public String getDeviceFriendlyName() {
        return deviceFriendlyName;
    }
    
    public void setDeviceFriendlyName(String deviceFriendlyName) {
        this.deviceFriendlyName = deviceFriendlyName;
    }
    
    public String getDeviceIMEI() {
        return deviceIMEI;
    }
    
    public void setDeviceIMEI(String deviceIMEI) {
        this.deviceIMEI = deviceIMEI;
    }
    
    public String getDeviceMobileOperator() {
        return deviceMobileOperator;
    }
    
    public void setDeviceMobileOperator(String deviceMobileOperator) {
        this.deviceMobileOperator = deviceMobileOperator;
    }
    
    public String getProtocolVersion() {
        return protocolVersion;
    }
    
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public String getPartnershipId() {
        return partnershipId;
    }
    
    public void setPartnershipId(String partnershipId) {
        this.partnershipId = partnershipId;
    }
    
    public DeviceStatus getStatus() {
        return status;
    }
    
    public void setStatus(DeviceStatus status) {
        this.status = status;
    }
    
    public String getSyncKey() {
        return syncKey;
    }
    
    public void setSyncKey(String syncKey) {
        this.syncKey = syncKey;
    }
    
    public String getFolderSyncKey() {
        return folderSyncKey;
    }
    
    public void setFolderSyncKey(String folderSyncKey) {
        this.folderSyncKey = folderSyncKey;
    }
    
    public String getPolicyKey() {
        return policyKey;
    }
    
    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }
    
    public Boolean getPolicyAcknowledged() {
        return policyAcknowledged;
    }
    
    public void setPolicyAcknowledged(Boolean policyAcknowledged) {
        this.policyAcknowledged = policyAcknowledged;
    }
    
    public Boolean getRemoteWipeRequested() {
        return remoteWipeRequested;
    }
    
    public void setRemoteWipeRequested(Boolean remoteWipeRequested) {
        this.remoteWipeRequested = remoteWipeRequested;
    }
    
    public Boolean getRemoteWipeAcknowledged() {
        return remoteWipeAcknowledged;
    }
    
    public void setRemoteWipeAcknowledged(Boolean remoteWipeAcknowledged) {
        this.remoteWipeAcknowledged = remoteWipeAcknowledged;
    }
    
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    public String getLastSyncIP() {
        return lastSyncIP;
    }
    
    public void setLastSyncIP(String lastSyncIP) {
        this.lastSyncIP = lastSyncIP;
    }
    
    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    public Integer getMaxItemsToSync() {
        return maxItemsToSync;
    }
    
    public void setMaxItemsToSync(Integer maxItemsToSync) {
        this.maxItemsToSync = maxItemsToSync;
    }
    
    public ConflictResolution getSyncConflictsResolution() {
        return syncConflictsResolution;
    }
    
    public void setSyncConflictsResolution(ConflictResolution syncConflictsResolution) {
        this.syncConflictsResolution = syncConflictsResolution;
    }
    
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public String getBlockReason() {
        return blockReason;
    }
    
    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
    
    public LocalDateTime getFirstSyncTime() {
        return firstSyncTime;
    }
    
    public void setFirstSyncTime(LocalDateTime firstSyncTime) {
        this.firstSyncTime = firstSyncTime;
    }
    
    public Long getTotalSyncCount() {
        return totalSyncCount;
    }
    
    public void setTotalSyncCount(Long totalSyncCount) {
        this.totalSyncCount = totalSyncCount;
    }
    
    public Integer getFailedSyncCount() {
        return failedSyncCount;
    }
    
    public void setFailedSyncCount(Integer failedSyncCount) {
        this.failedSyncCount = failedSyncCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<ActiveSyncFolder> getFolders() {
        return folders;
    }
    
    public void setFolders(List<ActiveSyncFolder> folders) {
        this.folders = folders;
    }
    
    public List<ActiveSyncLog> getSyncLogs() {
        return syncLogs;
    }
    
    public void setSyncLogs(List<ActiveSyncLog> syncLogs) {
        this.syncLogs = syncLogs;
    }
    
    /**
     * 生成新的同步密钥
     */
    public void generateNewSyncKey() {
        this.syncKey = java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成新的文件夹同步密钥
     */
    public void generateNewFolderSyncKey() {
        this.folderSyncKey = java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成新的策略密钥
     */
    public void generateNewPolicyKey() {
        this.policyKey = java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 更新同步统计
     */
    public void updateSyncStats(boolean success) {
        this.lastSyncTime = LocalDateTime.now();
        this.totalSyncCount++;
        
        if (!success) {
            this.failedSyncCount++;
        } else {
            // 成功同步后重置失败计数
            this.failedSyncCount = 0;
        }
    }
    
    /**
     * 阻止设备
     */
    public void blockDevice(String reason) {
        this.status = DeviceStatus.BLOCKED;
        this.isBlocked = true;
        this.blockReason = reason;
    }
    
    /**
     * 允许设备
     */
    public void allowDevice() {
        this.status = DeviceStatus.ALLOWED;
        this.isBlocked = false;
        this.blockReason = null;
    }
    
    /**
     * 请求远程擦除
     */
    public void requestRemoteWipe() {
        this.remoteWipeRequested = true;
        this.status = DeviceStatus.WIPED;
    }
    
    /**
     * 确认远程擦除
     */
    public void acknowledgeRemoteWipe() {
        this.remoteWipeAcknowledged = true;
    }
    
    /**
     * 检查设备是否可以同步
     */
    public boolean canSync() {
        return status == DeviceStatus.ALLOWED && !isBlocked && !remoteWipeRequested;
    }
    
    /**
     * 检查设备是否需要策略确认
     */
    public boolean needsPolicyAcknowledgment() {
        return status == DeviceStatus.PROVISION_PENDING || !policyAcknowledged;
    }
    
    /**
     * 获取设备描述信息
     */
    public String getDeviceDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (deviceFriendlyName != null) {
            desc.append(deviceFriendlyName);
        } else {
            if (deviceType != null) desc.append(deviceType);
            if (deviceModel != null) {
                if (desc.length() > 0) desc.append(" ");
                desc.append(deviceModel);
            }
        }
        
        if (deviceOS != null) {
            if (desc.length() > 0) desc.append(" (");
            desc.append(deviceOS);
            if (desc.charAt(desc.length() - 1) != ')') desc.append(")");
        }
        
        return desc.length() > 0 ? desc.toString() : deviceId;
    }
}