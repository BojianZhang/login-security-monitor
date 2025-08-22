package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 邮件附件实体
 */
@Entity
@Table(name = "email_attachments")
public class EmailAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private EmailMessage message;
    
    @NotBlank(message = "文件名不能为空")
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "file_size")
    private Long fileSize = 0L;
    
    @Column(name = "file_hash", length = 64)
    private String fileHash; // SHA-256 hash
    
    @Column(name = "storage_path", length = 500)
    private String storagePath;
    
    @Column(name = "is_inline")
    private Boolean isInline = false;
    
    @Column(name = "content_id")
    private String contentId; // 用于内嵌图片
    
    @Column(name = "is_quarantined")
    private Boolean isQuarantined = false; // 是否已隔离
    
    @Column(name = "quarantine_reason", length = 500)
    private String quarantineReason; // 隔离原因
    
    @Column(name = "virus_scan_status", length = 20)
    private String virusScanStatus; // 病毒扫描状态
    
    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt; // 最后扫描时间
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors
    public EmailAttachment() {}
    
    public EmailAttachment(EmailMessage message, String filename) {
        this.message = message;
        this.filename = filename;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailMessage getMessage() {
        return message;
    }
    
    public void setMessage(EmailMessage message) {
        this.message = message;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public String getStoragePath() {
        return storagePath;
    }
    
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
    
    public Boolean getIsInline() {
        return isInline;
    }
    
    public void setIsInline(Boolean isInline) {
        this.isInline = isInline;
    }
    
    public String getContentId() {
        return contentId;
    }
    
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsQuarantined() {
        return isQuarantined;
    }
    
    public void setIsQuarantined(Boolean isQuarantined) {
        this.isQuarantined = isQuarantined;
    }
    
    public String getQuarantineReason() {
        return quarantineReason;
    }
    
    public void setQuarantineReason(String quarantineReason) {
        this.quarantineReason = quarantineReason;
    }
    
    public String getVirusScanStatus() {
        return virusScanStatus;
    }
    
    public void setVirusScanStatus(String virusScanStatus) {
        this.virusScanStatus = virusScanStatus;
    }
    
    public LocalDateTime getLastScannedAt() {
        return lastScannedAt;
    }
    
    public void setLastScannedAt(LocalDateTime lastScannedAt) {
        this.lastScannedAt = lastScannedAt;
    }
    
    /**
     * 获取人类可读的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = fileSize.doubleValue();
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
    
    /**
     * 检查是否是图片文件
     */
    public boolean isImage() {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }
    
    /**
     * 检查是否是文档文件
     */
    public boolean isDocument() {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("application/pdf") ||
               contentType.contains("document") ||
               contentType.contains("text") ||
               contentType.contains("spreadsheet") ||
               contentType.contains("presentation");
    }
}