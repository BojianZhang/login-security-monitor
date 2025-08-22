package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ActiveSync同步日志实体
 */
@Entity
@Table(name = "activesync_logs")
public class ActiveSyncLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private ActiveSyncDevice device;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private ActiveSyncFolder folder;
    
    @Column(name = "sync_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SyncType syncType;
    
    @Column(name = "command", nullable = false, length = 50)
    private String command; // ActiveSync命令：FolderSync, Sync, Ping等
    
    @Column(name = "protocol_version", length = 10)
    private String protocolVersion;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "client_ip", length = 45)
    private String clientIP;
    
    @Column(name = "sync_key_sent", length = 100)
    private String syncKeySent; // 客户端发送的同步密钥
    
    @Column(name = "sync_key_returned", length = 100)
    private String syncKeyReturned; // 服务器返回的同步密钥
    
    @Column(name = "items_added")
    private Integer itemsAdded = 0; // 添加的项目数
    
    @Column(name = "items_changed")
    private Integer itemsChanged = 0; // 修改的项目数
    
    @Column(name = "items_deleted")
    private Integer itemsDeleted = 0; // 删除的项目数
    
    @Column(name = "items_fetched")
    private Integer itemsFetched = 0; // 获取的项目数
    
    @Column(name = "data_sent_bytes")
    private Long dataSentBytes = 0L; // 发送的数据量（字节）
    
    @Column(name = "data_received_bytes")
    private Long dataReceivedBytes = 0L; // 接收的数据量（字节）
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs; // 处理时间（毫秒）
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SyncStatus status;
    
    @Column(name = "error_code", length = 10)
    private String errorCode; // ActiveSync错误代码
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage; // 错误消息
    
    @Column(name = "http_status_code")
    private Integer httpStatusCode; // HTTP状态码
    
    @Column(name = "request_body", columnDefinition = "LONGTEXT")
    private String requestBody; // 请求体（调试用）
    
    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody; // 响应体（调试用）
    
    @Column(name = "session_id", length = 100)
    private String sessionId; // 会话ID
    
    @Column(name = "heartbeat_interval")
    private Integer heartbeatInterval; // 心跳间隔
    
    @Column(name = "policy_key", length = 100)
    private String policyKey; // 策略密钥
    
    @Column(name = "policy_applied", nullable = false)
    private Boolean policyApplied = false; // 是否应用了策略
    
    @Column(name = "wipe_requested", nullable = false)
    private Boolean wipeRequested = false; // 是否请求了擦除
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 同步类型枚举
    public enum SyncType {
        FOLDER_SYNC("文件夹同步"),
        SYNC("内容同步"),
        PING("心跳"),
        PROVISION("策略配置"),
        SETTINGS("设置"),
        GET_ITEM_ESTIMATE("项目估计"),
        MOVE_ITEMS("移动项目"),
        SEARCH("搜索"),
        SEND_MAIL("发送邮件"),
        SMART_REPLY("智能回复"),
        SMART_FORWARD("智能转发"),
        MEETING_RESPONSE("会议响应"),
        RESOLVE_RECIPIENTS("解析收件人"),
        VALIDATE_CERT("验证证书");
        
        private final String description;
        
        SyncType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 同步状态枚举
    public enum SyncStatus {
        SUCCESS("成功"),
        PARTIAL_SUCCESS("部分成功"),
        FAILED("失败"),
        PROTOCOL_ERROR("协议错误"),
        AUTHENTICATION_ERROR("认证错误"),
        POLICY_ERROR("策略错误"),
        FOLDER_ERROR("文件夹错误"),
        SYNC_ERROR("同步错误"),
        SERVER_ERROR("服务器错误"),
        CLIENT_ERROR("客户端错误");
        
        private final String description;
        
        SyncStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public ActiveSyncLog() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ActiveSyncLog(ActiveSyncDevice device, SyncType syncType, String command) {
        this();
        this.device = device;
        this.syncType = syncType;
        this.command = command;
        this.status = SyncStatus.SUCCESS;
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
    
    public ActiveSyncFolder getFolder() {
        return folder;
    }
    
    public void setFolder(ActiveSyncFolder folder) {
        this.folder = folder;
    }
    
    public SyncType getSyncType() {
        return syncType;
    }
    
    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public String getProtocolVersion() {
        return protocolVersion;
    }
    
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getClientIP() {
        return clientIP;
    }
    
    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }
    
    public String getSyncKeySent() {
        return syncKeySent;
    }
    
    public void setSyncKeySent(String syncKeySent) {
        this.syncKeySent = syncKeySent;
    }
    
    public String getSyncKeyReturned() {
        return syncKeyReturned;
    }
    
    public void setSyncKeyReturned(String syncKeyReturned) {
        this.syncKeyReturned = syncKeyReturned;
    }
    
    public Integer getItemsAdded() {
        return itemsAdded;
    }
    
    public void setItemsAdded(Integer itemsAdded) {
        this.itemsAdded = itemsAdded;
    }
    
    public Integer getItemsChanged() {
        return itemsChanged;
    }
    
    public void setItemsChanged(Integer itemsChanged) {
        this.itemsChanged = itemsChanged;
    }
    
    public Integer getItemsDeleted() {
        return itemsDeleted;
    }
    
    public void setItemsDeleted(Integer itemsDeleted) {
        this.itemsDeleted = itemsDeleted;
    }
    
    public Integer getItemsFetched() {
        return itemsFetched;
    }
    
    public void setItemsFetched(Integer itemsFetched) {
        this.itemsFetched = itemsFetched;
    }
    
    public Long getDataSentBytes() {
        return dataSentBytes;
    }
    
    public void setDataSentBytes(Long dataSentBytes) {
        this.dataSentBytes = dataSentBytes;
    }
    
    public Long getDataReceivedBytes() {
        return dataReceivedBytes;
    }
    
    public void setDataReceivedBytes(Long dataReceivedBytes) {
        this.dataReceivedBytes = dataReceivedBytes;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public SyncStatus getStatus() {
        return status;
    }
    
    public void setStatus(SyncStatus status) {
        this.status = status;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
    
    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
    
    public String getRequestBody() {
        return requestBody;
    }
    
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
    
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
    
    public String getPolicyKey() {
        return policyKey;
    }
    
    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }
    
    public Boolean getPolicyApplied() {
        return policyApplied;
    }
    
    public void setPolicyApplied(Boolean policyApplied) {
        this.policyApplied = policyApplied;
    }
    
    public Boolean getWipeRequested() {
        return wipeRequested;
    }
    
    public void setWipeRequested(Boolean wipeRequested) {
        this.wipeRequested = wipeRequested;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 计算总的同步项目数
     */
    public int getTotalSyncItems() {
        return (itemsAdded != null ? itemsAdded : 0) +
               (itemsChanged != null ? itemsChanged : 0) +
               (itemsDeleted != null ? itemsDeleted : 0) +
               (itemsFetched != null ? itemsFetched : 0);
    }
    
    /**
     * 计算总的数据传输量
     */
    public long getTotalDataTransfer() {
        return (dataSentBytes != null ? dataSentBytes : 0) +
               (dataReceivedBytes != null ? dataReceivedBytes : 0);
    }
    
    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return status == SyncStatus.SUCCESS || status == SyncStatus.PARTIAL_SUCCESS;
    }
    
    /**
     * 设置错误信息
     */
    public void setError(String errorCode, String errorMessage, SyncStatus status) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }
    
    /**
     * 更新统计信息
     */
    public void updateStats(int added, int changed, int deleted, int fetched, long sentBytes, long receivedBytes) {
        this.itemsAdded = added;
        this.itemsChanged = changed;
        this.itemsDeleted = deleted;
        this.itemsFetched = fetched;
        this.dataSentBytes = sentBytes;
        this.dataReceivedBytes = receivedBytes;
    }
}