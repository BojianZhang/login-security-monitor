package com.security.monitor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件域名实体
 */
@Entity
@Table(name = "email_domains")
public class EmailDomain {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "域名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", 
             message = "域名格式不正确")
    @Column(name = "domain_name", unique = true, nullable = false)
    private String domainName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Column(name = "mx_record")
    private String mxRecord;
    
    @Column(name = "dkim_selector", length = 100)
    private String dkimSelector;
    
    @Column(name = "dkim_private_key", columnDefinition = "TEXT")
    private String dkimPrivateKey;
    
    @Column(name = "dkim_public_key", columnDefinition = "TEXT")
    private String dkimPublicKey;
    
    @Column(name = "spf_record", columnDefinition = "TEXT")
    private String spfRecord;
    
    @Column(name = "dmarc_policy", length = 50)
    private String dmarcPolicy = "none";
    
    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL)
    private List<EmailAlias> aliases;
    
    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL)
    private List<EmailStatistics> statistics;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EmailDomain() {}
    
    public EmailDomain(String domainName) {
        this.domainName = domainName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDomainName() {
        return domainName;
    }
    
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public String getMxRecord() {
        return mxRecord;
    }
    
    public void setMxRecord(String mxRecord) {
        this.mxRecord = mxRecord;
    }
    
    public String getDkimSelector() {
        return dkimSelector;
    }
    
    public void setDkimSelector(String dkimSelector) {
        this.dkimSelector = dkimSelector;
    }
    
    public String getDkimPrivateKey() {
        return dkimPrivateKey;
    }
    
    public void setDkimPrivateKey(String dkimPrivateKey) {
        this.dkimPrivateKey = dkimPrivateKey;
    }
    
    public String getDkimPublicKey() {
        return dkimPublicKey;
    }
    
    public void setDkimPublicKey(String dkimPublicKey) {
        this.dkimPublicKey = dkimPublicKey;
    }
    
    public String getSpfRecord() {
        return spfRecord;
    }
    
    public void setSpfRecord(String spfRecord) {
        this.spfRecord = spfRecord;
    }
    
    public String getDmarcPolicy() {
        return dmarcPolicy;
    }
    
    public void setDmarcPolicy(String dmarcPolicy) {
        this.dmarcPolicy = dmarcPolicy;
    }
    
    public List<EmailAlias> getAliases() {
        return aliases;
    }
    
    public void setAliases(List<EmailAlias> aliases) {
        this.aliases = aliases;
    }
    
    public List<EmailStatistics> getStatistics() {
        return statistics;
    }
    
    public void setStatistics(List<EmailStatistics> statistics) {
        this.statistics = statistics;
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