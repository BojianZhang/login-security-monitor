package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 邮件规则过滤器服务
 * 提供邮件自动处理规则的创建、管理和执行功能
 */
@Service
@Transactional
public class EmailRuleService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailRuleService.class);
    
    @Autowired
    private EmailRuleRepository ruleRepository;
    
    @Autowired
    private EmailRuleActionRepository ruleActionRepository;
    
    @Autowired
    private EmailRuleExecutionLogRepository executionLogRepository;
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private EmailFolderRepository folderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 应用邮件规则到新收到的邮件
     */
    public void applyRulesToMessage(EmailMessage message) {
        logger.debug("为邮件 {} 应用过滤规则", message.getId());
        
        User user = message.getUser();
        List<EmailRule> rules = ruleRepository.findActiveRulesByUser(user);
        
        for (EmailRule rule : rules) {
            try {
                if (evaluateRule(rule, message)) {
                    executeRuleActions(rule, message);
                    
                    // 记录执行日志
                    logRuleExecution(rule, message, true, null);
                    
                    // 如果规则设置为停止处理后续规则，则跳出循环
                    if (!rule.getContinueProcessing()) {
                        logger.debug("规则 {} 设置为停止处理，跳过后续规则", rule.getRuleName());
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("执行规则 {} 时发生错误", rule.getRuleName(), e);
                logRuleExecution(rule, message, false, e.getMessage());
            }
        }
    }
    
    /**
     * 批量应用规则到现有邮件
     */
    public RuleBatchResult applyRuleToExistingMessages(User user, Long ruleId, EmailRuleBatchRequest request) {
        logger.info("用户 {} 批量应用规则 {} 到现有邮件", user.getUsername(), ruleId);
        
        EmailRule rule = ruleRepository.findByIdAndUser(ruleId, user)
            .orElseThrow(() -> new RuntimeException("规则不存在或无权限"));
        
        if (!rule.getIsActive()) {
            throw new RuntimeException("规则未激活");
        }
        
        // 获取要处理的邮件
        List<EmailMessage> messages = getMessagesForBatchProcessing(user, request);
        
        int processedCount = 0;
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (EmailMessage message : messages) {
            try {
                processedCount++;
                
                if (evaluateRule(rule, message)) {
                    executeRuleActions(rule, message);
                    successCount++;
                    
                    logRuleExecution(rule, message, true, null);
                } else {
                    // 规则条件不匹配，跳过
                }
            } catch (Exception e) {
                errorCount++;
                String error = "邮件 " + message.getSubject() + " 处理失败: " + e.getMessage();
                errors.add(error);
                
                logRuleExecution(rule, message, false, e.getMessage());
                logger.error("批量应用规则时处理邮件失败", e);
            }
        }
        
        // 更新规则应用统计
        rule.setAppliedCount(rule.getAppliedCount() + successCount);
        rule.setLastAppliedAt(LocalDateTime.now());
        ruleRepository.save(rule);
        
        logger.info("批量应用规则完成: 处理={}, 成功={}, 失败={}", processedCount, successCount, errorCount);
        
        return new RuleBatchResult(processedCount, successCount, errorCount, errors);
    }
    
    /**
     * 评估规则条件是否匹配邮件
     */
    private boolean evaluateRule(EmailRule rule, EmailMessage message) {
        try {
            // 解析规则条件（JSON格式）
            RuleCondition condition = parseRuleCondition(rule.getConditions());
            
            return evaluateCondition(condition, message);
            
        } catch (Exception e) {
            logger.error("评估规则条件时发生错误: " + rule.getRuleName(), e);
            return false;
        }
    }
    
    /**
     * 递归评估条件
     */
    private boolean evaluateCondition(RuleCondition condition, EmailMessage message) {
        if (condition.getOperator() != null) {
            // 逻辑操作符（AND, OR）
            boolean result = "OR".equals(condition.getOperator()) ? false : true;
            
            for (RuleCondition subCondition : condition.getConditions()) {
                boolean subResult = evaluateCondition(subCondition, message);
                
                if ("OR".equals(condition.getOperator())) {
                    result = result || subResult;
                } else { // AND
                    result = result && subResult;
                }
            }
            
            return result;
        } else {
            // 叶子条件
            return evaluateLeafCondition(condition, message);
        }
    }
    
    /**
     * 评估叶子条件
     */
    private boolean evaluateLeafCondition(RuleCondition condition, EmailMessage message) {
        String field = condition.getField();
        String operator = condition.getOperator();
        String value = condition.getValue();
        
        if (value == null) return false;
        
        String messageValue = getMessageFieldValue(message, field);
        if (messageValue == null) messageValue = "";
        
        switch (operator.toLowerCase()) {
            case "equals":
                return messageValue.equals(value);
            case "not_equals":
                return !messageValue.equals(value);
            case "contains":
                return messageValue.toLowerCase().contains(value.toLowerCase());
            case "not_contains":
                return !messageValue.toLowerCase().contains(value.toLowerCase());
            case "starts_with":
                return messageValue.toLowerCase().startsWith(value.toLowerCase());
            case "ends_with":
                return messageValue.toLowerCase().endsWith(value.toLowerCase());
            case "regex":
                try {
                    return Pattern.compile(value, Pattern.CASE_INSENSITIVE).matcher(messageValue).find();
                } catch (PatternSyntaxException e) {
                    logger.warn("无效的正则表达式: {}", value);
                    return false;
                }
            case "greater_than":
                return compareNumeric(messageValue, value) > 0;
            case "less_than":
                return compareNumeric(messageValue, value) < 0;
            case "is_empty":
                return messageValue.trim().isEmpty();
            case "is_not_empty":
                return !messageValue.trim().isEmpty();
            default:
                logger.warn("未知的操作符: {}", operator);
                return false;
        }
    }
    
    /**
     * 获取邮件字段值
     */
    private String getMessageFieldValue(EmailMessage message, String field) {
        switch (field.toLowerCase()) {
            case "subject":
                return message.getSubject();
            case "from":
            case "from_address":
                return message.getFromAddress();
            case "to":
            case "to_addresses":
                return message.getToAddresses();
            case "cc":
            case "cc_addresses":
                return message.getCcAddresses();
            case "body":
            case "body_text":
                return message.getBodyText();
            case "body_html":
                return message.getBodyHtml();
            case "size":
                return String.valueOf(message.getMessageSize());
            case "priority":
            case "priority_level":
                return String.valueOf(message.getPriorityLevel());
            case "has_attachments":
                // 这里需要检查是否有附件
                return "false"; // 简化处理
            default:
                logger.warn("未知的邮件字段: {}", field);
                return "";
        }
    }
    
    /**
     * 数值比较
     */
    private int compareNumeric(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Double.compare(num1, num2);
        } catch (NumberFormatException e) {
            return value1.compareTo(value2);
        }
    }
    
    /**
     * 执行规则动作
     */
    private void executeRuleActions(EmailRule rule, EmailMessage message) {
        List<EmailRuleAction> actions = ruleActionRepository.findByRuleOrderByPriority(rule);
        
        for (EmailRuleAction action : actions) {
            executeAction(action, message);
        }
    }
    
    /**
     * 执行单个动作
     */
    private void executeAction(EmailRuleAction action, EmailMessage message) {
        logger.debug("执行动作: {} 对邮件: {}", action.getActionType(), message.getId());
        
        switch (action.getActionType()) {
            case MOVE_TO_FOLDER:
                moveToFolder(message, action.getActionValue());
                break;
            case MARK_AS_READ:
                message.setIsRead(true);
                messageRepository.save(message);
                break;
            case MARK_AS_UNREAD:
                message.setIsRead(false);
                messageRepository.save(message);
                break;
            case ADD_STAR:
                message.setIsStarred(true);
                messageRepository.save(message);
                break;
            case REMOVE_STAR:
                message.setIsStarred(false);
                messageRepository.save(message);
                break;
            case DELETE:
                message.setIsDeleted(true);
                messageRepository.save(message);
                break;
            case MARK_AS_SPAM:
                moveToSpamFolder(message);
                break;
            case ADD_LABEL:
                addLabel(message, action.getActionValue());
                break;
            case FORWARD_TO:
                forwardMessage(message, action.getActionValue());
                break;
            default:
                logger.warn("未知的动作类型: {}", action.getActionType());
        }
    }
    
    /**
     * 移动邮件到指定文件夹
     */
    private void moveToFolder(EmailMessage message, String folderName) {
        try {
            EmailFolder targetFolder = folderRepository.findByUserAndFolderName(message.getUser(), folderName)
                .orElseGet(() -> createCustomFolder(message.getUser(), folderName));
            
            EmailFolder oldFolder = message.getFolder();
            message.setFolder(targetFolder);
            messageRepository.save(message);
            
            // 更新文件夹统计
            updateFolderStatistics(oldFolder);
            updateFolderStatistics(targetFolder);
            
            logger.debug("邮件已移动到文件夹: {}", folderName);
        } catch (Exception e) {
            logger.error("移动邮件到文件夹失败: " + folderName, e);
        }
    }
    
    /**
     * 移动邮件到垃圾邮件文件夹
     */
    private void moveToSpamFolder(EmailMessage message) {
        try {
            EmailFolder spamFolder = folderRepository.findByUserAndFolderType(
                message.getUser(), EmailFolder.FolderType.SPAM)
                .orElseThrow(() -> new RuntimeException("垃圾邮件文件夹不存在"));
            
            EmailFolder oldFolder = message.getFolder();
            message.setFolder(spamFolder);
            messageRepository.save(message);
            
            // 更新文件夹统计
            updateFolderStatistics(oldFolder);
            updateFolderStatistics(spamFolder);
            
        } catch (Exception e) {
            logger.error("移动邮件到垃圾邮件文件夹失败", e);
        }
    }
    
    /**
     * 创建自定义文件夹
     */
    private EmailFolder createCustomFolder(User user, String folderName) {
        EmailFolder folder = new EmailFolder();
        folder.setUser(user);
        folder.setFolderName(folderName);
        folder.setFolderType(EmailFolder.FolderType.CUSTOM);
        folder.setIsSystemFolder(false);
        folder.setMessageCount(0);
        folder.setUnreadCount(0);
        
        return folderRepository.save(folder);
    }
    
    /**
     * 添加标签（简化实现）
     */
    private void addLabel(EmailMessage message, String label) {
        // 这里可以实现标签功能，暂时记录日志
        logger.info("为邮件 {} 添加标签: {}", message.getId(), label);
    }
    
    /**
     * 转发邮件（简化实现）
     */
    private void forwardMessage(EmailMessage message, String forwardTo) {
        // 这里可以实现邮件转发功能，暂时记录日志
        logger.info("转发邮件 {} 到: {}", message.getId(), forwardTo);
    }
    
    /**
     * 更新文件夹统计
     */
    private void updateFolderStatistics(EmailFolder folder) {
        if (folder == null) return;
        
        int messageCount = messageRepository.countByFolder(folder);
        int unreadCount = messageRepository.countByFolderAndIsRead(folder, false);
        
        folder.setMessageCount(messageCount);
        folder.setUnreadCount(unreadCount);
        folderRepository.save(folder);
    }
    
    /**
     * 记录规则执行日志
     */
    private void logRuleExecution(EmailRule rule, EmailMessage message, boolean success, String errorMessage) {
        EmailRuleExecutionLog log = new EmailRuleExecutionLog();
        log.setRule(rule);
        log.setMessage(message);
        log.setExecutedAt(LocalDateTime.now());
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        
        executionLogRepository.save(log);
    }
    
    /**
     * 获取批量处理的邮件列表
     */
    private List<EmailMessage> getMessagesForBatchProcessing(User user, EmailRuleBatchRequest request) {
        if (request.getFolderId() != null) {
            EmailFolder folder = folderRepository.findById(request.getFolderId())
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));
            
            if (!folder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权限访问该文件夹");
            }
            
            return messageRepository.findByUserAndFolderAndIsDeletedFalseOrderByReceivedAtDesc(
                user, folder, null).getContent();
        } else {
            // 获取用户所有未删除的邮件
            return messageRepository.findByUserAndIsDeletedFalseOrderByReceivedAtDesc(
                user, null).getContent();
        }
    }
    
    /**
     * 解析规则条件JSON
     */
    private RuleCondition parseRuleCondition(String conditionsJson) {
        // 这里应该使用JSON解析库（如Jackson）来解析
        // 为了简化，这里返回一个示例条件
        RuleCondition condition = new RuleCondition();
        condition.setField("subject");
        condition.setOperator("contains");
        condition.setValue("重要");
        return condition;
    }
    
    // 内部数据类
    
    /**
     * 规则条件
     */
    public static class RuleCondition {
        private String field;
        private String operator;
        private String value;
        private List<RuleCondition> conditions;
        
        // Getters and Setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public List<RuleCondition> getConditions() { return conditions; }
        public void setConditions(List<RuleCondition> conditions) { this.conditions = conditions; }
    }
    
    /**
     * 批量处理请求
     */
    public static class EmailRuleBatchRequest {
        private Long folderId;
        private LocalDateTime dateFrom;
        private LocalDateTime dateTo;
        private Boolean includeRead;
        
        // Getters and Setters
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public LocalDateTime getDateFrom() { return dateFrom; }
        public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }
        
        public LocalDateTime getDateTo() { return dateTo; }
        public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }
        
        public Boolean getIncludeRead() { return includeRead; }
        public void setIncludeRead(Boolean includeRead) { this.includeRead = includeRead; }
    }
    
    /**
     * 批量处理结果
     */
    public static class RuleBatchResult {
        private final int processedCount;
        private final int successCount;
        private final int errorCount;
        private final List<String> errors;
        
        public RuleBatchResult(int processedCount, int successCount, int errorCount, List<String> errors) {
            this.processedCount = processedCount;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.errors = errors;
        }
        
        // Getters
        public int getProcessedCount() { return processedCount; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public List<String> getErrors() { return errors; }
    }
}