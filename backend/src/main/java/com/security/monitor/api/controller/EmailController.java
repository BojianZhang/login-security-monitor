package com.security.monitor.api.controller;

import com.security.monitor.api.dto.*;
import com.security.monitor.model.*;
import com.security.monitor.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 邮件管理API控制器
 */
@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Email Management", description = "邮件管理API")
public class EmailController {
    
    @Autowired
    private EmailMessageService messageService;
    
    @Autowired
    private EmailFolderService folderService;
    
    @Autowired
    private EmailAttachmentService attachmentService;
    
    @Autowired
    private EmailQueueService queueService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取邮件列表
     */
    @GetMapping
    @Operation(summary = "获取邮件列表", description = "分页获取用户的邮件列表")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmailMessageDTO>>> getEmails(
            Authentication auth,
            @Parameter(description = "文件夹ID") @RequestParam(required = false) Long folderId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "receivedAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        
        User user = userService.getUserByUsername(auth.getName());
        
        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<EmailMessage> messagePage;
        if (folderId != null) {
            EmailFolder folder = folderService.getFolderById(folderId);
            messagePage = messageService.getMessagesByUserAndFolder(user, folder, pageable);
        } else {
            messagePage = messageService.getMessagesByUser(user, pageable);
        }
        
        List<EmailMessageDTO> messageDTOs = messagePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        PaginationInfo pagination = new PaginationInfo(page, size, messagePage.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(messageDTOs, pagination));
    }
    
    /**
     * 根据ID获取邮件详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取邮件详情", description = "根据邮件ID获取详细信息")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmailMessageDTO>> getEmail(
            Authentication auth,
            @Parameter(description = "邮件ID", required = true) @PathVariable Long id) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        
        // 标记为已读
        if (!message.getIsRead()) {
            messageService.markAsRead(message);
        }
        
        EmailMessageDTO messageDTO = convertToDTO(message);
        
        return ResponseEntity.ok(ApiResponse.success(messageDTO));
    }
    
    /**
     * 发送邮件
     */
    @PostMapping("/send")
    @Operation(summary = "发送邮件", description = "发送新邮件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> sendEmail(
            Authentication auth,
            @Parameter(description = "发送邮件请求", required = true) @Valid @RequestBody SendEmailRequest request) {
        
        User user = userService.getUserByUsername(auth.getName());
        
        // 创建邮件对象
        EmailMessage message = new EmailMessage();
        message.setUser(user);
        message.setFromAddress(request.getFrom() != null ? request.getFrom() : user.getEmail());
        message.setToAddresses(String.join(",", request.getTo()));
        if (request.getCc() != null) message.setCcAddresses(String.join(",", request.getCc()));
        if (request.getBcc() != null) message.setBccAddresses(String.join(",", request.getBcc()));
        message.setSubject(request.getSubject());
        message.setBodyText(request.getBodyText());
        message.setBodyHtml(request.getBodyHtml());
        message.setPriorityLevel(request.getPriority() != null ? request.getPriority() : 3);
        message.setReplyTo(request.getReplyTo());
        
        // 发送邮件
        messageService.sendEmail(message);
        
        return ResponseEntity.ok(ApiResponse.success(null, "邮件发送成功"));
    }
    
    /**
     * 回复邮件
     */
    @PostMapping("/{id}/reply")
    @Operation(summary = "回复邮件", description = "回复指定邮件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> replyEmail(
            Authentication auth,
            @Parameter(description = "原邮件ID", required = true) @PathVariable Long id,
            @Parameter(description = "发送邮件请求", required = true) @Valid @RequestBody SendEmailRequest request) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage originalMessage = messageService.getMessageByIdAndUser(id, user);
        
        // 创建回复邮件
        EmailMessage replyMessage = new EmailMessage();
        replyMessage.setUser(user);
        replyMessage.setFromAddress(request.getFrom() != null ? request.getFrom() : user.getEmail());
        replyMessage.setToAddresses(originalMessage.getFromAddress());
        replyMessage.setSubject("Re: " + originalMessage.getSubject());
        replyMessage.setBodyText(request.getBodyText());
        replyMessage.setBodyHtml(request.getBodyHtml());
        replyMessage.setThreadId(originalMessage.getThreadId());
        replyMessage.setPriorityLevel(request.getPriority() != null ? request.getPriority() : 3);
        
        // 发送回复
        messageService.sendEmail(replyMessage);
        
        return ResponseEntity.ok(ApiResponse.success(null, "回复发送成功"));
    }
    
    /**
     * 转发邮件
     */
    @PostMapping("/{id}/forward")
    @Operation(summary = "转发邮件", description = "转发指定邮件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> forwardEmail(
            Authentication auth,
            @Parameter(description = "原邮件ID", required = true) @PathVariable Long id,
            @Parameter(description = "发送邮件请求", required = true) @Valid @RequestBody SendEmailRequest request) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage originalMessage = messageService.getMessageByIdAndUser(id, user);
        
        // 创建转发邮件
        EmailMessage forwardMessage = new EmailMessage();
        forwardMessage.setUser(user);
        forwardMessage.setFromAddress(request.getFrom() != null ? request.getFrom() : user.getEmail());
        forwardMessage.setToAddresses(String.join(",", request.getTo()));
        if (request.getCc() != null) forwardMessage.setCcAddresses(String.join(",", request.getCc()));
        forwardMessage.setSubject("Fwd: " + originalMessage.getSubject());
        
        // 构建转发内容
        String forwardContent = buildForwardContent(originalMessage, request.getBodyText());
        forwardMessage.setBodyText(forwardContent);
        forwardMessage.setBodyHtml(request.getBodyHtml());
        forwardMessage.setPriorityLevel(request.getPriority() != null ? request.getPriority() : 3);
        
        // 发送转发
        messageService.sendEmail(forwardMessage);
        
        return ResponseEntity.ok(ApiResponse.success(null, "转发发送成功"));
    }
    
    /**
     * 标记邮件为已读/未读
     */
    @PostMapping("/{id}/mark-read")
    @Operation(summary = "标记已读状态", description = "标记邮件为已读或未读")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication auth,
            @Parameter(description = "邮件ID", required = true) @PathVariable Long id,
            @Parameter(description = "是否已读") @RequestParam boolean isRead) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        
        if (isRead) {
            messageService.markAsRead(message);
        } else {
            messageService.markAsUnread(message);
        }
        
        return ResponseEntity.ok(ApiResponse.success(null, "邮件状态已更新"));
    }
    
    /**
     * 标记邮件为星标/取消星标
     */
    @PostMapping("/{id}/star")
    @Operation(summary = "设置星标状态", description = "设置或取消邮件星标")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> toggleStar(
            Authentication auth,
            @Parameter(description = "邮件ID", required = true) @PathVariable Long id,
            @Parameter(description = "是否星标") @RequestParam boolean isStarred) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        
        messageService.toggleStar(message, isStarred);
        
        return ResponseEntity.ok(ApiResponse.success(null, "星标状态已更新"));
    }
    
    /**
     * 移动邮件到文件夹
     */
    @PostMapping("/{id}/move")
    @Operation(summary = "移动邮件", description = "将邮件移动到指定文件夹")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> moveEmail(
            Authentication auth,
            @Parameter(description = "邮件ID", required = true) @PathVariable Long id,
            @Parameter(description = "目标文件夹ID", required = true) @RequestParam Long targetFolderId) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        EmailFolder targetFolder = folderService.getFolderByIdAndUser(targetFolderId, user);
        
        messageService.moveToFolder(message, targetFolder);
        
        return ResponseEntity.ok(ApiResponse.success(null, "邮件已移动"));
    }
    
    /**
     * 删除邮件
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除邮件", description = "删除指定邮件（移到垃圾箱）")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteEmail(
            Authentication auth,
            @Parameter(description = "邮件ID", required = true) @PathVariable Long id,
            @Parameter(description = "是否永久删除") @RequestParam(defaultValue = "false") boolean permanent) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        
        if (permanent) {
            messageService.permanentlyDelete(message);
        } else {
            messageService.moveToTrash(message);
        }
        
        return ResponseEntity.ok(ApiResponse.success(null, "邮件已删除"));
    }
    
    /**
     * 批量操作邮件
     */
    @PostMapping("/batch")
    @Operation(summary = "批量操作邮件", description = "对多个邮件执行批量操作")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> batchOperation(
            Authentication auth,
            @Parameter(description = "邮件ID列表", required = true) @RequestParam List<Long> messageIds,
            @Parameter(description = "操作类型", required = true) @RequestParam String operation,
            @Parameter(description = "目标文件夹ID") @RequestParam(required = false) Long targetFolderId) {
        
        User user = userService.getUserByUsername(auth.getName());
        
        switch (operation.toLowerCase()) {
            case "mark_read":
                messageService.batchMarkAsRead(messageIds, user);
                break;
            case "mark_unread":
                messageService.batchMarkAsUnread(messageIds, user);
                break;
            case "star":
                messageService.batchToggleStar(messageIds, user, true);
                break;
            case "unstar":
                messageService.batchToggleStar(messageIds, user, false);
                break;
            case "move":
                if (targetFolderId != null) {
                    EmailFolder targetFolder = folderService.getFolderByIdAndUser(targetFolderId, user);
                    messageService.batchMoveToFolder(messageIds, user, targetFolder);
                }
                break;
            case "delete":
                messageService.batchMoveToTrash(messageIds, user);
                break;
            case "permanent_delete":
                messageService.batchPermanentlyDelete(messageIds, user);
                break;
            default:
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("不支持的操作类型: " + operation));
        }
        
        return ResponseEntity.ok(ApiResponse.success(null, "批量操作完成"));
    }
    
    /**
     * 搜索邮件
     */
    @PostMapping("/search")
    @Operation(summary = "搜索邮件", description = "根据条件搜索邮件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmailMessageDTO>>> searchEmails(
            Authentication auth,
            @Parameter(description = "搜索条件", required = true) @Valid @RequestBody EmailSearchRequest request) {
        
        User user = userService.getUserByUsername(auth.getName());
        
        Sort.Direction direction = Sort.Direction.fromString(request.getSortDirection());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), 
                                         Sort.by(direction, request.getSortBy()));
        
        Page<EmailMessage> searchResults = messageService.searchMessages(user, request, pageable);
        
        List<EmailMessageDTO> messageDTOs = searchResults.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        PaginationInfo pagination = new PaginationInfo(request.getPage(), request.getSize(), 
                                                      searchResults.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(messageDTOs, pagination));
    }
    
    /**
     * 获取邮件附件
     */
    @GetMapping("/{id}/attachments")
    @Operation(summary = "获取邮件附件", description = "获取指定邮件的所有附件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmailAttachmentDTO>>> getEmailAttachments(
            Authentication auth,
            @Parameter(description = "邮件ID", required = true) @PathVariable Long id) {
        
        User user = userService.getUserByUsername(auth.getName());
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        
        List<EmailAttachmentDTO> attachments = attachmentService.getAttachmentsByMessage(message).stream()
                .map(this::convertAttachmentToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }
    
    /**
     * 下载邮件附件
     */
    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "下载附件", description = "下载指定的邮件附件")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadAttachment(
            Authentication auth,
            @Parameter(description = "附件ID", required = true) @PathVariable Long attachmentId) {
        
        User user = userService.getUserByUsername(auth.getName());
        return attachmentService.downloadAttachment(attachmentId, user);
    }
    
    // 私有方法：DTO转换
    
    private EmailMessageDTO convertToDTO(EmailMessage message) {
        EmailMessageDTO dto = new EmailMessageDTO();
        dto.setId(message.getId());
        dto.setMessageId(message.getMessageId());
        dto.setThreadId(message.getThreadId());
        dto.setSubject(message.getSubject());
        dto.setFromAddress(message.getFromAddress());
        
        // 解析收件人地址
        if (message.getToAddresses() != null) {
            dto.setToAddresses(Arrays.asList(message.getToAddresses().split(",")));
        }
        if (message.getCcAddresses() != null) {
            dto.setCcAddresses(Arrays.asList(message.getCcAddresses().split(",")));
        }
        if (message.getBccAddresses() != null) {
            dto.setBccAddresses(Arrays.asList(message.getBccAddresses().split(",")));
        }
        
        dto.setReplyTo(message.getReplyTo());
        dto.setBodyText(message.getBodyText());
        dto.setBodyHtml(message.getBodyHtml());
        dto.setMessageSize(message.getMessageSize());
        dto.setIsRead(message.getIsRead());
        dto.setIsStarred(message.getIsStarred());
        dto.setIsDeleted(message.getIsDeleted());
        dto.setIsSpam(message.getIsSpam());
        dto.setPriorityLevel(message.getPriorityLevel());
        dto.setReceivedAt(message.getReceivedAt());
        dto.setSentAt(message.getSentAt());
        dto.setCreatedAt(message.getCreatedAt());
        
        if (message.getFolder() != null) {
            dto.setFolderName(message.getFolder().getFolderName());
        }
        
        return dto;
    }
    
    private EmailAttachmentDTO convertAttachmentToDTO(EmailAttachment attachment) {
        EmailAttachmentDTO dto = new EmailAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFilename(attachment.getFilename());
        dto.setContentType(attachment.getContentType());
        dto.setFileSize(attachment.getFileSize());
        dto.setFileHash(attachment.getFileHash());
        dto.setIsInline(attachment.getIsInline());
        dto.setContentId(attachment.getContentId());
        dto.setIsQuarantined(attachment.getIsQuarantined());
        dto.setQuarantineReason(attachment.getQuarantineReason());
        dto.setVirusScanStatus(attachment.getVirusScanStatus());
        dto.setLastScannedAt(attachment.getLastScannedAt());
        dto.setCreatedAt(attachment.getCreatedAt());
        
        return dto;
    }
    
    private String buildForwardContent(EmailMessage originalMessage, String additionalText) {
        StringBuilder content = new StringBuilder();
        
        if (additionalText != null && !additionalText.trim().isEmpty()) {
            content.append(additionalText).append("\n\n");
        }
        
        content.append("---------- Forwarded message ----------\n");
        content.append("From: ").append(originalMessage.getFromAddress()).append("\n");
        content.append("Date: ").append(originalMessage.getReceivedAt()).append("\n");
        content.append("Subject: ").append(originalMessage.getSubject()).append("\n");
        content.append("To: ").append(originalMessage.getToAddresses()).append("\n\n");
        content.append(originalMessage.getBodyText());
        
        return content.toString();
    }
}