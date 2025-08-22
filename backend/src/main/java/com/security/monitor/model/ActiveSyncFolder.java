package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ActiveSync文件夹实体
 */
@Entity
@Table(name = "activesync_folders")
public class ActiveSyncFolder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private ActiveSyncDevice device;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_folder_id")
    private EmailFolder emailFolder; // 关联的邮件文件夹
    
    @Column(name = "folder_id", nullable = false, length = 100)
    private String folderId; // ActiveSync文件夹ID
    
    @Column(name = "parent_id", length = 100)
    private String parentId; // 父文件夹ID
    
    @Column(name = "folder_name", nullable = false, length = 255)
    private String folderName; // 文件夹名称
    
    @Column(name = "folder_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FolderType folderType;
    
    @Column(name = "folder_class", length = 50)
    private String folderClass; // 文件夹类别：Email, Calendar, Contacts等
    
    @Column(name = "sync_key", length = 100)
    private String syncKey; // 文件夹同步密钥
    
    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime; // 最后同步时间
    
    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled = true; // 是否启用同步
    
    @Column(name = "sync_filter_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SyncFilterType syncFilterType = SyncFilterType.ALL; // 同步过滤类型
    
    @Column(name = "sync_truncation_size")
    private Integer syncTruncationSize; // 同步截断大小
    
    @Column(name = "sync_conflict_resolution", length = 20)
    @Enumerated(EnumType.STRING)
    private ConflictResolution syncConflictResolution = ConflictResolution.SERVER_WINS;
    
    @Column(name = "estimated_data_size")
    private Long estimatedDataSize = 0L; // 估计数据大小
    
    @Column(name = "total_item_count")
    private Integer totalItemCount = 0; // 总项目数
    
    @Column(name = "synced_item_count")
    private Integer syncedItemCount = 0; // 已同步项目数
    
    @Column(name = "last_item_sync_time")
    private LocalDateTime lastItemSyncTime; // 最后项目同步时间
    
    @Column(name = "has_changes", nullable = false)
    private Boolean hasChanges = false; // 是否有变更
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 文件夹类型枚举
    public enum FolderType {
        INBOX("收件箱"),
        SENT("已发送"),
        DRAFTS("草稿箱"),
        DELETED("已删除"),
        SPAM("垃圾邮件"),
        CALENDAR("日历"),
        CONTACTS("联系人"),
        TASKS("任务"),
        NOTES("备忘录"),
        CUSTOM("自定义");
        
        private final String description;
        
        FolderType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 同步过滤类型枚举
    public enum SyncFilterType {
        ALL("全部"),
        ONE_DAY("1天"),
        THREE_DAYS("3天"),
        ONE_WEEK("1周"),
        TWO_WEEKS("2周"),
        ONE_MONTH("1个月"),
        THREE_MONTHS("3个月"),
        SIX_MONTHS("6个月");
        
        private final String description;
        
        SyncFilterType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getDays() {
            switch (this) {
                case ONE_DAY: return 1;
                case THREE_DAYS: return 3;
                case ONE_WEEK: return 7;
                case TWO_WEEKS: return 14;
                case ONE_MONTH: return 30;
                case THREE_MONTHS: return 90;
                case SIX_MONTHS: return 180;
                default: return -1; // ALL
            }
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
    public ActiveSyncFolder() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ActiveSyncFolder(ActiveSyncDevice device, String folderId, String folderName, FolderType folderType) {
        this();
        this.device = device;
        this.folderId = folderId;
        this.folderName = folderName;
        this.folderType = folderType;
        this.generateNewSyncKey();
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
    
    public ActiveSyncDevice getDevice() {
        return device;
    }
    
    public void setDevice(ActiveSyncDevice device) {
        this.device = device;
    }
    
    public EmailFolder getEmailFolder() {
        return emailFolder;
    }
    
    public void setEmailFolder(EmailFolder emailFolder) {
        this.emailFolder = emailFolder;
    }
    
    public String getFolderId() {
        return folderId;
    }
    
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    
    public String getFolderName() {
        return folderName;
    }
    
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    
    public FolderType getFolderType() {
        return folderType;
    }
    
    public void setFolderType(FolderType folderType) {
        this.folderType = folderType;
    }
    
    public String getFolderClass() {
        return folderClass;
    }
    
    public void setFolderClass(String folderClass) {
        this.folderClass = folderClass;
    }
    
    public String getSyncKey() {
        return syncKey;
    }
    
    public void setSyncKey(String syncKey) {
        this.syncKey = syncKey;
    }
    
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }
    
    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    public Boolean getSyncEnabled() {
        return syncEnabled;
    }
    
    public void setSyncEnabled(Boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }
    
    public SyncFilterType getSyncFilterType() {
        return syncFilterType;
    }
    
    public void setSyncFilterType(SyncFilterType syncFilterType) {
        this.syncFilterType = syncFilterType;
    }
    
    public Integer getSyncTruncationSize() {
        return syncTruncationSize;
    }
    
    public void setSyncTruncationSize(Integer syncTruncationSize) {
        this.syncTruncationSize = syncTruncationSize;
    }
    
    public ConflictResolution getSyncConflictResolution() {
        return syncConflictResolution;
    }
    
    public void setSyncConflictResolution(ConflictResolution syncConflictResolution) {
        this.syncConflictResolution = syncConflictResolution;
    }
    
    public Long getEstimatedDataSize() {
        return estimatedDataSize;
    }
    
    public void setEstimatedDataSize(Long estimatedDataSize) {
        this.estimatedDataSize = estimatedDataSize;
    }
    
    public Integer getTotalItemCount() {
        return totalItemCount;
    }
    
    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }
    
    public Integer getSyncedItemCount() {
        return syncedItemCount;
    }
    
    public void setSyncedItemCount(Integer syncedItemCount) {
        this.syncedItemCount = syncedItemCount;
    }
    
    public LocalDateTime getLastItemSyncTime() {
        return lastItemSyncTime;
    }
    
    public void setLastItemSyncTime(LocalDateTime lastItemSyncTime) {
        this.lastItemSyncTime = lastItemSyncTime;
    }
    
    public Boolean getHasChanges() {
        return hasChanges;
    }
    
    public void setHasChanges(Boolean hasChanges) {
        this.hasChanges = hasChanges;
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
    
    /**
     * 生成新的同步密钥
     */
    public void generateNewSyncKey() {
        this.syncKey = java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 更新同步状态
     */
    public void updateSyncStatus(int totalItems, int syncedItems) {
        this.totalItemCount = totalItems;
        this.syncedItemCount = syncedItems;
        this.lastSyncTime = LocalDateTime.now();
        this.lastItemSyncTime = LocalDateTime.now();
        this.hasChanges = false;
    }
    
    /**
     * 标记有变更
     */
    public void markChanged() {
        this.hasChanges = true;
    }
    
    /**
     * 计算同步进度
     */
    public double getSyncProgress() {
        if (totalItemCount == null || totalItemCount == 0) {
            return 0.0;
        }
        return (double) (syncedItemCount != null ? syncedItemCount : 0) / totalItemCount * 100.0;
    }
    
    /**
     * 检查是否需要同步
     */
    public boolean needsSync() {
        return syncEnabled && hasChanges;
    }
    
    /**
     * 获取同步过滤的截止日期
     */
    public LocalDateTime getSyncCutoffDate() {
        if (syncFilterType == SyncFilterType.ALL) {
            return null;
        }
        return LocalDateTime.now().minusDays(syncFilterType.getDays());
    }
}