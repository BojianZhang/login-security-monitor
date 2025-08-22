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

/**
 * 自动回复服务
 */
@Service
@Transactional
public class AutoReplyService {
    
    private static final Logger logger = LoggerFactory.getLogger(AutoReplyService.class);
    
    @Autowired
    private AutoReplySettingsRepository autoReplySettingsRepository;
    
    @Autowired
    private AutoReplyHistoryRepository autoReplyHistoryRepository;
    
    @Autowired
    private EmailAliasRepository aliasRepository;
    
    @Autowired
    private EmailSendService emailSendService;
    
    /**
     * 为指定别名创建或更新自动回复设置
     */
    public AutoReplySettings createOrUpdateAutoReplySettings(User user, Long aliasId, AutoReplyRequest request) {
        // 验证别名所有权
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        EmailAlias alias = aliasOpt.get();
        
        // 查找现有设置或创建新的
        Optional<AutoReplySettings> existingOpt = autoReplySettingsRepository.findByAlias(alias);
        AutoReplySettings settings = existingOpt.orElse(new AutoReplySettings(alias));
        
        // 更新设置
        settings.setIsEnabled(request.getIsEnabled());
        settings.setReplySubject(request.getReplySubject());
        settings.setReplyContent(request.getReplyContent());
        settings.setStartDate(request.getStartDate());
        settings.setEndDate(request.getEndDate());
        settings.setOnlyExternal(request.getOnlyExternal());
        settings.setMaxRepliesPerSender(request.getMaxRepliesPerSender());
        
        AutoReplySettings saved = autoReplySettingsRepository.save(settings);
        
        logger.info("自动回复设置已保存: 别名={}, 启用={}", alias.getFullEmail(), saved.getIsEnabled());
        return saved;
    }
    
    /**
     * 获取指定别名的自动回复设置
     */
    @Transactional(readOnly = true)
    public Optional<AutoReplySettings> getAutoReplySettings(User user, Long aliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        return autoReplySettingsRepository.findByAlias(aliasOpt.get());
    }
    
    /**
     * 删除自动回复设置
     */
    public void deleteAutoReplySettings(User user, Long aliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        Optional<AutoReplySettings> settingsOpt = autoReplySettingsRepository.findByAlias(aliasOpt.get());
        if (settingsOpt.isPresent()) {
            autoReplySettingsRepository.delete(settingsOpt.get());
            logger.info("自动回复设置已删除: 别名={}", aliasOpt.get().getFullEmail());
        }
    }
    
    /**
     * 处理邮件自动回复（在收到邮件时调用）
     */
    public void processAutoReply(EmailMessage incomingMessage) {
        try {
            // 解析收件人地址，获取对应的别名
            String toAddress = incomingMessage.getToAddresses();
            Optional<EmailAlias> aliasOpt = aliasRepository.findByFullEmailAddress(toAddress);
            
            if (aliasOpt.isEmpty()) {
                logger.debug("未找到别名，跳过自动回复: {}", toAddress);
                return;
            }
            
            EmailAlias alias = aliasOpt.get();
            
            // 获取自动回复设置
            Optional<AutoReplySettings> settingsOpt = autoReplySettingsRepository.findByAlias(alias);
            if (settingsOpt.isEmpty()) {
                logger.debug("无自动回复设置，跳过: {}", toAddress);
                return;
            }
            
            AutoReplySettings settings = settingsOpt.get();
            
            // 检查是否应该回复
            if (!settings.shouldReplyTo(incomingMessage.getFromAddress())) {
                logger.debug("不符合自动回复条件，跳过: 发件人={}, 收件人={}", 
                    incomingMessage.getFromAddress(), toAddress);
                return;
            }
            
            // 检查是否已经达到回复次数限制
            long replyCount = autoReplyHistoryRepository.countBySettingsAndFromAddress(
                settings, incomingMessage.getFromAddress());
            
            if (replyCount >= settings.getMaxRepliesPerSender()) {
                logger.debug("已达到最大回复次数限制，跳过: 发件人={}, 已回复={}次", 
                    incomingMessage.getFromAddress(), replyCount);
                return;
            }
            
            // 创建回复历史记录
            AutoReplyHistory history = new AutoReplyHistory(
                settings,
                incomingMessage.getFromAddress(),
                toAddress,
                incomingMessage.getSubject(),
                settings.getReplySubject()
            );
            
            try {
                // 发送自动回复邮件
                EmailSendService.EmailSendRequest replyRequest = new EmailSendService.EmailSendRequest(
                    toAddress, // 从收件人地址发出
                    incomingMessage.getFromAddress(), // 发给原发件人
                    settings.getReplySubject(),
                    settings.getReplyContent(),
                    null // 纯文本回复
                );
                
                boolean sent = emailSendService.sendEmail(replyRequest);
                
                history.setReplySent(sent);
                if (!sent) {
                    history.setReplyError("邮件发送失败");
                }
                
                logger.info("自动回复处理完成: 发件人={}, 收件人={}, 发送={}", 
                    incomingMessage.getFromAddress(), toAddress, sent);
                
            } catch (Exception e) {
                history.setReplySent(false);
                history.setReplyError("发送异常: " + e.getMessage());
                logger.error("自动回复发送失败", e);
            }
            
            // 保存历史记录
            autoReplyHistoryRepository.save(history);
            
        } catch (Exception e) {
            logger.error("自动回复处理异常", e);
        }
    }
    
    /**
     * 获取用户的所有自动回复设置
     */
    @Transactional(readOnly = true)
    public List<AutoReplySettings> getUserAutoReplySettings(User user) {
        return autoReplySettingsRepository.findByAliasUser(user);
    }
    
    /**
     * 获取指定设置的回复历史
     */
    @Transactional(readOnly = true)
    public List<AutoReplyHistory> getAutoReplyHistory(User user, Long settingsId) {
        Optional<AutoReplySettings> settingsOpt = autoReplySettingsRepository.findById(settingsId);
        if (settingsOpt.isEmpty() || !settingsOpt.get().getAlias().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("设置不存在或无权限");
        }
        
        return autoReplyHistoryRepository.findBySettingsOrderByCreatedAtDesc(settingsOpt.get());
    }
    
    /**
     * 清理指定天数前的回复历史
     */
    public void cleanupOldHistory(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<AutoReplyHistory> oldHistory = autoReplyHistoryRepository.findByCreatedAtBefore(cutoffDate);
        
        if (!oldHistory.isEmpty()) {
            autoReplyHistoryRepository.deleteAll(oldHistory);
            logger.info("已清理 {} 条旧的自动回复历史记录", oldHistory.size());
        }
    }
    
    // 内部请求类
    public static class AutoReplyRequest {
        private Boolean isEnabled = false;
        private String replySubject;
        private String replyContent;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean onlyExternal = false;
        private Integer maxRepliesPerSender = 1;
        
        // Getters and Setters
        public Boolean getIsEnabled() { return isEnabled; }
        public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
        public String getReplySubject() { return replySubject; }
        public void setReplySubject(String replySubject) { this.replySubject = replySubject; }
        public String getReplyContent() { return replyContent; }
        public void setReplyContent(String replyContent) { this.replyContent = replyContent; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public Boolean getOnlyExternal() { return onlyExternal; }
        public void setOnlyExternal(Boolean onlyExternal) { this.onlyExternal = onlyExternal; }
        public Integer getMaxRepliesPerSender() { return maxRepliesPerSender; }
        public void setMaxRepliesPerSender(Integer maxRepliesPerSender) { this.maxRepliesPerSender = maxRepliesPerSender; }
    }
}