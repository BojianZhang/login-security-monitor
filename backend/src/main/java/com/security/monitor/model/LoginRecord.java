package com.security.monitor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登录记录实体类
 */
@Entity
@Table(name = "login_records")
public class LoginRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 100)
    private String region;
    
    @Column(length = 100)
    private String city;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(length = 50)
    private String timezone;
    
    @Column(length = 100)
    private String isp;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    @Column(length = 50)
    private String browser;
    
    @Column(length = 50)
    private String os;
    
    @Column(name = "login_time")
    private LocalDateTime loginTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "login_status")
    private LoginStatus loginStatus = LoginStatus.SUCCESS;
    
    @Column(name = "risk_score")
    private Integer riskScore = 0;
    
    @Column(name = "is_suspicious")
    private Boolean isSuspicious = false;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;

    // 枚举类型
    public enum LoginStatus {
        SUCCESS, FAILED, BLOCKED
    }

    // 构造函数
    public LoginRecord() {}
    
    public LoginRecord(User user, String ipAddress, String userAgent) {
        this.user = user;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginTime = LocalDateTime.now();
    }

    // JPA生命周期回调
    @PrePersist
    protected void onCreate() {
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LoginStatus getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(LoginStatus loginStatus) {
        this.loginStatus = loginStatus;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Boolean getIsSuspicious() {
        return isSuspicious;
    }

    public void setIsSuspicious(Boolean isSuspicious) {
        this.isSuspicious = isSuspicious;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}