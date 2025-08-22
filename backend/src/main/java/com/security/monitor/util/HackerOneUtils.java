package com.security.monitor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HackerOne 平台工具类
 * 提供HackerOne邮箱格式处理的实用方法
 */
public class HackerOneUtils {
    
    // HackerOne相关常量
    public static final String HACKERONE_DOMAIN = "wearehackerone.com";
    public static final String HACKERONE_PLATFORM_NAME = "HackerOne";
    public static final String DISPLAY_NAME_PREFIX = "HackerOne: ";
    public static final String EXTERNAL_ID_PREFIX = "hackerone_";
    
    // 正则表达式模式
    private static final Pattern HACKERONE_EMAIL_PATTERN = 
        Pattern.compile("^([a-zA-Z0-9._-]+)(\\+[a-zA-Z0-9._-]*)?@" + HACKERONE_DOMAIN + "$", 
                       Pattern.CASE_INSENSITIVE);
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    
    private static final Pattern SUFFIX_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9._-]{0,20}$");
    
    /**
     * 检查是否为HackerOne邮箱格式
     */
    public static boolean isHackerOneEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return HACKERONE_EMAIL_PATTERN.matcher(email.toLowerCase().trim()).matches();
    }
    
    /**
     * 从HackerOne邮箱提取用户名
     */
    public static String extractUsername(String email) {
        if (!isHackerOneEmail(email)) {
            return null;
        }
        
        Matcher matcher = HACKERONE_EMAIL_PATTERN.matcher(email.toLowerCase().trim());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 从HackerOne邮箱提取后缀（+号后面的部分）
     */
    public static String extractSuffix(String email) {
        if (!isHackerOneEmail(email)) {
            return null;
        }
        
        Matcher matcher = HACKERONE_EMAIL_PATTERN.matcher(email.toLowerCase().trim());
        if (matcher.matches()) {
            String suffixPart = matcher.group(2); // 包含+号的部分
            return suffixPart != null ? suffixPart.substring(1) : null; // 移除+号
        }
        return null;
    }
    
    /**
     * 生成HackerOne显示名称 - 保持完整邮箱格式
     */
    public static String generateDisplayName(String email) {
        if (isHackerOneEmail(email)) {
            return normalizeEmail(email); // 返回完整的邮箱地址
        }
        return null;
    }
    
    /**
     * 生成自定义HackerOne显示名称
     */
    public static String generateDisplayName(String email, String template) {
        String username = extractUsername(email);
        if (username == null || template == null) {
            return generateDisplayName(email);
        }
        
        return template.replace("{username}", username)
                      .replace("{email}", email)
                      .replace("{domain}", HACKERONE_DOMAIN)
                      .replace("{platform}", HACKERONE_PLATFORM_NAME);
    }
    
    /**
     * 生成外部别名ID
     */
    public static String generateExternalId(String email) {
        String username = extractUsername(email);
        return username != null ? EXTERNAL_ID_PREFIX + username : null;
    }
    
    /**
     * 生成别名描述
     */
    public static String generateDescription(String email) {
        String username = extractUsername(email);
        String suffix = extractSuffix(email);
        
        if (username == null) {
            return null;
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append("HackerOne platform alias: ").append(normalizeEmail(email));
        
        if (suffix != null && !suffix.isEmpty()) {
            desc.append(" (suffix: ").append(suffix).append(")");
        }
        
        return desc.toString();
    }
    
    /**
     * 构建HackerOne邮箱地址
     */
    public static String buildEmail(String username, String suffix) {
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid HackerOne username format: " + username);
        }
        
        StringBuilder email = new StringBuilder(username.toLowerCase().trim());
        
        if (suffix != null && !suffix.trim().isEmpty()) {
            if (!isValidSuffix(suffix)) {
                throw new IllegalArgumentException("Invalid HackerOne email suffix format: " + suffix);
            }
            email.append("+").append(suffix.toLowerCase().trim());
        }
        
        email.append("@").append(HACKERONE_DOMAIN);
        return email.toString();
    }
    
    /**
     * 验证HackerOne用户名格式
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }
    
    /**
     * 验证HackerOne邮箱后缀格式
     */
    public static boolean isValidSuffix(String suffix) {
        if (suffix == null) {
            return true; // null后缀是允许的
        }
        return SUFFIX_PATTERN.matcher(suffix.trim()).matches();
    }
    
    /**
     * 标准化邮箱地址（转为小写并去除空格）
     */
    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.toLowerCase().trim();
    }
    
    /**
     * 检查显示名称是否为HackerOne格式
     */
    public static boolean isHackerOneDisplayName(String displayName) {
        if (displayName == null) {
            return false;
        }
        return displayName.startsWith(DISPLAY_NAME_PREFIX);
    }
    
    /**
     * 从HackerOne格式的显示名称提取用户名
     */
    public static String extractUsernameFromDisplayName(String displayName) {
        if (!isHackerOneDisplayName(displayName)) {
            return null;
        }
        return displayName.substring(DISPLAY_NAME_PREFIX.length()).trim();
    }
    
    /**
     * 检查外部别名ID是否为HackerOne格式
     */
    public static boolean isHackerOneExternalId(String externalId) {
        if (externalId == null) {
            return false;
        }
        return externalId.startsWith(EXTERNAL_ID_PREFIX);
    }
    
    /**
     * 从HackerOne格式的外部别名ID提取用户名
     */
    public static String extractUsernameFromExternalId(String externalId) {
        if (!isHackerOneExternalId(externalId)) {
            return null;
        }
        return externalId.substring(EXTERNAL_ID_PREFIX.length());
    }
    
    /**
     * HackerOne邮箱信息类
     */
    public static class HackerOneEmailInfo {
        private final String email;
        private final String username;
        private final String suffix;
        private final String displayName;
        private final String externalId;
        private final String description;
        
        public HackerOneEmailInfo(String email) {
            this.email = normalizeEmail(email);
            this.username = extractUsername(this.email);
            this.suffix = extractSuffix(this.email);
            this.displayName = generateDisplayName(this.email);
            this.externalId = generateExternalId(this.email);
            this.description = generateDescription(this.email);
        }
        
        // Getters
        public String getEmail() { return email; }
        public String getUsername() { return username; }
        public String getSuffix() { return suffix; }
        public String getDisplayName() { return displayName; }
        public String getExternalId() { return externalId; }
        public String getDescription() { return description; }
        public boolean isValid() { return username != null; }
        
        @Override
        public String toString() {
            return String.format("HackerOneEmailInfo{email='%s', username='%s', suffix='%s', displayName='%s'}", 
                email, username, suffix, displayName);
        }
    }
    
    /**
     * 解析HackerOne邮箱信息
     */
    public static HackerOneEmailInfo parseEmail(String email) {
        return new HackerOneEmailInfo(email);
    }
}