package com.security.monitor.service;

import com.security.monitor.model.IpWhitelist;
import com.security.monitor.model.AccessControlLog;
import com.security.monitor.repository.IpWhitelistRepository;
import com.security.monitor.repository.AccessControlLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * IP白名单和访问控制服务
 */
@Service
@Transactional
public class IpAccessControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpAccessControlService.class);
    
    @Autowired
    private IpWhitelistRepository whitelistRepository;
    
    @Autowired
    private AccessControlLogRepository accessLogRepository;
    
    @Value("${app.access-control.enabled:true}")
    private boolean accessControlEnabled;
    
    @Value("${app.access-control.default-allow:false}")
    private boolean defaultAllow; // 默认是否允许访问
    
    @Value("${app.access-control.log-all-access:true}")
    private boolean logAllAccess;
    
    // IPv4地址模式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // IPv6地址模式
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );
    
    /**
     * 检查IP访问权限
     */
    public AccessCheckResult checkAccess(String ipAddress, IpWhitelist.WhitelistType serviceType, 
                                       String username, String userAgent) {
        
        if (!accessControlEnabled) {
            return new AccessCheckResult(AccessControlLog.AccessResult.ALLOWED, "访问控制已禁用");
        }
        
        logger.debug("检查IP访问权限: ip={}, service={}, user={}", ipAddress, serviceType, username);
        
        AccessCheckResult result = new AccessCheckResult();
        result.setIpAddress(ipAddress);
        result.setServiceType(serviceType);
        result.setUsername(username);
        result.setUserAgent(userAgent);
        result.setCheckStartTime(LocalDateTime.now());
        
        try {
            // 验证IP地址格式
            if (!isValidIpAddress(ipAddress)) {
                result.setAccessResult(AccessControlLog.AccessResult.ERROR);
                result.setDenyReason("无效的IP地址格式");
                return result;
            }
            
            // 查找匹配的白名单条目
            IpWhitelist matchedWhitelist = findMatchingWhitelist(ipAddress, serviceType);
            
            if (matchedWhitelist != null) {
                // 检查白名单是否已过期
                if (matchedWhitelist.isExpired()) {
                    result.setAccessResult(AccessControlLog.AccessResult.DENIED);
                    result.setDenyReason("白名单条目已过期");
                } else {
                    result.setAccessResult(AccessControlLog.AccessResult.ALLOWED);
                    result.setMatchedWhitelistId(matchedWhitelist.getId());
                    result.setAllowReason("匹配白名单: " + matchedWhitelist.getDescription());
                    
                    // 更新白名单访问统计
                    updateWhitelistAccess(matchedWhitelist);
                }
            } else {
                // 没有匹配的白名单，使用默认策略
                if (defaultAllow) {
                    result.setAccessResult(AccessControlLog.AccessResult.ALLOWED);
                    result.setAllowReason("默认允许策略");
                } else {
                    result.setAccessResult(AccessControlLog.AccessResult.DENIED);
                    result.setDenyReason("IP地址不在白名单中");
                }
            }
            
        } catch (Exception e) {
            logger.error("检查IP访问权限失败", e);
            result.setAccessResult(AccessControlLog.AccessResult.ERROR);
            result.setDenyReason("系统错误: " + e.getMessage());
        } finally {
            result.setCheckEndTime(LocalDateTime.now());
        }
        
        // 记录访问日志
        if (logAllAccess || result.getAccessResult() != AccessControlLog.AccessResult.ALLOWED) {
            logAccess(result);
        }
        
        return result;
    }
    
    /**
     * 批量检查IP访问权限
     */
    public List<AccessCheckResult> batchCheckAccess(List<String> ipAddresses, 
                                                   IpWhitelist.WhitelistType serviceType) {
        List<AccessCheckResult> results = new ArrayList<>();
        
        for (String ipAddress : ipAddresses) {
            AccessCheckResult result = checkAccess(ipAddress, serviceType, null, null);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * 添加IP到白名单
     */
    public IpWhitelist addToWhitelist(String ipAddress, IpWhitelist.WhitelistType whitelistType, 
                                     String description, String createdBy) {
        
        // 检查IP是否已存在
        if (whitelistRepository.existsByIpAddressAndWhitelistTypeAndIsActive(ipAddress, whitelistType, true)) {
            throw new RuntimeException("IP地址已在白名单中");
        }
        
        IpWhitelist whitelist = new IpWhitelist(ipAddress, whitelistType);
        whitelist.setDescription(description);
        whitelist.setCreatedBy(createdBy);
        whitelist.setIpType(detectIpType(ipAddress));
        
        return whitelistRepository.save(whitelist);
    }
    
    /**
     * 添加CIDR网段到白名单
     */
    public IpWhitelist addCidrToWhitelist(String cidrRange, IpWhitelist.WhitelistType whitelistType, 
                                        String description, String createdBy) {
        
        // 验证CIDR格式
        if (!isValidCidr(cidrRange)) {
            throw new RuntimeException("无效的CIDR格式");
        }
        
        // 提取网络地址
        String networkAddress = cidrRange.split("/")[0];
        
        IpWhitelist whitelist = new IpWhitelist(networkAddress, whitelistType);
        whitelist.setCidrRange(cidrRange);
        whitelist.setIpType(IpWhitelist.IpType.CIDR);
        whitelist.setDescription(description);
        whitelist.setCreatedBy(createdBy);
        
        return whitelistRepository.save(whitelist);
    }
    
    /**
     * 从白名单移除IP
     */
    public void removeFromWhitelist(Long whitelistId) {
        IpWhitelist whitelist = whitelistRepository.findById(whitelistId)
            .orElseThrow(() -> new RuntimeException("白名单条目不存在"));
        
        whitelist.setIsActive(false);
        whitelistRepository.save(whitelist);
    }
    
    /**
     * 清理过期的白名单条目
     */
    @Transactional
    public int cleanupExpiredWhitelists() {
        List<IpWhitelist> expiredWhitelists = whitelistRepository.findExpiredWhitelists(LocalDateTime.now());
        
        for (IpWhitelist whitelist : expiredWhitelists) {
            whitelist.setIsActive(false);
        }
        
        whitelistRepository.saveAll(expiredWhitelists);
        
        logger.info("清理了 {} 个过期的白名单条目", expiredWhitelists.size());
        return expiredWhitelists.size();
    }
    
    /**
     * 获取访问统计
     */
    public AccessStatistics getAccessStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        AccessStatistics stats = new AccessStatistics();
        
        // 总访问次数
        long totalAccess = accessLogRepository.countByAccessedAtBetween(startDate, endDate);
        stats.setTotalAccess(totalAccess);
        
        // 允许的访问次数
        long allowedAccess = accessLogRepository.countByAccessResultAndAccessedAtBetween(
            AccessControlLog.AccessResult.ALLOWED, startDate, endDate);
        stats.setAllowedAccess(allowedAccess);
        
        // 拒绝的访问次数
        long deniedAccess = accessLogRepository.countByAccessResultAndAccessedAtBetween(
            AccessControlLog.AccessResult.DENIED, startDate, endDate);
        stats.setDeniedAccess(deniedAccess);
        
        // 计算通过率
        if (totalAccess > 0) {
            stats.setAllowRate((double) allowedAccess / totalAccess * 100);
        }
        
        return stats;
    }
    
    // 私有方法
    
    /**
     * 查找匹配的白名单条目
     */
    private IpWhitelist findMatchingWhitelist(String ipAddress, IpWhitelist.WhitelistType serviceType) {
        List<IpWhitelist> whitelists = whitelistRepository
            .findByWhitelistTypeAndIsActiveOrderByPriority(serviceType, true);
        
        // 也检查全部服务类型的白名单
        List<IpWhitelist> allServiceWhitelists = whitelistRepository
            .findByWhitelistTypeAndIsActiveOrderByPriority(IpWhitelist.WhitelistType.ALL, true);
        
        // 合并列表
        List<IpWhitelist> allWhitelists = new ArrayList<>(whitelists);
        allWhitelists.addAll(allServiceWhitelists);
        
        // 按优先级排序
        allWhitelists.sort(Comparator.comparing(IpWhitelist::getPriority));
        
        for (IpWhitelist whitelist : allWhitelists) {
            if (isIpMatched(ipAddress, whitelist)) {
                return whitelist;
            }
        }
        
        return null;
    }
    
    /**
     * 检查IP是否匹配白名单条目
     */
    private boolean isIpMatched(String ipAddress, IpWhitelist whitelist) {
        try {
            switch (whitelist.getIpType()) {
                case IPV4:
                case IPV6:
                    return ipAddress.equals(whitelist.getIpAddress());
                
                case CIDR:
                    return isIpInCidr(ipAddress, whitelist.getCidrRange());
                
                case RANGE:
                    // TODO: 实现IP范围匹配
                    return false;
                
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.error("IP匹配检查失败: ip={}, whitelist={}", ipAddress, whitelist.getId(), e);
            return false;
        }
    }
    
    /**
     * 检查IP是否在CIDR网段内
     */
    private boolean isIpInCidr(String ipAddress, String cidrRange) {
        try {
            String[] parts = cidrRange.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            InetAddress targetAddr = InetAddress.getByName(ipAddress);
            InetAddress networkAddr = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            
            // 简化的CIDR匹配实现
            // 实际应用中可能需要更复杂的IPv6支持
            if (targetAddr instanceof java.net.Inet4Address && 
                networkAddr instanceof java.net.Inet4Address) {
                return isIPv4InCidr(ipAddress, cidrRange);
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("CIDR匹配检查失败: ip={}, cidr={}", ipAddress, cidrRange, e);
            return false;
        }
    }
    
    /**
     * IPv4 CIDR匹配
     */
    private boolean isIPv4InCidr(String ipAddress, String cidrRange) {
        try {
            String[] cidrParts = cidrRange.split("/");
            String networkAddress = cidrParts[0];
            int prefixLength = Integer.parseInt(cidrParts[1]);
            
            long ipLong = ipToLong(ipAddress);
            long networkLong = ipToLong(networkAddress);
            long mask = (-1L << (32 - prefixLength)) & 0xFFFFFFFFL;
            
            return (ipLong & mask) == (networkLong & mask);
            
        } catch (Exception e) {
            logger.error("IPv4 CIDR匹配失败", e);
            return false;
        }
    }
    
    /**
     * IP地址转换为长整型
     */
    private long ipToLong(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Long.parseLong(parts[i]) << (24 - i * 8));
        }
        return result & 0xFFFFFFFFL;
    }
    
    /**
     * 验证IP地址格式
     */
    private boolean isValidIpAddress(String ipAddress) {
        return IPV4_PATTERN.matcher(ipAddress).matches() || 
               IPV6_PATTERN.matcher(ipAddress).matches();
    }
    
    /**
     * 验证CIDR格式
     */
    private boolean isValidCidr(String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            String ip = parts[0];
            int prefix = Integer.parseInt(parts[1]);
            
            if (!isValidIpAddress(ip)) {
                return false;
            }
            
            // 检查前缀长度
            if (IPV4_PATTERN.matcher(ip).matches()) {
                return prefix >= 0 && prefix <= 32;
            } else if (IPV6_PATTERN.matcher(ip).matches()) {
                return prefix >= 0 && prefix <= 128;
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测IP类型
     */
    private IpWhitelist.IpType detectIpType(String ipAddress) {
        if (IPV4_PATTERN.matcher(ipAddress).matches()) {
            return IpWhitelist.IpType.IPV4;
        } else if (IPV6_PATTERN.matcher(ipAddress).matches()) {
            return IpWhitelist.IpType.IPV6;
        } else if (ipAddress.contains("/")) {
            return IpWhitelist.IpType.CIDR;
        } else {
            return IpWhitelist.IpType.RANGE;
        }
    }
    
    /**
     * 更新白名单访问统计
     */
    private void updateWhitelistAccess(IpWhitelist whitelist) {
        try {
            whitelist.incrementAccessCount();
            whitelistRepository.save(whitelist);
        } catch (Exception e) {
            logger.error("更新白名单访问统计失败", e);
        }
    }
    
    /**
     * 记录访问日志
     */
    private void logAccess(AccessCheckResult result) {
        try {
            AccessControlLog log = new AccessControlLog();
            log.setIpAddress(result.getIpAddress());
            log.setServiceType(convertToServiceType(result.getServiceType()));
            log.setAccessResult(result.getAccessResult());
            log.setUsername(result.getUsername());
            log.setUserAgent(result.getUserAgent());
            log.setWhitelistId(result.getMatchedWhitelistId());
            
            String details = result.getAccessResult() == AccessControlLog.AccessResult.ALLOWED ? 
                result.getAllowReason() : result.getDenyReason();
            log.setAccessDetails(details);
            
            if (result.getCheckStartTime() != null && result.getCheckEndTime() != null) {
                long processingTime = java.time.Duration
                    .between(result.getCheckStartTime(), result.getCheckEndTime()).toMillis();
                log.setProcessingTimeMs(processingTime);
            }
            
            accessLogRepository.save(log);
            
        } catch (Exception e) {
            logger.error("记录访问日志失败", e);
        }
    }
    
    /**
     * 转换服务类型
     */
    private AccessControlLog.ServiceType convertToServiceType(IpWhitelist.WhitelistType whitelistType) {
        switch (whitelistType) {
            case LOGIN: return AccessControlLog.ServiceType.LOGIN;
            case SMTP: return AccessControlLog.ServiceType.SMTP;
            case IMAP: return AccessControlLog.ServiceType.IMAP;
            case POP3: return AccessControlLog.ServiceType.POP3;
            case WEBMAIL: return AccessControlLog.ServiceType.WEBMAIL;
            case API: return AccessControlLog.ServiceType.API;
            case ADMIN: return AccessControlLog.ServiceType.ADMIN;
            default: return AccessControlLog.ServiceType.UNKNOWN;
        }
    }
    
    // 结果类
    
    public static class AccessCheckResult {
        private String ipAddress;
        private IpWhitelist.WhitelistType serviceType;
        private String username;
        private String userAgent;
        private AccessControlLog.AccessResult accessResult;
        private Long matchedWhitelistId;
        private String allowReason;
        private String denyReason;
        private LocalDateTime checkStartTime;
        private LocalDateTime checkEndTime;
        
        public AccessCheckResult() {}
        
        public AccessCheckResult(AccessControlLog.AccessResult accessResult, String reason) {
            this.accessResult = accessResult;
            if (accessResult == AccessControlLog.AccessResult.ALLOWED) {
                this.allowReason = reason;
            } else {
                this.denyReason = reason;
            }
        }
        
        // Getters and Setters
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public IpWhitelist.WhitelistType getServiceType() { return serviceType; }
        public void setServiceType(IpWhitelist.WhitelistType serviceType) { this.serviceType = serviceType; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public AccessControlLog.AccessResult getAccessResult() { return accessResult; }
        public void setAccessResult(AccessControlLog.AccessResult accessResult) { this.accessResult = accessResult; }
        
        public Long getMatchedWhitelistId() { return matchedWhitelistId; }
        public void setMatchedWhitelistId(Long matchedWhitelistId) { this.matchedWhitelistId = matchedWhitelistId; }
        
        public String getAllowReason() { return allowReason; }
        public void setAllowReason(String allowReason) { this.allowReason = allowReason; }
        
        public String getDenyReason() { return denyReason; }
        public void setDenyReason(String denyReason) { this.denyReason = denyReason; }
        
        public LocalDateTime getCheckStartTime() { return checkStartTime; }
        public void setCheckStartTime(LocalDateTime checkStartTime) { this.checkStartTime = checkStartTime; }
        
        public LocalDateTime getCheckEndTime() { return checkEndTime; }
        public void setCheckEndTime(LocalDateTime checkEndTime) { this.checkEndTime = checkEndTime; }
        
        public boolean isAllowed() {
            return accessResult == AccessControlLog.AccessResult.ALLOWED;
        }
    }
    
    public static class AccessStatistics {
        private long totalAccess;
        private long allowedAccess;
        private long deniedAccess;
        private double allowRate;
        
        // Getters and Setters
        public long getTotalAccess() { return totalAccess; }
        public void setTotalAccess(long totalAccess) { this.totalAccess = totalAccess; }
        
        public long getAllowedAccess() { return allowedAccess; }
        public void setAllowedAccess(long allowedAccess) { this.allowedAccess = allowedAccess; }
        
        public long getDeniedAccess() { return deniedAccess; }
        public void setDeniedAccess(long deniedAccess) { this.deniedAccess = deniedAccess; }
        
        public double getAllowRate() { return allowRate; }
        public void setAllowRate(double allowRate) { this.allowRate = allowRate; }
    }
}