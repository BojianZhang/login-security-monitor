package com.security.monitor.controller;

import com.security.monitor.model.DnsBlacklist;
import com.security.monitor.model.DnsBlacklistCheckLog;
import com.security.monitor.service.DnsBlacklistService;
import com.security.monitor.service.DnsBlacklistService.BlacklistCheckResult;
import com.security.monitor.service.DnsBlacklistService.BlacklistStatistics;
import com.security.monitor.repository.DnsBlacklistRepository;
import com.security.monitor.repository.DnsBlacklistCheckLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DNS黑名单管理控制器
 */
@RestController
@RequestMapping("/api/dnsbl")
@PreAuthorize("hasRole('ADMIN') or hasRole('EMAIL_ADMIN')")
public class DnsBlacklistController {
    
    @Autowired
    private DnsBlacklistService dnsBlacklistService;
    
    @Autowired
    private DnsBlacklistRepository blacklistRepository;
    
    @Autowired
    private DnsBlacklistCheckLogRepository checkLogRepository;
    
    /**
     * 检查单个IP地址
     */
    @PostMapping("/check/{ipAddress}")
    public ResponseEntity<BlacklistCheckResult> checkIPAddress(@PathVariable String ipAddress) {
        BlacklistCheckResult result = dnsBlacklistService.checkIPAddress(ipAddress, null);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量检查IP地址
     */
    @PostMapping("/check/batch")
    public ResponseEntity<CompletableFuture<List<BlacklistCheckResult>>> batchCheckIPs(
            @RequestBody List<String> ipAddresses) {
        
        CompletableFuture<List<BlacklistCheckResult>> result = 
            dnsBlacklistService.batchCheckIPs(ipAddresses);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklists")
    public ResponseEntity<Page<DnsBlacklist>> getBlacklists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isActive) {
        
        Page<DnsBlacklist> blacklists;
        if (isActive != null) {
            blacklists = blacklistRepository.findByIsActive(isActive, PageRequest.of(page, size));
        } else {
            blacklists = blacklistRepository.findAll(PageRequest.of(page, size));
        }
        
        return ResponseEntity.ok(blacklists);
    }
    
    /**
     * 添加黑名单
     */
    @PostMapping("/blacklists")
    public ResponseEntity<DnsBlacklist> addBlacklist(@RequestBody DnsBlacklist blacklist) {
        DnsBlacklist saved = blacklistRepository.save(blacklist);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 更新黑名单
     */
    @PutMapping("/blacklists/{id}")
    public ResponseEntity<DnsBlacklist> updateBlacklist(
            @PathVariable Long id, @RequestBody DnsBlacklist blacklist) {
        
        blacklist.setId(id);
        DnsBlacklist updated = blacklistRepository.save(blacklist);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 删除黑名单
     */
    @DeleteMapping("/blacklists/{id}")
    public ResponseEntity<String> deleteBlacklist(@PathVariable Long id) {
        blacklistRepository.deleteById(id);
        return ResponseEntity.ok("黑名单已删除");
    }
    
    /**
     * 启用/禁用黑名单
     */
    @PatchMapping("/blacklists/{id}/toggle")
    public ResponseEntity<String> toggleBlacklist(
            @PathVariable Long id, @RequestBody ToggleRequest request) {
        
        DnsBlacklist blacklist = blacklistRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("黑名单不存在"));
        
        blacklist.setIsActive(request.isActive);
        blacklistRepository.save(blacklist);
        
        String status = request.isActive ? "启用" : "禁用";
        return ResponseEntity.ok("黑名单已" + status);
    }
    
    /**
     * 获取黑名单统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<BlacklistStatistics> getBlacklistStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        BlacklistStatistics stats = dnsBlacklistService.getBlacklistStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取检查日志
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<DnsBlacklistCheckLog>> getCheckLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Page<DnsBlacklistCheckLog> logs;
        if (startDate != null && endDate != null) {
            logs = checkLogRepository.findByCheckedAtBetween(startDate, endDate, PageRequest.of(page, size));
        } else {
            logs = checkLogRepository.findAll(PageRequest.of(page, size));
        }
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据IP地址查找检查日志
     */
    @GetMapping("/logs/ip/{ipAddress}")
    public ResponseEntity<Page<DnsBlacklistCheckLog>> getCheckLogsByIP(
            @PathVariable String ipAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<DnsBlacklistCheckLog> logs = checkLogRepository
            .findByIpAddress(ipAddress, PageRequest.of(page, size));
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 获取最常被检查的IP地址
     */
    @GetMapping("/statistics/top-ips")
    public ResponseEntity<List<Object[]>> getTopCheckedIPs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Object[]> topIPs = checkLogRepository.getMostCheckedIPs(
            startDate, endDate, PageRequest.of(0, limit));
        
        return ResponseEntity.ok(topIPs);
    }
    
    /**
     * 获取最常命中黑名单的IP地址
     */
    @GetMapping("/statistics/blacklisted-ips")
    public ResponseEntity<List<Object[]>> getBlacklistedIPs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Object[]> blacklistedIPs = checkLogRepository.getMostBlacklistedIPs(
            startDate, endDate, PageRequest.of(0, limit));
        
        return ResponseEntity.ok(blacklistedIPs);
    }
    
    /**
     * 获取每日检查统计
     */
    @GetMapping("/statistics/daily")
    public ResponseEntity<List<Object[]>> getDailyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Object[]> dailyStats = checkLogRepository.getDailyCheckStatistics(startDate, endDate);
        return ResponseEntity.ok(dailyStats);
    }
    
    /**
     * 获取黑名单命中率趋势
     */
    @GetMapping("/statistics/hit-rate-trend")
    public ResponseEntity<List<Object[]>> getHitRateTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Object[]> trend = checkLogRepository.getBlacklistHitRateTrend(startDate, endDate);
        return ResponseEntity.ok(trend);
    }
    
    /**
     * 获取检查状态分布
     */
    @GetMapping("/statistics/status-distribution")
    public ResponseEntity<List<Object[]>> getStatusDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Object[]> distribution = checkLogRepository.getCheckStatusDistribution(startDate, endDate);
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * 获取风险级别分布
     */
    @GetMapping("/statistics/risk-distribution")
    public ResponseEntity<List<Object[]>> getRiskDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Object[]> distribution = checkLogRepository.getRiskLevelDistribution(startDate, endDate);
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * 获取高风险IP列表
     */
    @GetMapping("/statistics/high-risk-ips")
    public ResponseEntity<List<String>> getHighRiskIPs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "3") int minOccurrences) {
        
        List<String> highRiskIPs = checkLogRepository.getHighRiskIPs(startDate, endDate, minOccurrences);
        return ResponseEntity.ok(highRiskIPs);
    }
    
    /**
     * 初始化默认黑名单
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeDefaultBlacklists() {
        dnsBlacklistService.initializeDefaultBlacklists();
        return ResponseEntity.ok("默认黑名单初始化完成");
    }
    
    /**
     * 测试黑名单连通性
     */
    @PostMapping("/test/{id}")
    public ResponseEntity<Object> testBlacklist(@PathVariable Long id) {
        DnsBlacklist blacklist = blacklistRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("黑名单不存在"));
        
        // 使用测试IP地址 127.0.0.2 (通常在测试环境中返回正向结果)
        BlacklistCheckResult result = dnsBlacklistService.checkIPAddress("127.0.0.2", null);
        
        var testResult = new Object() {
            public final String blacklistName = blacklist.getBlacklistName();
            public final String hostname = blacklist.getHostname();
            public final boolean responsive = !result.getQueryResults().isEmpty();
            public final String status = result.getOverallStatus().toString();
            public final LocalDateTime testTime = LocalDateTime.now();
        };
        
        return ResponseEntity.ok(testResult);
    }
    
    /**
     * 获取最常用的黑名单
     */
    @GetMapping("/blacklists/most-used")
    public ResponseEntity<List<DnsBlacklist>> getMostUsedBlacklists(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<DnsBlacklist> mostUsed = blacklistRepository
            .findMostUsedBlacklists(PageRequest.of(0, limit));
        
        return ResponseEntity.ok(mostUsed);
    }
    
    /**
     * 导出检查报告
     */
    @GetMapping("/report/export")
    public ResponseEntity<List<Object[]>> exportCheckReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<Object[]> report = checkLogRepository.getIPCheckStatistics(startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    // 内部类
    public static class ToggleRequest {
        public boolean isActive;
    }
}