package com.security.monitor.controller;

import com.security.monitor.config.AttackDetectionAndProtectionSystem;
import com.security.monitor.service.DataProtectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 数据保护和安全管理控制器
 */
@RestController
@RequestMapping("/admin/security")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SecurityManagementController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityManagementController.class);

    @Autowired
    private DataProtectionService dataProtectionService;

    @Autowired
    private AttackDetectionAndProtectionSystem attackDetectionSystem;

    /**
     * 获取系统安全状态
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecurityStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 数据保护状态
        status.putAll(dataProtectionService.getSystemStatus());
        
        // IP黑白名单状态
        status.put("blacklistedIPs", attackDetectionSystem.getBlacklistedIPs());
        status.put("whitelistedIPs", attackDetectionSystem.getWhitelistedIPs());
        
        return ResponseEntity.ok(status);
    }

    /**
     * 手动触发数据备份
     */
    @PostMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerManualBackup(@RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "手动触发");
        
        logger.info("管理员触发手动备份 - 原因: {}", reason);
        
        CompletableFuture<Boolean> backupFuture = dataProtectionService.manualBackup(reason);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "备份任务已启动");
        response.put("reason", reason);
        response.put("status", "processing");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 触发紧急备份
     */
    @PostMapping("/emergency-backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerEmergencyBackup(@RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "管理员手动触发紧急备份");
        
        logger.warn("管理员触发紧急备份 - 原因: {}", reason);
        
        CompletableFuture<Boolean> backupFuture = dataProtectionService.triggerEmergencyBackup(reason);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "紧急备份任务已启动");
        response.put("reason", reason);
        response.put("status", "processing");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 激活紧急模式
     */
    @PostMapping("/emergency-mode/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activateEmergencyMode(@RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "管理员手动激活");
        
        logger.warn("管理员激活紧急模式 - 原因: {}", reason);
        
        dataProtectionService.activateEmergencyMode(reason);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "紧急模式已激活");
        response.put("reason", reason);
        response.put("status", "activated");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 退出紧急模式
     */
    @PostMapping("/emergency-mode/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deactivateEmergencyMode() {
        logger.info("管理员退出紧急模式");
        
        dataProtectionService.deactivateEmergencyMode();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "已退出紧急模式");
        response.put("status", "deactivated");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取IP黑名单
     */
    @GetMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<String>> getBlacklist() {
        return ResponseEntity.ok(attackDetectionSystem.getBlacklistedIPs());
    }

    /**
     * 获取IP白名单
     */
    @GetMapping("/whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<String>> getWhitelist() {
        return ResponseEntity.ok(attackDetectionSystem.getWhitelistedIPs());
    }

    /**
     * 添加IP到白名单
     */
    @PostMapping("/whitelist/{ip}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> addToWhitelist(@PathVariable String ip) {
        attackDetectionSystem.addToWhitelist(ip);
        
        logger.info("管理员将IP {} 添加到白名单", ip);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "IP已添加到白名单");
        response.put("ip", ip);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 从黑名单移除IP
     */
    @DeleteMapping("/blacklist/{ip}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeFromBlacklist(@PathVariable String ip) {
        attackDetectionSystem.removeFromBlacklist(ip);
        
        logger.info("管理员将IP {} 从黑名单移除", ip);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "IP已从黑名单移除");
        response.put("ip", ip);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查备份系统状态
            Map<String, Object> protectionStatus = dataProtectionService.getSystemStatus();
            health.put("dataProtection", protectionStatus);
            
            // 检查攻击检测系统状态
            health.put("attackDetection", Map.of(
                "blacklistedCount", attackDetectionSystem.getBlacklistedIPs().size(),
                "whitelistedCount", attackDetectionSystem.getWhitelistedIPs().size()
            ));
            
            health.put("status", "healthy");
            health.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("系统健康检查失败", e);
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}