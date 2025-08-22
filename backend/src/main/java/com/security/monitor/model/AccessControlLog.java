package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 访问控制日志实体
 */
@Entity
@Table(name = "access_control_logs")
public class AccessControlLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "service_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;
    
    @Column(name = "access_result", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccessResult accessResult;
    
    @Column(name = "user_id")
    private Long userId; // 如果有用户关联
    
    @Column(name = "username", length = 100)
    private String username;
    
    @Column(name = "whitelist_id")
    private Long whitelistId; // 匹配的白名单ID
    
    @Column(name = "access_details", length = 1000)
    private String accessDetails;
    
    @Column(name = "request_path", length = 500)
    private String requestPath; // 请求路径
    
    @Column(name = "request_method", length = 10)
    private String requestMethod; // HTTP方法
    
    @Column(name = "response_code")
    private Integer responseCode; // 响应码
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs; // 处理时间
    
    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;
    
    // 服务类型枚举
    public enum ServiceType {
        LOGIN("登录"),
        SMTP("SMTP"),
        IMAP("IMAP"),
        POP3("POP3"),
        WEBMAIL("网页邮箱"),
        API("API"),
        ADMIN("管理后台"),
        UNKNOWN("未知");
        
        private final String description;
        
        ServiceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 访问结果枚举
    public enum AccessResult {
        ALLOWED("允许"),
        DENIED("拒绝"),
        BLOCKED("阻止"),
        RATE_LIMITED("限流"),
        SUSPENDED("暂停"),
        ERROR("错误");
        
        private final String description;
        
        AccessResult(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public AccessControlLog() {
        this.accessedAt = LocalDateTime.now();
    }
    
    public AccessControlLog(String ipAddress, ServiceType serviceType, AccessResult accessResult) {
        this();
        this.ipAddress = ipAddress;
        this.serviceType = serviceType;
        this.accessResult = accessResult;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public ServiceType getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
    
    public AccessResult getAccessResult() {
        return accessResult;
    }
    
    public void setAccessResult(AccessResult accessResult) {
        this.accessResult = accessResult;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getWhitelistId() {
        return whitelistId;
    }
    
    public void setWhitelistId(Long whitelistId) {
        this.whitelistId = whitelistId;
    }
    
    public String getAccessDetails() {
        return accessDetails;
    }
    
    public void setAccessDetails(String accessDetails) {
        this.accessDetails = accessDetails;
    }
    
    public String getRequestPath() {
        return requestPath;
    }
    
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
    
    public String getRequestMethod() {
        return requestMethod;
    }
    
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
    
    public Integer getResponseCode() {
        return responseCode;
    }
    
    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public LocalDateTime getAccessedAt() {
        return accessedAt;
    }
    
    public void setAccessedAt(LocalDateTime accessedAt) {
        this.accessedAt = accessedAt;
    }
}