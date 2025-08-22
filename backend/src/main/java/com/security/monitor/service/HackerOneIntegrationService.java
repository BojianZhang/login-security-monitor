package com.security.monitor.service;

import com.security.monitor.model.EmailAlias;
import com.security.monitor.model.User;
import com.security.monitor.repository.EmailAliasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HackerOne 平台集成服务
 * 专门处理从 hackerone.com 指向的别名同步和显示名称管理
 */
@Service
@Transactional
public class HackerOneIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(HackerOneIntegrationService.class);
    
    // HackerOne 邮箱格式：username+@wearehackerone.com 或 username+suffix@wearehackerone.com
    private static final String HACKERONE_DOMAIN = "wearehackerone.com";
    private static final Pattern HACKERONE_EMAIL_PATTERN = 
        Pattern.compile("^([a-zA-Z0-9._-]+)(\\+[a-zA-Z0-9._-]*)?@" + HACKERONE_DOMAIN + "$", Pattern.CASE_INSENSITIVE);
    
    @Autowired
    private EmailAliasRepository aliasRepository;
    
    @Autowired
    private EmailManagementService emailManagementService;
    
    /**
     * 检测是否为 HackerOne 邮箱
     */
    public boolean isHackerOneEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return HACKERONE_EMAIL_PATTERN.matcher(email.toLowerCase()).matches();
    }
    
    /**
     * 从 HackerOne 邮箱地址提取用户名
     */
    public String extractHackerOneUsername(String email) {
        if (!isHackerOneEmail(email)) {
            return null;
        }
        
        Matcher matcher = HACKERONE_EMAIL_PATTERN.matcher(email.toLowerCase());
        if (matcher.matches()) {
            return matcher.group(1); // 返回用户名部分
        }
        return null;
    }
    
    /**
     * 生成 HackerOne 显示名称 - 保持完整邮箱格式
     */
    public String generateHackerOneDisplayName(String email) {
        if (isHackerOneEmail(email)) {
            // 直接返回完整的邮箱地址，保持原始格式
            return email.toLowerCase();
        }
        return null;
    }
    
    /**
     * 同步用户所有的 HackerOne 别名显示名称
     */
    public int syncUserHackerOneAliases(User user) {
        List<EmailAlias> userAliases = emailManagementService.getUserAliases(user);
        int syncedCount = 0;
        
        for (EmailAlias alias : userAliases) {
            String fullEmail = alias.getFullEmail();
            
            if (isHackerOneEmail(fullEmail)) {
                try {
                    String hackerOneUsername = extractHackerOneUsername(fullEmail);
                    String displayName = fullEmail; // 直接使用完整邮箱地址作为显示名称
                    String description = "HackerOne platform alias: " + fullEmail;
                    String externalId = "hackerone_" + hackerOneUsername;
                    
                    // 只有当显示名称不同时才更新
                    if (!displayName.equals(alias.getDisplayName()) || 
                        !externalId.equals(alias.getExternalAliasId())) {
                        
                        emailManagementService.updateAliasDisplay(
                            user, 
                            alias.getId(), 
                            displayName, 
                            description, 
                            externalId
                        );
                        
                        syncedCount++;
                        logger.info("已同步 HackerOne 别名: {} -> 显示名称: {}", fullEmail, displayName);
                    }
                    
                } catch (Exception e) {
                    logger.error("同步 HackerOne 别名 {} 失败", fullEmail, e);
                }
            }
        }
        
        logger.info("用户 {} 的 HackerOne 别名同步完成，共同步 {} 个别名", user.getUsername(), syncedCount);
        return syncedCount;
    }
    
    /**
     * 自动检测并设置单个 HackerOne 别名
     */
    public boolean autoSetHackerOneAlias(EmailAlias alias) {
        if (!isHackerOneEmail(alias.getFullEmail())) {
            return false;
        }
        
        try {
            String hackerOneUsername = extractHackerOneUsername(alias.getFullEmail());
            String displayName = alias.getFullEmail(); // 直接使用完整邮箱地址
            String description = "HackerOne platform alias: " + alias.getFullEmail();
            String externalId = "hackerone_" + hackerOneUsername;
            
            alias.setDisplayName(displayName);
            alias.setDescription(description);
            alias.setExternalAliasId(externalId);
            
            aliasRepository.save(alias);
            
            logger.info("自动设置 HackerOne 别名: {} -> 显示名称: {}", alias.getFullEmail(), displayName);
            return true;
            
        } catch (Exception e) {
            logger.error("自动设置 HackerOne 别名 {} 失败", alias.getFullEmail(), e);
            return false;
        }
    }
    
    /**
     * 获取用户的所有 HackerOne 别名
     */
    @Transactional(readOnly = true)
    public List<EmailAlias> getUserHackerOneAliases(User user) {
        List<EmailAlias> allAliases = emailManagementService.getUserAliases(user);
        return allAliases.stream()
            .filter(alias -> isHackerOneEmail(alias.getFullEmail()))
            .toList();
    }
    
    /**
     * 根据 HackerOne 用户名查找别名
     */
    @Transactional(readOnly = true)
    public Optional<EmailAlias> findAliasByHackerOneUsername(User user, String hackerOneUsername) {
        String externalId = "hackerone_" + hackerOneUsername.toLowerCase();
        return emailManagementService.findAliasbyExternalId(user, externalId);
    }
    
    /**
     * 验证 HackerOne 用户名格式
     */
    public boolean isValidHackerOneUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // HackerOne 用户名通常允许字母、数字、下划线、点号和短横线
        Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
        return usernamePattern.matcher(username.trim()).matches();
    }
    
    /**
     * 构建 HackerOne 邮箱地址
     */
    public String buildHackerOneEmail(String username, String suffix) {
        if (!isValidHackerOneUsername(username)) {
            throw new IllegalArgumentException("无效的 HackerOne 用户名格式");
        }
        
        StringBuilder email = new StringBuilder(username.toLowerCase());
        if (suffix != null && !suffix.trim().isEmpty()) {
            email.append("+").append(suffix.trim().toLowerCase());
        }
        email.append("@").append(HACKERONE_DOMAIN);
        
        return email.toString();
    }
    
    /**
     * HackerOne 别名统计信息
     */
    @Transactional(readOnly = true)
    public HackerOneAliasStats getHackerOneAliasStats(User user) {
        List<EmailAlias> hackerOneAliases = getUserHackerOneAliases(user);
        
        long totalCount = hackerOneAliases.size();
        long activeCount = hackerOneAliases.stream()
            .mapToLong(alias -> alias.getIsActive() ? 1 : 0)
            .sum();
        long syncedCount = hackerOneAliases.stream()
            .mapToLong(alias -> alias.getExternalAliasId() != null && 
                               alias.getExternalAliasId().startsWith("hackerone_") ? 1 : 0)
            .sum();
        
        return new HackerOneAliasStats(totalCount, activeCount, syncedCount);
    }
    
    /**
     * HackerOne 别名统计数据类
     */
    public static class HackerOneAliasStats {
        private final long totalCount;
        private final long activeCount;
        private final long syncedCount;
        
        public HackerOneAliasStats(long totalCount, long activeCount, long syncedCount) {
            this.totalCount = totalCount;
            this.activeCount = activeCount;
            this.syncedCount = syncedCount;
        }
        
        public long getTotalCount() { return totalCount; }
        public long getActiveCount() { return activeCount; }
        public long getSyncedCount() { return syncedCount; }
        public long getUnsyncedCount() { return totalCount - syncedCount; }
        
        @Override
        public String toString() {
            return String.format("HackerOne别名统计: 总数=%d, 活跃=%d, 已同步=%d, 未同步=%d", 
                totalCount, activeCount, syncedCount, getUnsyncedCount());
        }
    }
}