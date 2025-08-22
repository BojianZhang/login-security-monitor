package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 备份记录实体
 */
@Entity
@Table(name = "backup_records")
public class BackupRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private BackupTask task;
    
    @Column(name = "backup_name", nullable = false, length = 255)
    private String backupName;
    
    @Column(name = "backup_path", nullable = false, length = 500)
    private String backupPath;
    
    @Column(name = "backup_size", nullable = false)
    private Long backupSize = 0L;
    
    @Column(name = "compressed_size")
    private Long compressedSize;
    
    @Column(name = "backup_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupTask.BackupStatus backupStatus;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "duration_seconds")
    private Long durationSeconds;
    
    @Column(name = "files_processed")
    private Integer filesProcessed = 0;
    
    @Column(name = "files_skipped")
    private Integer filesSkipped = 0;
    
    @Column(name = "files_failed")
    private Integer filesFailed = 0;
    
    @Column(name = "database_tables")
    private Integer databaseTables = 0;
    
    @Column(name = "database_records")
    private Long databaseRecords = 0L;
    
    @Column(name = "checksum", length = 64)
    private String checksum; // SHA-256校验和
    
    @Column(name = "compression_ratio")
    private Double compressionRatio; // 压缩比
    
    @Column(name = "encryption_algorithm", length = 50)
    private String encryptionAlgorithm;
    
    @Column(name = "storage_location", length = 500)
    private String storageLocation;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "log_file_path", length = 500)
    private String logFilePath;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // 额外的元数据信息
    
    @Column(name = "is_restorable", nullable = false)
    private Boolean isRestorable = true;
    
    @Column(name = "restored_count", nullable = false)
    private Integer restoredCount = 0;
    
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;
    
    @Column(name = "verification_status", length = 20)
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 验证状态枚举
    public enum VerificationStatus {
        NOT_VERIFIED("未验证"),
        VERIFIED("已验证"),
        VERIFICATION_FAILED("验证失败"),
        CORRUPTED("已损坏");
        
        private final String description;
        
        VerificationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public BackupRecord() {
        this.createdAt = LocalDateTime.now();
        this.startTime = LocalDateTime.now();
    }
    
    public BackupRecord(BackupTask task, String backupName, String backupPath) {
        this();
        this.task = task;
        this.backupName = backupName;
        this.backupPath = backupPath;
        this.backupStatus = BackupTask.BackupStatus.RUNNING;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BackupTask getTask() {
        return task;
    }
    
    public void setTask(BackupTask task) {
        this.task = task;
    }
    
    public String getBackupName() {
        return backupName;
    }
    
    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }
    
    public String getBackupPath() {
        return backupPath;
    }
    
    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }
    
    public Long getBackupSize() {
        return backupSize;
    }
    
    public void setBackupSize(Long backupSize) {
        this.backupSize = backupSize;
    }
    
    public Long getCompressedSize() {
        return compressedSize;
    }
    
    public void setCompressedSize(Long compressedSize) {
        this.compressedSize = compressedSize;
    }
    
    public BackupTask.BackupStatus getBackupStatus() {
        return backupStatus;
    }
    
    public void setBackupStatus(BackupTask.BackupStatus backupStatus) {
        this.backupStatus = backupStatus;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
    
    public Long getDurationSeconds() {
        return durationSeconds;
    }
    
    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    
    public Integer getFilesProcessed() {
        return filesProcessed;
    }
    
    public void setFilesProcessed(Integer filesProcessed) {
        this.filesProcessed = filesProcessed;
    }
    
    public Integer getFilesSkipped() {
        return filesSkipped;
    }
    
    public void setFilesSkipped(Integer filesSkipped) {
        this.filesSkipped = filesSkipped;
    }
    
    public Integer getFilesFailed() {
        return filesFailed;
    }
    
    public void setFilesFailed(Integer filesFailed) {
        this.filesFailed = filesFailed;
    }
    
    public Integer getDatabaseTables() {
        return databaseTables;
    }
    
    public void setDatabaseTables(Integer databaseTables) {
        this.databaseTables = databaseTables;
    }
    
    public Long getDatabaseRecords() {
        return databaseRecords;
    }
    
    public void setDatabaseRecords(Long databaseRecords) {
        this.databaseRecords = databaseRecords;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public Double getCompressionRatio() {
        return compressionRatio;
    }
    
    public void setCompressionRatio(Double compressionRatio) {
        this.compressionRatio = compressionRatio;
    }
    
    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }
    
    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }
    
    public String getStorageLocation() {
        return storageLocation;
    }
    
    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getLogFilePath() {
        return logFilePath;
    }
    
    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public Boolean getIsRestorable() {
        return isRestorable;
    }
    
    public void setIsRestorable(Boolean isRestorable) {
        this.isRestorable = isRestorable;
    }
    
    public Integer getRestoredCount() {
        return restoredCount;
    }
    
    public void setRestoredCount(Integer restoredCount) {
        this.restoredCount = restoredCount;
    }
    
    public LocalDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }
    
    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }
    
    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }
    
    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 完成备份
     */
    public void completeBackup(BackupTask.BackupStatus status, String errorMessage) {
        this.backupStatus = status;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        
        if (startTime != null) {
            this.durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        }
        
        // 计算压缩比
        if (compressedSize != null && backupSize > 0) {
            this.compressionRatio = (double) compressedSize / backupSize;
        }
    }
    
    /**
     * 增加恢复计数
     */
    public void incrementRestoredCount() {
        this.restoredCount++;
    }
    
    /**
     * 设置验证结果
     */
    public void setVerificationResult(VerificationStatus status) {
        this.verificationStatus = status;
        this.lastVerifiedAt = LocalDateTime.now();
        
        if (status == VerificationStatus.VERIFICATION_FAILED || 
            status == VerificationStatus.CORRUPTED) {
            this.isRestorable = false;
        }
    }
    
    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return backupStatus == BackupTask.BackupStatus.SUCCESS;
    }
    
    /**
     * 获取格式化的备份大小
     */
    public String getFormattedBackupSize() {
        if (backupSize == null) return "0 B";
        
        if (backupSize < 1024) return backupSize + " B";
        if (backupSize < 1024 * 1024) return String.format("%.1f KB", backupSize / 1024.0);
        if (backupSize < 1024 * 1024 * 1024) return String.format("%.1f MB", backupSize / (1024.0 * 1024.0));
        return String.format("%.1f GB", backupSize / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * 获取格式化的压缩大小
     */
    public String getFormattedCompressedSize() {
        if (compressedSize == null) return "N/A";
        
        if (compressedSize < 1024) return compressedSize + " B";
        if (compressedSize < 1024 * 1024) return String.format("%.1f KB", compressedSize / 1024.0);
        if (compressedSize < 1024 * 1024 * 1024) return String.format("%.1f MB", compressedSize / (1024.0 * 1024.0));
        return String.format("%.1f GB", compressedSize / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * 获取格式化的持续时间
     */
    public String getFormattedDuration() {
        if (durationSeconds == null) return "N/A";
        
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * 获取格式化的压缩比
     */
    public String getFormattedCompressionRatio() {
        if (compressionRatio == null) return "N/A";
        return String.format("%.1f%%", compressionRatio * 100);
    }
    
    /**
     * 计算总文件数
     */
    public int getTotalFiles() {
        return (filesProcessed != null ? filesProcessed : 0) + 
               (filesSkipped != null ? filesSkipped : 0) + 
               (filesFailed != null ? filesFailed : 0);
    }
    
    /**
     * 计算文件处理成功率
     */
    public double getFileSuccessRate() {
        int total = getTotalFiles();
        if (total == 0) return 0.0;
        
        return (double) (filesProcessed != null ? filesProcessed : 0) / total * 100.0;
    }
}