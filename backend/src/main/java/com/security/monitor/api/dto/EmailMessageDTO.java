package com.security.monitor.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件消息DTO
 */
public class EmailMessageDTO {
    
    private Long id;
    private String messageId;
    private String threadId;
    private String subject;
    private String fromAddress;
    private List<String> toAddresses;
    private List<String> ccAddresses;
    private List<String> bccAddresses;
    private String replyTo;
    private String bodyText;
    private String bodyHtml;
    private Long messageSize;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean isDeleted;
    private Boolean isSpam;
    private Integer priorityLevel;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String folderName;
    private List<EmailAttachmentDTO> attachments;
    
    // 构造函数
    public EmailMessageDTO() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    public List<String> getToAddresses() {
        return toAddresses;
    }
    
    public void setToAddresses(List<String> toAddresses) {
        this.toAddresses = toAddresses;
    }
    
    public List<String> getCcAddresses() {
        return ccAddresses;
    }
    
    public void setCcAddresses(List<String> ccAddresses) {
        this.ccAddresses = ccAddresses;
    }
    
    public List<String> getBccAddresses() {
        return bccAddresses;
    }
    
    public void setBccAddresses(List<String> bccAddresses) {
        this.bccAddresses = bccAddresses;
    }
    
    public String getReplyTo() {
        return replyTo;
    }
    
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }
    
    public String getBodyText() {
        return bodyText;
    }
    
    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }
    
    public String getBodyHtml() {
        return bodyHtml;
    }
    
    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }
    
    public Long getMessageSize() {
        return messageSize;
    }
    
    public void setMessageSize(Long messageSize) {
        this.messageSize = messageSize;
    }
    
    public Boolean getIsRead() {
        return isRead;
    }
    
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
    
    public Boolean getIsStarred() {
        return isStarred;
    }
    
    public void setIsStarred(Boolean isStarred) {
        this.isStarred = isStarred;
    }
    
    public Boolean getIsDeleted() {
        return isDeleted;
    }
    
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    public Boolean getIsSpam() {
        return isSpam;
    }
    
    public void setIsSpam(Boolean isSpam) {
        this.isSpam = isSpam;
    }
    
    public Integer getPriorityLevel() {
        return priorityLevel;
    }
    
    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getFolderName() {
        return folderName;
    }
    
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    
    public List<EmailAttachmentDTO> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<EmailAttachmentDTO> attachments) {
        this.attachments = attachments;
    }
    
    /**
     * 获取邮件大小（格式化）
     */
    public String getFormattedSize() {
        if (messageSize == null) return "0 B";
        
        if (messageSize < 1024) return messageSize + " B";
        if (messageSize < 1024 * 1024) return String.format("%.1f KB", messageSize / 1024.0);
        return String.format("%.1f MB", messageSize / (1024.0 * 1024.0));
    }
    
    /**
     * 获取优先级描述
     */
    public String getPriorityDescription() {
        if (priorityLevel == null) return "Normal";
        
        switch (priorityLevel) {
            case 1: return "High";
            case 2: return "Medium High";
            case 3: return "Normal";
            case 4: return "Medium Low";
            case 5: return "Low";
            default: return "Unknown";
        }
    }
    
    /**
     * 检查是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
    
    /**
     * 获取附件数量
     */
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
}

/**
 * 邮件附件DTO
 */
class EmailAttachmentDTO {
    private Long id;
    private String filename;
    private String contentType;
    private Long fileSize;
    private String fileHash;
    private Boolean isInline;
    private String contentId;
    private Boolean isQuarantined;
    private String quarantineReason;
    private String virusScanStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastScannedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public Boolean getIsInline() { return isInline; }
    public void setIsInline(Boolean isInline) { this.isInline = isInline; }
    
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    
    public Boolean getIsQuarantined() { return isQuarantined; }
    public void setIsQuarantined(Boolean isQuarantined) { this.isQuarantined = isQuarantined; }
    
    public String getQuarantineReason() { return quarantineReason; }
    public void setQuarantineReason(String quarantineReason) { this.quarantineReason = quarantineReason; }
    
    public String getVirusScanStatus() { return virusScanStatus; }
    public void setVirusScanStatus(String virusScanStatus) { this.virusScanStatus = virusScanStatus; }
    
    public LocalDateTime getLastScannedAt() { return lastScannedAt; }
    public void setLastScannedAt(LocalDateTime lastScannedAt) { this.lastScannedAt = lastScannedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * 获取文件大小（格式化）
     */
    public String getFormattedSize() {
        if (fileSize == null) return "0 B";
        
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
    
    /**
     * 检查是否安全
     */
    public boolean isSafe() {
        return !Boolean.TRUE.equals(isQuarantined) && 
               ("CLEAN".equals(virusScanStatus) || "PASSED".equals(virusScanStatus));
    }
}