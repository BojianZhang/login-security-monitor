package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 防病毒扫描服务
 * 提供类似ClamAV的病毒扫描和恶意软件检测功能
 */
@Service
@Transactional
public class AntiVirusService {
    
    private static final Logger logger = LoggerFactory.getLogger(AntiVirusService.class);
    
    @Autowired
    private VirusDefinitionRepository virusDefinitionRepository;
    
    @Autowired
    private VirusScanLogRepository scanLogRepository;
    
    @Autowired
    private EmailAttachmentRepository attachmentRepository;
    
    @Value("${app.antivirus.enabled:true}")
    private boolean antiVirusEnabled;
    
    @Value("${app.antivirus.max-file-size:104857600}") // 100MB
    private long maxScanFileSize;
    
    @Value("${app.antivirus.quarantine.path:/opt/quarantine}")
    private String quarantinePath;
    
    @Value("${app.antivirus.scan-timeout:30000}") // 30秒
    private long scanTimeoutMs;
    
    // 危险文件扩展名
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        ".exe", ".bat", ".com", ".cmd", ".scr", ".pif", ".vbs", ".vbe", 
        ".js", ".jse", ".jar", ".msi", ".dll", ".hta", ".reg", ".ps1"
    );
    
    // 可疑MIME类型
    private static final Set<String> SUSPICIOUS_MIME_TYPES = Set.of(
        "application/x-msdownload", "application/x-executable", 
        "application/x-winexe", "application/x-msdos-program",
        "application/vnd.microsoft.portable-executable"
    );
    
    // 病毒特征码（简化版本，实际应该从病毒定义库加载）
    private static final Map<String, String> VIRUS_SIGNATURES = Map.of(
        "EICAR-TEST-SIGNATURE", "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*",
        "JS-MALWARE-1", "eval(unescape(",
        "VBA-MALWARE-1", "Auto_Open()",
        "PDF-MALWARE-1", "/JavaScript",
        "DOC-MALWARE-1", "macros"
    );
    
    /**
     * 扫描邮件附件
     */
    public ScanResult scanEmailAttachments(EmailMessage message) {
        if (!antiVirusEnabled) {
            return new ScanResult(ScanStatus.DISABLED, "防病毒扫描已禁用");
        }
        
        logger.info("开始扫描邮件附件: messageId={}", message.getMessageId());
        
        ScanResult overallResult = new ScanResult(ScanStatus.CLEAN, "无威胁");
        List<AttachmentScanResult> attachmentResults = new ArrayList<>();
        
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            for (EmailAttachment attachment : message.getAttachments()) {
                try {
                    AttachmentScanResult result = scanAttachment(attachment);
                    attachmentResults.add(result);
                    
                    // 如果发现威胁，更新整体结果
                    if (result.getStatus() == ScanStatus.INFECTED) {
                        overallResult.setStatus(ScanStatus.INFECTED);
                        overallResult.setDetails("发现病毒: " + result.getThreatName());
                    } else if (result.getStatus() == ScanStatus.SUSPICIOUS && 
                              overallResult.getStatus() == ScanStatus.CLEAN) {
                        overallResult.setStatus(ScanStatus.SUSPICIOUS);
                        overallResult.setDetails("发现可疑文件");
                    }
                    
                } catch (Exception e) {
                    logger.error("扫描附件失败: {}", attachment.getFilename(), e);
                    AttachmentScanResult errorResult = new AttachmentScanResult();
                    errorResult.setAttachmentId(attachment.getId());
                    errorResult.setFilename(attachment.getFilename());
                    errorResult.setStatus(ScanStatus.ERROR);
                    errorResult.setDetails("扫描失败: " + e.getMessage());
                    attachmentResults.add(errorResult);
                }
            }
        }
        
        overallResult.setAttachmentResults(attachmentResults);
        
        // 记录扫描日志
        logScanResult(message, overallResult);
        
        logger.info("邮件附件扫描完成: messageId={}, status={}", 
            message.getMessageId(), overallResult.getStatus());
        
        return overallResult;
    }
    
    /**
     * 扫描单个附件
     */
    public AttachmentScanResult scanAttachment(EmailAttachment attachment) {
        AttachmentScanResult result = new AttachmentScanResult();
        result.setAttachmentId(attachment.getId());
        result.setFilename(attachment.getFilename());
        result.setScanStartTime(LocalDateTime.now());
        
        try {
            // 1. 检查文件大小
            if (attachment.getFileSize() > maxScanFileSize) {
                result.setStatus(ScanStatus.TOO_LARGE);
                result.setDetails("文件过大，跳过扫描");
                return result;
            }
            
            // 2. 检查文件扩展名
            String filename = attachment.getFilename().toLowerCase();
            boolean isDangerous = DANGEROUS_EXTENSIONS.stream()
                .anyMatch(filename::endsWith);
            
            if (isDangerous) {
                result.setStatus(ScanStatus.SUSPICIOUS);
                result.setDetails("危险文件类型");
            }
            
            // 3. 检查MIME类型
            if (SUSPICIOUS_MIME_TYPES.contains(attachment.getContentType())) {
                result.setStatus(ScanStatus.SUSPICIOUS);
                result.setDetails("可疑MIME类型: " + attachment.getContentType());
            }
            
            // 4. 文件内容扫描
            if (Files.exists(Paths.get(attachment.getStoragePath()))) {
                ScanStatus contentScanResult = scanFileContent(attachment.getStoragePath());
                
                if (contentScanResult == ScanStatus.INFECTED) {
                    result.setStatus(ScanStatus.INFECTED);
                    result.setThreatName("发现病毒特征码");
                    
                    // 隔离文件
                    quarantineFile(attachment);
                }
            }
            
            // 5. 哈希检查
            String fileHash = attachment.getFileHash();
            if (fileHash != null && !fileHash.isEmpty()) {
                Optional<VirusDefinition> virusDefinition = 
                    virusDefinitionRepository.findByHashSignature(fileHash);
                
                if (virusDefinition.isPresent()) {
                    result.setStatus(ScanStatus.INFECTED);
                    result.setThreatName(virusDefinition.get().getVirusName());
                }
            }
            
            // 默认为清洁状态
            if (result.getStatus() == null) {
                result.setStatus(ScanStatus.CLEAN);
                result.setDetails("未发现威胁");
            }
            
        } catch (Exception e) {
            logger.error("扫描附件时发生错误", e);
            result.setStatus(ScanStatus.ERROR);
            result.setDetails("扫描错误: " + e.getMessage());
        } finally {
            result.setScanEndTime(LocalDateTime.now());
        }
        
        return result;
    }
    
    /**
     * 扫描文件内容
     */
    private ScanStatus scanFileContent(String filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            String contentString = new String(fileContent);
            
            // 检查病毒特征码
            for (Map.Entry<String, String> signature : VIRUS_SIGNATURES.entrySet()) {
                if (contentString.contains(signature.getValue())) {
                    logger.warn("发现病毒特征码: {} in file: {}", signature.getKey(), filePath);
                    return ScanStatus.INFECTED;
                }
            }
            
            // 检查可疑模式
            if (hasSuspiciousPatterns(contentString)) {
                return ScanStatus.SUSPICIOUS;
            }
            
            return ScanStatus.CLEAN;
            
        } catch (Exception e) {
            logger.error("读取文件内容失败: {}", filePath, e);
            return ScanStatus.ERROR;
        }
    }
    
    /**
     * 检查可疑模式
     */
    private boolean hasSuspiciousPatterns(String content) {
        // PowerShell恶意代码模式
        Pattern[] suspiciousPatterns = {
            Pattern.compile("powershell.*-encodedcommand", Pattern.CASE_INSENSITIVE),
            Pattern.compile("invoke-expression", Pattern.CASE_INSENSITIVE),
            Pattern.compile("downloadstring", Pattern.CASE_INSENSITIVE),
            Pattern.compile("base64.*decode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("document\\.write", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : suspiciousPatterns) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 隔离文件
     */
    private void quarantineFile(EmailAttachment attachment) {
        try {
            Path sourceFile = Paths.get(attachment.getStoragePath());
            Path quarantineDir = Paths.get(quarantinePath);
            
            // 创建隔离目录
            Files.createDirectories(quarantineDir);
            
            // 生成隔离文件名
            String quarantineFileName = String.format("%d_%s_%s", 
                System.currentTimeMillis(), 
                attachment.getId(),
                attachment.getFilename());
            
            Path quarantineFile = quarantineDir.resolve(quarantineFileName);
            
            // 移动文件到隔离区
            Files.move(sourceFile, quarantineFile);
            
            logger.info("文件已隔离: {} -> {}", sourceFile, quarantineFile);
            
            // 更新附件记录
            attachment.setStoragePath(quarantineFile.toString());
            attachment.setIsQuarantined(true);
            attachmentRepository.save(attachment);
            
        } catch (Exception e) {
            logger.error("隔离文件失败: {}", attachment.getFilename(), e);
        }
    }
    
    /**
     * 异步批量扫描
     */
    @Async
    public CompletableFuture<BatchScanResult> batchScanAttachments(List<EmailAttachment> attachments) {
        logger.info("开始批量扫描 {} 个附件", attachments.size());
        
        BatchScanResult batchResult = new BatchScanResult();
        batchResult.setTotalFiles(attachments.size());
        batchResult.setScanStartTime(LocalDateTime.now());
        
        int cleanCount = 0;
        int infectedCount = 0;
        int suspiciousCount = 0;
        int errorCount = 0;
        
        for (EmailAttachment attachment : attachments) {
            try {
                AttachmentScanResult result = scanAttachment(attachment);
                
                switch (result.getStatus()) {
                    case CLEAN:
                        cleanCount++;
                        break;
                    case INFECTED:
                        infectedCount++;
                        batchResult.getInfectedFiles().add(result);
                        break;
                    case SUSPICIOUS:
                        suspiciousCount++;
                        batchResult.getSuspiciousFiles().add(result);
                        break;
                    default:
                        errorCount++;
                        break;
                }
                
            } catch (Exception e) {
                errorCount++;
                logger.error("批量扫描附件失败: {}", attachment.getFilename(), e);
            }
        }
        
        batchResult.setCleanFiles(cleanCount);
        batchResult.setInfectedFiles(infectedCount);
        batchResult.setSuspiciousFiles(suspiciousCount);
        batchResult.setErrorFiles(errorCount);
        batchResult.setScanEndTime(LocalDateTime.now());
        
        logger.info("批量扫描完成: 总计={}, 清洁={}, 感染={}, 可疑={}, 错误={}", 
            attachments.size(), cleanCount, infectedCount, suspiciousCount, errorCount);
        
        return CompletableFuture.completedFuture(batchResult);
    }
    
    /**
     * 更新病毒定义库
     */
    @Transactional
    public void updateVirusDefinitions() {
        logger.info("开始更新病毒定义库");
        
        try {
            // 这里应该从外部源下载最新的病毒定义
            // 简化实现，添加一些基本的病毒定义
            
            List<VirusDefinition> definitions = Arrays.asList(
                new VirusDefinition("EICAR-Test-File", "测试病毒文件", "44d88612fea8a8f36de82e1278abb02f"),
                new VirusDefinition("JS.Malware.Generic", "通用JS恶意软件", "pattern:eval(unescape("),
                new VirusDefinition("VBA.Malware.Generic", "通用VBA恶意软件", "pattern:Auto_Open()"),
                new VirusDefinition("PDF.Malware.Generic", "通用PDF恶意软件", "pattern:/JavaScript")
            );
            
            for (VirusDefinition definition : definitions) {
                if (!virusDefinitionRepository.existsByVirusName(definition.getVirusName())) {
                    virusDefinitionRepository.save(definition);
                }
            }
            
            logger.info("病毒定义库更新完成");
            
        } catch (Exception e) {
            logger.error("更新病毒定义库失败", e);
        }
    }
    
    /**
     * 记录扫描日志
     */
    private void logScanResult(EmailMessage message, ScanResult result) {
        try {
            VirusScanLog log = new VirusScanLog();
            log.setMessage(message);
            log.setScanStatus(result.getStatus().toString());
            log.setThreatFound(result.getStatus() == ScanStatus.INFECTED);
            log.setScanDetails(result.getDetails());
            log.setScannedAt(LocalDateTime.now());
            
            if (result.getAttachmentResults() != null) {
                log.setFilesScanned(result.getAttachmentResults().size());
                log.setThreatsFound((int) result.getAttachmentResults().stream()
                    .filter(r -> r.getStatus() == ScanStatus.INFECTED)
                    .count());
            }
            
            scanLogRepository.save(log);
            
        } catch (Exception e) {
            logger.error("保存扫描日志失败", e);
        }
    }
    
    /**
     * 获取扫描统计
     */
    public ScanStatistics getScanStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        long totalScans = scanLogRepository.countByScannedAtBetween(startDate, endDate);
        long threatsFound = scanLogRepository.countByThreatFoundTrueAndScannedAtBetween(startDate, endDate);
        long filesScanned = scanLogRepository.sumFilesScannedBetween(startDate, endDate);
        
        ScanStatistics stats = new ScanStatistics();
        stats.setTotalScans(totalScans);
        stats.setThreatsFound(threatsFound);
        stats.setFilesScanned(filesScanned);
        stats.setCleanScans(totalScans - threatsFound);
        
        if (totalScans > 0) {
            stats.setThreatRate((double) threatsFound / totalScans * 100);
        }
        
        return stats;
    }
    
    // 枚举和数据类
    
    public enum ScanStatus {
        CLEAN("清洁"),
        INFECTED("感染"),
        SUSPICIOUS("可疑"),
        ERROR("错误"),
        TOO_LARGE("文件过大"),
        DISABLED("扫描禁用");
        
        private final String description;
        
        ScanStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 扫描结果
     */
    public static class ScanResult {
        private ScanStatus status;
        private String details;
        private List<AttachmentScanResult> attachmentResults;
        
        public ScanResult(ScanStatus status, String details) {
            this.status = status;
            this.details = details;
            this.attachmentResults = new ArrayList<>();
        }
        
        // Getters and Setters
        public ScanStatus getStatus() { return status; }
        public void setStatus(ScanStatus status) { this.status = status; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public List<AttachmentScanResult> getAttachmentResults() { return attachmentResults; }
        public void setAttachmentResults(List<AttachmentScanResult> attachmentResults) { this.attachmentResults = attachmentResults; }
    }
    
    /**
     * 附件扫描结果
     */
    public static class AttachmentScanResult {
        private Long attachmentId;
        private String filename;
        private ScanStatus status;
        private String threatName;
        private String details;
        private LocalDateTime scanStartTime;
        private LocalDateTime scanEndTime;
        
        // Getters and Setters
        public Long getAttachmentId() { return attachmentId; }
        public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public ScanStatus getStatus() { return status; }
        public void setStatus(ScanStatus status) { this.status = status; }
        
        public String getThreatName() { return threatName; }
        public void setThreatName(String threatName) { this.threatName = threatName; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public LocalDateTime getScanStartTime() { return scanStartTime; }
        public void setScanStartTime(LocalDateTime scanStartTime) { this.scanStartTime = scanStartTime; }
        
        public LocalDateTime getScanEndTime() { return scanEndTime; }
        public void setScanEndTime(LocalDateTime scanEndTime) { this.scanEndTime = scanEndTime; }
    }
    
    /**
     * 批量扫描结果
     */
    public static class BatchScanResult {
        private int totalFiles;
        private int cleanFiles;
        private int infectedFiles;
        private int suspiciousFiles;
        private int errorFiles;
        private List<AttachmentScanResult> infectedFiles = new ArrayList<>();
        private List<AttachmentScanResult> suspiciousFiles = new ArrayList<>();
        private LocalDateTime scanStartTime;
        private LocalDateTime scanEndTime;
        
        // Getters and Setters省略，为了简洁
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        
        public int getCleanFiles() { return cleanFiles; }
        public void setCleanFiles(int cleanFiles) { this.cleanFiles = cleanFiles; }
        
        public int getInfectedFiles() { return infectedFiles; }
        public void setInfectedFiles(int infectedFiles) { this.infectedFiles = infectedFiles; }
        
        public int getSuspiciousFiles() { return suspiciousFiles; }
        public void setSuspiciousFiles(int suspiciousFiles) { this.suspiciousFiles = suspiciousFiles; }
        
        public int getErrorFiles() { return errorFiles; }
        public void setErrorFiles(int errorFiles) { this.errorFiles = errorFiles; }
        
        public List<AttachmentScanResult> getInfectedFiles() { return infectedFiles; }
        public List<AttachmentScanResult> getSuspiciousFiles() { return suspiciousFiles; }
        
        public LocalDateTime getScanStartTime() { return scanStartTime; }
        public void setScanStartTime(LocalDateTime scanStartTime) { this.scanStartTime = scanStartTime; }
        
        public LocalDateTime getScanEndTime() { return scanEndTime; }
        public void setScanEndTime(LocalDateTime scanEndTime) { this.scanEndTime = scanEndTime; }
    }
    
    /**
     * 扫描统计
     */
    public static class ScanStatistics {
        private long totalScans;
        private long threatsFound;
        private long cleanScans;
        private long filesScanned;
        private double threatRate;
        
        // Getters and Setters
        public long getTotalScans() { return totalScans; }
        public void setTotalScans(long totalScans) { this.totalScans = totalScans; }
        
        public long getThreatsFound() { return threatsFound; }
        public void setThreatsFound(long threatsFound) { this.threatsFound = threatsFound; }
        
        public long getCleanScans() { return cleanScans; }
        public void setCleanScans(long cleanScans) { this.cleanScans = cleanScans; }
        
        public long getFilesScanned() { return filesScanned; }
        public void setFilesScanned(long filesScanned) { this.filesScanned = filesScanned; }
        
        public double getThreatRate() { return threatRate; }
        public void setThreatRate(double threatRate) { this.threatRate = threatRate; }
    }
}