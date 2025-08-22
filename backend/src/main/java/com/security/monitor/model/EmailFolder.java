package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件文件夹实体
 */
@Entity
@Table(name = "email_folders",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "folder_name"}))
public class EmailFolder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank(message = "文件夹名称不能为空")
    @Column(name = "folder_name", nullable = false, length = 100)
    private String folderName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "folder_type")
    private FolderType folderType = FolderType.CUSTOM;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private EmailFolder parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<EmailFolder> children;
    
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL)
    private List<EmailMessage> messages;
    
    @Column(name = "message_count")
    private Integer messageCount = 0;
    
    @Column(name = "unread_count")
    private Integer unreadCount = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum FolderType {
        INBOX, SENT, DRAFT, TRASH, SPAM, CUSTOM
    }
    
    // Constructors
    public EmailFolder() {}
    
    public EmailFolder(User user, String folderName, FolderType folderType) {
        this.user = user;
        this.folderName = folderName;
        this.folderType = folderType;
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
    
    public String getFolderName() {
        return folderName;
    }
    
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    
    public FolderType getFolderType() {
        return folderType;
    }
    
    public void setFolderType(FolderType folderType) {
        this.folderType = folderType;
    }
    
    public EmailFolder getParent() {
        return parent;
    }
    
    public void setParent(EmailFolder parent) {
        this.parent = parent;
    }
    
    public List<EmailFolder> getChildren() {
        return children;
    }
    
    public void setChildren(List<EmailFolder> children) {
        this.children = children;
    }
    
    public List<EmailMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<EmailMessage> messages) {
        this.messages = messages;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public Integer getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 增加消息计数
     */
    public void incrementMessageCount() {
        this.messageCount = (this.messageCount == null ? 0 : this.messageCount) + 1;
    }
    
    /**
     * 减少消息计数
     */
    public void decrementMessageCount() {
        if (this.messageCount != null && this.messageCount > 0) {
            this.messageCount--;
        }
    }
    
    /**
     * 增加未读消息计数
     */
    public void incrementUnreadCount() {
        this.unreadCount = (this.unreadCount == null ? 0 : this.unreadCount) + 1;
    }
    
    /**
     * 减少未读消息计数
     */
    public void decrementUnreadCount() {
        if (this.unreadCount != null && this.unreadCount > 0) {
            this.unreadCount--;
        }
    }
}