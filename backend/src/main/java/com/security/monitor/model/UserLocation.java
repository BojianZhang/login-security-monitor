package com.security.monitor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户常用位置实体类
 */
@Entity
@Table(name = "user_locations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "country", "region", "city"}))
public class UserLocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
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
    
    @Column(name = "login_count")
    private Integer loginCount = 1;
    
    @Column(name = "first_seen")
    private LocalDateTime firstSeen;
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    @Column(name = "is_trusted")
    private Boolean isTrusted = false;

    // 构造函数
    public UserLocation() {}
    
    public UserLocation(User user, String country, String region, String city, 
                       BigDecimal latitude, BigDecimal longitude) {
        this.user = user;
        this.country = country;
        this.region = region;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firstSeen = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    // JPA生命周期回调
    @PrePersist
    protected void onCreate() {
        if (this.firstSeen == null) {
            this.firstSeen = LocalDateTime.now();
        }
        if (this.lastSeen == null) {
            this.lastSeen = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastSeen = LocalDateTime.now();
    }

    // 业务方法
    public void incrementLoginCount() {
        this.loginCount++;
        this.lastSeen = LocalDateTime.now();
    }

    public void markAsTrusted() {
        this.isTrusted = true;
    }

    public void markAsUntrusted() {
        this.isTrusted = false;
    }

    /**
     * 计算与另一个位置的距离（公里）
     */
    public double calculateDistance(BigDecimal otherLat, BigDecimal otherLon) {
        if (this.latitude == null || this.longitude == null || 
            otherLat == null || otherLon == null) {
            return Double.MAX_VALUE;
        }

        double lat1 = this.latitude.doubleValue();
        double lon1 = this.longitude.doubleValue();
        double lat2 = otherLat.doubleValue();
        double lon2 = otherLon.doubleValue();

        // 使用Haversine公式计算地球表面两点间的距离
        final int R = 6371; // 地球半径（公里）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public String getLocationString() {
        return String.format("%s, %s, %s", city, region, country);
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

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public LocalDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Boolean getIsTrusted() {
        return isTrusted;
    }

    public void setIsTrusted(Boolean isTrusted) {
        this.isTrusted = isTrusted;
    }
}