package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 邮件搜索服务
 * 提供全文搜索、高级筛选、搜索历史管理等功能
 */
@Service
@Transactional(readOnly = true)
public class EmailSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSearchService.class);
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private EmailAttachmentRepository attachmentRepository;
    
    @Autowired
    private EmailFolderRepository folderRepository;
    
    @Autowired
    private EmailSearchHistoryRepository searchHistoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 全文搜索邮件
     */
    public SearchResult searchEmails(User user, SearchRequest request) {
        logger.info("用户 {} 执行邮件搜索: {}", user.getUsername(), request.getQuery());
        
        try {
            // 记录搜索历史
            saveSearchHistory(user, request);
            
            // 根据搜索类型选择不同的搜索策略
            Page<EmailMessage> messages;
            if (request.isFullTextSearch()) {
                messages = performFullTextSearch(user, request);
            } else {
                messages = performAdvancedSearch(user, request);
            }
            
            // 构建搜索结果
            SearchResult result = new SearchResult();
            result.setMessages(messages.getContent());
            result.setTotalElements(messages.getTotalElements());
            result.setTotalPages(messages.getTotalPages());
            result.setCurrentPage(messages.getNumber());
            result.setPageSize(messages.getSize());
            result.setSearchQuery(request.getQuery());
            result.setSearchTime(System.currentTimeMillis() - request.getStartTime());
            
            // 添加搜索高亮和统计信息
            if (request.isHighlightEnabled()) {
                highlightSearchResults(result, request.getQuery());
            }
            
            logger.info("搜索完成: 找到 {} 封邮件，耗时 {}ms", 
                result.getTotalElements(), result.getSearchTime());
            
            return result;
            
        } catch (Exception e) {
            logger.error("邮件搜索失败: " + request.getQuery(), e);
            throw new RuntimeException("邮件搜索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 全文搜索（使用MySQL FULLTEXT索引）
     */
    private Page<EmailMessage> performFullTextSearch(User user, SearchRequest request) {
        Pageable pageable = createPageable(request);
        
        if (request.getFolderId() != null) {
            // 在指定文件夹中搜索
            EmailFolder folder = folderRepository.findById(request.getFolderId())
                .orElseThrow(() -> new RuntimeException("文件夹不存在"));
            
            if (!folder.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("无权限访问该文件夹");
            }
            
            return messageRepository.searchInFolder(
                request.getQuery(), 
                user.getId(),
                folder.getId(),
                request.getDateFrom(), 
                request.getDateTo(),
                pageable
            );
        } else {
            // 在用户所有邮件中搜索
            return messageRepository.searchByUser(
                request.getQuery(), 
                user.getId(),
                request.getDateFrom(), 
                request.getDateTo(),
                pageable
            );
        }
    }
    
    /**
     * 高级搜索（精确匹配）
     */
    private Page<EmailMessage> performAdvancedSearch(User user, SearchRequest request) {
        Pageable pageable = createPageable(request);
        
        return messageRepository.advancedSearch(
            user,
            request.getFromAddress(),
            request.getToAddress(),
            request.getSubject(),
            request.getBodyText(),
            request.getFolderId(),
            request.getDateFrom(),
            request.getDateTo(),
            request.getIsRead(),
            request.getHasAttachments(),
            request.getPriorityLevel(),
            pageable
        );
    }
    
    /**
     * 搜索邮件附件
     */
    public Page<EmailAttachment> searchAttachments(User user, AttachmentSearchRequest request) {
        logger.info("用户 {} 搜索附件: {}", user.getUsername(), request.getFilename());
        
        Pageable pageable = PageRequest.of(
            request.getPage(), 
            request.getSize(),
            Sort.by(Sort.Direction.DESC, "message.receivedAt")
        );
        
        return attachmentRepository.searchByUser(
            user,
            request.getFilename(),
            request.getContentType(),
            request.getMinSize(),
            request.getMaxSize(),
            request.getDateFrom(),
            request.getDateTo(),
            pageable
        );
    }
    
    /**
     * 快速搜索建议
     */
    public List<SearchSuggestion> getSearchSuggestions(User user, String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }
        
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        // 从发件人地址获取建议
        List<String> senderSuggestions = messageRepository.findSenderSuggestions(user, query, limit / 2);
        senderSuggestions.forEach(sender -> 
            suggestions.add(new SearchSuggestion("from:" + sender, "发件人: " + sender, "sender"))
        );
        
        // 从主题获取建议
        List<String> subjectSuggestions = messageRepository.findSubjectSuggestions(user, query, limit / 2);
        subjectSuggestions.forEach(subject -> 
            suggestions.add(new SearchSuggestion("subject:" + subject, "主题: " + subject, "subject"))
        );
        
        return suggestions.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * 获取搜索历史
     */
    public List<EmailSearchHistory> getSearchHistory(User user, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return searchHistoryRepository.findByUser(user, pageable).getContent();
    }
    
    /**
     * 保存搜索历史
     */
    @Transactional
    public void saveSearchHistory(User user, SearchRequest request) {
        // 检查是否为重复搜索
        Optional<EmailSearchHistory> existingHistory = searchHistoryRepository
            .findByUserAndQueryAndCreatedAtAfter(
                user, 
                request.getQuery(), 
                LocalDateTime.now().minusMinutes(5)
            );
        
        if (existingHistory.isPresent()) {
            // 5分钟内的相同搜索不重复记录
            return;
        }
        
        EmailSearchHistory history = new EmailSearchHistory();
        history.setUser(user);
        history.setQuery(request.getQuery());
        history.setSearchType(request.isFullTextSearch() ? "FULLTEXT" : "ADVANCED");
        history.setResultCount(0); // 将在搜索完成后更新
        history.setCreatedAt(LocalDateTime.now());
        
        searchHistoryRepository.save(history);
        
        // 清理过期的搜索历史（保留最近100条）
        cleanupSearchHistory(user);
    }
    
    /**
     * 更新搜索历史的结果数量
     */
    @Transactional
    public void updateSearchHistoryResultCount(User user, String query, long resultCount) {
        Optional<EmailSearchHistory> history = searchHistoryRepository
            .findByUserAndQueryAndCreatedAtAfter(
                user, 
                query, 
                LocalDateTime.now().minusMinutes(10)
            );
        
        if (history.isPresent()) {
            history.get().setResultCount(resultCount);
            searchHistoryRepository.save(history.get());
        }
    }
    
    /**
     * 清理搜索历史
     */
    @Transactional
    public void cleanupSearchHistory(User user) {
        List<EmailSearchHistory> allHistory = searchHistoryRepository
            .findByUserOrderByCreatedAtDesc(user);
        
        if (allHistory.size() > 100) {
            List<EmailSearchHistory> toDelete = allHistory.subList(100, allHistory.size());
            searchHistoryRepository.deleteAll(toDelete);
        }
    }
    
    /**
     * 删除搜索历史
     */
    @Transactional
    public void deleteSearchHistory(User user, Long historyId) {
        Optional<EmailSearchHistory> history = searchHistoryRepository.findById(historyId);
        if (history.isPresent() && history.get().getUser().getId().equals(user.getId())) {
            searchHistoryRepository.delete(history.get());
        }
    }
    
    /**
     * 清空搜索历史
     */
    @Transactional
    public void clearSearchHistory(User user) {
        searchHistoryRepository.deleteByUser(user);
    }
    
    /**
     * 获取邮件统计信息
     */
    public EmailStatistics getEmailStatistics(User user) {
        EmailStatistics stats = new EmailStatistics();
        
        stats.setTotalMessages(messageRepository.countByUser(user));
        stats.setUnreadMessages(messageRepository.countByUserAndIsRead(user, false));
        stats.setTodayMessages(messageRepository.countByUserAndReceivedAtAfter(
            user, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)));
        
        // 按文件夹统计
        List<EmailFolder> folders = folderRepository.findByUser(user);
        Map<String, Long> folderStats = new HashMap<>();
        for (EmailFolder folder : folders) {
            long count = messageRepository.countByFolder(folder);
            folderStats.put(folder.getFolderName(), count);
        }
        stats.setFolderStatistics(folderStats);
        
        // 按发件人统计（top 10）
        List<Object[]> senderStats = messageRepository.findTopSendersByUser(user, PageRequest.of(0, 10));
        Map<String, Long> senderCounts = new LinkedHashMap<>();
        for (Object[] row : senderStats) {
            senderCounts.put((String) row[0], (Long) row[1]);
        }
        stats.setSenderStatistics(senderCounts);
        
        return stats;
    }
    
    /**
     * 创建分页对象
     */
    private Pageable createPageable(SearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "receivedAt");
        
        if (request.getSortBy() != null) {
            Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortOrder()) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, request.getSortBy());
        }
        
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
    
    /**
     * 高亮搜索结果
     */
    private void highlightSearchResults(SearchResult result, String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        String[] keywords = query.toLowerCase().split("\\s+");
        
        for (EmailMessage message : result.getMessages()) {
            // 高亮主题
            if (message.getSubject() != null) {
                String highlightedSubject = highlightText(message.getSubject(), keywords);
                message.setSubject(highlightedSubject);
            }
            
            // 高亮正文（截取片段）
            if (message.getBodyText() != null) {
                String snippet = extractSnippet(message.getBodyText(), keywords, 200);
                String highlightedSnippet = highlightText(snippet, keywords);
                message.setBodyText(highlightedSnippet);
            }
        }
    }
    
    /**
     * 高亮文本
     */
    private String highlightText(String text, String[] keywords) {
        String result = text;
        for (String keyword : keywords) {
            if (keyword.length() > 1) {
                result = result.replaceAll("(?i)(" + escapeRegex(keyword) + ")", 
                    "<mark>$1</mark>");
            }
        }
        return result;
    }
    
    /**
     * 提取包含关键字的文本片段
     */
    private String extractSnippet(String text, String[] keywords, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        
        // 查找第一个关键字的位置
        int firstMatch = text.length();
        for (String keyword : keywords) {
            int index = text.toLowerCase().indexOf(keyword.toLowerCase());
            if (index >= 0 && index < firstMatch) {
                firstMatch = index;
            }
        }
        
        if (firstMatch == text.length()) {
            // 没有找到关键字，返回开头部分
            return text.substring(0, Math.min(maxLength, text.length())) + "...";
        }
        
        // 以关键字为中心提取片段
        int start = Math.max(0, firstMatch - maxLength / 3);
        int end = Math.min(text.length(), start + maxLength);
        
        String snippet = text.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";
        
        return snippet;
    }
    
    /**
     * 转义正则表达式特殊字符
     */
    private String escapeRegex(String input) {
        return input.replaceAll("[\\\\\\[\\]{}()*+?.^$|]", "\\\\$0");
    }
    
    // 内部数据类
    
    /**
     * 搜索请求
     */
    public static class SearchRequest {
        private String query;
        private String fromAddress;
        private String toAddress;
        private String subject;
        private String bodyText;
        private Long folderId;
        private LocalDateTime dateFrom;
        private LocalDateTime dateTo;
        private Boolean isRead;
        private Boolean hasAttachments;
        private Integer priorityLevel;
        private int page = 0;
        private int size = 20;
        private String sortBy = "receivedAt";
        private String sortOrder = "desc";
        private boolean fullTextSearch = true;
        private boolean highlightEnabled = true;
        private long startTime = System.currentTimeMillis();
        
        // Constructors
        public SearchRequest() {}
        
        public SearchRequest(String query) {
            this.query = query;
        }
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        
        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
        
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public LocalDateTime getDateFrom() { return dateFrom; }
        public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }
        
        public LocalDateTime getDateTo() { return dateTo; }
        public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }
        
        public Boolean getIsRead() { return isRead; }
        public void setIsRead(Boolean isRead) { this.isRead = isRead; }
        
        public Boolean getHasAttachments() { return hasAttachments; }
        public void setHasAttachments(Boolean hasAttachments) { this.hasAttachments = hasAttachments; }
        
        public Integer getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
        
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
        
        public boolean isFullTextSearch() { return fullTextSearch; }
        public void setFullTextSearch(boolean fullTextSearch) { this.fullTextSearch = fullTextSearch; }
        
        public boolean isHighlightEnabled() { return highlightEnabled; }
        public void setHighlightEnabled(boolean highlightEnabled) { this.highlightEnabled = highlightEnabled; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
    }
    
    /**
     * 附件搜索请求
     */
    public static class AttachmentSearchRequest {
        private String filename;
        private String contentType;
        private Long minSize;
        private Long maxSize;
        private LocalDateTime dateFrom;
        private LocalDateTime dateTo;
        private int page = 0;
        private int size = 20;
        
        // Getters and Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public Long getMinSize() { return minSize; }
        public void setMinSize(Long minSize) { this.minSize = minSize; }
        
        public Long getMaxSize() { return maxSize; }
        public void setMaxSize(Long maxSize) { this.maxSize = maxSize; }
        
        public LocalDateTime getDateFrom() { return dateFrom; }
        public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }
        
        public LocalDateTime getDateTo() { return dateTo; }
        public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }
        
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }
    
    /**
     * 搜索结果
     */
    public static class SearchResult {
        private List<EmailMessage> messages;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private String searchQuery;
        private long searchTime;
        
        // Getters and Setters
        public List<EmailMessage> getMessages() { return messages; }
        public void setMessages(List<EmailMessage> messages) { this.messages = messages; }
        
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        
        public String getSearchQuery() { return searchQuery; }
        public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
        
        public long getSearchTime() { return searchTime; }
        public void setSearchTime(long searchTime) { this.searchTime = searchTime; }
    }
    
    /**
     * 搜索建议
     */
    public static class SearchSuggestion {
        private String query;
        private String displayText;
        private String type;
        
        public SearchSuggestion(String query, String displayText, String type) {
            this.query = query;
            this.displayText = displayText;
            this.type = type;
        }
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getDisplayText() { return displayText; }
        public void setDisplayText(String displayText) { this.displayText = displayText; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    
    /**
     * 邮件统计信息
     */
    public static class EmailStatistics {
        private long totalMessages;
        private long unreadMessages;
        private long todayMessages;
        private Map<String, Long> folderStatistics;
        private Map<String, Long> senderStatistics;
        
        // Getters and Setters
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        
        public long getUnreadMessages() { return unreadMessages; }
        public void setUnreadMessages(long unreadMessages) { this.unreadMessages = unreadMessages; }
        
        public long getTodayMessages() { return todayMessages; }
        public void setTodayMessages(long todayMessages) { this.todayMessages = todayMessages; }
        
        public Map<String, Long> getFolderStatistics() { return folderStatistics; }
        public void setFolderStatistics(Map<String, Long> folderStatistics) { this.folderStatistics = folderStatistics; }
        
        public Map<String, Long> getSenderStatistics() { return senderStatistics; }
        public void setSenderStatistics(Map<String, Long> senderStatistics) { this.senderStatistics = senderStatistics; }
    }
}