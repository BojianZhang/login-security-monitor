package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件消息实体
 */
@Entity
@Table(name = "email_messages")
public class EmailMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private EmailFolder folder;
    
    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;
    
    @Column(name = "thread_id")
    private String threadId;
    
    @Size(max = 998, message = "邮件主题长度不能超过998字符")
    @Column(name = "subject", length = 998)
    private String subject;
    
    @NotBlank(message = "发件人地址不能为空")
    @Column(name = "from_address", nullable = false, length = 320)
    private String fromAddress;
    
    @Column(name = "to_addresses", columnDefinition = "TEXT")
    private String toAddresses; // JSON格式存储
    
    @Column(name = "cc_addresses", columnDefinition = "TEXT")
    private String ccAddresses; // JSON格式存储
    
    @Column(name = "bcc_addresses", columnDefinition = "TEXT")
    private String bccAddresses; // JSON格式存储
    
    @Column(name = "reply_to", length = 320)
    private String replyTo;
    
    @Lob
    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    private String bodyText;
    
    @Lob
    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    private String bodyHtml;
    
    @Column(name = "message_size")
    private Long messageSize = 0L;
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "is_starred")
    private Boolean isStarred = false;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "is_spam")
    private Boolean isSpam = false;
    
    @Column(name = "priority_level")
    private Integer priorityLevel = 3; // 1=高, 3=正常, 5=低
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailAttachment> attachments;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public EmailMessage() {
        this.receivedAt = LocalDateTime.now();
    }
    
    public EmailMessage(User user, EmailFolder folder, String messageId) {
        this();
        this.user = user;
        this.folder = folder;
        this.messageId = messageId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public EmailFolder getFolder() {
        return folder;
    }
    
    public void setFolder(EmailFolder folder) {
        this.folder = folder;
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
    
    public String getToAddresses() {
        return toAddresses;
    }
    
    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }
    
    public String getCcAddresses() {
        return ccAddresses;
    }
    
    public void setCcAddresses(String ccAddresses) {
        this.ccAddresses = ccAddresses;
    }
    
    public String getBccAddresses() {
        return bccAddresses;
    }
    
    public void setBccAddresses(String bccAddresses) {
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
    
    public List<EmailAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<EmailAttachment> attachments) {
        this.attachments = attachments;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 标记为已读
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            if (this.folder != null) {
                this.folder.decrementUnreadCount();
            }
        }
    }
    
    /**
     * 标记为未读
     */
    public void markAsUnread() {
        if (this.isRead) {
            this.isRead = false;
            if (this.folder != null) {
                this.folder.incrementUnreadCount();
            }
        }
    }
    
    /**
     * 检查是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
    
    /**
     * 获取优先级描述
     */
    public String getPriorityDescription() {
        return switch (priorityLevel) {
            case 1 -> "高优先级";
            case 2 -> "较高优先级";
            case 3 -> "普通";
            case 4 -> "较低优先级";
            case 5 -> "低优先级";
            default -> "未知";
        };
    }
}