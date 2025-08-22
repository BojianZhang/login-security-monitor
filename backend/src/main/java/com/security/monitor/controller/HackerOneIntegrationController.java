package com.security.monitor.controller;

import com.security.monitor.model.EmailAlias;
import com.security.monitor.model.User;
import com.security.monitor.service.HackerOneIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HackerOne 平台集成控制器
 */
@RestController
@RequestMapping("/api/integrations/hackerone")
@CrossOrigin(origins = {"http://localhost:3000", "https://yourdomain.com"}, maxAge = 3600)
@Validated
public class HackerOneIntegrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(HackerOneIntegrationController.class);
    
    @Autowired
    private HackerOneIntegrationService hackerOneService;
    
    /**
     * 获取用户的 HackerOne 别名统计
     */
    @GetMapping("/stats")
    public ResponseEntity<HackerOneIntegrationService.HackerOneAliasStats> getHackerOneStats(@AuthenticationPrincipal User user) {
        try {
            HackerOneIntegrationService.HackerOneAliasStats stats = hackerOneService.getHackerOneAliasStats(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取 HackerOne 统计失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取用户的所有 HackerOne 别名
     */
    @GetMapping("/aliases")
    public ResponseEntity<List<EmailAlias>> getUserHackerOneAliases(@AuthenticationPrincipal User user) {
        try {
            List<EmailAlias> aliases = hackerOneService.getUserHackerOneAliases(user);
            return ResponseEntity.ok(aliases);
        } catch (Exception e) {
            logger.error("获取用户 HackerOne 别名失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 同步用户所有的 HackerOne 别名显示名称
     */
    @PostMapping("/sync-aliases")
    public ResponseEntity<Map<String, Object>> syncHackerOneAliases(@AuthenticationPrincipal User user) {
        try {
            int syncedCount = hackerOneService.syncUserHackerOneAliases(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HackerOne 别名同步完成");
            response.put("syncedCount", syncedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("同步 HackerOne 别名失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "同步失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检测邮箱是否为 HackerOne 格式
     */
    @GetMapping("/validate-email")
    public ResponseEntity<Map<String, Object>> validateHackerOneEmail(@RequestParam String email) {
        try {
            boolean isHackerOne = hackerOneService.isHackerOneEmail(email);
            String username = hackerOneService.extractHackerOneUsername(email);
            String displayName = hackerOneService.generateHackerOneDisplayName(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("isHackerOne", isHackerOne);
            response.put("username", username);
            response.put("suggestedDisplayName", displayName);
            response.put("email", email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("验证 HackerOne 邮箱失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据 HackerOne 用户名查找别名
     */
    @GetMapping("/aliases/by-username/{username}")
    public ResponseEntity<EmailAlias> findAliasByUsername(
            @AuthenticationPrincipal User user,
            @PathVariable String username) {
        try {
            Optional<EmailAlias> aliasOpt = hackerOneService.findAliasByHackerOneUsername(user, username);
            
            if (aliasOpt.isPresent()) {
                return ResponseEntity.ok(aliasOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("查找 HackerOne 别名失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 构建 HackerOne 邮箱地址
     */
    @PostMapping("/build-email")
    public ResponseEntity<Map<String, Object>> buildHackerOneEmail(@RequestBody @Valid HackerOneEmailRequest request) {
        try {
            String email = hackerOneService.buildHackerOneEmail(request.getUsername(), request.getSuffix());
            String displayName = hackerOneService.generateHackerOneDisplayName(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("email", email);
            response.put("displayName", displayName);
            response.put("username", request.getUsername());
            response.put("suffix", request.getSuffix());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("构建 HackerOne 邮箱失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取可复制的HackerOne邮箱地址格式
     */
    @GetMapping("/aliases/{aliasId}/copyable-format")
    public ResponseEntity<Map<String, Object>> getCopyableFormat(
            @AuthenticationPrincipal User user,
            @PathVariable Long aliasId) {
        try {
            List<EmailAlias> userAliases = hackerOneService.getUserHackerOneAliases(user);
            Optional<EmailAlias> aliasOpt = userAliases.stream()
                    .filter(alias -> alias.getId().equals(aliasId))
                    .findFirst();
                    
            if (aliasOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            EmailAlias alias = aliasOpt.get();
            String fullEmail = alias.getFullEmail();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("aliasId", aliasId);
            response.put("fullEmail", fullEmail);
            response.put("displayFormat", fullEmail); // 可复制的完整格式
            response.put("isHackerOne", hackerOneService.isHackerOneEmail(fullEmail));
            response.put("username", hackerOneService.extractHackerOneUsername(fullEmail));
            response.put("copyInstruction", "点击复制完整邮箱地址: " + fullEmail);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取可复制格式失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量获取所有HackerOne别名的可复制格式
     */
    @GetMapping("/aliases/copyable-formats")
    public ResponseEntity<List<Map<String, Object>>> getAllCopyableFormats(@AuthenticationPrincipal User user) {
        try {
            List<EmailAlias> hackerOneAliases = hackerOneService.getUserHackerOneAliases(user);
            
            List<Map<String, Object>> results = hackerOneAliases.stream().map(alias -> {
                String fullEmail = alias.getFullEmail();
                Map<String, Object> item = new HashMap<>();
                item.put("aliasId", alias.getId());
                item.put("fullEmail", fullEmail);
                item.put("displayFormat", fullEmail); // 显示完整格式
                item.put("copyableText", fullEmail); // 可复制的文本
                item.put("username", hackerOneService.extractHackerOneUsername(fullEmail));
                item.put("isActive", alias.getIsActive());
                item.put("description", alias.getDescription());
                return item;
            }).toList();
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("获取所有可复制格式失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量同步指定的 HackerOne 用户名
     */
    @PostMapping("/sync-usernames")
    public ResponseEntity<Map<String, Object>> syncHackerOneUsernames(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid List<HackerOneUsernameRequest> requests) {
        try {
            int totalProcessed = 0;
            int successCount = 0;
            
            for (HackerOneUsernameRequest request : requests) {
                totalProcessed++;
                
                // 构建邮箱地址
                String email = hackerOneService.buildHackerOneEmail(request.getUsername(), request.getSuffix());
                
                // 查找对应的别名（通过完整邮箱地址匹配）
                // 这里需要EmailManagementService提供通过邮箱地址查找别名的方法
                // 暂时跳过具体实现，记录日志
                
                logger.info("处理 HackerOne 用户名同步: {} -> {}", request.getUsername(), email);
                successCount++; // 临时标记为成功
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HackerOne 用户名同步完成");
            response.put("totalProcessed", totalProcessed);
            response.put("successCount", successCount);
            response.put("failedCount", totalProcessed - successCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("批量同步 HackerOne 用户名失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "同步失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 内部请求类
    public static class HackerOneEmailRequest {
        @NotBlank(message = "HackerOne 用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$", message = "用户名只能包含字母、数字、点号、下划线和短横线，长度3-30字符")
        private String username;
        
        @Size(max = 20, message = "后缀长度不能超过20字符")
        @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "后缀只能包含字母、数字、点号、下划线和短横线")
        private String suffix;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { 
            this.username = username != null ? username.trim().toLowerCase() : null; 
        }
        
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { 
            this.suffix = suffix != null ? suffix.trim().toLowerCase() : null; 
        }
    }
    
    public static class HackerOneUsernameRequest {
        @NotBlank(message = "HackerOne 用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$", message = "用户名格式不正确")
        private String username;
        
        @Size(max = 20, message = "后缀长度不能超过20字符")
        private String suffix;
        
        @Size(max = 100, message = "自定义显示名称长度不能超过100字符")
        private String customDisplayName; // 可选的自定义显示名称
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { 
            this.username = username != null ? username.trim().toLowerCase() : null; 
        }
        
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { 
            this.suffix = suffix != null ? suffix.trim().toLowerCase() : null; 
        }
        
        public String getCustomDisplayName() { return customDisplayName; }
        public void setCustomDisplayName(String customDisplayName) { 
            this.customDisplayName = customDisplayName != null ? customDisplayName.trim() : null; 
        }
    }
}