package com.security.monitor.api.controller;

import com.security.monitor.api.dto.ApiResponse;
import com.security.monitor.service.protocol.MailProtocolManager;
import com.security.monitor.service.protocol.OptimizedImapServer;
import com.security.monitor.service.protocol.OptimizedPop3Server;
import com.security.monitor.service.protocol.OptimizedSmtpServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 邮件协议管理API控制器
 */
@RestController
@RequestMapping("/api/v1/protocol")
@Tag(name = "邮件协议管理", description = "SMTP/IMAP/POP3协议服务器管理API")
public class MailProtocolController {
    
    @Autowired
    private MailProtocolManager protocolManager;
    
    @Autowired
    private OptimizedSmtpServer smtpServer;
    
    @Autowired
    private OptimizedImapServer imapServer;
    
    @Autowired
    private OptimizedPop3Server pop3Server;
    
    /**
     * 获取所有协议服务器状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取协议服务器状态", description = "获取SMTP/IMAP/POP3服务器的运行状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MailProtocolManager.MailProtocolStatus>> getProtocolStatus() {
        MailProtocolManager.MailProtocolStatus status = protocolManager.getOverallStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 获取SMTP服务器状态
     */
    @GetMapping("/smtp/status")
    @Operation(summary = "获取SMTP服务器状态", description = "获取SMTP服务器的详细运行状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizedSmtpServer.SmtpServerStatus>> getSmtpStatus() {
        OptimizedSmtpServer.SmtpServerStatus status = smtpServer.getStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 获取IMAP服务器状态
     */
    @GetMapping("/imap/status")
    @Operation(summary = "获取IMAP服务器状态", description = "获取IMAP服务器的详细运行状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizedImapServer.ImapServerStatus>> getImapStatus() {
        OptimizedImapServer.ImapServerStatus status = imapServer.getStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 获取POP3服务器状态
     */
    @GetMapping("/pop3/status")
    @Operation(summary = "获取POP3服务器状态", description = "获取POP3服务器的详细运行状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizedPop3Server.Pop3ServerStatus>> getPop3Status() {
        OptimizedPop3Server.Pop3ServerStatus status = pop3Server.getStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 重启SMTP服务器
     */
    @PostMapping("/smtp/restart")
    @Operation(summary = "重启SMTP服务器", description = "重启SMTP服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restartSmtpServer() {
        try {
            smtpServer.stop();
            Thread.sleep(2000);
            smtpServer.start();
            return ResponseEntity.ok(ApiResponse.success("", "SMTP服务器重启成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("SMTP服务器重启失败: " + e.getMessage(), "PROTOCOL_001"));
        }
    }
    
    /**
     * 重启IMAP服务器
     */
    @PostMapping("/imap/restart")
    @Operation(summary = "重启IMAP服务器", description = "重启IMAP服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restartImapServer() {
        try {
            imapServer.stop();
            Thread.sleep(2000);
            imapServer.start();
            return ResponseEntity.ok(ApiResponse.success("", "IMAP服务器重启成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("IMAP服务器重启失败: " + e.getMessage(), "PROTOCOL_002"));
        }
    }
    
    /**
     * 重启POP3服务器
     */
    @PostMapping("/pop3/restart")
    @Operation(summary = "重启POP3服务器", description = "重启POP3服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restartPop3Server() {
        try {
            pop3Server.stop();
            Thread.sleep(2000);
            pop3Server.start();
            return ResponseEntity.ok(ApiResponse.success("", "POP3服务器重启成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("POP3服务器重启失败: " + e.getMessage(), "PROTOCOL_003"));
        }
    }
    
    /**
     * 重启所有协议服务器
     */
    @PostMapping("/restart-all")
    @Operation(summary = "重启所有协议服务器", description = "重启SMTP、IMAP、POP3所有协议服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> restartAllServers() {
        try {
            // 停止所有服务器
            smtpServer.stop();
            imapServer.stop();
            pop3Server.stop();
            
            Thread.sleep(3000); // 等待3秒
            
            // 启动所有服务器
            smtpServer.start();
            imapServer.start();
            pop3Server.start();
            
            return ResponseEntity.ok(ApiResponse.success("", "所有协议服务器重启成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("协议服务器重启失败: " + e.getMessage(), "PROTOCOL_004"));
        }
    }
    
    /**
     * 停止SMTP服务器
     */
    @PostMapping("/smtp/stop")
    @Operation(summary = "停止SMTP服务器", description = "停止SMTP服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> stopSmtpServer() {
        try {
            smtpServer.stop();
            return ResponseEntity.ok(ApiResponse.success("", "SMTP服务器已停止"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("停止SMTP服务器失败: " + e.getMessage(), "PROTOCOL_005"));
        }
    }
    
    /**
     * 启动SMTP服务器
     */
    @PostMapping("/smtp/start")
    @Operation(summary = "启动SMTP服务器", description = "启动SMTP服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> startSmtpServer() {
        try {
            smtpServer.start();
            return ResponseEntity.ok(ApiResponse.success("", "SMTP服务器已启动"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("启动SMTP服务器失败: " + e.getMessage(), "PROTOCOL_006"));
        }
    }
    
    /**
     * 停止IMAP服务器
     */
    @PostMapping("/imap/stop")
    @Operation(summary = "停止IMAP服务器", description = "停止IMAP服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> stopImapServer() {
        try {
            imapServer.stop();
            return ResponseEntity.ok(ApiResponse.success("", "IMAP服务器已停止"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("停止IMAP服务器失败: " + e.getMessage(), "PROTOCOL_007"));
        }
    }
    
    /**
     * 启动IMAP服务器
     */
    @PostMapping("/imap/start")
    @Operation(summary = "启动IMAP服务器", description = "启动IMAP服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> startImapServer() {
        try {
            imapServer.start();
            return ResponseEntity.ok(ApiResponse.success("", "IMAP服务器已启动"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("启动IMAP服务器失败: " + e.getMessage(), "PROTOCOL_008"));
        }
    }
    
    /**
     * 停止POP3服务器
     */
    @PostMapping("/pop3/stop")
    @Operation(summary = "停止POP3服务器", description = "停止POP3服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> stopPop3Server() {
        try {
            pop3Server.stop();
            return ResponseEntity.ok(ApiResponse.success("", "POP3服务器已停止"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("停止POP3服务器失败: " + e.getMessage(), "PROTOCOL_009"));
        }
    }
    
    /**
     * 启动POP3服务器
     */
    @PostMapping("/pop3/start")
    @Operation(summary = "启动POP3服务器", description = "启动POP3服务器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> startPop3Server() {
        try {
            pop3Server.start();
            return ResponseEntity.ok(ApiResponse.success("", "POP3服务器已启动"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("启动POP3服务器失败: " + e.getMessage(), "PROTOCOL_010"));
        }
    }
}