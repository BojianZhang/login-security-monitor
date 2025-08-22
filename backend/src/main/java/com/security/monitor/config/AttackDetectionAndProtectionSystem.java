package com.security.monitor.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.net.InetAddress;

/**
 * 攻击检测和防护系统
 */
@Component
public class AttackDetectionAndProtectionSystem implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AttackDetectionAndProtectionSystem.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    // 攻击检测计数器
    private final ConcurrentHashMap<String, AtomicInteger> suspiciousIPs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastAttackTime = new ConcurrentHashMap<>();
    private final Set<String> blacklistedIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedIPs = ConcurrentHashMap.newKeySet();
    
    // 攻击特征检测
    private final Set<Pattern> sqlInjectionPatterns = new HashSet<>();
    private final Set<Pattern> xssPatterns = new HashSet<>();
    private final Set<Pattern> pathTraversalPatterns = new HashSet<>();
    
    @Value("${app.security.attack-threshold:10}")
    private int attackThreshold;
    
    @Value("${app.security.auto-block:true}")
    private boolean autoBlockEnabled;

    @Autowired
    private DataProtectionService dataProtectionService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSecurityPatterns() {
        // SQL注入攻击模式
        sqlInjectionPatterns.add(Pattern.compile("(?i).*(union|select|insert|update|delete|drop|exec|script|alert|onload).*"));
        sqlInjectionPatterns.add(Pattern.compile("(?i).*('|(\\-\\-)|(;)|(\\|)|(\\*)).*"));
        sqlInjectionPatterns.add(Pattern.compile("(?i).*(or\\s+1=1|and\\s+1=1|'\\s*or\\s*'1'='1).*"));
        
        // XSS攻击模式
        xssPatterns.add(Pattern.compile("(?i).*<\\s*script[^>]*>.*"));
        xssPatterns.add(Pattern.compile("(?i).*javascript\\s*:.*"));
        xssPatterns.add(Pattern.compile("(?i).*on(load|error|click|mouseover)\\s*=.*"));
        
        // 路径遍历攻击模式
        pathTraversalPatterns.add(Pattern.compile(".*(\\.\\.[\\\\/])+.*"));
        pathTraversalPatterns.add(Pattern.compile(".*[\\\\/]etc[\\\\/]passwd.*"));
        pathTraversalPatterns.add(Pattern.compile(".*[\\\\/]windows[\\\\/]system32.*"));
        
        // 初始化白名单（本地IP）
        whitelistedIPs.add("127.0.0.1");
        whitelistedIPs.add("::1");
        whitelistedIPs.add("localhost");
        
        securityLogger.info("攻击检测系统已启动 - 检测规则: {} SQL注入, {} XSS, {} 路径遍历", 
            sqlInjectionPatterns.size(), xssPatterns.size(), pathTraversalPatterns.size());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIP = getClientIpAddress(request);
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        
        // 检查IP白名单
        if (whitelistedIPs.contains(clientIP)) {
            return true;
        }
        
        // 检查IP黑名单
        if (blacklistedIPs.contains(clientIP)) {
            handleBlockedRequest(response, clientIP, "IP已被拉黑");
            return false;
        }
        
        // 攻击检测
        AttackType attackType = detectAttack(request, clientIP, requestURI, queryString, userAgent);
        
        if (attackType != AttackType.NONE) {
            return handleAttackDetected(response, clientIP, attackType, requestURI);
        }
        
        return true;
    }

    private AttackType detectAttack(HttpServletRequest request, String clientIP, String requestURI, String queryString, String userAgent) {
        String fullRequest = requestURI + (queryString != null ? "?" + queryString : "");
        
        // 1. SQL注入检测
        if (detectSQLInjection(fullRequest) || detectSQLInjection(userAgent)) {
            securityLogger.warn("🚨 SQL注入攻击检测 - IP: {}, URI: {}, UA: {}", clientIP, requestURI, userAgent);
            return AttackType.SQL_INJECTION;
        }
        
        // 2. XSS攻击检测
        if (detectXSS(fullRequest) || detectXSS(userAgent)) {
            securityLogger.warn("🚨 XSS攻击检测 - IP: {}, URI: {}, UA: {}", clientIP, requestURI, userAgent);
            return AttackType.XSS;
        }
        
        // 3. 路径遍历攻击检测
        if (detectPathTraversal(requestURI)) {
            securityLogger.warn("🚨 路径遍历攻击检测 - IP: {}, URI: {}", clientIP, requestURI);
            return AttackType.PATH_TRAVERSAL;
        }
        
        // 4. 暴力破解检测
        if (requestURI.contains("/auth/login")) {
            AtomicInteger attempts = suspiciousIPs.computeIfAbsent(clientIP, k -> new AtomicInteger(0));
            if (attempts.incrementAndGet() > 20) { // 20次登录尝试
                securityLogger.warn("🚨 暴力破解攻击检测 - IP: {}, 尝试次数: {}", clientIP, attempts.get());
                return AttackType.BRUTE_FORCE;
            }
        }
        
        // 5. 异常User-Agent检测
        if (userAgent == null || userAgent.length() < 10 || detectMaliciousUserAgent(userAgent)) {
            securityLogger.warn("🚨 恶意User-Agent检测 - IP: {}, UA: {}", clientIP, userAgent);
            return AttackType.MALICIOUS_USER_AGENT;
        }
        
        // 6. 快速请求检测（可能的DDoS）
        if (detectRapidRequests(clientIP)) {
            securityLogger.warn("🚨 快速请求攻击检测 - IP: {}", clientIP);
            return AttackType.RAPID_REQUESTS;
        }
        
        return AttackType.NONE;
    }

    private boolean detectSQLInjection(String input) {
        if (input == null) return false;
        return sqlInjectionPatterns.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }

    private boolean detectXSS(String input) {
        if (input == null) return false;
        return xssPatterns.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }

    private boolean detectPathTraversal(String input) {
        if (input == null) return false;
        return pathTraversalPatterns.stream().anyMatch(pattern -> pattern.matcher(input).matches());
    }

    private boolean detectMaliciousUserAgent(String userAgent) {
        if (userAgent == null) return true;
        
        String lowerUA = userAgent.toLowerCase();
        // 检测恶意工具的User-Agent
        return lowerUA.contains("sqlmap") || 
               lowerUA.contains("nikto") || 
               lowerUA.contains("nmap") || 
               lowerUA.contains("masscan") ||
               lowerUA.contains("burpsuite") ||
               lowerUA.contains("owasp") ||
               lowerUA.contains("scanner");
    }

    private boolean detectRapidRequests(String clientIP) {
        long currentTime = System.currentTimeMillis();
        AtomicLong lastTime = lastAttackTime.computeIfAbsent(clientIP, k -> new AtomicLong(currentTime));
        
        long timeDiff = currentTime - lastTime.getAndSet(currentTime);
        
        // 如果请求间隔小于100毫秒，可能是攻击
        return timeDiff < 100;
    }

    private boolean handleAttackDetected(HttpServletResponse response, String clientIP, AttackType attackType, String requestURI) throws IOException {
        // 增加攻击计数
        AtomicInteger attackCount = suspiciousIPs.computeIfAbsent(clientIP, k -> new AtomicInteger(0));
        int currentCount = attackCount.incrementAndGet();
        
        // 记录攻击事件
        securityLogger.error("🚨🚨🚨 攻击检测 - IP: {}, 类型: {}, URI: {}, 累计次数: {}", 
            clientIP, attackType, requestURI, currentCount);
        
        // 触发数据保护措施
        dataProtectionService.handleSecurityThreat(clientIP, attackType.toString(), requestURI);
        
        // 自动拉黑IP
        if (autoBlockEnabled && currentCount >= attackThreshold) {
            blacklistedIPs.add(clientIP);
            securityLogger.error("🔒 IP已自动拉黑 - IP: {}, 攻击次数: {}", clientIP, currentCount);
            
            // 触发紧急数据备份
            dataProtectionService.triggerEmergencyBackup("攻击检测触发");
        }
        
        // 发送攻击响应
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Access Denied\",\"code\":\"SECURITY_VIOLATION\"}");
        
        return false;
    }

    private void handleBlockedRequest(HttpServletResponse response, String clientIP, String reason) throws IOException {
        securityLogger.warn("🔒 阻止请求 - IP: {}, 原因: {}", clientIP, reason);
        
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Access Denied\",\"reason\":\"" + reason + "\"}");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    // 定时清理攻击计数器（每小时执行一次）
    @Scheduled(fixedRate = 3600000)
    public void cleanupAttackCounters() {
        int beforeSize = suspiciousIPs.size();
        long currentTime = System.currentTimeMillis();
        
        // 清理1小时前的记录
        suspiciousIPs.entrySet().removeIf(entry -> {
            long lastAttack = lastAttackTime.getOrDefault(entry.getKey(), new AtomicLong(0)).get();
            return (currentTime - lastAttack) > 3600000; // 1小时
        });
        
        int afterSize = suspiciousIPs.size();
        if (beforeSize != afterSize) {
            securityLogger.info("清理攻击计数器 - 清理前: {}, 清理后: {}", beforeSize, afterSize);
        }
    }

    // 管理员手动操作接口
    public void addToWhitelist(String ip) {
        whitelistedIPs.add(ip);
        blacklistedIPs.remove(ip);
        securityLogger.info("IP已添加到白名单: {}", ip);
    }

    public void removeFromBlacklist(String ip) {
        blacklistedIPs.remove(ip);
        suspiciousIPs.remove(ip);
        securityLogger.info("IP已从黑名单移除: {}", ip);
    }

    public Set<String> getBlacklistedIPs() {
        return new HashSet<>(blacklistedIPs);
    }

    public Set<String> getWhitelistedIPs() {
        return new HashSet<>(whitelistedIPs);
    }

    // 攻击类型枚举
    public enum AttackType {
        NONE,
        SQL_INJECTION,
        XSS,
        PATH_TRAVERSAL,
        BRUTE_FORCE,
        MALICIOUS_USER_AGENT,
        RAPID_REQUESTS
    }
}