package com.security.monitor.service;

import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.EmailValidationLog;
import com.security.monitor.repository.EmailValidationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.security.PublicKey;
import java.security.Signature;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * SPF/DKIM/DMARC邮件验证服务
 */
@Service
@Transactional
public class EmailValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailValidationService.class);
    
    @Autowired
    private EmailValidationLogRepository validationLogRepository;
    
    @Value("${app.email.validation.enabled:true}")
    private boolean validationEnabled;
    
    @Value("${app.email.validation.strict-mode:false}")
    private boolean strictMode;
    
    // SPF相关常量
    private static final Pattern SPF_RECORD_PATTERN = Pattern.compile("v=spf1\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPF_MECHANISM_PATTERN = Pattern.compile("(\\+|-|~|\\?)?(all|include|a|mx|ptr|ip4|ip6|exists|redirect)(:([^\\s]+))?");
    
    // DKIM相关常量
    private static final Pattern DKIM_SIGNATURE_PATTERN = Pattern.compile("^DKIM-Signature:\\s*(.+)$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern DKIM_TAG_PATTERN = Pattern.compile("([a-z])=([^;]+)");
    
    // DMARC相关常量
    private static final Pattern DMARC_RECORD_PATTERN = Pattern.compile("v=DMARC1\\s*;(.+)", Pattern.CASE_INSENSITIVE);
    
    /**
     * 验证邮件的SPF、DKIM和DMARC
     */
    public EmailValidationResult validateEmail(EmailMessage message, String senderIP) {
        if (!validationEnabled) {
            return new EmailValidationResult(ValidationStatus.DISABLED, "邮件验证已禁用");
        }
        
        logger.info("开始验证邮件: messageId={}, sender={}", message.getMessageId(), message.getFromAddress());
        
        EmailValidationResult result = new EmailValidationResult();
        result.setMessageId(message.getMessageId());
        result.setSenderIP(senderIP);
        result.setValidationStartTime(LocalDateTime.now());
        
        try {
            // 1. SPF验证
            SPFResult spfResult = validateSPF(message.getFromAddress(), senderIP);
            result.setSpfResult(spfResult);
            
            // 2. DKIM验证
            DKIMResult dkimResult = validateDKIM(message);
            result.setDkimResult(dkimResult);
            
            // 3. DMARC验证
            DMARCResult dmarcResult = validateDMARC(message.getFromAddress(), spfResult, dkimResult);
            result.setDmarcResult(dmarcResult);
            
            // 4. 计算总体验证状态
            result.setOverallStatus(calculateOverallStatus(spfResult, dkimResult, dmarcResult));
            
        } catch (Exception e) {
            logger.error("邮件验证失败", e);
            result.setOverallStatus(ValidationStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setValidationEndTime(LocalDateTime.now());
        }
        
        // 记录验证日志
        logValidationResult(message, result);
        
        logger.info("邮件验证完成: messageId={}, status={}", 
            message.getMessageId(), result.getOverallStatus());
        
        return result;
    }
    
    /**
     * SPF验证
     */
    public SPFResult validateSPF(String fromAddress, String senderIP) {
        try {
            String domain = extractDomain(fromAddress);
            String spfRecord = lookupSPFRecord(domain);
            
            if (spfRecord == null || spfRecord.isEmpty()) {
                return new SPFResult(SPFStatus.NONE, "未找到SPF记录");
            }
            
            SPFStatus status = evaluateSPF(spfRecord, senderIP, domain);
            return new SPFResult(status, spfRecord);
            
        } catch (Exception e) {
            logger.error("SPF验证失败: domain={}, ip={}", extractDomain(fromAddress), senderIP, e);
            return new SPFResult(SPFStatus.TEMPERROR, "SPF验证错误: " + e.getMessage());
        }
    }
    
    /**
     * DKIM验证
     */
    public DKIMResult validateDKIM(EmailMessage message) {
        try {
            String dkimSignature = extractDKIMSignature(message.getRawContent());
            if (dkimSignature == null || dkimSignature.isEmpty()) {
                return new DKIMResult(DKIMStatus.NONE, "未找到DKIM签名");
            }
            
            Map<String, String> dkimTags = parseDKIMSignature(dkimSignature);
            String domain = dkimTags.get("d");
            String selector = dkimTags.get("s");
            
            if (domain == null || selector == null) {
                return new DKIMResult(DKIMStatus.INVALID, "DKIM签名格式无效");
            }
            
            String publicKey = lookupDKIMPublicKey(domain, selector);
            if (publicKey == null) {
                return new DKIMResult(DKIMStatus.INVALID, "未找到DKIM公钥");
            }
            
            boolean isValid = verifyDKIMSignature(message, dkimTags, publicKey);
            DKIMStatus status = isValid ? DKIMStatus.PASS : DKIMStatus.FAIL;
            
            return new DKIMResult(status, dkimSignature, domain, selector);
            
        } catch (Exception e) {
            logger.error("DKIM验证失败", e);
            return new DKIMResult(DKIMStatus.TEMPERROR, "DKIM验证错误: " + e.getMessage());
        }
    }
    
    /**
     * DMARC验证
     */
    public DMARCResult validateDMARC(String fromAddress, SPFResult spfResult, DKIMResult dkimResult) {
        try {
            String domain = extractDomain(fromAddress);
            String dmarcRecord = lookupDMARCRecord(domain);
            
            if (dmarcRecord == null || dmarcRecord.isEmpty()) {
                return new DMARCResult(DMARCStatus.NONE, "未找到DMARC记录");
            }
            
            Map<String, String> dmarcPolicy = parseDMARCRecord(dmarcRecord);
            String policy = dmarcPolicy.get("p");
            String alignment = dmarcPolicy.get("adkim");
            String aspf = dmarcPolicy.get("aspf");
            
            DMARCStatus status = evaluateDMARCPolicy(spfResult, dkimResult, policy, alignment, aspf);
            
            return new DMARCResult(status, dmarcRecord, policy, dmarcPolicy);
            
        } catch (Exception e) {
            logger.error("DMARC验证失败: domain={}", extractDomain(fromAddress), e);
            return new DMARCResult(DMARCStatus.TEMPERROR, "DMARC验证错误: " + e.getMessage());
        }
    }
    
    /**
     * 查找SPF记录
     */
    private String lookupSPFRecord(String domain) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        
        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(domain, new String[]{"TXT"});
        
        Attribute txtAttribute = attrs.get("TXT");
        if (txtAttribute != null) {
            for (int i = 0; i < txtAttribute.size(); i++) {
                String record = txtAttribute.get(i).toString();
                if (record.toLowerCase().startsWith("v=spf1")) {
                    return record;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 查找DKIM公钥
     */
    private String lookupDKIMPublicKey(String domain, String selector) throws NamingException {
        String dkimDomain = selector + "._domainkey." + domain;
        
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        
        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(dkimDomain, new String[]{"TXT"});
        
        Attribute txtAttribute = attrs.get("TXT");
        if (txtAttribute != null && txtAttribute.size() > 0) {
            String record = txtAttribute.get(0).toString();
            // 提取公钥部分
            Pattern pattern = Pattern.compile("p=([A-Za-z0-9+/=]+)");
            Matcher matcher = pattern.matcher(record);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * 查找DMARC记录
     */
    private String lookupDMARCRecord(String domain) throws NamingException {
        String dmarcDomain = "_dmarc." + domain;
        
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        
        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes(dmarcDomain, new String[]{"TXT"});
        
        Attribute txtAttribute = attrs.get("TXT");
        if (txtAttribute != null && txtAttribute.size() > 0) {
            return txtAttribute.get(0).toString();
        }
        
        return null;
    }
    
    /**
     * 评估SPF策略
     */
    private SPFStatus evaluateSPF(String spfRecord, String senderIP, String domain) {
        Matcher matcher = SPF_RECORD_PATTERN.matcher(spfRecord);
        if (!matcher.find()) {
            return SPFStatus.PERMERROR;
        }
        
        String mechanisms = matcher.group(1);
        String[] parts = mechanisms.split("\\s+");
        
        for (String part : parts) {
            Matcher mechMatcher = SPF_MECHANISM_PATTERN.matcher(part);
            if (mechMatcher.find()) {
                String qualifier = mechMatcher.group(1);
                String mechanism = mechMatcher.group(2);
                String value = mechMatcher.group(4);
                
                if (qualifier == null) qualifier = "+"; // 默认为pass
                
                boolean matches = evaluateMechanism(mechanism, value, senderIP, domain);
                if (matches) {
                    return mapQualifierToStatus(qualifier);
                }
            }
        }
        
        return SPFStatus.NEUTRAL;
    }
    
    /**
     * 评估SPF机制
     */
    private boolean evaluateMechanism(String mechanism, String value, String senderIP, String domain) {
        switch (mechanism.toLowerCase()) {
            case "all":
                return true;
            case "ip4":
                return isIPv4InRange(senderIP, value);
            case "ip6":
                return isIPv6InRange(senderIP, value);
            case "a":
                return checkARecord(value != null ? value : domain, senderIP);
            case "mx":
                return checkMXRecord(value != null ? value : domain, senderIP);
            case "include":
                return evaluateInclude(value, senderIP);
            default:
                return false;
        }
    }
    
    /**
     * 提取DKIM签名
     */
    private String extractDKIMSignature(String rawContent) {
        if (rawContent == null) return null;
        
        Matcher matcher = DKIM_SIGNATURE_PATTERN.matcher(rawContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 解析DKIM签名
     */
    private Map<String, String> parseDKIMSignature(String signature) {
        Map<String, String> tags = new HashMap<>();
        
        Matcher matcher = DKIM_TAG_PATTERN.matcher(signature);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            tags.put(key, value);
        }
        
        return tags;
    }
    
    /**
     * 验证DKIM签名
     */
    private boolean verifyDKIMSignature(EmailMessage message, Map<String, String> dkimTags, String publicKeyStr) {
        try {
            // 这里应该实现完整的DKIM签名验证
            // 简化实现，实际需要使用RSA公钥验证签名
            return publicKeyStr != null && !publicKeyStr.isEmpty();
        } catch (Exception e) {
            logger.error("DKIM签名验证失败", e);
            return false;
        }
    }
    
    /**
     * 解析DMARC记录
     */
    private Map<String, String> parseDMARCRecord(String record) {
        Map<String, String> policy = new HashMap<>();
        
        String[] parts = record.split(";");
        for (String part : parts) {
            String[] keyValue = part.trim().split("=", 2);
            if (keyValue.length == 2) {
                policy.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        
        return policy;
    }
    
    /**
     * 评估DMARC策略
     */
    private DMARCStatus evaluateDMARCPolicy(SPFResult spfResult, DKIMResult dkimResult, 
                                          String policy, String alignment, String aspf) {
        boolean spfAligned = isAligned(spfResult.getStatus(), aspf);
        boolean dkimAligned = isAligned(dkimResult.getStatus(), alignment);
        
        if (spfAligned || dkimAligned) {
            return DMARCStatus.PASS;
        } else {
            return DMARCStatus.FAIL;
        }
    }
    
    // 辅助方法
    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : email;
    }
    
    private SPFStatus mapQualifierToStatus(String qualifier) {
        switch (qualifier) {
            case "+": return SPFStatus.PASS;
            case "-": return SPFStatus.FAIL;
            case "~": return SPFStatus.SOFTFAIL;
            case "?": return SPFStatus.NEUTRAL;
            default: return SPFStatus.NEUTRAL;
        }
    }
    
    private boolean isIPv4InRange(String ip, String range) {
        // 简化实现，实际需要处理CIDR等
        return ip.equals(range);
    }
    
    private boolean isIPv6InRange(String ip, String range) {
        // 简化实现
        return ip.equals(range);
    }
    
    private boolean checkARecord(String domain, String ip) {
        // 简化实现，实际需要DNS查询
        return false;
    }
    
    private boolean checkMXRecord(String domain, String ip) {
        // 简化实现，实际需要DNS查询
        return false;
    }
    
    private boolean evaluateInclude(String domain, String ip) {
        // 简化实现，实际需要递归SPF查询
        return false;
    }
    
    private boolean isAligned(Object status, String alignment) {
        // 简化实现，实际需要检查域名对齐
        return status == SPFStatus.PASS || status == DKIMStatus.PASS;
    }
    
    private ValidationStatus calculateOverallStatus(SPFResult spfResult, DKIMResult dkimResult, DMARCResult dmarcResult) {
        if (dmarcResult.getStatus() == DMARCStatus.PASS) {
            return ValidationStatus.PASS;
        }
        
        if (strictMode) {
            if (dmarcResult.getStatus() == DMARCStatus.FAIL) {
                return ValidationStatus.FAIL;
            }
        }
        
        // 综合评估
        if (spfResult.getStatus() == SPFStatus.PASS || dkimResult.getStatus() == DKIMStatus.PASS) {
            return ValidationStatus.PASS;
        }
        
        return ValidationStatus.FAIL;
    }
    
    private void logValidationResult(EmailMessage message, EmailValidationResult result) {
        try {
            EmailValidationLog log = new EmailValidationLog();
            log.setMessage(message);
            log.setValidationStatus(result.getOverallStatus().toString());
            log.setSpfStatus(result.getSpfResult().getStatus().toString());
            log.setDkimStatus(result.getDkimResult().getStatus().toString());
            log.setDmarcStatus(result.getDmarcResult().getStatus().toString());
            log.setSenderIp(result.getSenderIP());
            log.setValidationDetails(formatValidationDetails(result));
            log.setValidatedAt(LocalDateTime.now());
            
            validationLogRepository.save(log);
            
        } catch (Exception e) {
            logger.error("保存验证日志失败", e);
        }
    }
    
    private String formatValidationDetails(EmailValidationResult result) {
        return String.format("SPF: %s, DKIM: %s, DMARC: %s", 
            result.getSpfResult().getDetails(),
            result.getDkimResult().getDetails(),
            result.getDmarcResult().getDetails());
    }
    
    // 枚举和结果类
    public enum ValidationStatus {
        PASS, FAIL, DISABLED, ERROR
    }
    
    public enum SPFStatus {
        PASS, FAIL, SOFTFAIL, NEUTRAL, NONE, TEMPERROR, PERMERROR
    }
    
    public enum DKIMStatus {
        PASS, FAIL, INVALID, NONE, TEMPERROR
    }
    
    public enum DMARCStatus {
        PASS, FAIL, NONE, TEMPERROR
    }
    
    // 结果类
    public static class EmailValidationResult {
        private String messageId;
        private String senderIP;
        private ValidationStatus overallStatus;
        private SPFResult spfResult;
        private DKIMResult dkimResult;
        private DMARCResult dmarcResult;
        private String errorMessage;
        private LocalDateTime validationStartTime;
        private LocalDateTime validationEndTime;
        
        public EmailValidationResult() {}
        
        public EmailValidationResult(ValidationStatus status, String message) {
            this.overallStatus = status;
            this.errorMessage = message;
        }
        
        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getSenderIP() { return senderIP; }
        public void setSenderIP(String senderIP) { this.senderIP = senderIP; }
        
        public ValidationStatus getOverallStatus() { return overallStatus; }
        public void setOverallStatus(ValidationStatus overallStatus) { this.overallStatus = overallStatus; }
        
        public SPFResult getSpfResult() { return spfResult; }
        public void setSpfResult(SPFResult spfResult) { this.spfResult = spfResult; }
        
        public DKIMResult getDkimResult() { return dkimResult; }
        public void setDkimResult(DKIMResult dkimResult) { this.dkimResult = dkimResult; }
        
        public DMARCResult getDmarcResult() { return dmarcResult; }
        public void setDmarcResult(DMARCResult dmarcResult) { this.dmarcResult = dmarcResult; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getValidationStartTime() { return validationStartTime; }
        public void setValidationStartTime(LocalDateTime validationStartTime) { this.validationStartTime = validationStartTime; }
        
        public LocalDateTime getValidationEndTime() { return validationEndTime; }
        public void setValidationEndTime(LocalDateTime validationEndTime) { this.validationEndTime = validationEndTime; }
    }
    
    public static class SPFResult {
        private SPFStatus status;
        private String details;
        private String record;
        
        public SPFResult(SPFStatus status, String details) {
            this.status = status;
            this.details = details;
        }
        
        public SPFResult(SPFStatus status, String details, String record) {
            this.status = status;
            this.details = details;
            this.record = record;
        }
        
        public SPFStatus getStatus() { return status; }
        public String getDetails() { return details; }
        public String getRecord() { return record; }
    }
    
    public static class DKIMResult {
        private DKIMStatus status;
        private String details;
        private String signature;
        private String domain;
        private String selector;
        
        public DKIMResult(DKIMStatus status, String details) {
            this.status = status;
            this.details = details;
        }
        
        public DKIMResult(DKIMStatus status, String signature, String domain, String selector) {
            this.status = status;
            this.signature = signature;
            this.domain = domain;
            this.selector = selector;
        }
        
        public DKIMStatus getStatus() { return status; }
        public String getDetails() { return details; }
        public String getSignature() { return signature; }
        public String getDomain() { return domain; }
        public String getSelector() { return selector; }
    }
    
    public static class DMARCResult {
        private DMARCStatus status;
        private String details;
        private String record;
        private String policy;
        private Map<String, String> policyMap;
        
        public DMARCResult(DMARCStatus status, String details) {
            this.status = status;
            this.details = details;
        }
        
        public DMARCResult(DMARCStatus status, String record, String policy, Map<String, String> policyMap) {
            this.status = status;
            this.record = record;
            this.policy = policy;
            this.policyMap = policyMap;
        }
        
        public DMARCStatus getStatus() { return status; }
        public String getDetails() { return details; }
        public String getRecord() { return record; }
        public String getPolicy() { return policy; }
        public Map<String, String> getPolicyMap() { return policyMap; }
    }
}