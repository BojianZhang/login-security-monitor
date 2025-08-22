package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 病毒扫描日志实体
 */
@Entity
@Table(name = "virus_scan_logs")
public class VirusScanLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private EmailMessage message;
    
    @Column(name = "scan_status", nullable = false, length = 20)
    private String scanStatus;
    
    @Column(name = "threat_found", nullable = false)
    private Boolean threatFound = false;
    
    @Column(name = "threats_found", nullable = false)
    private Integer threatsFound = 0;
    
    @Column(name = "files_scanned", nullable = false)
    private Integer filesScanned = 0;
    
    @Column(name = "scan_details", length = 1000)
    private String scanDetails;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "quarantined_files", nullable = false)
    private Integer quarantinedFiles = 0;
    
    @Column(name = "scan_engine_version", length = 50)
    private String scanEngineVersion;
    
    @Column(name = "virus_definitions_version", length = 50)
    private String virusDefinitionsVersion;
    
    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;
    
    // 构造函数
    public VirusScanLog() {
        this.scannedAt = LocalDateTime.now();
    }
    
    public VirusScanLog(EmailMessage message, String scanStatus) {
        this();
        this.message = message;
        this.scanStatus = scanStatus;
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
    
    public String getScanStatus() {
        return scanStatus;
    }
    
    public void setScanStatus(String scanStatus) {
        this.scanStatus = scanStatus;
    }
    
    public Boolean getThreatFound() {
        return threatFound;
    }
    
    public void setThreatFound(Boolean threatFound) {
        this.threatFound = threatFound;
    }
    
    public Integer getThreatsFound() {
        return threatsFound;
    }
    
    public void setThreatsFound(Integer threatsFound) {
        this.threatsFound = threatsFound;
    }
    
    public Integer getFilesScanned() {
        return filesScanned;
    }
    
    public void setFilesScanned(Integer filesScanned) {
        this.filesScanned = filesScanned;
    }
    
    public String getScanDetails() {
        return scanDetails;
    }
    
    public void setScanDetails(String scanDetails) {
        this.scanDetails = scanDetails;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Integer getQuarantinedFiles() {
        return quarantinedFiles;
    }
    
    public void setQuarantinedFiles(Integer quarantinedFiles) {
        this.quarantinedFiles = quarantinedFiles;
    }
    
    public String getScanEngineVersion() {
        return scanEngineVersion;
    }
    
    public void setScanEngineVersion(String scanEngineVersion) {
        this.scanEngineVersion = scanEngineVersion;
    }
    
    public String getVirusDefinitionsVersion() {
        return virusDefinitionsVersion;
    }
    
    public void setVirusDefinitionsVersion(String virusDefinitionsVersion) {
        this.virusDefinitionsVersion = virusDefinitionsVersion;
    }
    
    public LocalDateTime getScannedAt() {
        return scannedAt;
    }
    
    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}