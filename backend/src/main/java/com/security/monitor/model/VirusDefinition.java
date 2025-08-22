package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 病毒定义实体
 */
@Entity
@Table(name = "virus_definitions")
public class VirusDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "virus_name", nullable = false, length = 100)
    private String virusName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "signature_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SignatureType signatureType;
    
    @Column(name = "signature_value", nullable = false, length = 1000)
    private String signatureValue;
    
    @Column(name = "hash_signature", length = 64)
    private String hashSignature; // MD5/SHA256等哈希值
    
    @Column(name = "pattern_signature", length = 1000)
    private String patternSignature; // 正则表达式或字符串模式
    
    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "detection_count", nullable = false)
    private Long detectionCount = 0L;
    
    @Column(name = "last_detected_at")
    private LocalDateTime lastDetectedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 枚举定义
    public enum SignatureType {
        HASH("哈希特征"),
        PATTERN("模式特征"),
        BYTE_SEQUENCE("字节序列"),
        FILE_EXTENSION("文件扩展名"),
        MIME_TYPE("MIME类型");
        
        private final String description;
        
        SignatureType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum Severity {
        LOW("低"),
        MEDIUM("中"),
        HIGH("高"),
        CRITICAL("严重");
        
        private final String description;
        
        Severity(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public VirusDefinition() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public VirusDefinition(String virusName, String description, String signatureValue) {
        this();
        this.virusName = virusName;
        this.description = description;
        this.signatureValue = signatureValue;
        this.severity = Severity.MEDIUM;
        
        // 根据signatureValue自动判断类型
        if (signatureValue.startsWith("pattern:")) {
            this.signatureType = SignatureType.PATTERN;
            this.patternSignature = signatureValue.substring(8);
        } else if (signatureValue.matches("[a-fA-F0-9]{32}|[a-fA-F0-9]{64}")) {
            this.signatureType = SignatureType.HASH;
            this.hashSignature = signatureValue;
        } else {
            this.signatureType = SignatureType.BYTE_SEQUENCE;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getVirusName() {
        return virusName;
    }
    
    public void setVirusName(String virusName) {
        this.virusName = virusName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public SignatureType getSignatureType() {
        return signatureType;
    }
    
    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }
    
    public String getSignatureValue() {
        return signatureValue;
    }
    
    public void setSignatureValue(String signatureValue) {
        this.signatureValue = signatureValue;
    }
    
    public String getHashSignature() {
        return hashSignature;
    }
    
    public void setHashSignature(String hashSignature) {
        this.hashSignature = hashSignature;
    }
    
    public String getPatternSignature() {
        return patternSignature;
    }
    
    public void setPatternSignature(String patternSignature) {
        this.patternSignature = patternSignature;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Long getDetectionCount() {
        return detectionCount;
    }
    
    public void setDetectionCount(Long detectionCount) {
        this.detectionCount = detectionCount;
    }
    
    public LocalDateTime getLastDetectedAt() {
        return lastDetectedAt;
    }
    
    public void setLastDetectedAt(LocalDateTime lastDetectedAt) {
        this.lastDetectedAt = lastDetectedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}