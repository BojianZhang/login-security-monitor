package com.security.monitor.service;

import com.security.monitor.model.DnsBlacklist;
import com.security.monitor.model.DnsBlacklistCheckLog;
import com.security.monitor.model.EmailMessage;
import com.security.monitor.repository.DnsBlacklistRepository;
import com.security.monitor.repository.DnsBlacklistCheckLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * DNS黑名单检查服务
 * 支持多种DNSBL提供商，如Spamhaus, SURBL, Barracuda等
 */
@Service
@Transactional
public class DnsBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(DnsBlacklistService.class);
    
    @Autowired
    private DnsBlacklistRepository blacklistRepository;
    
    @Autowired
    private DnsBlacklistCheckLogRepository checkLogRepository;
    
    @Value("${app.dnsbl.enabled:true}")
    private boolean dnsblEnabled;
    
    @Value("${app.dnsbl.timeout:5000}")
    private int queryTimeoutMs;
    
    @Value("${app.dnsbl.max-concurrent:10}")
    private int maxConcurrentQueries;
    
    // IPv4地址模式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // 默认的DNSBL提供商列表
    private static final List<String> DEFAULT_BLACKLISTS = Arrays.asList(
        "zen.spamhaus.org",
        "bl.spamcop.net", 
        "b.barracudacentral.org",
        "dnsbl.sorbs.net",
        "psbl.surriel.com",
        "ubl.unsubscore.com",
        "dnsbl-1.uceprotect.net",
        "dnsbl-2.uceprotect.net",
        "dnsbl-3.uceprotect.net",
        "sbl.spamhaus.org",
        "css.spamhaus.org",
        "xbl.spamhaus.org",
        "dul.dnsbl.sorbs.net",
        "http.dnsbl.sorbs.net",
        "misc.dnsbl.sorbs.net",
        "smtp.dnsbl.sorbs.net",
        "spam.dnsbl.sorbs.net",
        "web.dnsbl.sorbs.net",
        "zombie.dnsbl.sorbs.net"
    );
    
    /**
     * 检查IP地址是否在黑名单中
     */
    public BlacklistCheckResult checkIPAddress(String ipAddress, EmailMessage message) {
        if (!dnsblEnabled) {
            return new BlacklistCheckResult(CheckStatus.DISABLED, "DNSBL检查已禁用");
        }
        
        if (!isValidIPv4(ipAddress)) {
            return new BlacklistCheckResult(CheckStatus.INVALID, "无效的IPv4地址");
        }
        
        logger.info("开始DNSBL检查: IP={}, messageId={}", ipAddress, 
            message != null ? message.getMessageId() : "N/A");
        
        BlacklistCheckResult result = new BlacklistCheckResult();
        result.setIpAddress(ipAddress);
        result.setCheckStartTime(LocalDateTime.now());
        
        List<DnsBlacklist> activeBlacklists = blacklistRepository.findByIsActiveOrderByWeightDesc(true);
        List<BlacklistHit> hits = new ArrayList<>();
        
        for (DnsBlacklist blacklist : activeBlacklists) {
            try {
                BlacklistQueryResult queryResult = queryBlacklist(ipAddress, blacklist);
                
                // 更新黑名单查询统计
                updateBlacklistStatistics(blacklist, queryResult.isListed());
                
                if (queryResult.isListed()) {
                    BlacklistHit hit = new BlacklistHit();
                    hit.setBlacklistName(blacklist.getBlacklistName());
                    hit.setHostname(blacklist.getHostname());
                    hit.setReturnCode(queryResult.getReturnCode());
                    hit.setDescription(queryResult.getDescription());
                    hit.setWeight(blacklist.getWeight());
                    hits.add(hit);
                }
                
                result.getQueryResults().add(queryResult);
                
            } catch (Exception e) {
                logger.error("查询黑名单失败: {}", blacklist.getHostname(), e);
                
                BlacklistQueryResult errorResult = new BlacklistQueryResult();
                errorResult.setBlacklistName(blacklist.getBlacklistName());
                errorResult.setHostname(blacklist.getHostname());
                errorResult.setListed(false);
                errorResult.setError(true);
                errorResult.setErrorMessage(e.getMessage());
                result.getQueryResults().add(errorResult);
            }
        }
        
        result.setHits(hits);
        result.setHitCount(hits.size());
        result.setTotalWeight(hits.stream().mapToDouble(BlacklistHit::getWeight).sum());
        result.setCheckEndTime(LocalDateTime.now());
        
        // 计算风险等级
        result.setRiskLevel(calculateRiskLevel(hits));
        result.setOverallStatus(hits.isEmpty() ? CheckStatus.CLEAN : CheckStatus.LISTED);
        
        // 记录检查日志
        if (message != null) {
            logBlacklistCheck(message, result);
        }
        
        logger.info("DNSBL检查完成: IP={}, 命中数={}, 风险等级={}", 
            ipAddress, hits.size(), result.getRiskLevel());
        
        return result;
    }
    
    /**
     * 异步批量检查多个IP地址
     */
    @Async
    public CompletableFuture<List<BlacklistCheckResult>> batchCheckIPs(List<String> ipAddresses) {
        logger.info("开始批量DNSBL检查: {} 个IP地址", ipAddresses.size());
        
        List<BlacklistCheckResult> results = new ArrayList<>();
        
        for (String ip : ipAddresses) {
            try {
                BlacklistCheckResult result = checkIPAddress(ip, null);
                results.add(result);
                
                // 防止过快查询
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.error("批量检查IP失败: {}", ip, e);
                
                BlacklistCheckResult errorResult = new BlacklistCheckResult(CheckStatus.ERROR, e.getMessage());
                errorResult.setIpAddress(ip);
                results.add(errorResult);
            }
        }
        
        logger.info("批量DNSBL检查完成: 总计={}, 完成={}", ipAddresses.size(), results.size());
        
        return CompletableFuture.completedFuture(results);
    }
    
    /**
     * 查询单个黑名单
     */
    private BlacklistQueryResult queryBlacklist(String ipAddress, DnsBlacklist blacklist) 
            throws NamingException, UnknownHostException {
        
        String reversedIP = reverseIPAddress(ipAddress);
        String queryHost = reversedIP + "." + blacklist.getHostname();
        
        BlacklistQueryResult result = new BlacklistQueryResult();
        result.setBlacklistName(blacklist.getBlacklistName());
        result.setHostname(blacklist.getHostname());
        result.setQueryHost(queryHost);
        result.setQueryStartTime(LocalDateTime.now());
        
        try {
            // 设置DNS查询超时
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(blacklist.getQueryTimeoutMs()));
            env.put("com.sun.jndi.dns.timeout.retries", "2");
            
            DirContext ctx = new InitialDirContext(env);
            
            // 查询A记录
            Attributes attrs = ctx.getAttributes(queryHost, new String[]{"A"});
            Attribute aAttribute = attrs.get("A");
            
            if (aAttribute != null && aAttribute.size() > 0) {
                String returnCode = aAttribute.get(0).toString();
                result.setListed(true);
                result.setReturnCode(returnCode);
                result.setDescription(interpretReturnCode(blacklist.getBlacklistName(), returnCode));
            } else {
                result.setListed(false);
            }
            
            ctx.close();
            
        } catch (NamingException e) {
            // NXDOMAIN表示未列入黑名单
            if (e.getMessage().contains("NXDOMAIN") || e.getMessage().contains("Name or service not known")) {
                result.setListed(false);
            } else {
                result.setError(true);
                result.setErrorMessage(e.getMessage());
                throw e;
            }
        }
        
        result.setQueryEndTime(LocalDateTime.now());
        result.setQueryTimeMs(calculateQueryTime(result.getQueryStartTime(), result.getQueryEndTime()));
        
        return result;
    }
    
    /**
     * 反转IP地址用于DNS查询
     */
    private String reverseIPAddress(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        return parts[3] + "." + parts[2] + "." + parts[1] + "." + parts[0];
    }
    
    /**
     * 解释返回代码
     */
    private String interpretReturnCode(String blacklistName, String returnCode) {
        // 根据不同的黑名单提供商解释返回代码
        switch (blacklistName.toLowerCase()) {
            case "zen.spamhaus.org":
            case "sbl.spamhaus.org":
                return interpretSpamhausCode(returnCode);
            case "bl.spamcop.net":
                return "SpamCop列入黑名单";
            case "b.barracudacentral.org":
                return "Barracuda声誉不良";
            case "dnsbl.sorbs.net":
                return interpretSorbsCode(returnCode);
            default:
                return "列入" + blacklistName + "黑名单 (返回码: " + returnCode + ")";
        }
    }
    
    /**
     * 解释Spamhaus返回代码
     */
    private String interpretSpamhausCode(String code) {
        switch (code) {
            case "127.0.0.2": return "SBL - 垃圾邮件源";
            case "127.0.0.3": return "SBL - 垃圾邮件源";
            case "127.0.0.4": return "PBL - 策略阻止列表";
            case "127.0.0.5": return "PBL - 策略阻止列表";
            case "127.0.0.6": return "PBL - 策略阻止列表";
            case "127.0.0.7": return "PBL - 策略阻止列表";
            case "127.0.0.9": return "XBL - 被劫持网络";
            case "127.0.0.10": return "XBL - 被劫持网络";
            case "127.0.0.11": return "CSS - 被劫持网络";
            default: return "Spamhaus列表 (代码: " + code + ")";
        }
    }
    
    /**
     * 解释SORBS返回代码
     */
    private String interpretSorbsCode(String code) {
        switch (code) {
            case "127.0.0.2": return "开放HTTP代理";
            case "127.0.0.3": return "开放SOCKS代理";
            case "127.0.0.4": return "开放SMTP中继";
            case "127.0.0.5": return "开放SMTP中继";
            case "127.0.0.6": return "垃圾邮件源";
            case "127.0.0.7": return "开放SMTP中继";
            case "127.0.0.8": return "已确认垃圾邮件源";
            case "127.0.0.9": return "动态IP";
            case "127.0.0.10": return "POP3/IMAP中继";
            case "127.0.0.11": return "开放SMTP中继";
            case "127.0.0.12": return "漏洞利用网络";
            case "127.0.0.14": return "开放代理";
            default: return "SORBS列表 (代码: " + code + ")";
        }
    }
    
    /**
     * 计算风险等级
     */
    private RiskLevel calculateRiskLevel(List<BlacklistHit> hits) {
        if (hits.isEmpty()) {
            return RiskLevel.CLEAN;
        }
        
        double totalWeight = hits.stream().mapToDouble(BlacklistHit::getWeight).sum();
        int hitCount = hits.size();
        
        // 检查是否有高威胁黑名单
        boolean hasHighThreatList = hits.stream().anyMatch(hit -> 
            hit.getBlacklistName().contains("spamhaus") || 
            hit.getBlacklistName().contains("spamcop") ||
            hit.getWeight() >= 5.0
        );
        
        if (hasHighThreatList || totalWeight >= 10.0 || hitCount >= 5) {
            return RiskLevel.HIGH;
        } else if (totalWeight >= 5.0 || hitCount >= 3) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
    
    /**
     * 更新黑名单统计
     */
    private void updateBlacklistStatistics(DnsBlacklist blacklist, boolean wasHit) {
        try {
            blacklistRepository.updateQueryStatistics(blacklist.getId(), LocalDateTime.now());
            
            if (wasHit) {
                blacklistRepository.updateHitStatistics(blacklist.getId());
            }
        } catch (Exception e) {
            logger.error("更新黑名单统计失败", e);
        }
    }
    
    /**
     * 记录黑名单检查日志
     */
    private void logBlacklistCheck(EmailMessage message, BlacklistCheckResult result) {
        try {
            DnsBlacklistCheckLog log = new DnsBlacklistCheckLog();
            log.setMessage(message);
            log.setIpAddress(result.getIpAddress());
            log.setCheckStatus(result.getOverallStatus().toString());
            log.setHitCount(result.getHitCount());
            log.setTotalWeight(result.getTotalWeight());
            log.setRiskLevel(result.getRiskLevel().toString());
            log.setCheckDetails(formatCheckDetails(result));
            log.setProcessingTimeMs(calculateQueryTime(result.getCheckStartTime(), result.getCheckEndTime()));
            log.setCheckedAt(LocalDateTime.now());
            
            checkLogRepository.save(log);
            
        } catch (Exception e) {
            logger.error("保存黑名单检查日志失败", e);
        }
    }
    
    /**
     * 格式化检查详情
     */
    private String formatCheckDetails(BlacklistCheckResult result) {
        if (result.getHits().isEmpty()) {
            return "未发现黑名单记录";
        }
        
        StringBuilder details = new StringBuilder();
        details.append("检测到黑名单记录: ");
        
        for (BlacklistHit hit : result.getHits()) {
            details.append(hit.getBlacklistName())
                   .append("(").append(hit.getDescription()).append("), ");
        }
        
        // 移除最后的逗号和空格
        if (details.length() > 2) {
            details.setLength(details.length() - 2);
        }
        
        return details.toString();
    }
    
    /**
     * 计算查询时间
     */
    private long calculateQueryTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        
        return java.time.Duration.between(start, end).toMillis();
    }
    
    /**
     * 验证IPv4地址
     */
    private boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches();
    }
    
    /**
     * 初始化默认黑名单
     */
    @Transactional
    public void initializeDefaultBlacklists() {
        logger.info("初始化默认DNSBL黑名单");
        
        for (String hostname : DEFAULT_BLACKLISTS) {
            DnsBlacklist existing = blacklistRepository.findByHostname(hostname);
            
            if (existing == null) {
                DnsBlacklist blacklist = new DnsBlacklist();
                blacklist.setBlacklistName(generateBlacklistName(hostname));
                blacklist.setHostname(hostname);
                blacklist.setDescription(generateDescription(hostname));
                blacklist.setWeight(calculateDefaultWeight(hostname));
                blacklist.setIsActive(true);
                
                blacklistRepository.save(blacklist);
                logger.info("添加默认黑名单: {}", hostname);
            }
        }
        
        logger.info("默认DNSBL黑名单初始化完成");
    }
    
    /**
     * 生成黑名单名称
     */
    private String generateBlacklistName(String hostname) {
        if (hostname.contains("spamhaus")) {
            return "Spamhaus";
        } else if (hostname.contains("spamcop")) {
            return "SpamCop";
        } else if (hostname.contains("barracuda")) {
            return "Barracuda";
        } else if (hostname.contains("sorbs")) {
            return "SORBS";
        } else if (hostname.contains("uceprotect")) {
            return "UCEPROTECT";
        } else {
            return hostname.toUpperCase();
        }
    }
    
    /**
     * 生成描述
     */
    private String generateDescription(String hostname) {
        if (hostname.contains("spamhaus")) {
            return "Spamhaus项目维护的综合性反垃圾邮件黑名单";
        } else if (hostname.contains("spamcop")) {
            return "SpamCop用户报告的垃圾邮件源列表";
        } else if (hostname.contains("barracuda")) {
            return "Barracuda Networks维护的声誉数据库";
        } else if (hostname.contains("sorbs")) {
            return "SORBS开放中继和垃圾邮件源数据库";
        } else {
            return "DNS黑名单服务: " + hostname;
        }
    }
    
    /**
     * 计算默认权重
     */
    private double calculateDefaultWeight(String hostname) {
        if (hostname.contains("spamhaus") || hostname.contains("spamcop")) {
            return 5.0; // 高权重
        } else if (hostname.contains("barracuda") || hostname.contains("sorbs")) {
            return 3.0; // 中等权重
        } else {
            return 2.0; // 低权重
        }
    }
    
    /**
     * 获取黑名单统计信息
     */
    public BlacklistStatistics getBlacklistStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Object[] stats = blacklistRepository.getBlacklistStatistics();
        
        BlacklistStatistics statistics = new BlacklistStatistics();
        if (stats != null && stats.length >= 4) {
            statistics.setTotalBlacklists(((Number) stats[0]).longValue());
            statistics.setActiveBlacklists(((Number) stats[1]).longValue());
            statistics.setTotalQueries(((Number) stats[2]).longValue());
            statistics.setTotalHits(((Number) stats[3]).longValue());
        }
        
        // 计算命中率
        if (statistics.getTotalQueries() > 0) {
            statistics.setHitRate((double) statistics.getTotalHits() / statistics.getTotalQueries() * 100);
        }
        
        return statistics;
    }
    
    // 枚举和结果类
    public enum CheckStatus {
        CLEAN, LISTED, ERROR, DISABLED, INVALID
    }
    
    public enum RiskLevel {
        CLEAN, LOW, MEDIUM, HIGH
    }
    
    // 结果类
    public static class BlacklistCheckResult {
        private String ipAddress;
        private CheckStatus overallStatus;
        private RiskLevel riskLevel;
        private int hitCount;
        private double totalWeight;
        private List<BlacklistHit> hits = new ArrayList<>();
        private List<BlacklistQueryResult> queryResults = new ArrayList<>();
        private String errorMessage;
        private LocalDateTime checkStartTime;
        private LocalDateTime checkEndTime;
        
        public BlacklistCheckResult() {}
        
        public BlacklistCheckResult(CheckStatus status, String message) {
            this.overallStatus = status;
            this.errorMessage = message;
        }
        
        // Getters and Setters
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public CheckStatus getOverallStatus() { return overallStatus; }
        public void setOverallStatus(CheckStatus overallStatus) { this.overallStatus = overallStatus; }
        
        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
        
        public int getHitCount() { return hitCount; }
        public void setHitCount(int hitCount) { this.hitCount = hitCount; }
        
        public double getTotalWeight() { return totalWeight; }
        public void setTotalWeight(double totalWeight) { this.totalWeight = totalWeight; }
        
        public List<BlacklistHit> getHits() { return hits; }
        public void setHits(List<BlacklistHit> hits) { this.hits = hits; }
        
        public List<BlacklistQueryResult> getQueryResults() { return queryResults; }
        public void setQueryResults(List<BlacklistQueryResult> queryResults) { this.queryResults = queryResults; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getCheckStartTime() { return checkStartTime; }
        public void setCheckStartTime(LocalDateTime checkStartTime) { this.checkStartTime = checkStartTime; }
        
        public LocalDateTime getCheckEndTime() { return checkEndTime; }
        public void setCheckEndTime(LocalDateTime checkEndTime) { this.checkEndTime = checkEndTime; }
    }
    
    public static class BlacklistHit {
        private String blacklistName;
        private String hostname;
        private String returnCode;
        private String description;
        private double weight;
        
        // Getters and Setters
        public String getBlacklistName() { return blacklistName; }
        public void setBlacklistName(String blacklistName) { this.blacklistName = blacklistName; }
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        
        public String getReturnCode() { return returnCode; }
        public void setReturnCode(String returnCode) { this.returnCode = returnCode; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
    }
    
    public static class BlacklistQueryResult {
        private String blacklistName;
        private String hostname;
        private String queryHost;
        private boolean listed;
        private String returnCode;
        private String description;
        private boolean error;
        private String errorMessage;
        private long queryTimeMs;
        private LocalDateTime queryStartTime;
        private LocalDateTime queryEndTime;
        
        // Getters and Setters
        public String getBlacklistName() { return blacklistName; }
        public void setBlacklistName(String blacklistName) { this.blacklistName = blacklistName; }
        
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        
        public String getQueryHost() { return queryHost; }
        public void setQueryHost(String queryHost) { this.queryHost = queryHost; }
        
        public boolean isListed() { return listed; }
        public void setListed(boolean listed) { this.listed = listed; }
        
        public String getReturnCode() { return returnCode; }
        public void setReturnCode(String returnCode) { this.returnCode = returnCode; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isError() { return error; }
        public void setError(boolean error) { this.error = error; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getQueryTimeMs() { return queryTimeMs; }
        public void setQueryTimeMs(long queryTimeMs) { this.queryTimeMs = queryTimeMs; }
        
        public LocalDateTime getQueryStartTime() { return queryStartTime; }
        public void setQueryStartTime(LocalDateTime queryStartTime) { this.queryStartTime = queryStartTime; }
        
        public LocalDateTime getQueryEndTime() { return queryEndTime; }
        public void setQueryEndTime(LocalDateTime queryEndTime) { this.queryEndTime = queryEndTime; }
    }
    
    public static class BlacklistStatistics {
        private long totalBlacklists;
        private long activeBlacklists;
        private long totalQueries;
        private long totalHits;
        private double hitRate;
        
        // Getters and Setters
        public long getTotalBlacklists() { return totalBlacklists; }
        public void setTotalBlacklists(long totalBlacklists) { this.totalBlacklists = totalBlacklists; }
        
        public long getActiveBlacklists() { return activeBlacklists; }
        public void setActiveBlacklists(long activeBlacklists) { this.activeBlacklists = activeBlacklists; }
        
        public long getTotalQueries() { return totalQueries; }
        public void setTotalQueries(long totalQueries) { this.totalQueries = totalQueries; }
        
        public long getTotalHits() { return totalHits; }
        public void setTotalHits(long totalHits) { this.totalHits = totalHits; }
        
        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }
    }
}