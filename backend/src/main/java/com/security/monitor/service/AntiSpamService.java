package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 反垃圾邮件服务
 * 提供类似Rspamd的邮件过滤和垃圾邮件检测功能
 */
@Service
@Transactional
public class AntiSpamService {
    
    private static final Logger logger = LoggerFactory.getLogger(AntiSpamService.class);
    
    @Autowired
    private SpamFilterRuleRepository ruleRepository;
    
    @Autowired
    private SpamDetectionLogRepository detectionLogRepository;
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private DnsBlacklistRepository dnsBlacklistRepository;
    
    @Value("${app.antispam.score.threshold:5.0}")
    private double spamScoreThreshold;
    
    @Value("${app.antispam.enabled:true}")
    private boolean antiSpamEnabled;
    
    @Value("${app.antispam.learn.enabled:true}")
    private boolean learningEnabled;
    
    // 垃圾邮件特征关键词
    private static final List<String> SPAM_KEYWORDS = Arrays.asList(
        "免费", "优惠", "特价", "限时", "立即", "马上", "赚钱", "发财", 
        "lottery", "winner", "congratulations", "urgent", "act now",
        "click here", "limited time", "make money", "free money"
    );
    
    // 可疑发件人模式
    private static final List<Pattern> SUSPICIOUS_SENDER_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\d{6,}.*@.*", Pattern.CASE_INSENSITIVE), // 包含6位以上数字
        Pattern.compile(".*noreply.*@.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*no-reply.*@.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*[a-z]{20,}.*@.*", Pattern.CASE_INSENSITIVE) // 超长随机字符
    );
    
    /**
     * 检测邮件是否为垃圾邮件
     */
    public SpamDetectionResult detectSpam(EmailMessage message) {
        if (!antiSpamEnabled) {
            return new SpamDetectionResult(false, 0.0, "反垃圾邮件功能已禁用");
        }
        
        logger.debug("检测邮件垃圾邮件特征: messageId={}", message.getMessageId());
        
        SpamDetectionResult result = new SpamDetectionResult();
        double totalScore = 0.0;
        List<String> reasons = new ArrayList<>();
        
        try {
            // 1. 检查发件人
            double senderScore = checkSender(message.getFromAddress());
            if (senderScore > 0) {
                totalScore += senderScore;
                reasons.add(String.format("可疑发件人 (+%.1f)", senderScore));
            }
            
            // 2. 检查主题
            double subjectScore = checkSubject(message.getSubject());
            if (subjectScore > 0) {
                totalScore += subjectScore;
                reasons.add(String.format("可疑主题 (+%.1f)", subjectScore));
            }
            
            // 3. 检查邮件内容
            double contentScore = checkContent(message.getBodyText(), message.getBodyHtml());
            if (contentScore > 0) {
                totalScore += contentScore;
                reasons.add(String.format("可疑内容 (+%.1f)", contentScore));
            }
            
            // 4. 检查URL
            double urlScore = checkUrls(message.getBodyText(), message.getBodyHtml());
            if (urlScore > 0) {
                totalScore += urlScore;
                reasons.add(String.format("可疑链接 (+%.1f)", urlScore));
            }
            
            // 5. 检查发件人IP（如果有）
            double ipScore = checkSenderIP(message);
            if (ipScore > 0) {
                totalScore += ipScore;
                reasons.add(String.format("可疑IP (+%.1f)", ipScore));
            }
            
            // 6. 检查附件
            double attachmentScore = checkAttachments(message);
            if (attachmentScore > 0) {
                totalScore += attachmentScore;
                reasons.add(String.format("可疑附件 (+%.1f)", attachmentScore));
            }
            
            // 7. 应用自定义规则
            double customScore = applyCustomRules(message);
            if (customScore > 0) {
                totalScore += customScore;
                reasons.add(String.format("自定义规则 (+%.1f)", customScore));
            }
            
            result.setSpamScore(totalScore);
            result.setIsSpam(totalScore >= spamScoreThreshold);
            result.setDetails(String.join("; ", reasons));
            
            // 记录检测日志
            logDetection(message, result);
            
            logger.debug("垃圾邮件检测完成: score={}, isSpam={}", totalScore, result.isSpam());
            
        } catch (Exception e) {
            logger.error("垃圾邮件检测失败", e);
            result.setSpamScore(0.0);
            result.setIsSpam(false);
            result.setDetails("检测失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查发件人
     */
    private double checkSender(String fromAddress) {
        if (fromAddress == null || fromAddress.trim().isEmpty()) {
            return 2.0; // 没有发件人地址
        }
        
        double score = 0.0;
        String lowerAddress = fromAddress.toLowerCase();
        
        // 检查可疑模式
        for (Pattern pattern : SUSPICIOUS_SENDER_PATTERNS) {
            if (pattern.matcher(lowerAddress).matches()) {
                score += 1.5;
                break;
            }
        }
        
        // 检查临时邮箱域名
        String[] tempDomains = {"10minutemail.com", "tempmail.org", "guerrillamail.com"};
        for (String domain : tempDomains) {
            if (lowerAddress.contains(domain)) {
                score += 3.0;
                break;
            }
        }
        
        // 检查是否包含过多数字
        long digitCount = lowerAddress.chars().filter(Character::isDigit).count();
        if (digitCount > 8) {
            score += 1.0;
        }
        
        return score;
    }
    
    /**
     * 检查主题
     */
    private double checkSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return 1.0; // 没有主题
        }
        
        double score = 0.0;
        String lowerSubject = subject.toLowerCase();
        
        // 检查垃圾邮件关键词
        for (String keyword : SPAM_KEYWORDS) {
            if (lowerSubject.contains(keyword.toLowerCase())) {
                score += 0.5;
            }
        }
        
        // 检查过多的大写字母
        long upperCount = subject.chars().filter(Character::isUpperCase).count();
        double upperRatio = (double) upperCount / subject.length();
        if (upperRatio > 0.7) {
            score += 2.0;
        }
        
        // 检查过多的感叹号或问号
        long exclamationCount = subject.chars().filter(ch -> ch == '!' || ch == '?').count();
        if (exclamationCount > 3) {
            score += 1.0;
        }
        
        // 检查是否包含"Re:"但实际不是回复
        if (lowerSubject.startsWith("re:") || lowerSubject.startsWith("fwd:")) {
            // 这里可以检查是否真的是回复邮件
            // 简化处理，暂不实现
        }
        
        return Math.min(score, 5.0); // 限制最大分数
    }
    
    /**
     * 检查邮件内容
     */
    private double checkContent(String bodyText, String bodyHtml) {
        double score = 0.0;
        
        String content = "";
        if (bodyHtml != null && !bodyHtml.trim().isEmpty()) {
            content = bodyHtml.replaceAll("<[^>]*>", ""); // 移除HTML标签
        } else if (bodyText != null) {
            content = bodyText;
        }
        
        if (content.trim().isEmpty()) {
            return 1.0; // 空内容
        }
        
        String lowerContent = content.toLowerCase();
        
        // 检查垃圾邮件关键词
        for (String keyword : SPAM_KEYWORDS) {
            if (lowerContent.contains(keyword.toLowerCase())) {
                score += 0.3;
            }
        }
        
        // 检查过多的大写字母
        if (content.length() > 50) {
            long upperCount = content.chars().filter(Character::isUpperCase).count();
            double upperRatio = (double) upperCount / content.length();
            if (upperRatio > 0.5) {
                score += 1.5;
            }
        }
        
        // 检查重复内容
        if (hasRepetitiveContent(content)) {
            score += 1.0;
        }
        
        // 检查可疑短语
        String[] suspiciousPhrases = {
            "click here", "act now", "limited time", "urgent", "congratulations",
            "you have won", "claim now", "free trial", "no obligation"
        };
        
        for (String phrase : suspiciousPhrases) {
            if (lowerContent.contains(phrase)) {
                score += 0.8;
            }
        }
        
        return Math.min(score, 5.0);
    }
    
    /**
     * 检查URL
     */
    private double checkUrls(String bodyText, String bodyHtml) {
        double score = 0.0;
        
        String content = bodyHtml != null ? bodyHtml : (bodyText != null ? bodyText : "");
        
        // 简单的URL匹配
        Pattern urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");
        java.util.regex.Matcher matcher = urlPattern.matcher(content);
        
        int urlCount = 0;
        while (matcher.find()) {
            urlCount++;
            String url = matcher.group();
            
            // 检查短链接服务
            if (url.contains("bit.ly") || url.contains("tinyurl.com") || 
                url.contains("t.co") || url.contains("goo.gl")) {
                score += 1.0;
            }
            
            // 检查可疑域名
            if (url.contains(".tk") || url.contains(".ml") || url.contains(".ga")) {
                score += 1.5;
            }
            
            // 检查IP地址链接
            if (url.matches(".*://\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
                score += 2.0;
            }
        }
        
        // 过多的链接
        if (urlCount > 5) {
            score += 1.0;
        }
        
        return Math.min(score, 4.0);
    }
    
    /**
     * 检查发件人IP
     */
    private double checkSenderIP(EmailMessage message) {
        // 这里需要从邮件头中提取发件人IP
        // 然后检查DNSBL黑名单
        // 简化实现
        return 0.0;
    }
    
    /**
     * 检查附件
     */
    private double checkAttachments(EmailMessage message) {
        double score = 0.0;
        
        // 这里需要检查附件类型和内容
        // 简化实现，检查附件数量
        if (message.getAttachments() != null) {
            int attachmentCount = message.getAttachments().size();
            if (attachmentCount > 5) {
                score += 1.0;
            }
            
            // 检查可疑文件类型
            for (EmailAttachment attachment : message.getAttachments()) {
                String filename = attachment.getFilename().toLowerCase();
                if (filename.endsWith(".exe") || filename.endsWith(".scr") || 
                    filename.endsWith(".bat") || filename.endsWith(".com")) {
                    score += 3.0;
                }
            }
        }
        
        return score;
    }
    
    /**
     * 应用自定义规则
     */
    private double applyCustomRules(EmailMessage message) {
        double score = 0.0;
        
        List<SpamFilterRule> rules = ruleRepository.findActiveRules();
        
        for (SpamFilterRule rule : rules) {
            if (evaluateRule(rule, message)) {
                score += rule.getScoreModifier();
            }
        }
        
        return score;
    }
    
    /**
     * 评估自定义规则
     */
    private boolean evaluateRule(SpamFilterRule rule, EmailMessage message) {
        try {
            String field = rule.getFieldName();
            String pattern = rule.getPattern();
            String operator = rule.getOperator();
            
            String fieldValue = getMessageFieldValue(message, field);
            if (fieldValue == null) fieldValue = "";
            
            switch (operator.toLowerCase()) {
                case "contains":
                    return fieldValue.toLowerCase().contains(pattern.toLowerCase());
                case "equals":
                    return fieldValue.equalsIgnoreCase(pattern);
                case "regex":
                    return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(fieldValue).find();
                case "not_contains":
                    return !fieldValue.toLowerCase().contains(pattern.toLowerCase());
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.warn("规则评估失败: {}", rule.getRuleName(), e);
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
                return message.getFromAddress();
            case "to":
                return message.getToAddresses();
            case "body":
                return message.getBodyText();
            case "body_html":
                return message.getBodyHtml();
            default:
                return "";
        }
    }
    
    /**
     * 检查重复内容
     */
    private boolean hasRepetitiveContent(String content) {
        if (content.length() < 100) return false;
        
        String[] words = content.split("\\s+");
        Map<String, Integer> wordCount = new HashMap<>();
        
        for (String word : words) {
            if (word.length() > 3) {
                wordCount.put(word.toLowerCase(), wordCount.getOrDefault(word.toLowerCase(), 0) + 1);
            }
        }
        
        // 检查是否有单词重复超过10次
        return wordCount.values().stream().anyMatch(count -> count > 10);
    }
    
    /**
     * 记录检测日志
     */
    private void logDetection(EmailMessage message, SpamDetectionResult result) {
        try {
            SpamDetectionLog log = new SpamDetectionLog();
            log.setMessage(message);
            log.setSpamScore(result.getSpamScore());
            log.setIsSpam(result.isSpam());
            log.setDetectionDetails(result.getDetails());
            log.setDetectedAt(LocalDateTime.now());
            
            detectionLogRepository.save(log);
        } catch (Exception e) {
            logger.error("保存垃圾邮件检测日志失败", e);
        }
    }
    
    /**
     * 学习垃圾邮件（用户标记）
     */
    @Transactional
    public void learnSpam(EmailMessage message, boolean isSpam) {
        if (!learningEnabled) {
            return;
        }
        
        logger.info("学习垃圾邮件: messageId={}, isSpam={}", message.getMessageId(), isSpam);
        
        // 更新消息的垃圾邮件状态
        message.setIsSpam(isSpam);
        messageRepository.save(message);
        
        // 这里可以实现机器学习算法来改进检测
        // 简化实现，仅记录用户反馈
        try {
            SpamDetectionLog log = new SpamDetectionLog();
            log.setMessage(message);
            log.setIsSpam(isSpam);
            log.setDetectionDetails("用户标记: " + (isSpam ? "垃圾邮件" : "正常邮件"));
            log.setDetectedAt(LocalDateTime.now());
            log.setIsUserFeedback(true);
            
            detectionLogRepository.save(log);
        } catch (Exception e) {
            logger.error("保存用户反馈失败", e);
        }
    }
    
    /**
     * 获取垃圾邮件统计
     */
    public SpamStatistics getSpamStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        long totalMessages = detectionLogRepository.countByDetectedAtBetween(startDate, endDate);
        long spamMessages = detectionLogRepository.countByIsSpamTrueAndDetectedAtBetween(startDate, endDate);
        long falsePositives = detectionLogRepository.countFalsePositives(startDate, endDate);
        long falseNegatives = detectionLogRepository.countFalseNegatives(startDate, endDate);
        
        SpamStatistics stats = new SpamStatistics();
        stats.setTotalMessages(totalMessages);
        stats.setSpamMessages(spamMessages);
        stats.setHamMessages(totalMessages - spamMessages);
        stats.setFalsePositives(falsePositives);
        stats.setFalseNegatives(falseNegatives);
        
        if (totalMessages > 0) {
            stats.setSpamRate((double) spamMessages / totalMessages * 100);
            stats.setAccuracy((double) (totalMessages - falsePositives - falseNegatives) / totalMessages * 100);
        }
        
        return stats;
    }
    
    // 内部数据类
    
    /**
     * 垃圾邮件检测结果
     */
    public static class SpamDetectionResult {
        private boolean isSpam;
        private double spamScore;
        private String details;
        
        public SpamDetectionResult() {}
        
        public SpamDetectionResult(boolean isSpam, double spamScore, String details) {
            this.isSpam = isSpam;
            this.spamScore = spamScore;
            this.details = details;
        }
        
        // Getters and Setters
        public boolean isSpam() { return isSpam; }
        public void setIsSpam(boolean isSpam) { this.isSpam = isSpam; }
        
        public double getSpamScore() { return spamScore; }
        public void setSpamScore(double spamScore) { this.spamScore = spamScore; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
    
    /**
     * 垃圾邮件统计
     */
    public static class SpamStatistics {
        private long totalMessages;
        private long spamMessages;
        private long hamMessages;
        private long falsePositives;
        private long falseNegatives;
        private double spamRate;
        private double accuracy;
        
        // Getters and Setters
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        
        public long getSpamMessages() { return spamMessages; }
        public void setSpamMessages(long spamMessages) { this.spamMessages = spamMessages; }
        
        public long getHamMessages() { return hamMessages; }
        public void setHamMessages(long hamMessages) { this.hamMessages = hamMessages; }
        
        public long getFalsePositives() { return falsePositives; }
        public void setFalsePositives(long falsePositives) { this.falsePositives = falsePositives; }
        
        public long getFalseNegatives() { return falseNegatives; }
        public void setFalseNegatives(long falseNegatives) { this.falseNegatives = falseNegatives; }
        
        public double getSpamRate() { return spamRate; }
        public void setSpamRate(double spamRate) { this.spamRate = spamRate; }
        
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    }
}