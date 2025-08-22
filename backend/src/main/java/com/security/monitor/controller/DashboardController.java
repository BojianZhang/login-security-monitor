package com.security.monitor.controller;

import com.security.monitor.model.LoginRecord;
import com.security.monitor.model.SecurityAlert;
import com.security.monitor.repository.LoginRecordRepository;
import com.security.monitor.repository.SecurityAlertRepository;
import com.security.monitor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表板控制器
 */
@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginRecordRepository loginRecordRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    /**
     * 获取仪表板概览数据
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardOverview> getOverview() {
        DashboardOverview overview = new DashboardOverview();
        
        // 基础统计
        overview.totalUsers = userRepository.count();
        overview.activeUsers = userRepository.countActiveUsers();
        
        // 24小时内的登录统计
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        overview.loginsLast24h = loginRecordRepository.countLoginsByUserSince(null, last24Hours);
        
        // 安全警报统计
        overview.totalAlerts = securityAlertRepository.count();
        overview.openAlerts = securityAlertRepository.countByStatus(SecurityAlert.Status.OPEN);
        overview.criticalAlerts = securityAlertRepository.countBySeverity(SecurityAlert.Severity.CRITICAL);
        
        // 可疑登录统计
        overview.suspiciousLogins = loginRecordRepository.findByIsSuspiciousTrue().size();

        return ResponseEntity.ok(overview);
    }

    /**
     * 获取最近的登录记录
     */
    @GetMapping("/recent-logins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<LoginRecord>> getRecentLogins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<LoginRecord> loginRecords = loginRecordRepository.findAll(pageable);
        
        return ResponseEntity.ok(loginRecords);
    }

    /**
     * 获取安全警报列表
     */
    @GetMapping("/security-alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SecurityAlert>> getSecurityAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SecurityAlert> alerts;
        
        if (status != null) {
            SecurityAlert.Status alertStatus = SecurityAlert.Status.valueOf(status.toUpperCase());
            alerts = securityAlertRepository.findByStatusOrderByCreatedAtDesc(alertStatus, pageable);
        } else {
            alerts = securityAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * 获取登录趋势数据
     */
    @GetMapping("/login-trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoginTrend>> getLoginTrends(
            @RequestParam(defaultValue = "7") int days) {
        
        // 这里应该实现具体的趋势分析逻辑
        // 暂时返回空列表
        return ResponseEntity.ok(List.of());
    }

    /**
     * 获取地理位置分布数据
     */
    @GetMapping("/geo-distribution")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GeoDistribution>> getGeoDistribution() {
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        
        // 这里应该实现地理位置统计逻辑
        // 暂时返回空列表
        return ResponseEntity.ok(List.of());
    }

    /**
     * 获取风险评分分布
     */
    @GetMapping("/risk-distribution")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> getRiskDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        
        // 统计不同风险等级的登录数量
        List<LoginRecord> highRisk = loginRecordRepository.findHighRiskLogins(70);
        List<LoginRecord> mediumRisk = loginRecordRepository.findHighRiskLogins(40);
        List<LoginRecord> lowRisk = loginRecordRepository.findHighRiskLogins(0);
        
        distribution.put("high", highRisk.size());
        distribution.put("medium", mediumRisk.size() - highRisk.size());
        distribution.put("low", lowRisk.size() - mediumRisk.size());
        
        return ResponseEntity.ok(distribution);
    }

    /**
     * 仪表板概览数据类
     */
    public static class DashboardOverview {
        public long totalUsers;
        public long activeUsers;
        public long loginsLast24h;
        public long totalAlerts;
        public long openAlerts;
        public long criticalAlerts;
        public long suspiciousLogins;
    }

    /**
     * 登录趋势数据类
     */
    public static class LoginTrend {
        public String date;
        public long successfulLogins;
        public long failedLogins;
        public long suspiciousLogins;

        public LoginTrend(String date, long successfulLogins, long failedLogins, long suspiciousLogins) {
            this.date = date;
            this.successfulLogins = successfulLogins;
            this.failedLogins = failedLogins;
            this.suspiciousLogins = suspiciousLogins;
        }
    }

    /**
     * 地理分布数据类
     */
    public static class GeoDistribution {
        public String country;
        public String region;
        public String city;
        public long loginCount;
        public double latitude;
        public double longitude;

        public GeoDistribution(String country, String region, String city, long loginCount, 
                             double latitude, double longitude) {
            this.country = country;
            this.region = region;
            this.city = city;
            this.loginCount = loginCount;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}