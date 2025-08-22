package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 邮件转发服务
 */
@Service
@Transactional
public class EmailForwardingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailForwardingService.class);
    
    @Autowired
    private EmailForwardingRuleRepository forwardingRuleRepository;
    
    @Autowired
    private EmailAliasRepository aliasRepository;
    
    @Autowired
    private EmailSendService emailSendService;
    
    /**
     * 创建转发规则
     */
    public EmailForwardingRule createForwardingRule(User user, Long aliasId, ForwardingRuleRequest request) {
        // 验证别名所有权
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        EmailAlias alias = aliasOpt.get();
        
        // 创建转发规则
        EmailForwardingRule rule = new EmailForwardingRule(alias);
        rule.setRuleName(request.getRuleName());
        rule.setForwardTo(request.getForwardTo());
        rule.setIsActive(request.getIsActive());
        rule.setForwardSubject(request.getForwardSubject());
        rule.setKeepOriginal(request.getKeepOriginal());
        rule.setConditions(request.getConditions());
        
        EmailForwardingRule saved = forwardingRuleRepository.save(rule);
        
        logger.info("转发规则已创建: 别名={}, 规则={}, 转发到={}", 
            alias.getFullEmail(), saved.getRuleName(), saved.getForwardTo());
        
        return saved;
    }
    
    /**
     * 更新转发规则
     */
    public EmailForwardingRule updateForwardingRule(User user, Long ruleId, ForwardingRuleRequest request) {
        Optional<EmailForwardingRule> ruleOpt = forwardingRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getAlias().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("转发规则不存在或无权限");
        }
        
        EmailForwardingRule rule = ruleOpt.get();
        rule.setRuleName(request.getRuleName());
        rule.setForwardTo(request.getForwardTo());
        rule.setIsActive(request.getIsActive());
        rule.setForwardSubject(request.getForwardSubject());
        rule.setKeepOriginal(request.getKeepOriginal());
        rule.setConditions(request.getConditions());
        
        return forwardingRuleRepository.save(rule);
    }
    
    /**
     * 删除转发规则
     */
    public void deleteForwardingRule(User user, Long ruleId) {
        Optional<EmailForwardingRule> ruleOpt = forwardingRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getAlias().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("转发规则不存在或无权限");
        }
        
        forwardingRuleRepository.delete(ruleOpt.get());
        logger.info("转发规则已删除: ID={}", ruleId);
    }
    
    /**
     * 获取别名的转发规则
     */
    @Transactional(readOnly = true)
    public List<EmailForwardingRule> getForwardingRules(User user, Long aliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        return forwardingRuleRepository.findByAliasOrderByPriorityDesc(aliasOpt.get());
    }
    
    /**
     * 处理邮件转发（在收到邮件时调用）
     */
    public void processForwarding(EmailMessage incomingMessage) {
        try {
            // 解析收件人地址，获取对应的别名
            String toAddress = incomingMessage.getToAddresses();
            Optional<EmailAlias> aliasOpt = aliasRepository.findByFullEmailAddress(toAddress);
            
            if (aliasOpt.isEmpty()) {
                logger.debug("未找到别名，跳过转发: {}", toAddress);
                return;
            }
            
            EmailAlias alias = aliasOpt.get();
            
            // 获取活跃的转发规则
            List<EmailForwardingRule> rules = forwardingRuleRepository.findByAliasAndIsActiveTrueOrderByPriorityDesc(alias);
            
            if (rules.isEmpty()) {
                logger.debug("无转发规则，跳过: {}", toAddress);
                return;
            }
            
            // 按优先级处理转发规则
            for (EmailForwardingRule rule : rules) {
                if (shouldForward(incomingMessage, rule)) {
                    forwardMessage(incomingMessage, rule);
                    
                    // 如果规则设置为停止处理，则不继续后续规则
                    if (!rule.getContinueProcessing()) {
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("邮件转发处理异常", e);
        }
    }
    
    /**
     * 判断是否应该转发邮件
     */
    private boolean shouldForward(EmailMessage message, EmailForwardingRule rule) {
        String conditions = rule.getConditions();
        if (conditions == null || conditions.trim().isEmpty()) {
            return true; // 无条件则转发所有邮件
        }
        
        try {
            // 解析条件（简化版本，实际可以使用JSON或规则引擎）
            if (conditions.contains("sender:")) {
                String senderPattern = extractPattern(conditions, "sender:");
                if (senderPattern != null && !matchesPattern(message.getFromAddress(), senderPattern)) {
                    return false;
                }
            }
            
            if (conditions.contains("subject:")) {
                String subjectPattern = extractPattern(conditions, "subject:");
                if (subjectPattern != null && !matchesPattern(message.getSubject(), subjectPattern)) {
                    return false;
                }
            }
            
            if (conditions.contains("content:")) {
                String contentPattern = extractPattern(conditions, "content:");
                if (contentPattern != null && !matchesPattern(message.getBodyText(), contentPattern)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.warn("转发条件解析失败，默认不转发: {}", conditions, e);
            return false;
        }
    }
    
    private String extractPattern(String conditions, String prefix) {
        int start = conditions.indexOf(prefix);
        if (start == -1) return null;
        
        start += prefix.length();
        int end = conditions.indexOf(";", start);
        if (end == -1) end = conditions.length();
        
        return conditions.substring(start, end).trim();
    }
    
    private boolean matchesPattern(String text, String pattern) {
        if (text == null) text = "";
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            // 包含匹配
            return text.toLowerCase().contains(pattern.substring(1, pattern.length() - 1).toLowerCase());
        } else if (pattern.startsWith("*")) {
            // 后缀匹配
            return text.toLowerCase().endsWith(pattern.substring(1).toLowerCase());
        } else if (pattern.endsWith("*")) {
            // 前缀匹配
            return text.toLowerCase().startsWith(pattern.substring(0, pattern.length() - 1).toLowerCase());
        } else if (pattern.startsWith("regex:")) {
            // 正则匹配
            try {
                Pattern regex = Pattern.compile(pattern.substring(6), Pattern.CASE_INSENSITIVE);
                return regex.matcher(text).find();
            } catch (Exception e) {
                logger.warn("正则表达式格式错误: {}", pattern, e);
                return false;
            }
        } else {
            // 精确匹配
            return text.toLowerCase().equals(pattern.toLowerCase());
        }
    }
    
    /**
     * 转发邮件
     */
    private void forwardMessage(EmailMessage originalMessage, EmailForwardingRule rule) {
        try {
            String forwardSubject = rule.getForwardSubject();
            if (forwardSubject == null || forwardSubject.trim().isEmpty()) {
                forwardSubject = "Fwd: " + originalMessage.getSubject();
            } else {
                // 替换变量
                forwardSubject = forwardSubject
                    .replace("{original_subject}", originalMessage.getSubject())
                    .replace("{from}", originalMessage.getFromAddress())
                    .replace("{alias}", rule.getAlias().getFullEmail());
            }
            
            // 构造转发内容
            StringBuilder forwardContent = new StringBuilder();
            forwardContent.append("---------- 转发邮件 ----------\n");
            forwardContent.append("发件人: ").append(originalMessage.getFromAddress()).append("\n");
            forwardContent.append("收件人: ").append(originalMessage.getToAddresses()).append("\n");
            forwardContent.append("主题: ").append(originalMessage.getSubject()).append("\n");
            forwardContent.append("时间: ").append(originalMessage.getReceivedAt()).append("\n\n");
            forwardContent.append(originalMessage.getBodyText());
            
            // 发送转发邮件
            EmailSendService.EmailSendRequest forwardRequest = new EmailSendService.EmailSendRequest(
                rule.getAlias().getFullEmail(), // 从别名发出
                rule.getForwardTo(),            // 转发目标
                forwardSubject,
                forwardContent.toString(),
                null // 暂时只支持纯文本转发
            );
            
            boolean sent = emailSendService.sendEmail(forwardRequest);
            
            if (sent) {
                // 更新转发统计
                rule.incrementForwardCount();
                forwardingRuleRepository.save(rule);
                
                logger.info("邮件转发成功: {} -> {}", 
                    originalMessage.getFromAddress(), rule.getForwardTo());
            } else {
                logger.warn("邮件转发失败: {} -> {}", 
                    originalMessage.getFromAddress(), rule.getForwardTo());
            }
            
        } catch (Exception e) {
            logger.error("转发邮件异常", e);
        }
    }
    
    /**
     * 获取用户所有转发规则
     */
    @Transactional(readOnly = true)
    public List<EmailForwardingRule> getUserForwardingRules(User user) {
        return forwardingRuleRepository.findByAliasUserOrderByAliasIdAscPriorityDesc(user);
    }
    
    /**
     * 切换转发规则状态
     */
    public EmailForwardingRule toggleRuleStatus(User user, Long ruleId) {
        Optional<EmailForwardingRule> ruleOpt = forwardingRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty() || !ruleOpt.get().getAlias().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("转发规则不存在或无权限");
        }
        
        EmailForwardingRule rule = ruleOpt.get();
        rule.setIsActive(!rule.getIsActive());
        
        return forwardingRuleRepository.save(rule);
    }
    
    // 内部请求类
    public static class ForwardingRuleRequest {
        private String ruleName;
        private String forwardTo;
        private Boolean isActive = true;
        private String forwardSubject;
        private Boolean keepOriginal = true;
        private String conditions;
        private Integer priority = 0;
        private Boolean continueProcessing = false;
        
        // Getters and Setters
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getForwardTo() { return forwardTo; }
        public void setForwardTo(String forwardTo) { this.forwardTo = forwardTo; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public String getForwardSubject() { return forwardSubject; }
        public void setForwardSubject(String forwardSubject) { this.forwardSubject = forwardSubject; }
        public Boolean getKeepOriginal() { return keepOriginal; }
        public void setKeepOriginal(Boolean keepOriginal) { this.keepOriginal = keepOriginal; }
        public String getConditions() { return conditions; }
        public void setConditions(String conditions) { this.conditions = conditions; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public Boolean getContinueProcessing() { return continueProcessing; }
        public void setContinueProcessing(Boolean continueProcessing) { this.continueProcessing = continueProcessing; }
    }
}