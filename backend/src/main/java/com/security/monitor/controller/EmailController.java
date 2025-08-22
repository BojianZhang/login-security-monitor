package com.security.monitor.controller;

import com.security.monitor.model.AutoReplyHistory;
import com.security.monitor.model.AutoReplySettings;
import com.security.monitor.model.EmailAlias;
import com.security.monitor.model.EmailDomain;
import com.security.monitor.model.EmailFolder;
import com.security.monitor.model.EmailForwardingRule;
import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.User;
import com.security.monitor.service.AutoReplyService;
import com.security.monitor.service.EmailForwardingService;
import com.security.monitor.service.EmailManagementService;
import com.security.monitor.service.EmailReceiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邮件管理控制器 - 安全增强版
 */
@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = {"http://localhost:3000", "https://yourdomain.com"}, maxAge = 3600)
@Validated
public class EmailController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    
    @Autowired
    private EmailManagementService emailService;
    
    @Autowired
    private EmailReceiveService emailReceiveService;
    
    @Autowired
    private AutoReplyService autoReplyService;
    
    @Autowired
    private EmailForwardingService forwardingService;
    
    /**
     * 获取用户的邮件统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserEmailStats(@AuthenticationPrincipal User user) {
        try {
            Map<String, Object> stats = emailService.getUserEmailStatistics(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取用户邮件统计失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户的所有邮箱别名
     */
    @GetMapping("/aliases")
    public ResponseEntity<List<EmailAlias>> getUserAliases(@AuthenticationPrincipal User user) {
        try {
            List<EmailAlias> aliases = emailService.getUserAliases(user);
            return ResponseEntity.ok(aliases);
        } catch (Exception e) {
            logger.error("获取用户别名失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取用户在指定域名的别名
     */
    @GetMapping("/aliases/domain/{domainId}")
    public ResponseEntity<List<EmailAlias>> getUserAliasesByDomain(
            @AuthenticationPrincipal User user, 
            @PathVariable Long domainId) {
        try {
            List<EmailAlias> aliases = emailService.getUserAliasesByDomain(user, domainId);
            return ResponseEntity.ok(aliases);
        } catch (Exception e) {
            logger.error("获取用户域名别名失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建新的邮箱别名
     */
    @PostMapping("/aliases")
    public ResponseEntity<Map<String, Object>> createAlias(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid AliasCreateRequest request) {
        try {
            EmailAlias alias = emailService.createAlias(user, request.getAliasEmail(), request.getDomainId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "邮箱别名创建成功");
            response.put("alias", alias);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建邮箱别名失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除邮箱别名
     */
    @DeleteMapping("/aliases/{aliasId}")
    public ResponseEntity<Map<String, Object>> deleteAlias(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            emailService.deleteAlias(user, aliasId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "邮箱别名删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除邮箱别名失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 切换别名激活状态
     */
    @PutMapping("/aliases/{aliasId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAliasStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            EmailAlias alias = emailService.toggleAliasStatus(user, aliasId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", alias.getIsActive() ? "别名已激活" : "别名已禁用");
            response.put("alias", alias);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("切换别名状态失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 设置别名转发
     */
    @PutMapping("/aliases/{aliasId}/forward")
    public ResponseEntity<Map<String, Object>> setAliasForward(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId,
            @RequestBody @Valid ForwardRequest request) {
        try {
            EmailAlias alias = emailService.setAliasForward(user, aliasId, request.getForwardTo());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "转发设置成功");
            response.put("alias", alias);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("设置别名转发失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新别名显示名称和描述
     */
    @PutMapping("/aliases/{aliasId}/display")
    public ResponseEntity<Map<String, Object>> updateAliasDisplay(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId,
            @RequestBody @Valid AliasDisplayRequest request) {
        try {
            EmailAlias alias = emailService.updateAliasDisplay(user, aliasId, 
                request.getDisplayName(), request.getDescription(), request.getExternalAliasId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "别名显示信息更新成功");
            response.put("alias", alias);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新别名显示信息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量更新别名显示名称（用于与外部平台同步）
     */
    @PostMapping("/aliases/sync-display-names")
    public ResponseEntity<Map<String, Object>> syncAliasDisplayNames(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid List<AliasSyncRequest> requests) {
        try {
            int updatedCount = emailService.syncAliasDisplayNames(user, requests);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量同步完成");
            response.put("updatedCount", updatedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量同步别名显示名称失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 搜索别名（根据显示名称或别名前缀）
     */
    @GetMapping("/aliases/search")
    public ResponseEntity<List<EmailAlias>> searchAliases(
            @AuthenticationPrincipal User user,
            @RequestParam String keyword) {
        try {
            List<EmailAlias> aliases = emailService.searchAliases(user, keyword);
            return ResponseEntity.ok(aliases);
        } catch (Exception e) {
            logger.error("搜索别名失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取活跃的域名列表
     */
    @GetMapping("/domains")
    public ResponseEntity<List<EmailDomain>> getActiveDomains() {
        try {
            List<EmailDomain> domains = emailService.getActiveDomains();
            return ResponseEntity.ok(domains);
        } catch (Exception e) {
            logger.error("获取域名列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取用户的文件夹列表
     */
    @GetMapping("/folders")
    public ResponseEntity<List<EmailFolder>> getUserFolders(@AuthenticationPrincipal User user) {
        try {
            List<EmailFolder> folders = emailService.getUserFolders(user);
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            logger.error("获取用户文件夹失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建自定义文件夹
     */
    @PostMapping("/folders")
    public ResponseEntity<Map<String, Object>> createFolder(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid FolderCreateRequest request) {
        try {
            EmailFolder folder = emailService.createCustomFolder(user, request.getFolderName(), request.getParentId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "文件夹创建成功");
            response.put("folder", folder);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建文件夹失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取文件夹中的消息列表
     */
    @GetMapping("/folders/{folderId}/messages")
    public ResponseEntity<Page<EmailMessage>> getFolderMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<EmailMessage> messages = emailService.getUserMessages(user, folderId, pageable);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("获取文件夹消息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 搜索邮件
     */
    @GetMapping("/messages/search")
    public ResponseEntity<Page<EmailMessage>> searchMessages(
            @AuthenticationPrincipal User user,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<EmailMessage> messages = emailService.searchUserMessages(user, keyword, pageable);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("搜索邮件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 标记消息为已读
     */
    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<Map<String, Object>> markMessageAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long messageId) {
        try {
            emailService.markMessageAsRead(user, messageId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "消息已标记为已读");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("标记消息已读失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的邮件地址列表（带统计信息）
     */
    @GetMapping("/aliases/overview")
    public ResponseEntity<List<EmailReceiveService.EmailAliasOverview>> getUserAliasesOverview(@AuthenticationPrincipal User user) {
        try {
            List<EmailReceiveService.EmailAliasOverview> overview = emailReceiveService.getUserAliasesOverview(user);
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            logger.error("获取用户别名概览失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定别名的邮件列表（核心功能：点击别名查看邮件）
     */
    @GetMapping("/aliases/{aliasId}/messages")
    public ResponseEntity<Page<EmailMessage>> getAliasMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 验证用户权限
            List<EmailAlias> userAliases = emailService.getUserAliases(user);
            boolean hasPermission = userAliases.stream()
                    .anyMatch(alias -> alias.getId().equals(aliasId));
            
            if (!hasPermission) {
                return ResponseEntity.status(403).build();
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EmailMessage> messages = emailReceiveService.getMessagesByAlias(aliasId, pageable);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("获取别名邮件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取别名统计信息
     */
    @GetMapping("/aliases/{aliasId}/stats")
    public ResponseEntity<EmailReceiveService.EmailAliasStats> getAliasStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            // 验证用户权限
            List<EmailAlias> userAliases = emailService.getUserAliases(user);
            boolean hasPermission = userAliases.stream()
                    .anyMatch(alias -> alias.getId().equals(aliasId));
            
            if (!hasPermission) {
                return ResponseEntity.status(403).build();
            }
            
            EmailReceiveService.EmailAliasStats stats = emailReceiveService.getAliasStats(aliasId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取别名统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 标记别名的所有邮件为已读
     */
    @PutMapping("/aliases/{aliasId}/mark-read")
    public ResponseEntity<Map<String, Object>> markAliasMessagesAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            emailReceiveService.markAliasMessagesAsRead(aliasId, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "邮件已标记为已读");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("标记邮件已读失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的邮件地址列表
     */
    @GetMapping("/addresses")
    public ResponseEntity<List<String>> getUserEmailAddresses(@AuthenticationPrincipal User user) {
        try {
            List<String> addresses = emailService.getUserEmailAddresses(user);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            logger.error("获取用户邮件地址失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 初始化用户邮件系统（创建默认文件夹等）
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeUserEmail(@AuthenticationPrincipal User user) {
        try {
            emailService.initializeUserFolders(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "邮件系统初始化成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("初始化用户邮件系统失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取别名的转发规则
     */
    @GetMapping("/aliases/{aliasId}/forwarding")
    public ResponseEntity<List<EmailForwardingRule>> getForwardingRules(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            List<EmailForwardingRule> rules = forwardingService.getForwardingRules(user, aliasId);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("获取转发规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建转发规则
     */
    @PostMapping("/aliases/{aliasId}/forwarding")
    public ResponseEntity<Map<String, Object>> createForwardingRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId,
            @RequestBody @Valid ForwardingRuleRequest request) {
        try {
            EmailForwardingRule rule = forwardingService.createForwardingRule(user, aliasId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "转发规则创建成功");
            response.put("rule", rule);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建转发规则失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新转发规则
     */
    @PutMapping("/forwarding/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateForwardingRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long ruleId,
            @RequestBody @Valid ForwardingRuleRequest request) {
        try {
            EmailForwardingRule rule = forwardingService.updateForwardingRule(user, ruleId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "转发规则更新成功");
            response.put("rule", rule);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新转发规则失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除转发规则
     */
    @DeleteMapping("/forwarding/{ruleId}")
    public ResponseEntity<Map<String, Object>> deleteForwardingRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long ruleId) {
        try {
            forwardingService.deleteForwardingRule(user, ruleId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "转发规则删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除转发规则失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 切换转发规则状态
     */
    @PutMapping("/forwarding/{ruleId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleForwardingRuleStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long ruleId) {
        try {
            EmailForwardingRule rule = forwardingService.toggleRuleStatus(user, ruleId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", rule.getIsActive() ? "转发规则已激活" : "转发规则已禁用");
            response.put("rule", rule);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("切换转发规则状态失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的所有转发规则
     */
    @GetMapping("/forwarding")
    public ResponseEntity<List<EmailForwardingRule>> getUserForwardingRules(@AuthenticationPrincipal User user) {
        try {
            List<EmailForwardingRule> rules = forwardingService.getUserForwardingRules(user);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("获取用户转发规则失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取别名的自动回复设置
     */
    @GetMapping("/aliases/{aliasId}/auto-reply")
    public ResponseEntity<AutoReplySettings> getAutoReplySettings(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            Optional<AutoReplySettings> settings = autoReplyService.getAutoReplySettings(user, aliasId);
            return settings.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("获取自动回复设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建或更新别名的自动回复设置
     */
    @PostMapping("/aliases/{aliasId}/auto-reply")
    public ResponseEntity<Map<String, Object>> createOrUpdateAutoReplySettings(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId,
            @RequestBody @Valid AutoReplyRequest request) {
        try {
            AutoReplySettings settings = autoReplyService.createOrUpdateAutoReplySettings(user, aliasId, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "自动回复设置保存成功");
            response.put("settings", settings);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存自动回复设置失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除别名的自动回复设置
     */
    @DeleteMapping("/aliases/{aliasId}/auto-reply")
    public ResponseEntity<Map<String, Object>> deleteAutoReplySettings(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            autoReplyService.deleteAutoReplySettings(user, aliasId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "自动回复设置删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除自动回复设置失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的所有自动回复设置
     */
    @GetMapping("/auto-reply")
    public ResponseEntity<List<AutoReplySettings>> getUserAutoReplySettings(@AuthenticationPrincipal User user) {
        try {
            List<AutoReplySettings> settings = autoReplyService.getUserAutoReplySettings(user);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            logger.error("获取用户自动回复设置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取自动回复历史
     */
    @GetMapping("/auto-reply/{settingsId}/history")
    public ResponseEntity<List<AutoReplyHistory>> getAutoReplyHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long settingsId) {
        try {
            List<AutoReplyHistory> history = autoReplyService.getAutoReplyHistory(user, settingsId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取自动回复历史失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 内部请求类 - 增强验证
    public static class AliasCreateRequest {
        @NotBlank(message = "别名前缀不能为空")
        @Size(min = 1, max = 64, message = "别名长度必须在1-64字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "别名只能包含字母、数字、点号、下划线和短横线")
        private String aliasEmail;
        
        @NotBlank(message = "域名ID不能为空")
        private Long domainId;
        
        public String getAliasEmail() {
            return aliasEmail;
        }
        
        public void setAliasEmail(String aliasEmail) {
            this.aliasEmail = aliasEmail != null ? aliasEmail.toLowerCase().trim() : null;
        }
        
        public Long getDomainId() {
            return domainId;
        }
        
        public void setDomainId(Long domainId) {
            this.domainId = domainId;
        }
    }
    
    public static class ForwardRequest {
        @Email(message = "转发地址格式不正确")
        @Size(max = 320, message = "邮件地址长度不能超过320字符")
        private String forwardTo;
        
        public String getForwardTo() {
            return forwardTo;
        }
        
        public void setForwardTo(String forwardTo) {
            this.forwardTo = forwardTo != null ? forwardTo.trim() : null;
        }
    }
    
    public static class FolderCreateRequest {
        @NotBlank(message = "文件夹名称不能为空")
        @Size(min = 1, max = 100, message = "文件夹名称长度必须在1-100字符之间")
        @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9._\\-\\s]+$", message = "文件夹名称包含非法字符")
        private String folderName;
        
        private Long parentId;
        
        public String getFolderName() {
            return folderName;
        }
        
        public void setFolderName(String folderName) {
            this.folderName = folderName != null ? folderName.trim() : null;
        }
        
        public Long getParentId() {
            return parentId;
        }
        
        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }
    }
    
    public static class AutoReplyRequest {
        private Boolean isEnabled = false;
        
        @NotBlank(message = "回复主题不能为空")
        @Size(max = 200, message = "回复主题长度不能超过200字符")
        private String replySubject;
        
        @NotBlank(message = "回复内容不能为空") 
        @Size(max = 5000, message = "回复内容长度不能超过5000字符")
        private String replyContent;
        
        private java.time.LocalDateTime startDate;
        private java.time.LocalDateTime endDate;
        
        private Boolean onlyExternal = false;
        
        @jakarta.validation.constraints.Min(value = 1, message = "每个发件人最大回复次数不能小于1")
        @jakarta.validation.constraints.Max(value = 100, message = "每个发件人最大回复次数不能大于100")
        private Integer maxRepliesPerSender = 1;
        
        // Getters and Setters
        public Boolean getIsEnabled() { return isEnabled; }
        public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
        
        public String getReplySubject() { return replySubject; }
        public void setReplySubject(String replySubject) { 
            this.replySubject = replySubject != null ? replySubject.trim() : null; 
        }
        
        public String getReplyContent() { return replyContent; }
        public void setReplyContent(String replyContent) { 
            this.replyContent = replyContent != null ? replyContent.trim() : null; 
        }
        
        public java.time.LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(java.time.LocalDateTime startDate) { this.startDate = startDate; }
        
        public java.time.LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(java.time.LocalDateTime endDate) { this.endDate = endDate; }
        
        public Boolean getOnlyExternal() { return onlyExternal; }
        public void setOnlyExternal(Boolean onlyExternal) { this.onlyExternal = onlyExternal; }
        
        public Integer getMaxRepliesPerSender() { return maxRepliesPerSender; }
        public void setMaxRepliesPerSender(Integer maxRepliesPerSender) { 
            this.maxRepliesPerSender = maxRepliesPerSender; 
        }
    }
    
    public static class ForwardingRuleRequest {
        @NotBlank(message = "规则名称不能为空")
        @Size(min = 1, max = 100, message = "规则名称长度必须在1-100字符之间")
        private String ruleName;
        
        @Email(message = "转发地址格式不正确")
        @NotBlank(message = "转发地址不能为空")
        @Size(max = 320, message = "转发地址长度不能超过320字符")
        private String forwardTo;
        
        private Boolean isActive = true;
        
        @Size(max = 200, message = "转发主题长度不能超过200字符")
        private String forwardSubject;
        
        private Boolean keepOriginal = true;
        
        @Size(max = 1000, message = "转发条件长度不能超过1000字符")
        private String conditions;
        
        @jakarta.validation.constraints.Min(value = 0, message = "优先级不能小于0")
        @jakarta.validation.constraints.Max(value = 100, message = "优先级不能大于100")
        private Integer priority = 0;
        
        private Boolean continueProcessing = false;
        
        // Getters and Setters
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { 
            this.ruleName = ruleName != null ? ruleName.trim() : null; 
        }
        
        public String getForwardTo() { return forwardTo; }
        public void setForwardTo(String forwardTo) { 
            this.forwardTo = forwardTo != null ? forwardTo.trim() : null; 
        }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        
        public String getForwardSubject() { return forwardSubject; }
        public void setForwardSubject(String forwardSubject) { 
            this.forwardSubject = forwardSubject != null ? forwardSubject.trim() : null; 
        }
        
        public Boolean getKeepOriginal() { return keepOriginal; }
        public void setKeepOriginal(Boolean keepOriginal) { this.keepOriginal = keepOriginal; }
        
        public String getConditions() { return conditions; }
        public void setConditions(String conditions) { 
            this.conditions = conditions != null ? conditions.trim() : null; 
        }
        
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        
        public Boolean getContinueProcessing() { return continueProcessing; }
        public void setContinueProcessing(Boolean continueProcessing) { 
            this.continueProcessing = continueProcessing; 
        }
    }
    
    public static class AliasDisplayRequest {
        @Size(max = 100, message = "显示名称长度不能超过100字符")
        private String displayName;
        
        @Size(max = 500, message = "描述长度不能超过500字符")
        private String description;
        
        @Size(max = 100, message = "外部别名ID长度不能超过100字符")
        private String externalAliasId;
        
        // Getters and Setters
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { 
            this.displayName = displayName != null ? displayName.trim() : null; 
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { 
            this.description = description != null ? description.trim() : null; 
        }
        
        public String getExternalAliasId() { return externalAliasId; }
        public void setExternalAliasId(String externalAliasId) { 
            this.externalAliasId = externalAliasId != null ? externalAliasId.trim() : null; 
        }
    }
    
    public static class AliasSyncRequest {
        @NotBlank(message = "邮箱地址不能为空")
        private String fullEmail; // 完整邮箱地址，用于匹配现有别名
        
        @Size(max = 100, message = "显示名称长度不能超过100字符")
        private String displayName;
        
        @Size(max = 100, message = "外部别名ID长度不能超过100字符")
        private String externalAliasId;
        
        @Size(max = 500, message = "描述长度不能超过500字符")
        private String description;
        
        // Getters and Setters
        public String getFullEmail() { return fullEmail; }
        public void setFullEmail(String fullEmail) { 
            this.fullEmail = fullEmail != null ? fullEmail.trim().toLowerCase() : null; 
        }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { 
            this.displayName = displayName != null ? displayName.trim() : null; 
        }
        
        public String getExternalAliasId() { return externalAliasId; }
        public void setExternalAliasId(String externalAliasId) { 
            this.externalAliasId = externalAliasId != null ? externalAliasId.trim() : null; 
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { 
            this.description = description != null ? description.trim() : null; 
        }
    }
}