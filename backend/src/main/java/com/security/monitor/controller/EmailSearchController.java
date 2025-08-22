package com.security.monitor.controller;

import com.security.monitor.model.EmailSearchHistory;
import com.security.monitor.model.User;
import com.security.monitor.service.EmailSearchService;
import com.security.monitor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邮件搜索控制器
 * 提供邮件搜索、附件搜索、搜索建议、搜索历史等功能
 */
@RestController
@RequestMapping("/api/email/search")
public class EmailSearchController {
    
    @Autowired
    private EmailSearchService searchService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 搜索邮件
     */
    @PostMapping("/messages")
    public ResponseEntity<?> searchMessages(
            @RequestBody EmailSearchService.SearchRequest request,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            EmailSearchService.SearchResult result = searchService.searchEmails(user, request);
            
            // 更新搜索历史的结果数量
            searchService.updateSearchHistoryResultCount(user, request.getQuery(), result.getTotalElements());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "搜索失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 快速搜索（简单查询）
     */
    @GetMapping("/quick")
    public ResponseEntity<?> quickSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            EmailSearchService.SearchRequest request = new EmailSearchService.SearchRequest(q);
            request.setPage(page);
            request.setSize(size);
            request.setFullTextSearch(true);
            
            EmailSearchService.SearchResult result = searchService.searchEmails(user, request);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "快速搜索失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 高级搜索
     */
    @PostMapping("/advanced")
    public ResponseEntity<?> advancedSearch(
            @RequestBody Map<String, Object> searchParams,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            EmailSearchService.SearchRequest request = new EmailSearchService.SearchRequest();
            request.setFullTextSearch(false);
            
            // 设置搜索参数
            if (searchParams.containsKey("fromAddress")) {
                request.setFromAddress((String) searchParams.get("fromAddress"));
            }
            if (searchParams.containsKey("toAddress")) {
                request.setToAddress((String) searchParams.get("toAddress"));
            }
            if (searchParams.containsKey("subject")) {
                request.setSubject((String) searchParams.get("subject"));
            }
            if (searchParams.containsKey("bodyText")) {
                request.setBodyText((String) searchParams.get("bodyText"));
            }
            if (searchParams.containsKey("folderId")) {
                request.setFolderId(Long.valueOf(searchParams.get("folderId").toString()));
            }
            if (searchParams.containsKey("isRead")) {
                request.setIsRead((Boolean) searchParams.get("isRead"));
            }
            if (searchParams.containsKey("hasAttachments")) {
                request.setHasAttachments((Boolean) searchParams.get("hasAttachments"));
            }
            if (searchParams.containsKey("priorityLevel")) {
                request.setPriorityLevel((Integer) searchParams.get("priorityLevel"));
            }
            if (searchParams.containsKey("dateFrom")) {
                request.setDateFrom(LocalDateTime.parse((String) searchParams.get("dateFrom")));
            }
            if (searchParams.containsKey("dateTo")) {
                request.setDateTo(LocalDateTime.parse((String) searchParams.get("dateTo")));
            }
            if (searchParams.containsKey("page")) {
                request.setPage((Integer) searchParams.get("page"));
            }
            if (searchParams.containsKey("size")) {
                request.setSize((Integer) searchParams.get("size"));
            }
            if (searchParams.containsKey("sortBy")) {
                request.setSortBy((String) searchParams.get("sortBy"));
            }
            if (searchParams.containsKey("sortOrder")) {
                request.setSortOrder((String) searchParams.get("sortOrder"));
            }
            
            EmailSearchService.SearchResult result = searchService.searchEmails(user, request);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "高级搜索失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 搜索附件
     */
    @PostMapping("/attachments")
    public ResponseEntity<?> searchAttachments(
            @RequestBody EmailSearchService.AttachmentSearchRequest request,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            Page<?> result = searchService.searchAttachments(user, request);
            
            return ResponseEntity.ok(Map.of(
                "attachments", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "currentPage", result.getNumber(),
                "pageSize", result.getSize()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "附件搜索失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    public ResponseEntity<?> getSearchSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            List<EmailSearchService.SearchSuggestion> suggestions = 
                searchService.getSearchSuggestions(user, q, limit);
            
            return ResponseEntity.ok(Map.of("suggestions", suggestions));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取搜索建议失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public ResponseEntity<?> getSearchHistory(
            @RequestParam(defaultValue = "20") int limit,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            List<EmailSearchHistory> history = searchService.getSearchHistory(user, limit);
            
            return ResponseEntity.ok(Map.of("history", history));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取搜索历史失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 删除搜索历史记录
     */
    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<?> deleteSearchHistory(
            @PathVariable Long historyId,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            searchService.deleteSearchHistory(user, historyId);
            
            return ResponseEntity.ok(Map.of("message", "搜索历史已删除"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "删除搜索历史失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 清空搜索历史
     */
    @DeleteMapping("/history")
    public ResponseEntity<?> clearSearchHistory(Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            searchService.clearSearchHistory(user);
            
            return ResponseEntity.ok(Map.of("message", "搜索历史已清空"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "清空搜索历史失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取邮件统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getEmailStatistics(Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            EmailSearchService.EmailStatistics statistics = searchService.getEmailStatistics(user);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "获取统计信息失败",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 导出搜索结果
     */
    @PostMapping("/export")
    public ResponseEntity<?> exportSearchResults(
            @RequestBody EmailSearchService.SearchRequest request,
            @RequestParam(defaultValue = "csv") String format,
            Authentication auth) {
        
        try {
            User user = userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 设置较大的分页大小用于导出
            request.setSize(10000);
            request.setPage(0);
            
            EmailSearchService.SearchResult result = searchService.searchEmails(user, request);
            
            // 这里可以实现具体的导出逻辑（CSV、Excel等）
            // 暂时返回搜索结果的JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("exportFormat", format);
            exportData.put("exportTime", LocalDateTime.now());
            exportData.put("totalMessages", result.getTotalElements());
            exportData.put("searchQuery", result.getSearchQuery());
            exportData.put("messages", result.getMessages());
            
            return ResponseEntity.ok(exportData);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "导出搜索结果失败",
                "message", e.getMessage()
            ));
        }
    }
}