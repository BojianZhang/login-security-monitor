package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 备份任务实体
 */
@Entity
@Table(name = "backup_tasks")
public class BackupTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;
    
    @Column(name = "backup_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupType backupType;
    
    @Column(name = "backup_scope", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupScope backupScope;
    
    @Column(name = "schedule_expression", length = 100)
    private String scheduleExpression; // Cron表达式
    
    @Column(name = "storage_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;
    
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;
    
    @Column(name = "compression_enabled", nullable = false)
    private Boolean compressionEnabled = true;
    
    @Column(name = "encryption_enabled", nullable = false)
    private Boolean encryptionEnabled = false;
    
    @Column(name = "encryption_key", length = 255)
    private String encryptionKey;
    
    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays = 30;
    
    @Column(name = "max_backup_size")
    private Long maxBackupSize; // 最大备份大小（字节）
    
    @Column(name = "include_attachments", nullable = false)
    private Boolean includeAttachments = true;
    
    @Column(name = "include_logs", nullable = false)
    private Boolean includeLogs = false;
    
    @Column(name = "exclude_patterns", length = 1000)
    private String excludePatterns; // 排除模式，逗号分隔
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "last_backup_time")
    private LocalDateTime lastBackupTime;
    
    @Column(name = "next_backup_time")
    private LocalDateTime nextBackupTime;
    
    @Column(name = "last_backup_size")
    private Long lastBackupSize;
    
    @Column(name = "last_backup_status", length = 20)
    @Enumerated(EnumType.STRING)
    private BackupStatus lastBackupStatus;
    
    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;
    
    @Column(name = "backup_count", nullable = false)
    private Long backupCount = 0L;
    
    @Column(name = "successful_count", nullable = false)
    private Long successfulCount = 0L;
    
    @Column(name = "failed_count", nullable = false)
    private Long failedCount = 0L;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 备份类型枚举
    public enum BackupType {
        FULL("完整备份"),
        INCREMENTAL("增量备份"),
        DIFFERENTIAL("差异备份"),
        DATABASE_ONLY("仅数据库"),
        FILES_ONLY("仅文件");
        
        private final String description;
        
        BackupType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 备份范围枚举
    public enum BackupScope {
        SYSTEM("整个系统"),
        USER("指定用户"),
        DOMAIN("指定域名"),
        FOLDER("指定文件夹"),
        CUSTOM("自定义");
        
        private final String description;
        
        BackupScope(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 存储类型枚举
    public enum StorageType {
        LOCAL("本地存储"),
        FTP("FTP服务器"),
        SFTP("SFTP服务器"),
        S3("Amazon S3"),
        AZURE("Azure Blob"),
        GOOGLE_CLOUD("Google Cloud"),
        WEBDAV("WebDAV");
        
        private final String description;
        
        StorageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 备份状态枚举
    public enum BackupStatus {
        SUCCESS("成功"),
        FAILED("失败"),
        RUNNING("运行中"),
        CANCELLED("已取消"),
        PARTIAL("部分成功");
        
        private final String description;
        
        BackupStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public BackupTask() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public BackupTask(String taskName, BackupType backupType, BackupScope backupScope) {
        this();
        this.taskName = taskName;
        this.backupType = backupType;
        this.backupScope = backupScope;
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
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public BackupType getBackupType() {
        return backupType;
    }
    
    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }
    
    public BackupScope getBackupScope() {
        return backupScope;
    }
    
    public void setBackupScope(BackupScope backupScope) {
        this.backupScope = backupScope;
    }
    
    public String getScheduleExpression() {
        return scheduleExpression;
    }
    
    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }
    
    public StorageType getStorageType() {
        return storageType;
    }
    
    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }
    
    public String getStoragePath() {
        return storagePath;
    }
    
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
    
    public Boolean getCompressionEnabled() {
        return compressionEnabled;
    }
    
    public void setCompressionEnabled(Boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }
    
    public Boolean getEncryptionEnabled() {
        return encryptionEnabled;
    }
    
    public void setEncryptionEnabled(Boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }
    
    public String getEncryptionKey() {
        return encryptionKey;
    }
    
    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public Integer getRetentionDays() {
        return retentionDays;
    }
    
    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }
    
    public Long getMaxBackupSize() {
        return maxBackupSize;
    }
    
    public void setMaxBackupSize(Long maxBackupSize) {
        this.maxBackupSize = maxBackupSize;
    }
    
    public Boolean getIncludeAttachments() {
        return includeAttachments;
    }
    
    public void setIncludeAttachments(Boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }
    
    public Boolean getIncludeLogs() {
        return includeLogs;
    }
    
    public void setIncludeLogs(Boolean includeLogs) {
        this.includeLogs = includeLogs;
    }
    
    public String getExcludePatterns() {
        return excludePatterns;
    }
    
    public void setExcludePatterns(String excludePatterns) {
        this.excludePatterns = excludePatterns;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getLastBackupTime() {
        return lastBackupTime;
    }
    
    public void setLastBackupTime(LocalDateTime lastBackupTime) {
        this.lastBackupTime = lastBackupTime;
    }
    
    public LocalDateTime getNextBackupTime() {
        return nextBackupTime;
    }
    
    public void setNextBackupTime(LocalDateTime nextBackupTime) {
        this.nextBackupTime = nextBackupTime;
    }
    
    public Long getLastBackupSize() {
        return lastBackupSize;
    }
    
    public void setLastBackupSize(Long lastBackupSize) {
        this.lastBackupSize = lastBackupSize;
    }
    
    public BackupStatus getLastBackupStatus() {
        return lastBackupStatus;
    }
    
    public void setLastBackupStatus(BackupStatus lastBackupStatus) {
        this.lastBackupStatus = lastBackupStatus;
    }
    
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
    
    public Long getBackupCount() {
        return backupCount;
    }
    
    public void setBackupCount(Long backupCount) {
        this.backupCount = backupCount;
    }
    
    public Long getSuccessfulCount() {
        return successfulCount;
    }
    
    public void setSuccessfulCount(Long successfulCount) {
        this.successfulCount = successfulCount;
    }
    
    public Long getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
     * 更新备份统计
     */
    public void updateBackupStats(BackupStatus status, Long backupSize, String errorMessage) {
        this.backupCount++;
        this.lastBackupTime = LocalDateTime.now();
        this.lastBackupStatus = status;
        this.lastBackupSize = backupSize;
        this.lastErrorMessage = errorMessage;
        
        if (status == BackupStatus.SUCCESS) {
            this.successfulCount++;
        } else if (status == BackupStatus.FAILED) {
            this.failedCount++;
        }
    }
    
    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        if (backupCount == 0) {
            return 0.0;
        }
        return (double) successfulCount / backupCount * 100.0;
    }
    
    /**
     * 检查是否需要备份
     */
    public boolean needsBackup() {
        return isActive && nextBackupTime != null && 
               LocalDateTime.now().isAfter(nextBackupTime);
    }
    
    /**
     * 获取格式化的备份大小
     */
    public String getFormattedBackupSize() {
        if (lastBackupSize == null) return "0 B";
        
        if (lastBackupSize < 1024) return lastBackupSize + " B";
        if (lastBackupSize < 1024 * 1024) return String.format("%.1f KB", lastBackupSize / 1024.0);
        if (lastBackupSize < 1024 * 1024 * 1024) return String.format("%.1f MB", lastBackupSize / (1024.0 * 1024.0));
        return String.format("%.1f GB", lastBackupSize / (1024.0 * 1024.0 * 1024.0));
    }
}