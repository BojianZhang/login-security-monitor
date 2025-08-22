package com.security.monitor.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ActiveSync设备DTO
 */
public class ActiveSyncDeviceDTO {
    
    private Long id;
    private String deviceId;
    private String deviceType;
    private String deviceModel;
    private String deviceOS;
    private String deviceFriendlyName;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
    
    private String lastSyncIP;
    private Long totalSyncCount;
    private Integer failedSyncCount;
    private Boolean isBlocked;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private List<ActiveSyncFolderDTO> folders;
    private List<ActiveSyncLogDTO> syncLogs;
    
    // 构造函数
    public ActiveSyncDeviceDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getDeviceFriendlyName() {
        return deviceFriendlyName;
    }
    
    public void setDeviceFriendlyName(String deviceFriendlyName) {
        this.deviceFriendlyName = deviceFriendlyName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<ActiveSyncFolderDTO> getFolders() {
        return folders;
    }
    
    public void setFolders(List<ActiveSyncFolderDTO> folders) {
        this.folders = folders;
    }
    
    public List<ActiveSyncLogDTO> getSyncLogs() {
        return syncLogs;
    }
    
    public void setSyncLogs(List<ActiveSyncLogDTO> syncLogs) {
        this.syncLogs = syncLogs;
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalSyncCount == null || totalSyncCount == 0) {
            return 0.0;
        }
        long successfulSyncs = totalSyncCount - (failedSyncCount != null ? failedSyncCount : 0);
        return (double) successfulSyncs / totalSyncCount * 100.0;
    }
    
    /**
     * 检查设备是否活跃
     */
    public boolean isActive() {
        if (lastSyncTime == null) return false;
        return lastSyncTime.isAfter(LocalDateTime.now().minusDays(7)); // 7天内有同步活动
    }
}

/**
 * ActiveSync文件夹DTO
 */
class ActiveSyncFolderDTO {
    private Long id;
    private String folderId;
    private String folderName;
    private String folderType;
    private Boolean syncEnabled;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
    
    private Integer totalItemCount;
    private Integer syncedItemCount;
    private Boolean hasChanges;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    
    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }
    
    public String getFolderType() { return folderType; }
    public void setFolderType(String folderType) { this.folderType = folderType; }
    
    public Boolean getSyncEnabled() { return syncEnabled; }
    public void setSyncEnabled(Boolean syncEnabled) { this.syncEnabled = syncEnabled; }
    
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    
    public Integer getTotalItemCount() { return totalItemCount; }
    public void setTotalItemCount(Integer totalItemCount) { this.totalItemCount = totalItemCount; }
    
    public Integer getSyncedItemCount() { return syncedItemCount; }
    public void setSyncedItemCount(Integer syncedItemCount) { this.syncedItemCount = syncedItemCount; }
    
    public Boolean getHasChanges() { return hasChanges; }
    public void setHasChanges(Boolean hasChanges) { this.hasChanges = hasChanges; }
    
    /**
     * 计算同步进度
     */
    public double getSyncProgress() {
        if (totalItemCount == null || totalItemCount == 0) return 0.0;
        return (double) (syncedItemCount != null ? syncedItemCount : 0) / totalItemCount * 100.0;
    }
}

/**
 * ActiveSync日志DTO
 */
class ActiveSyncLogDTO {
    private Long id;
    private String syncType;
    private String command;
    private String status;
    private Integer itemsAdded;
    private Integer itemsChanged;
    private Integer itemsDeleted;
    private Integer itemsFetched;
    private Long dataSentBytes;
    private Long dataReceivedBytes;
    private Long processingTimeMs;
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getItemsAdded() { return itemsAdded; }
    public void setItemsAdded(Integer itemsAdded) { this.itemsAdded = itemsAdded; }
    
    public Integer getItemsChanged() { return itemsChanged; }
    public void setItemsChanged(Integer itemsChanged) { this.itemsChanged = itemsChanged; }
    
    public Integer getItemsDeleted() { return itemsDeleted; }
    public void setItemsDeleted(Integer itemsDeleted) { this.itemsDeleted = itemsDeleted; }
    
    public Integer getItemsFetched() { return itemsFetched; }
    public void setItemsFetched(Integer itemsFetched) { this.itemsFetched = itemsFetched; }
    
    public Long getDataSentBytes() { return dataSentBytes; }
    public void setDataSentBytes(Long dataSentBytes) { this.dataSentBytes = dataSentBytes; }
    
    public Long getDataReceivedBytes() { return dataReceivedBytes; }
    public void setDataReceivedBytes(Long dataReceivedBytes) { this.dataReceivedBytes = dataReceivedBytes; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * 计算总同步项目数
     */
    public int getTotalSyncItems() {
        return (itemsAdded != null ? itemsAdded : 0) +
               (itemsChanged != null ? itemsChanged : 0) +
               (itemsDeleted != null ? itemsDeleted : 0) +
               (itemsFetched != null ? itemsFetched : 0);
    }
    
    /**
     * 计算总数据传输量
     */
    public long getTotalDataTransfer() {
        return (dataSentBytes != null ? dataSentBytes : 0) +
               (dataReceivedBytes != null ? dataReceivedBytes : 0);
    }
    
    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status) || "PARTIAL_SUCCESS".equals(status);
    }
}

/**
 * DMARC报告DTO
 */
class DmarcReportDTO {
    private Long id;
    private String reportId;
    private String domain;
    private String orgName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    private Long totalMessages;
    private Long compliantMessages;
    private Long failedMessages;
    private Double complianceRate;
    private Boolean isSent;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    
    public LocalDateTime getBeginTime() { return beginTime; }
    public void setBeginTime(LocalDateTime beginTime) { this.beginTime = beginTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public Long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(Long totalMessages) { this.totalMessages = totalMessages; }
    
    public Long getCompliantMessages() { return compliantMessages; }
    public void setCompliantMessages(Long compliantMessages) { this.compliantMessages = compliantMessages; }
    
    public Long getFailedMessages() { return failedMessages; }
    public void setFailedMessages(Long failedMessages) { this.failedMessages = failedMessages; }
    
    public Double getComplianceRate() { return complianceRate; }
    public void setComplianceRate(Double complianceRate) { this.complianceRate = complianceRate; }
    
    public Boolean getIsSent() { return isSent; }
    public void setIsSent(Boolean isSent) { this.isSent = isSent; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}