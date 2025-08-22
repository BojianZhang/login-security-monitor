package com.security.monitor.service;

import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.SieveFilter;
import com.security.monitor.model.SieveFilterLog;
import com.security.monitor.model.User;
import com.security.monitor.repository.SieveFilterRepository;
import com.security.monitor.repository.SieveFilterLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Sieve邮件过滤服务
 * 实现RFC 5228标准的Sieve邮件过滤语言
 */
@Service
@Transactional
public class SieveFilterService {
    
    private static final Logger logger = LoggerFactory.getLogger(SieveFilterService.class);
    
    @Autowired
    private SieveFilterRepository filterRepository;
    
    @Autowired
    private SieveFilterLogRepository filterLogRepository;
    
    // Sieve命令模式
    private static final Pattern IF_PATTERN = Pattern.compile("if\\s+(.+?)\\s*\\{", Pattern.CASE_INSENSITIVE);
    private static final Pattern ELSIF_PATTERN = Pattern.compile("elsif\\s+(.+?)\\s*\\{", Pattern.CASE_INSENSITIVE);
    private static final Pattern ELSE_PATTERN = Pattern.compile("else\\s*\\{", Pattern.CASE_INSENSITIVE);
    private static final Pattern REQUIRE_PATTERN = Pattern.compile("require\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    
    // Sieve测试模式
    private static final Pattern HEADER_PATTERN = Pattern.compile("header\\s+(?:(\\S+)\\s+)?\"([^\"]+)\"\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("address\\s+(?:(\\S+)\\s+)?\"([^\"]+)\"\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENVELOPE_PATTERN = Pattern.compile("envelope\\s+(?:(\\S+)\\s+)?\"([^\"]+)\"\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY_PATTERN = Pattern.compile("body\\s+(?:(\\S+)\\s+)?\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIZE_PATTERN = Pattern.compile("size\\s+:(over|under)\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXISTS_PATTERN = Pattern.compile("exists\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    
    // Sieve动作模式
    private static final Pattern KEEP_PATTERN = Pattern.compile("keep", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCARD_PATTERN = Pattern.compile("discard", Pattern.CASE_INSENSITIVE);
    private static final Pattern REDIRECT_PATTERN = Pattern.compile("redirect\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILEINTO_PATTERN = Pattern.compile("fileinto\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern REJECT_PATTERN = Pattern.compile("reject\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern VACATION_PATTERN = Pattern.compile("vacation\\s+(?::days\\s+(\\d+)\\s+)?(?::subject\\s+\"([^\"]+)\"\\s+)?\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    
    /**
     * 对邮件应用Sieve过滤器
     */
    public SieveFilterResult applyFilters(EmailMessage message, User user) {
        logger.info("开始应用Sieve过滤器: messageId={}, user={}", 
            message.getMessageId(), user.getUsername());
        
        SieveFilterResult result = new SieveFilterResult();
        result.setMessageId(message.getMessageId());
        result.setUserId(user.getId());
        result.setProcessingStartTime(LocalDateTime.now());
        
        List<SieveFilter> activeFilters = filterRepository.findByUserAndIsActiveOrderByPriority(user, true);
        
        for (SieveFilter filter : activeFilters) {
            try {
                FilterExecutionResult executionResult = executeFilter(message, filter);
                result.getExecutionResults().add(executionResult);
                
                // 记录过滤器执行日志
                logFilterExecution(message, filter, executionResult);
                
                // 如果过滤器执行了终止动作，停止后续过滤器
                if (executionResult.isTerminating()) {
                    result.setTerminatedBy(filter.getFilterName());
                    break;
                }
                
            } catch (Exception e) {
                logger.error("执行Sieve过滤器失败: filterId={}", filter.getId(), e);
                
                FilterExecutionResult errorResult = new FilterExecutionResult();
                errorResult.setFilterId(filter.getId());
                errorResult.setFilterName(filter.getFilterName());
                errorResult.setMatched(false);
                errorResult.setError(true);
                errorResult.setErrorMessage(e.getMessage());
                
                result.getExecutionResults().add(errorResult);
            }
        }
        
        result.setProcessingEndTime(LocalDateTime.now());
        result.setProcessingTimeMs(calculateProcessingTime(result));
        
        logger.info("Sieve过滤器处理完成: messageId={}, 执行了{}个过滤器", 
            message.getMessageId(), result.getExecutionResults().size());
        
        return result;
    }
    
    /**
     * 执行单个过滤器
     */
    private FilterExecutionResult executeFilter(EmailMessage message, SieveFilter filter) {
        FilterExecutionResult result = new FilterExecutionResult();
        result.setFilterId(filter.getId());
        result.setFilterName(filter.getFilterName());
        result.setExecutionStartTime(LocalDateTime.now());
        
        try {
            // 解析并执行Sieve脚本
            SieveScript script = parseSieveScript(filter.getFilterScript());
            SieveExecutionContext context = new SieveExecutionContext(message);
            
            boolean matched = executeSieveScript(script, context);
            result.setMatched(matched);
            
            if (matched) {
                result.setAction(context.getExecutedAction());
                result.setActionParameters(context.getActionParameters());
                result.setTerminating(context.isTerminating());
                
                // 更新过滤器统计
                updateFilterStatistics(filter);
            }
            
        } catch (Exception e) {
            result.setError(true);
            result.setErrorMessage(e.getMessage());
            logger.error("执行Sieve脚本失败: filterId={}", filter.getId(), e);
        } finally {
            result.setExecutionEndTime(LocalDateTime.now());
            result.setExecutionTimeMs(calculateExecutionTime(result));
        }
        
        return result;
    }
    
    /**
     * 解析Sieve脚本
     */
    private SieveScript parseSieveScript(String scriptContent) {
        SieveScript script = new SieveScript();
        String[] lines = scriptContent.split("\n");
        
        List<String> requirements = new ArrayList<>();
        List<SieveRule> rules = new ArrayList<>();
        
        StringBuilder currentRule = new StringBuilder();
        boolean inRule = false;
        int braceLevel = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            // 跳过注释和空行
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // 处理require语句
            Matcher requireMatcher = REQUIRE_PATTERN.matcher(line);
            if (requireMatcher.find()) {
                requirements.add(requireMatcher.group(1));
                continue;
            }
            
            // 处理规则
            if (line.contains("if") || line.contains("elsif") || line.contains("else")) {
                if (inRule && braceLevel == 0) {
                    // 保存前一个规则
                    rules.add(parseRule(currentRule.toString()));
                    currentRule = new StringBuilder();
                }
                inRule = true;
            }
            
            if (inRule) {
                currentRule.append(line).append("\n");
                
                // 统计大括号层级
                for (char c : line.toCharArray()) {
                    if (c == '{') braceLevel++;
                    if (c == '}') braceLevel--;
                }
                
                // 规则结束
                if (braceLevel == 0 && line.contains("}")) {
                    rules.add(parseRule(currentRule.toString()));
                    currentRule = new StringBuilder();
                    inRule = false;
                }
            }
        }
        
        // 处理最后一个规则
        if (currentRule.length() > 0) {
            rules.add(parseRule(currentRule.toString()));
        }
        
        script.setRequirements(requirements);
        script.setRules(rules);
        
        return script;
    }
    
    /**
     * 解析单个规则
     */
    private SieveRule parseRule(String ruleContent) {
        SieveRule rule = new SieveRule();
        
        // 提取条件
        List<SieveCondition> conditions = parseConditions(ruleContent);
        rule.setConditions(conditions);
        
        // 提取动作
        List<SieveAction> actions = parseActions(ruleContent);
        rule.setActions(actions);
        
        return rule;
    }
    
    /**
     * 解析条件
     */
    private List<SieveCondition> parseConditions(String content) {
        List<SieveCondition> conditions = new ArrayList<>();
        
        // 解析header测试
        Matcher headerMatcher = HEADER_PATTERN.matcher(content);
        while (headerMatcher.find()) {
            SieveCondition condition = new SieveCondition();
            condition.setType("header");
            condition.setComparator(headerMatcher.group(1) != null ? headerMatcher.group(1) : "is");
            condition.setHeaderName(headerMatcher.group(2));
            condition.setValue(headerMatcher.group(3));
            conditions.add(condition);
        }
        
        // 解析address测试
        Matcher addressMatcher = ADDRESS_PATTERN.matcher(content);
        while (addressMatcher.find()) {
            SieveCondition condition = new SieveCondition();
            condition.setType("address");
            condition.setComparator(addressMatcher.group(1) != null ? addressMatcher.group(1) : "is");
            condition.setHeaderName(addressMatcher.group(2));
            condition.setValue(addressMatcher.group(3));
            conditions.add(condition);
        }
        
        // 解析body测试
        Matcher bodyMatcher = BODY_PATTERN.matcher(content);
        while (bodyMatcher.find()) {
            SieveCondition condition = new SieveCondition();
            condition.setType("body");
            condition.setComparator(bodyMatcher.group(1) != null ? bodyMatcher.group(1) : "contains");
            condition.setValue(bodyMatcher.group(2));
            conditions.add(condition);
        }
        
        // 解析size测试
        Matcher sizeMatcher = SIZE_PATTERN.matcher(content);
        if (sizeMatcher.find()) {
            SieveCondition condition = new SieveCondition();
            condition.setType("size");
            condition.setComparator(sizeMatcher.group(1));
            condition.setValue(sizeMatcher.group(2));
            conditions.add(condition);
        }
        
        // 解析exists测试
        Matcher existsMatcher = EXISTS_PATTERN.matcher(content);
        while (existsMatcher.find()) {
            SieveCondition condition = new SieveCondition();
            condition.setType("exists");
            condition.setHeaderName(existsMatcher.group(1));
            conditions.add(condition);
        }
        
        return conditions;
    }
    
    /**
     * 解析动作
     */
    private List<SieveAction> parseActions(String content) {
        List<SieveAction> actions = new ArrayList<>();
        
        // 解析各种动作
        if (KEEP_PATTERN.matcher(content).find()) {
            actions.add(new SieveAction("keep", null));
        }
        
        if (DISCARD_PATTERN.matcher(content).find()) {
            actions.add(new SieveAction("discard", null));
        }
        
        Matcher redirectMatcher = REDIRECT_PATTERN.matcher(content);
        if (redirectMatcher.find()) {
            actions.add(new SieveAction("redirect", redirectMatcher.group(1)));
        }
        
        Matcher fileintoMatcher = FILEINTO_PATTERN.matcher(content);
        if (fileintoMatcher.find()) {
            actions.add(new SieveAction("fileinto", fileintoMatcher.group(1)));
        }
        
        Matcher rejectMatcher = REJECT_PATTERN.matcher(content);
        if (rejectMatcher.find()) {
            actions.add(new SieveAction("reject", rejectMatcher.group(1)));
        }
        
        Matcher vacationMatcher = VACATION_PATTERN.matcher(content);
        if (vacationMatcher.find()) {
            Map<String, String> params = new HashMap<>();
            if (vacationMatcher.group(1) != null) {
                params.put("days", vacationMatcher.group(1));
            }
            if (vacationMatcher.group(2) != null) {
                params.put("subject", vacationMatcher.group(2));
            }
            params.put("message", vacationMatcher.group(3));
            
            SieveAction action = new SieveAction("vacation", null);
            action.setParameters(params);
            actions.add(action);
        }
        
        return actions;
    }
    
    /**
     * 执行Sieve脚本
     */
    private boolean executeSieveScript(SieveScript script, SieveExecutionContext context) {
        for (SieveRule rule : script.getRules()) {
            if (evaluateConditions(rule.getConditions(), context)) {
                executeActions(rule.getActions(), context);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 评估条件
     */
    private boolean evaluateConditions(List<SieveCondition> conditions, SieveExecutionContext context) {
        for (SieveCondition condition : conditions) {
            if (!evaluateCondition(condition, context)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 评估单个条件
     */
    private boolean evaluateCondition(SieveCondition condition, SieveExecutionContext context) {
        EmailMessage message = context.getMessage();
        
        switch (condition.getType()) {
            case "header":
                return evaluateHeaderCondition(condition, message);
            case "address":
                return evaluateAddressCondition(condition, message);
            case "body":
                return evaluateBodyCondition(condition, message);
            case "size":
                return evaluateSizeCondition(condition, message);
            case "exists":
                return evaluateExistsCondition(condition, message);
            default:
                return false;
        }
    }
    
    /**
     * 评估header条件
     */
    private boolean evaluateHeaderCondition(SieveCondition condition, EmailMessage message) {
        String headerValue = getHeaderValue(message, condition.getHeaderName());
        if (headerValue == null) return false;
        
        return compareValues(headerValue, condition.getValue(), condition.getComparator());
    }
    
    /**
     * 评估address条件
     */
    private boolean evaluateAddressCondition(SieveCondition condition, EmailMessage message) {
        String addressValue = getAddressValue(message, condition.getHeaderName());
        if (addressValue == null) return false;
        
        return compareValues(addressValue, condition.getValue(), condition.getComparator());
    }
    
    /**
     * 评估body条件
     */
    private boolean evaluateBodyCondition(SieveCondition condition, EmailMessage message) {
        String bodyContent = message.getBodyText() != null ? message.getBodyText() : "";
        if (message.getBodyHtml() != null) {
            bodyContent += " " + message.getBodyHtml();
        }
        
        return compareValues(bodyContent, condition.getValue(), condition.getComparator());
    }
    
    /**
     * 评估size条件
     */
    private boolean evaluateSizeCondition(SieveCondition condition, EmailMessage message) {
        long messageSize = message.getMessageSize() != null ? message.getMessageSize() : 0;
        long threshold = Long.parseLong(condition.getValue());
        
        if ("over".equals(condition.getComparator())) {
            return messageSize > threshold;
        } else if ("under".equals(condition.getComparator())) {
            return messageSize < threshold;
        }
        
        return false;
    }
    
    /**
     * 评估exists条件
     */
    private boolean evaluateExistsCondition(SieveCondition condition, EmailMessage message) {
        return getHeaderValue(message, condition.getHeaderName()) != null;
    }
    
    /**
     * 比较值
     */
    private boolean compareValues(String actual, String expected, String comparator) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        
        switch (comparator != null ? comparator : "is") {
            case "is":
                return actual.equalsIgnoreCase(expected);
            case "contains":
                return actual.toLowerCase().contains(expected.toLowerCase());
            case "matches":
                return actual.matches(expected);
            case "regex":
                return Pattern.compile(expected, Pattern.CASE_INSENSITIVE).matcher(actual).find();
            default:
                return actual.equalsIgnoreCase(expected);
        }
    }
    
    /**
     * 执行动作
     */
    private void executeActions(List<SieveAction> actions, SieveExecutionContext context) {
        for (SieveAction action : actions) {
            executeAction(action, context);
        }
    }
    
    /**
     * 执行单个动作
     */
    private void executeAction(SieveAction action, SieveExecutionContext context) {
        context.setExecutedAction(action.getType());
        context.setActionParameters(action.getParameters());
        
        switch (action.getType()) {
            case "keep":
                // 保持邮件在收件箱
                break;
            case "discard":
                // 丢弃邮件
                context.setTerminating(true);
                break;
            case "redirect":
                // 转发邮件
                context.setTerminating(true);
                break;
            case "fileinto":
                // 移动到指定文件夹
                break;
            case "reject":
                // 拒绝邮件
                context.setTerminating(true);
                break;
            case "vacation":
                // 自动回复
                break;
        }
    }
    
    // 辅助方法
    private String getHeaderValue(EmailMessage message, String headerName) {
        // 这里应该从邮件头中提取指定字段的值
        // 简化实现
        switch (headerName.toLowerCase()) {
            case "from": return message.getFromAddress();
            case "to": return message.getToAddress();
            case "subject": return message.getSubject();
            default: return null;
        }
    }
    
    private String getAddressValue(EmailMessage message, String headerName) {
        return getHeaderValue(message, headerName);
    }
    
    private void updateFilterStatistics(SieveFilter filter) {
        try {
            filterRepository.updateHitStatistics(filter.getId(), LocalDateTime.now());
        } catch (Exception e) {
            logger.error("更新过滤器统计失败", e);
        }
    }
    
    private void logFilterExecution(EmailMessage message, SieveFilter filter, FilterExecutionResult result) {
        try {
            SieveFilterLog log = new SieveFilterLog();
            log.setMessage(message);
            log.setFilter(filter);
            log.setFilterMatched(result.isMatched());
            log.setExecutedAction(result.getAction());
            log.setActionParameters(result.getActionParameters() != null ? 
                result.getActionParameters().toString() : null);
            log.setExecutionTimeMs(result.getExecutionTimeMs());
            log.setExecutedAt(LocalDateTime.now());
            
            filterLogRepository.save(log);
            
        } catch (Exception e) {
            logger.error("保存过滤器执行日志失败", e);
        }
    }
    
    private long calculateProcessingTime(SieveFilterResult result) {
        if (result.getProcessingStartTime() == null || result.getProcessingEndTime() == null) {
            return 0;
        }
        return java.time.Duration.between(result.getProcessingStartTime(), result.getProcessingEndTime()).toMillis();
    }
    
    private long calculateExecutionTime(FilterExecutionResult result) {
        if (result.getExecutionStartTime() == null || result.getExecutionEndTime() == null) {
            return 0;
        }
        return java.time.Duration.between(result.getExecutionStartTime(), result.getExecutionEndTime()).toMillis();
    }
    
    // 内部类
    public static class SieveFilterResult {
        private String messageId;
        private Long userId;
        private List<FilterExecutionResult> executionResults = new ArrayList<>();
        private String terminatedBy;
        private LocalDateTime processingStartTime;
        private LocalDateTime processingEndTime;
        private long processingTimeMs;
        
        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public List<FilterExecutionResult> getExecutionResults() { return executionResults; }
        public void setExecutionResults(List<FilterExecutionResult> executionResults) { this.executionResults = executionResults; }
        
        public String getTerminatedBy() { return terminatedBy; }
        public void setTerminatedBy(String terminatedBy) { this.terminatedBy = terminatedBy; }
        
        public LocalDateTime getProcessingStartTime() { return processingStartTime; }
        public void setProcessingStartTime(LocalDateTime processingStartTime) { this.processingStartTime = processingStartTime; }
        
        public LocalDateTime getProcessingEndTime() { return processingEndTime; }
        public void setProcessingEndTime(LocalDateTime processingEndTime) { this.processingEndTime = processingEndTime; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
    
    public static class FilterExecutionResult {
        private Long filterId;
        private String filterName;
        private boolean matched;
        private String action;
        private Map<String, String> actionParameters;
        private boolean terminating;
        private boolean error;
        private String errorMessage;
        private LocalDateTime executionStartTime;
        private LocalDateTime executionEndTime;
        private long executionTimeMs;
        
        // Getters and Setters
        public Long getFilterId() { return filterId; }
        public void setFilterId(Long filterId) { this.filterId = filterId; }
        
        public String getFilterName() { return filterName; }
        public void setFilterName(String filterName) { this.filterName = filterName; }
        
        public boolean isMatched() { return matched; }
        public void setMatched(boolean matched) { this.matched = matched; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public Map<String, String> getActionParameters() { return actionParameters; }
        public void setActionParameters(Map<String, String> actionParameters) { this.actionParameters = actionParameters; }
        
        public boolean isTerminating() { return terminating; }
        public void setTerminating(boolean terminating) { this.terminating = terminating; }
        
        public boolean isError() { return error; }
        public void setError(boolean error) { this.error = error; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getExecutionStartTime() { return executionStartTime; }
        public void setExecutionStartTime(LocalDateTime executionStartTime) { this.executionStartTime = executionStartTime; }
        
        public LocalDateTime getExecutionEndTime() { return executionEndTime; }
        public void setExecutionEndTime(LocalDateTime executionEndTime) { this.executionEndTime = executionEndTime; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    }
    
    // Sieve脚本相关类
    private static class SieveScript {
        private List<String> requirements = new ArrayList<>();
        private List<SieveRule> rules = new ArrayList<>();
        
        public List<String> getRequirements() { return requirements; }
        public void setRequirements(List<String> requirements) { this.requirements = requirements; }
        
        public List<SieveRule> getRules() { return rules; }
        public void setRules(List<SieveRule> rules) { this.rules = rules; }
    }
    
    private static class SieveRule {
        private List<SieveCondition> conditions = new ArrayList<>();
        private List<SieveAction> actions = new ArrayList<>();
        
        public List<SieveCondition> getConditions() { return conditions; }
        public void setConditions(List<SieveCondition> conditions) { this.conditions = conditions; }
        
        public List<SieveAction> getActions() { return actions; }
        public void setActions(List<SieveAction> actions) { this.actions = actions; }
    }
    
    private static class SieveCondition {
        private String type;
        private String comparator;
        private String headerName;
        private String value;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getComparator() { return comparator; }
        public void setComparator(String comparator) { this.comparator = comparator; }
        
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
    
    private static class SieveAction {
        private String type;
        private String value;
        private Map<String, String> parameters;
        
        public SieveAction(String type, String value) {
            this.type = type;
            this.value = value;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    }
    
    private static class SieveExecutionContext {
        private EmailMessage message;
        private String executedAction;
        private Map<String, String> actionParameters;
        private boolean terminating;
        
        public SieveExecutionContext(EmailMessage message) {
            this.message = message;
        }
        
        public EmailMessage getMessage() { return message; }
        public void setMessage(EmailMessage message) { this.message = message; }
        
        public String getExecutedAction() { return executedAction; }
        public void setExecutedAction(String executedAction) { this.executedAction = executedAction; }
        
        public Map<String, String> getActionParameters() { return actionParameters; }
        public void setActionParameters(Map<String, String> actionParameters) { this.actionParameters = actionParameters; }
        
        public boolean isTerminating() { return terminating; }
        public void setTerminating(boolean terminating) { this.terminating = terminating; }
    }
}