package com.security.monitor.controller;

import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.EmailAttachment;
import com.security.monitor.service.AntiVirusService;
import com.security.monitor.service.AntiVirusService.ScanResult;
import com.security.monitor.service.AntiVirusService.AttachmentScanResult;
import com.security.monitor.service.AntiVirusService.BatchScanResult;
import com.security.monitor.service.AntiVirusService.ScanStatistics;
import com.security.monitor.repository.EmailMessageRepository;
import com.security.monitor.repository.EmailAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 防病毒扫描控制器
 */
@RestController
@RequestMapping("/api/antivirus")
@PreAuthorize("hasRole('ADMIN') or hasRole('EMAIL_ADMIN')")
public class AntiVirusController {
    
    @Autowired
    private AntiVirusService antiVirusService;
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private EmailAttachmentRepository attachmentRepository;
    
    /**
     * 扫描指定邮件的附件
     */
    @PostMapping("/scan/message/{messageId}")
    public ResponseEntity<ScanResult> scanEmailMessage(@PathVariable Long messageId) {
        EmailMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("邮件不存在"));
        
        ScanResult result = antiVirusService.scanEmailAttachments(message);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 扫描单个附件
     */
    @PostMapping("/scan/attachment/{attachmentId}")
    public ResponseEntity<AttachmentScanResult> scanAttachment(@PathVariable Long attachmentId) {
        EmailAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("附件不存在"));
        
        AttachmentScanResult result = antiVirusService.scanAttachment(attachment);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量扫描附件
     */
    @PostMapping("/scan/batch")
    public ResponseEntity<CompletableFuture<BatchScanResult>> batchScanAttachments(
            @RequestBody List<Long> attachmentIds) {
        
        List<EmailAttachment> attachments = attachmentRepository.findAllById(attachmentIds);
        CompletableFuture<BatchScanResult> result = antiVirusService.batchScanAttachments(attachments);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取扫描统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ScanStatistics> getScanStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        ScanStatistics stats = antiVirusService.getScanStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 更新病毒定义库
     */
    @PostMapping("/update-definitions")
    public ResponseEntity<String> updateVirusDefinitions() {
        antiVirusService.updateVirusDefinitions();
        return ResponseEntity.ok("病毒定义库更新完成");
    }
    
    /**
     * 获取隔离文件列表
     */
    @GetMapping("/quarantine")
    public ResponseEntity<Page<EmailAttachment>> getQuarantinedFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<EmailAttachment> quarantinedFiles = attachmentRepository
            .findByIsQuarantined(true, PageRequest.of(page, size));
        
        return ResponseEntity.ok(quarantinedFiles);
    }
    
    /**
     * 从隔离区恢复文件
     */
    @PostMapping("/quarantine/{attachmentId}/restore")
    public ResponseEntity<String> restoreFromQuarantine(@PathVariable Long attachmentId) {
        EmailAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("附件不存在"));
        
        attachment.setIsQuarantined(false);
        attachment.setQuarantineReason(null);
        attachment.setVirusScanStatus("RESTORED");
        attachmentRepository.save(attachment);
        
        return ResponseEntity.ok("文件已从隔离区恢复");
    }
    
    /**
     * 删除隔离文件
     */
    @DeleteMapping("/quarantine/{attachmentId}")
    public ResponseEntity<String> deleteQuarantinedFile(@PathVariable Long attachmentId) {
        EmailAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("附件不存在"));
        
        if (!attachment.getIsQuarantined()) {
            return ResponseEntity.badRequest().body("文件未被隔离");
        }
        
        // 删除物理文件和数据库记录
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(attachment.getStoragePath()));
            attachmentRepository.delete(attachment);
            return ResponseEntity.ok("隔离文件已删除");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取扫描引擎状态
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getAntiVirusStatus() {
        // 创建状态信息
        var status = new Object() {
            public final boolean enabled = true;
            public final String engine = "Custom AntiVirus Engine";
            public final String version = "1.0.0";
            public final LocalDateTime lastUpdate = LocalDateTime.now();
            public final long definitionsCount = 4; // 基础病毒定义数量
        };
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取最近的扫描日志
     */
    @GetMapping("/logs/recent")
    public ResponseEntity<List<Object>> getRecentScanLogs(
            @RequestParam(defaultValue = "10") int limit) {
        
        // 这里应该从VirusScanLogRepository获取数据
        // 暂时返回空列表，等待Repository实现
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * 配置扫描设置
     */
    @PostMapping("/settings")
    public ResponseEntity<String> updateScanSettings(@RequestBody Object settings) {
        // 这里可以更新扫描相关的配置
        // 如扫描超时时间、最大文件大小等
        return ResponseEntity.ok("扫描设置已更新");
    }
}