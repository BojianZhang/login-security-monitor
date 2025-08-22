package com.security.monitor.api.graphql;

import com.security.monitor.api.dto.*;
import com.security.monitor.model.*;
import com.security.monitor.service.*;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.kickstart.tools.GraphQLMutationResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL解析器
 */
@Component
public class GraphQLResolver implements GraphQLQueryResolver, GraphQLMutationResolver {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailMessageService messageService;
    
    @Autowired
    private EmailFolderService folderService;
    
    @Autowired
    private EmailAliasService aliasService;
    
    @Autowired
    private ActiveSyncService activeSyncService;
    
    @Autowired
    private DmarcReportService dmarcReportService;
    
    @Autowired
    private EmailDeliveryLogService deliveryLogService;
    
    // ============== 查询解析器 ==============
    
    /**
     * 获取当前用户信息
     */
    public UserDTO me(String username) {
        User user = userService.getUserByUsername(username);
        return convertUserToDTO(user);
    }
    
    /**
     * 获取用户列表
     */
    @PreAuthorize("hasRole('ADMIN')")
    public UserConnection users(String search, Integer first, String after, String sortBy, String sortDirection) {
        int page = after != null ? Integer.parseInt(after) : 0;
        int size = first != null ? first : 20;
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection != null ? sortDirection : "asc");
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy != null ? sortBy : "username"));
        
        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }
        
        List<UserDTO> users = userPage.getContent().stream()
                .map(this::convertUserToDTO)
                .collect(Collectors.toList());
        
        return new UserConnection(users, createPageInfo(userPage));
    }
    
    /**
     * 获取邮件列表
     */
    public EmailConnection emails(String username, Long folderId, Integer first, String after, String sortBy, String sortDirection) {
        User user = userService.getUserByUsername(username);
        
        int page = after != null ? Integer.parseInt(after) : 0;
        int size = first != null ? first : 20;
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection != null ? sortDirection : "desc");
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy != null ? sortBy : "receivedAt"));
        
        Page<EmailMessage> messagePage;
        if (folderId != null) {
            EmailFolder folder = folderService.getFolderById(folderId);
            messagePage = messageService.getMessagesByUserAndFolder(user, folder, pageable);
        } else {
            messagePage = messageService.getMessagesByUser(user, pageable);
        }
        
        List<EmailMessageDTO> messages = messagePage.getContent().stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());
        
        return new EmailConnection(messages, createPageInfo(messagePage));
    }
    
    /**
     * 获取邮件详情
     */
    public EmailMessageDTO email(String username, Long id) {
        User user = userService.getUserByUsername(username);
        EmailMessage message = messageService.getMessageByIdAndUser(id, user);
        return convertMessageToDTO(message);
    }
    
    /**
     * 搜索邮件
     */
    public EmailConnection searchEmails(String username, EmailSearchInput input) {
        User user = userService.getUserByUsername(username);
        
        Sort.Direction direction = Sort.Direction.fromString(input.getSortDirection() != null ? input.getSortDirection() : "desc");
        Pageable pageable = PageRequest.of(input.getPage() != null ? input.getPage() : 0, 
                                         input.getSize() != null ? input.getSize() : 20, 
                                         Sort.by(direction, input.getSortBy() != null ? input.getSortBy() : "receivedAt"));
        
        // 创建搜索请求
        EmailSearchRequest searchRequest = new EmailSearchRequest();
        searchRequest.setQuery(input.getQuery());
        searchRequest.setFolder(input.getFolder());
        searchRequest.setIsRead(input.getIsRead());
        searchRequest.setIsStarred(input.getIsStarred());
        searchRequest.setHasAttachments(input.getHasAttachments());
        searchRequest.setFromAddress(input.getFromAddress());
        searchRequest.setToAddress(input.getToAddress());
        searchRequest.setStartDate(input.getStartDate());
        searchRequest.setEndDate(input.getEndDate());
        
        Page<EmailMessage> searchResults = messageService.searchMessages(user, searchRequest, pageable);
        
        List<EmailMessageDTO> messages = searchResults.getContent().stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());
        
        return new EmailConnection(messages, createPageInfo(searchResults));
    }
    
    /**
     * 获取用户文件夹
     */
    public List<EmailFolderDTO> folders(String username) {
        User user = userService.getUserByUsername(username);
        return folderService.getUserFolders(user).stream()
                .map(this::convertFolderToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户别名
     */
    public List<EmailAliasDTO> aliases(String username) {
        User user = userService.getUserByUsername(username);
        return aliasService.getUserAliases(user).stream()
                .map(this::convertAliasToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取ActiveSync设备
     */
    public List<ActiveSyncDeviceDTO> activeSyncDevices(String username) {
        List<ActiveSyncDevice> devices = activeSyncService.getUserDevices(username);
        return devices.stream()
                .map(this::convertDeviceToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取邮件统计
     */
    public EmailStatistics emailStatistics(String username, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userService.getUserByUsername(username);
        return messageService.getEmailStatistics(user, startDate, endDate);
    }
    
    /**
     * 获取投递统计
     */
    @PreAuthorize("hasRole('ADMIN')")
    public DeliveryStatistics deliveryStatistics(LocalDateTime startDate, LocalDateTime endDate, String domain) {
        return deliveryLogService.getDeliveryStatistics(startDate, endDate, domain);
    }
    
    /**
     * 获取DMARC报告
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<DmarcReportDTO> dmarcReports(String domain, LocalDateTime startDate, LocalDateTime endDate) {
        return dmarcReportService.getReportsByDomainAndDateRange(domain, startDate, endDate).stream()
                .map(this::convertDmarcReportToDTO)
                .collect(Collectors.toList());
    }
    
    // ============== 变更解析器 ==============
    
    /**
     * 发送邮件
     */
    public Boolean sendEmail(String username, SendEmailInput input) {
        User user = userService.getUserByUsername(username);
        
        EmailMessage message = new EmailMessage();
        message.setUser(user);
        message.setFromAddress(input.getFrom() != null ? input.getFrom() : user.getEmail());
        message.setToAddresses(String.join(",", input.getTo()));
        if (input.getCc() != null) message.setCcAddresses(String.join(",", input.getCc()));
        if (input.getBcc() != null) message.setBccAddresses(String.join(",", input.getBcc()));
        message.setSubject(input.getSubject());
        message.setBodyText(input.getBodyText());
        message.setBodyHtml(input.getBodyHtml());
        message.setPriorityLevel(input.getPriority() != null ? input.getPriority() : 3);
        message.setReplyTo(input.getReplyTo());
        
        messageService.sendEmail(message);
        return true;
    }
    
    /**
     * 标记邮件为已读
     */
    public Boolean markEmailAsRead(String username, Long messageId, Boolean isRead) {
        User user = userService.getUserByUsername(username);
        EmailMessage message = messageService.getMessageByIdAndUser(messageId, user);
        
        if (isRead) {
            messageService.markAsRead(message);
        } else {
            messageService.markAsUnread(message);
        }
        
        return true;
    }
    
    /**
     * 设置邮件星标
     */
    public Boolean starEmail(String username, Long messageId, Boolean isStarred) {
        User user = userService.getUserByUsername(username);
        EmailMessage message = messageService.getMessageByIdAndUser(messageId, user);
        
        messageService.toggleStar(message, isStarred);
        return true;
    }
    
    /**
     * 移动邮件
     */
    public Boolean moveEmail(String username, Long messageId, Long targetFolderId) {
        User user = userService.getUserByUsername(username);
        EmailMessage message = messageService.getMessageByIdAndUser(messageId, user);
        EmailFolder targetFolder = folderService.getFolderByIdAndUser(targetFolderId, user);
        
        messageService.moveToFolder(message, targetFolder);
        return true;
    }
    
    /**
     * 删除邮件
     */
    public Boolean deleteEmail(String username, Long messageId, Boolean permanent) {
        User user = userService.getUserByUsername(username);
        EmailMessage message = messageService.getMessageByIdAndUser(messageId, user);
        
        if (permanent != null && permanent) {
            messageService.permanentlyDelete(message);
        } else {
            messageService.moveToTrash(message);
        }
        
        return true;
    }
    
    /**
     * 创建用户
     */
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO createUser(CreateUserInput input) {
        User user = new User();
        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        user.setFullName(input.getFullName());
        user.setPhone(input.getPhone());
        user.setIsAdmin(input.getIsAdmin() != null ? input.getIsAdmin() : false);
        user.setIsEmailAdmin(input.getIsEmailAdmin() != null ? input.getIsEmailAdmin() : false);
        user.setStorageQuota(input.getStorageQuota() != null ? input.getStorageQuota() : 1073741824L);
        user.setEmailEnabled(input.getEmailEnabled() != null ? input.getEmailEnabled() : true);
        
        User createdUser = userService.createUser(user, input.getPassword());
        return convertUserToDTO(createdUser);
    }
    
    /**
     * 更新用户
     */
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.principal.username")
    public UserDTO updateUser(String username, UpdateUserInput input) {
        User user = userService.getUserByUsername(username);
        
        if (input.getEmail() != null) user.setEmail(input.getEmail());
        if (input.getFullName() != null) user.setFullName(input.getFullName());
        if (input.getPhone() != null) user.setPhone(input.getPhone());
        if (input.getIsActive() != null) user.setIsActive(input.getIsActive());
        if (input.getStorageQuota() != null) user.setStorageQuota(input.getStorageQuota());
        if (input.getEmailEnabled() != null) user.setEmailEnabled(input.getEmailEnabled());
        
        User updatedUser = userService.updateUser(user);
        return convertUserToDTO(updatedUser);
    }
    
    /**
     * 创建文件夹
     */
    public EmailFolderDTO createFolder(String username, String folderName) {
        User user = userService.getUserByUsername(username);
        EmailFolder folder = folderService.createCustomFolder(user, folderName);
        return convertFolderToDTO(folder);
    }
    
    /**
     * 批量操作邮件
     */
    public Boolean batchEmailOperation(String username, List<Long> messageIds, String operation, Long targetFolderId) {
        User user = userService.getUserByUsername(username);
        
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
                return false;
        }
        
        return true;
    }
    
    // ============== 辅助方法 ==============
    
    private UserDTO convertUserToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setIsActive(user.getIsActive());
        dto.setIsAdmin(user.getIsAdmin());
        dto.setIsEmailAdmin(user.getIsEmailAdmin());
        dto.setStorageQuota(user.getStorageQuota());
        dto.setStorageUsed(user.getStorageUsed());
        dto.setLastLogin(user.getLastLogin());
        dto.setEmailEnabled(user.getEmailEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
    
    private EmailMessageDTO convertMessageToDTO(EmailMessage message) {
        EmailMessageDTO dto = new EmailMessageDTO();
        dto.setId(message.getId());
        dto.setMessageId(message.getMessageId());
        dto.setThreadId(message.getThreadId());
        dto.setSubject(message.getSubject());
        dto.setFromAddress(message.getFromAddress());
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
    
    private EmailFolderDTO convertFolderToDTO(EmailFolder folder) {
        EmailFolderDTO dto = new EmailFolderDTO();
        dto.setId(folder.getId());
        dto.setFolderName(folder.getFolderName());
        dto.setFolderType(folder.getFolderType().toString());
        dto.setMessageCount(folder.getMessageCount());
        dto.setUnreadCount(folder.getUnreadCount());
        dto.setCreatedAt(folder.getCreatedAt());
        return dto;
    }
    
    private EmailAliasDTO convertAliasToDTO(EmailAlias alias) {
        EmailAliasDTO dto = new EmailAliasDTO();
        dto.setId(alias.getId());
        dto.setAliasEmail(alias.getAliasEmail());
        dto.setDomainName(alias.getDomain().getDomainName());
        dto.setIsActive(alias.getIsActive());
        dto.setForwardTo(alias.getForwardTo());
        dto.setDisplayName(alias.getDisplayName());
        dto.setDescription(alias.getDescription());
        dto.setCreatedAt(alias.getCreatedAt());
        return dto;
    }
    
    private ActiveSyncDeviceDTO convertDeviceToDTO(ActiveSyncDevice device) {
        ActiveSyncDeviceDTO dto = new ActiveSyncDeviceDTO();
        dto.setId(device.getId());
        dto.setDeviceId(device.getDeviceId());
        dto.setDeviceType(device.getDeviceType());
        dto.setDeviceModel(device.getDeviceModel());
        dto.setDeviceOS(device.getDeviceOS());
        dto.setDeviceFriendlyName(device.getDeviceFriendlyName());
        dto.setStatus(device.getStatus().toString());
        dto.setLastSyncTime(device.getLastSyncTime());
        dto.setLastSyncIP(device.getLastSyncIP());
        dto.setTotalSyncCount(device.getTotalSyncCount());
        dto.setFailedSyncCount(device.getFailedSyncCount());
        dto.setIsBlocked(device.getIsBlocked());
        dto.setCreatedAt(device.getCreatedAt());
        return dto;
    }
    
    private DmarcReportDTO convertDmarcReportToDTO(DmarcReport report) {
        DmarcReportDTO dto = new DmarcReportDTO();
        dto.setId(report.getId());
        dto.setReportId(report.getReportId());
        dto.setDomain(report.getDomain());
        dto.setOrgName(report.getOrgName());
        dto.setBeginTime(report.getBeginTime());
        dto.setEndTime(report.getEndTime());
        dto.setTotalMessages(report.getTotalMessages());
        dto.setCompliantMessages(report.getCompliantMessages());
        dto.setFailedMessages(report.getFailedMessages());
        dto.setComplianceRate(report.getComplianceRate());
        dto.setIsSent(report.getIsSent());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }
    
    private PageInfo createPageInfo(Page<?> page) {
        PageInfo pageInfo = new PageInfo();
        pageInfo.setHasNextPage(page.hasNext());
        pageInfo.setHasPreviousPage(page.hasPrevious());
        pageInfo.setStartCursor(String.valueOf(page.getNumber()));
        pageInfo.setEndCursor(String.valueOf(page.getNumber()));
        return pageInfo;
    }
}

// ============== GraphQL 输入类型 ==============

class EmailSearchInput {
    private String query;
    private String folder;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean hasAttachments;
    private String fromAddress;
    private String toAddress;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
    
    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    
    public Boolean getIsStarred() { return isStarred; }
    public void setIsStarred(Boolean isStarred) { this.isStarred = isStarred; }
    
    public Boolean getHasAttachments() { return hasAttachments; }
    public void setHasAttachments(Boolean hasAttachments) { this.hasAttachments = hasAttachments; }
    
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    
    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}

class SendEmailInput {
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String bodyText;
    private String bodyHtml;
    private Integer priority;
    private String replyTo;
    
    // Getters and Setters
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    
    public List<String> getTo() { return to; }
    public void setTo(List<String> to) { this.to = to; }
    
    public List<String> getCc() { return cc; }
    public void setCc(List<String> cc) { this.cc = cc; }
    
    public List<String> getBcc() { return bcc; }
    public void setBcc(List<String> bcc) { this.bcc = bcc; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    
    public String getReplyTo() { return replyTo; }
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
}

class CreateUserInput {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private Boolean isAdmin;
    private Boolean isEmailAdmin;
    private Long storageQuota;
    private Boolean emailEnabled;
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
    
    public Boolean getIsEmailAdmin() { return isEmailAdmin; }
    public void setIsEmailAdmin(Boolean isEmailAdmin) { this.isEmailAdmin = isEmailAdmin; }
    
    public Long getStorageQuota() { return storageQuota; }
    public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
    
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
}

class UpdateUserInput {
    private String email;
    private String fullName;
    private String phone;
    private Boolean isActive;
    private Long storageQuota;
    private Boolean emailEnabled;
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Long getStorageQuota() { return storageQuota; }
    public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
    
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
}

// ============== GraphQL 连接类型 ==============

class UserConnection {
    private List<UserDTO> edges;
    private PageInfo pageInfo;
    
    public UserConnection(List<UserDTO> edges, PageInfo pageInfo) {
        this.edges = edges;
        this.pageInfo = pageInfo;
    }
    
    public List<UserDTO> getEdges() { return edges; }
    public void setEdges(List<UserDTO> edges) { this.edges = edges; }
    
    public PageInfo getPageInfo() { return pageInfo; }
    public void setPageInfo(PageInfo pageInfo) { this.pageInfo = pageInfo; }
}

class EmailConnection {
    private List<EmailMessageDTO> edges;
    private PageInfo pageInfo;
    
    public EmailConnection(List<EmailMessageDTO> edges, PageInfo pageInfo) {
        this.edges = edges;
        this.pageInfo = pageInfo;
    }
    
    public List<EmailMessageDTO> getEdges() { return edges; }
    public void setEdges(List<EmailMessageDTO> edges) { this.edges = edges; }
    
    public PageInfo getPageInfo() { return pageInfo; }
    public void setPageInfo(PageInfo pageInfo) { this.pageInfo = pageInfo; }
}

class PageInfo {
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;
    private String startCursor;
    private String endCursor;
    
    // Getters and Setters
    public Boolean getHasNextPage() { return hasNextPage; }
    public void setHasNextPage(Boolean hasNextPage) { this.hasNextPage = hasNextPage; }
    
    public Boolean getHasPreviousPage() { return hasPreviousPage; }
    public void setHasPreviousPage(Boolean hasPreviousPage) { this.hasPreviousPage = hasPreviousPage; }
    
    public String getStartCursor() { return startCursor; }
    public void setStartCursor(String startCursor) { this.startCursor = startCursor; }
    
    public String getEndCursor() { return endCursor; }
    public void setEndCursor(String endCursor) { this.endCursor = endCursor; }
}