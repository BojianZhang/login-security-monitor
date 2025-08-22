package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.EmailGroupRepository;
import com.security.monitor.repository.EmailGroupMemberRepository;
import com.security.monitor.repository.CatchAllMailboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 群组邮箱和Catch-All邮箱管理服务
 */
@Service
@Transactional
public class GroupMailboxService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupMailboxService.class);
    
    @Autowired
    private EmailGroupRepository groupRepository;
    
    @Autowired
    private EmailGroupMemberRepository memberRepository;
    
    @Autowired
    private CatchAllMailboxRepository catchAllRepository;
    
    @Autowired
    private EmailSendService emailSendService;
    
    /**
     * 处理发送到群组的邮件
     */
    public GroupDeliveryResult processGroupMessage(EmailMessage message, String recipientAddress) {
        logger.info("处理群组邮件: messageId={}, recipient={}", 
            message.getMessageId(), recipientAddress);
        
        GroupDeliveryResult result = new GroupDeliveryResult();
        result.setMessageId(message.getMessageId());
        result.setRecipientAddress(recipientAddress);
        result.setProcessingStartTime(LocalDateTime.now());
        
        try {
            // 查找目标群组
            EmailGroup group = groupRepository.findByGroupEmailAndIsActive(recipientAddress, true);
            
            if (group == null) {
                result.setDeliveryStatus(DeliveryStatus.GROUP_NOT_FOUND);
                result.setErrorMessage("群组不存在或已禁用");
                return result;
            }
            
            // 检查发送权限
            if (!checkSendPermission(message, group)) {
                result.setDeliveryStatus(DeliveryStatus.PERMISSION_DENIED);
                result.setErrorMessage("无权限发送到此群组");
                return result;
            }
            
            // 检查是否需要审核
            if (group.getRequireModeration()) {
                result = processModeratedMessage(message, group);
            } else {
                result = deliverToGroupMembers(message, group);
            }
            
            // 更新群组统计
            group.incrementMessageCount();
            groupRepository.save(group);
            
        } catch (Exception e) {
            logger.error("处理群组邮件失败", e);
            result.setDeliveryStatus(DeliveryStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setProcessingEndTime(LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 处理Catch-All邮件
     */
    public CatchAllDeliveryResult processCatchAllMessage(EmailMessage message, String recipientAddress) {
        logger.info("处理Catch-All邮件: messageId={}, recipient={}", 
            message.getMessageId(), recipientAddress);
        
        CatchAllDeliveryResult result = new CatchAllDeliveryResult();
        result.setMessageId(message.getMessageId());
        result.setOriginalRecipient(recipientAddress);
        result.setProcessingStartTime(LocalDateTime.now());
        
        try {
            // 提取域名
            String domain = extractDomain(recipientAddress);
            
            // 查找Catch-All配置
            List<CatchAllMailbox> catchAllBoxes = catchAllRepository
                .findByDomainNameAndIsActiveOrderByPriority(domain, true);
            
            if (catchAllBoxes.isEmpty()) {
                result.setDeliveryStatus(DeliveryStatus.NO_CATCH_ALL);
                result.setErrorMessage("未配置Catch-All邮箱");
                return result;
            }
            
            // 处理每个Catch-All配置
            for (CatchAllMailbox catchAll : catchAllBoxes) {
                try {
                    CatchAllProcessResult processResult = processCatchAllMailbox(message, catchAll);
                    result.getProcessResults().add(processResult);
                    
                    if (processResult.isSuccessful()) {
                        result.setDeliveryStatus(DeliveryStatus.SUCCESS);
                        result.setFinalRecipient(catchAll.getTargetEmailAddress());
                        break; // 第一个成功的就停止
                    }
                    
                } catch (Exception e) {
                    logger.error("处理Catch-All邮箱失败: {}", catchAll.getId(), e);
                    
                    CatchAllProcessResult errorResult = new CatchAllProcessResult();
                    errorResult.setCatchAllId(catchAll.getId());
                    errorResult.setSuccessful(false);
                    errorResult.setErrorMessage(e.getMessage());
                    result.getProcessResults().add(errorResult);
                }
            }
            
            // 如果所有Catch-All都失败了
            if (result.getDeliveryStatus() != DeliveryStatus.SUCCESS) {
                result.setDeliveryStatus(DeliveryStatus.ALL_CATCH_ALL_FAILED);
            }
            
        } catch (Exception e) {
            logger.error("处理Catch-All邮件失败", e);
            result.setDeliveryStatus(DeliveryStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setProcessingEndTime(LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 创建邮件群组
     */
    public EmailGroup createEmailGroup(EmailDomain domain, String groupName, String displayName, 
                                      EmailGroup.GroupType groupType) {
        
        // 检查群组名是否已存在
        if (groupRepository.existsByDomainAndGroupName(domain, groupName)) {
            throw new RuntimeException("群组名已存在");
        }
        
        EmailGroup group = new EmailGroup(domain, groupName);
        group.setDisplayName(displayName);
        group.setGroupType(groupType);
        
        return groupRepository.save(group);
    }
    
    /**
     * 添加群组成员
     */
    public EmailGroupMember addGroupMember(EmailGroup group, User user, 
                                          EmailGroupMember.MemberRole role) {
        
        // 检查成员是否已存在
        if (memberRepository.existsByGroupAndUser(group, user)) {
            throw new RuntimeException("用户已是群组成员");
        }
        
        // 检查群组容量
        if (group.isAtMaxCapacity()) {
            throw new RuntimeException("群组已达到最大成员数限制");
        }
        
        EmailGroupMember member = new EmailGroupMember(group, user);
        member.setMemberRole(role);
        
        return memberRepository.save(member);
    }
    
    /**
     * 添加外部成员
     */
    public EmailGroupMember addExternalMember(EmailGroup group, String email, String name) {
        
        // 检查外部邮箱是否已存在
        if (memberRepository.existsByGroupAndExternalEmail(group, email)) {
            throw new RuntimeException("外部邮箱已是群组成员");
        }
        
        // 检查群组容量
        if (group.isAtMaxCapacity()) {
            throw new RuntimeException("群组已达到最大成员数限制");
        }
        
        EmailGroupMember member = new EmailGroupMember(group, email, name);
        return memberRepository.save(member);
    }
    
    /**
     * 创建Catch-All邮箱
     */
    public CatchAllMailbox createCatchAllMailbox(EmailDomain domain, User targetUser, 
                                               CatchAllMailbox.CatchAllType type) {
        
        CatchAllMailbox catchAll = new CatchAllMailbox(domain, targetUser);
        catchAll.setCatchAllType(type);
        catchAll.setPriority(getNextCatchAllPriority(domain));
        
        return catchAllRepository.save(catchAll);
    }
    
    /**
     * 创建外部Catch-All邮箱
     */
    public CatchAllMailbox createExternalCatchAllMailbox(EmailDomain domain, String targetEmail, 
                                                       CatchAllMailbox.CatchAllType type) {
        
        CatchAllMailbox catchAll = new CatchAllMailbox(domain, targetEmail);
        catchAll.setCatchAllType(type);
        catchAll.setPriority(getNextCatchAllPriority(domain));
        
        return catchAllRepository.save(catchAll);
    }
    
    // 私有方法
    
    /**
     * 检查发送权限
     */
    private boolean checkSendPermission(EmailMessage message, EmailGroup group) {
        String senderAddress = message.getFromAddress();
        
        // 检查是否允许外部发件人
        if (!group.getAllowExternalSenders()) {
            // 检查发件人是否为群组成员
            return memberRepository.existsByGroupAndUserEmailOrExternalEmail(
                group, senderAddress, senderAddress);
        }
        
        return true;
    }
    
    /**
     * 投递到群组成员
     */
    private GroupDeliveryResult deliverToGroupMembers(EmailMessage message, EmailGroup group) {
        GroupDeliveryResult result = new GroupDeliveryResult();
        result.setDeliveryStatus(DeliveryStatus.SUCCESS);
        
        List<EmailGroupMember> activeMembers = memberRepository
            .findByGroupAndIsActiveAndCanReceive(group, true, true);
        
        List<MemberDeliveryResult> memberResults = new ArrayList<>();
        
        for (EmailGroupMember member : activeMembers) {
            try {
                MemberDeliveryResult memberResult = deliverToMember(message, member);
                memberResults.add(memberResult);
                
                if (memberResult.isSuccessful()) {
                    result.incrementSuccessfulDeliveries();
                } else {
                    result.incrementFailedDeliveries();
                }
                
            } catch (Exception e) {
                logger.error("投递到成员失败: memberId={}", member.getId(), e);
                
                MemberDeliveryResult errorResult = new MemberDeliveryResult();
                errorResult.setMemberId(member.getId());
                errorResult.setMemberEmail(member.getMemberEmail());
                errorResult.setSuccessful(false);
                errorResult.setErrorMessage(e.getMessage());
                memberResults.add(errorResult);
                
                result.incrementFailedDeliveries();
            }
        }
        
        result.setMemberResults(memberResults);
        result.setTotalMembers(activeMembers.size());
        
        return result;
    }
    
    /**
     * 处理需要审核的消息
     */
    private GroupDeliveryResult processModeratedMessage(EmailMessage message, EmailGroup group) {
        GroupDeliveryResult result = new GroupDeliveryResult();
        result.setDeliveryStatus(DeliveryStatus.PENDING_MODERATION);
        
        // 发送给审核员
        List<EmailGroupMember> moderators = memberRepository
            .findByGroupAndIsModeratorAndIsActive(group, true, true);
        
        for (EmailGroupMember moderator : moderators) {
            try {
                sendModerationNotification(message, group, moderator);
            } catch (Exception e) {
                logger.error("发送审核通知失败: moderatorId={}", moderator.getId(), e);
            }
        }
        
        return result;
    }
    
    /**
     * 投递到单个成员
     */
    private MemberDeliveryResult deliverToMember(EmailMessage message, EmailGroupMember member) {
        MemberDeliveryResult result = new MemberDeliveryResult();
        result.setMemberId(member.getId());
        result.setMemberEmail(member.getMemberEmail());
        
        try {
            // 检查订阅类型
            if (member.getSubscriptionType() == EmailGroupMember.SubscriptionType.NOMAIL) {
                result.setSuccessful(true);
                result.setSkipped(true);
                result.setSkipReason("用户设置不接收邮件");
                return result;
            }
            
            // TODO: 根据订阅类型处理（摘要模式等）
            
            // 创建副本并发送
            EmailMessage groupMessage = createGroupMessage(message, member);
            
            if (member.isInternalMember()) {
                // 内部用户直接投递到收件箱
                deliverToInternalUser(groupMessage, member.getUser());
            } else {
                // 外部用户通过SMTP发送
                emailSendService.forwardEmail(groupMessage, member.getExternalEmail());
            }
            
            // 更新成员活动时间
            member.updateActivity();
            memberRepository.save(member);
            
            result.setSuccessful(true);
            
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        }
        
        return result;
    }
    
    /**
     * 处理Catch-All邮箱
     */
    private CatchAllProcessResult processCatchAllMailbox(EmailMessage message, CatchAllMailbox catchAll) {
        CatchAllProcessResult result = new CatchAllProcessResult();
        result.setCatchAllId(catchAll.getId());
        
        try {
            // 检查每日限制
            if (catchAll.isAtDailyLimit()) {
                result.setSuccessful(false);
                result.setErrorMessage("已达到每日消息限制");
                return result;
            }
            
            // 根据类型处理
            switch (catchAll.getCatchAllType()) {
                case DELIVER:
                    deliverToCatchAllTarget(message, catchAll);
                    break;
                case FORWARD:
                    forwardToCatchAllTarget(message, catchAll);
                    break;
                case DISCARD:
                    // 丢弃邮件，不做任何处理
                    break;
                case BOUNCE:
                    bounceMessage(message, catchAll);
                    break;
                case QUARANTINE:
                    quarantineMessage(message, catchAll);
                    break;
            }
            
            // 更新统计
            catchAll.incrementMessageCount();
            catchAllRepository.save(catchAll);
            
            // 发送自动回复（如果启用）
            if (catchAll.getAutoReplyEnabled() && catchAll.getAutoReplyMessage() != null) {
                sendCatchAllAutoReply(message, catchAll);
            }
            
            result.setSuccessful(true);
            result.setTargetEmail(catchAll.getTargetEmailAddress());
            
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        }
        
        return result;
    }
    
    // 辅助方法
    
    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : email;
    }
    
    private int getNextCatchAllPriority(EmailDomain domain) {
        Integer maxPriority = catchAllRepository.getMaxPriorityByDomain(domain);
        return maxPriority != null ? maxPriority + 1 : 1;
    }
    
    private EmailMessage createGroupMessage(EmailMessage original, EmailGroupMember member) {
        // 创建群组消息副本，可以添加群组标识等
        EmailMessage groupMessage = new EmailMessage();
        // 复制原始消息的属性
        // TODO: 实现消息复制逻辑
        return groupMessage;
    }
    
    private void deliverToInternalUser(EmailMessage message, User user) {
        // TODO: 实现内部用户投递逻辑
    }
    
    private void sendModerationNotification(EmailMessage message, EmailGroup group, EmailGroupMember moderator) {
        // TODO: 实现审核通知发送逻辑
    }
    
    private void deliverToCatchAllTarget(EmailMessage message, CatchAllMailbox catchAll) {
        // TODO: 实现Catch-All投递逻辑
    }
    
    private void forwardToCatchAllTarget(EmailMessage message, CatchAllMailbox catchAll) {
        // TODO: 实现Catch-All转发逻辑
    }
    
    private void bounceMessage(EmailMessage message, CatchAllMailbox catchAll) {
        // TODO: 实现邮件退回逻辑
    }
    
    private void quarantineMessage(EmailMessage message, CatchAllMailbox catchAll) {
        // TODO: 实现邮件隔离逻辑
    }
    
    private void sendCatchAllAutoReply(EmailMessage message, CatchAllMailbox catchAll) {
        // TODO: 实现Catch-All自动回复逻辑
    }
    
    // 枚举和结果类
    
    public enum DeliveryStatus {
        SUCCESS, GROUP_NOT_FOUND, PERMISSION_DENIED, PENDING_MODERATION,
        NO_CATCH_ALL, ALL_CATCH_ALL_FAILED, ERROR
    }
    
    public static class GroupDeliveryResult {
        private String messageId;
        private String recipientAddress;
        private DeliveryStatus deliveryStatus;
        private int totalMembers;
        private int successfulDeliveries;
        private int failedDeliveries;
        private List<MemberDeliveryResult> memberResults = new ArrayList<>();
        private String errorMessage;
        private LocalDateTime processingStartTime;
        private LocalDateTime processingEndTime;
        
        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getRecipientAddress() { return recipientAddress; }
        public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
        
        public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
        
        public int getTotalMembers() { return totalMembers; }
        public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }
        
        public int getSuccessfulDeliveries() { return successfulDeliveries; }
        public void setSuccessfulDeliveries(int successfulDeliveries) { this.successfulDeliveries = successfulDeliveries; }
        public void incrementSuccessfulDeliveries() { this.successfulDeliveries++; }
        
        public int getFailedDeliveries() { return failedDeliveries; }
        public void setFailedDeliveries(int failedDeliveries) { this.failedDeliveries = failedDeliveries; }
        public void incrementFailedDeliveries() { this.failedDeliveries++; }
        
        public List<MemberDeliveryResult> getMemberResults() { return memberResults; }
        public void setMemberResults(List<MemberDeliveryResult> memberResults) { this.memberResults = memberResults; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getProcessingStartTime() { return processingStartTime; }
        public void setProcessingStartTime(LocalDateTime processingStartTime) { this.processingStartTime = processingStartTime; }
        
        public LocalDateTime getProcessingEndTime() { return processingEndTime; }
        public void setProcessingEndTime(LocalDateTime processingEndTime) { this.processingEndTime = processingEndTime; }
    }
    
    public static class MemberDeliveryResult {
        private Long memberId;
        private String memberEmail;
        private boolean successful;
        private boolean skipped;
        private String skipReason;
        private String errorMessage;
        
        // Getters and Setters
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        
        public String getMemberEmail() { return memberEmail; }
        public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public boolean isSkipped() { return skipped; }
        public void setSkipped(boolean skipped) { this.skipped = skipped; }
        
        public String getSkipReason() { return skipReason; }
        public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class CatchAllDeliveryResult {
        private String messageId;
        private String originalRecipient;
        private String finalRecipient;
        private DeliveryStatus deliveryStatus;
        private List<CatchAllProcessResult> processResults = new ArrayList<>();
        private String errorMessage;
        private LocalDateTime processingStartTime;
        private LocalDateTime processingEndTime;
        
        // Getters and Setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        
        public String getOriginalRecipient() { return originalRecipient; }
        public void setOriginalRecipient(String originalRecipient) { this.originalRecipient = originalRecipient; }
        
        public String getFinalRecipient() { return finalRecipient; }
        public void setFinalRecipient(String finalRecipient) { this.finalRecipient = finalRecipient; }
        
        public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
        
        public List<CatchAllProcessResult> getProcessResults() { return processResults; }
        public void setProcessResults(List<CatchAllProcessResult> processResults) { this.processResults = processResults; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public LocalDateTime getProcessingStartTime() { return processingStartTime; }
        public void setProcessingStartTime(LocalDateTime processingStartTime) { this.processingStartTime = processingStartTime; }
        
        public LocalDateTime getProcessingEndTime() { return processingEndTime; }
        public void setProcessingEndTime(LocalDateTime processingEndTime) { this.processingEndTime = processingEndTime; }
    }
    
    public static class CatchAllProcessResult {
        private Long catchAllId;
        private String targetEmail;
        private boolean successful;
        private String errorMessage;
        
        // Getters and Setters
        public Long getCatchAllId() { return catchAllId; }
        public void setCatchAllId(Long catchAllId) { this.catchAllId = catchAllId; }
        
        public String getTargetEmail() { return targetEmail; }
        public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}