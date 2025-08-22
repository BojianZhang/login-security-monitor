package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 邮件管理服务
 */
@Service
@Transactional
public class EmailManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailManagementService.class);
    
    @Autowired
    private EmailDomainRepository domainRepository;
    
    @Autowired
    private EmailAliasRepository aliasRepository;
    
    @Autowired
    private EmailFolderRepository folderRepository;
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 获取用户的所有邮箱别名 - 优化N+1查询
     */
    @Transactional(readOnly = true)
    public List<EmailAlias> getUserAliases(User user) {
        // 使用JOIN FETCH避免N+1查询问题
        return aliasRepository.findByUserWithDomainFetch(user);
    }
    
    /**
     * 获取用户在指定域名的别名
     */
    @Transactional(readOnly = true)
    public List<EmailAlias> getUserAliasesByDomain(User user, Long domainId) {
        Optional<EmailDomain> domain = domainRepository.findById(domainId);
        if (domain.isPresent()) {
            return aliasRepository.findByUserAndDomainOrderByCreatedAtDesc(user, domain.get());
        }
        return new ArrayList<>();
    }
    
    /**
     * 创建新的邮箱别名
     */
    public EmailAlias createAlias(User user, String aliasEmail, Long domainId) {
        Optional<EmailDomain> domainOpt = domainRepository.findById(domainId);
        if (domainOpt.isEmpty()) {
            throw new RuntimeException("域名不存在");
        }
        
        EmailDomain domain = domainOpt.get();
        
        if (!domain.getIsActive()) {
            throw new RuntimeException("域名未激活");
        }
        
        // 检查别名是否已存在
        if (aliasRepository.existsByAliasEmailAndDomain(aliasEmail, domain)) {
            throw new RuntimeException("该别名已存在");
        }
        
        EmailAlias alias = new EmailAlias(user, aliasEmail, domain);
        EmailAlias savedAlias = aliasRepository.save(alias);
        
        logger.info("用户 {} 创建了新的邮箱别名: {}@{}", 
                   user.getUsername(), aliasEmail, domain.getDomainName());
        
        return savedAlias;
    }
    
    /**
     * 删除邮箱别名
     */
    public void deleteAlias(User user, Long aliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty()) {
            throw new RuntimeException("别名不存在");
        }
        
        EmailAlias alias = aliasOpt.get();
        if (!alias.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权限删除此别名");
        }
        
        // 软删除：设置为非活跃状态
        alias.setIsActive(false);
        aliasRepository.save(alias);
        
        logger.info("用户 {} 删除了邮箱别名: {}", 
                   user.getUsername(), alias.getFullEmail());
    }
    
    /**
     * 切换别名激活状态
     */
    public EmailAlias toggleAliasStatus(User user, Long aliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty()) {
            throw new RuntimeException("别名不存在");
        }
        
        EmailAlias alias = aliasOpt.get();
        if (!alias.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权限操作此别名");
        }
        
        alias.setIsActive(!alias.getIsActive());
        EmailAlias savedAlias = aliasRepository.save(alias);
        
        logger.info("用户 {} {} 了邮箱别名: {}", 
                   user.getUsername(), 
                   alias.getIsActive() ? "激活" : "禁用", 
                   alias.getFullEmail());
        
        return savedAlias;
    }
    
    /**
     * 设置别名转发
     */
    public EmailAlias setAliasForward(User user, Long aliasId, String forwardTo) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty()) {
            throw new RuntimeException("别名不存在");
        }
        
        EmailAlias alias = aliasOpt.get();
        if (!alias.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权限操作此别名");
        }
        
        alias.setForwardTo(forwardTo);
        EmailAlias savedAlias = aliasRepository.save(alias);
        
        logger.info("用户 {} 设置别名 {} 转发到: {}", 
                   user.getUsername(), alias.getFullEmail(), forwardTo);
        
        return savedAlias;
    }
    
    /**
     * 获取活跃的域名列表
     */
    @Transactional(readOnly = true)
    public List<EmailDomain> getActiveDomains() {
        return domainRepository.findByIsActiveTrue();
    }
    
    /**
     * 获取用户的文件夹列表
     */
    @Transactional(readOnly = true)
    public List<EmailFolder> getUserFolders(User user) {
        return folderRepository.findByUserOrderByCreatedAtAsc(user);
    }
    
    /**
     * 初始化用户默认文件夹
     */
    public void initializeUserFolders(User user) {
        // 检查是否已经初始化过
        List<EmailFolder> existingFolders = folderRepository.findByUserOrderByCreatedAtAsc(user);
        if (!existingFolders.isEmpty()) {
            return;
        }
        
        // 创建默认文件夹
        EmailFolder.FolderType[] defaultTypes = {
            EmailFolder.FolderType.INBOX,
            EmailFolder.FolderType.SENT,
            EmailFolder.FolderType.DRAFT,
            EmailFolder.FolderType.TRASH,
            EmailFolder.FolderType.SPAM
        };
        
        String[] folderNames = {"收件箱", "已发送", "草稿箱", "回收站", "垃圾邮件"};
        
        for (int i = 0; i < defaultTypes.length; i++) {
            EmailFolder folder = new EmailFolder(user, folderNames[i], defaultTypes[i]);
            folderRepository.save(folder);
        }
        
        logger.info("为用户 {} 初始化了默认邮件文件夹", user.getUsername());
    }
    
    /**
     * 创建自定义文件夹
     */
    public EmailFolder createCustomFolder(User user, String folderName, Long parentId) {
        // 检查文件夹名是否已存在
        Optional<EmailFolder> existing = folderRepository.findByUserAndFolderName(user, folderName);
        if (existing.isPresent()) {
            throw new RuntimeException("文件夹名称已存在");
        }
        
        EmailFolder folder = new EmailFolder(user, folderName, EmailFolder.FolderType.CUSTOM);
        
        if (parentId != null) {
            Optional<EmailFolder> parentOpt = folderRepository.findById(parentId);
            if (parentOpt.isPresent() && parentOpt.get().getUser().getId().equals(user.getId())) {
                folder.setParent(parentOpt.get());
            }
        }
        
        EmailFolder savedFolder = folderRepository.save(folder);
        
        logger.info("用户 {} 创建了新文件夹: {}", user.getUsername(), folderName);
        
        return savedFolder;
    }
    
    /**
     * 获取用户的消息列表
     */
    @Transactional(readOnly = true)
    public Page<EmailMessage> getUserMessages(User user, Long folderId, Pageable pageable) {
        Optional<EmailFolder> folderOpt = folderRepository.findById(folderId);
        if (folderOpt.isEmpty()) {
            throw new RuntimeException("文件夹不存在");
        }
        
        EmailFolder folder = folderOpt.get();
        if (!folder.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权限访问此文件夹");
        }
        
        return messageRepository.findByUserAndFolderAndIsDeletedFalseOrderByReceivedAtDesc(
            user, folder, pageable);
    }
    
    /**
     * 搜索用户的消息
     */
    @Transactional(readOnly = true)
    public Page<EmailMessage> searchUserMessages(User user, String keyword, Pageable pageable) {
        return messageRepository.searchMessages(user, keyword, pageable);
    }
    
    /**
     * 标记消息为已读
     */
    public void markMessageAsRead(User user, Long messageId) {
        Optional<EmailMessage> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            throw new RuntimeException("消息不存在");
        }
        
        EmailMessage message = messageOpt.get();
        if (!message.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权限操作此消息");
        }
        
        message.markAsRead();
        messageRepository.save(message);
    }
    
    /**
     * 获取用户邮件统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserEmailStatistics(User user) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取别名统计
        long totalAliases = aliasRepository.countActiveAliasesByUser(user);
        List<Object[]> aliasStats = aliasRepository.getUserAliasStatistics(user);
        
        stats.put("totalAliases", totalAliases);
        stats.put("aliasesByDomain", aliasStats.stream()
            .collect(Collectors.toMap(
                arr -> (String) arr[0], 
                arr -> (Long) arr[1]
            )));
        
        // 获取消息统计
        List<Object[]> messageStats = messageRepository.getUserMessageStatistics(user);
        if (!messageStats.isEmpty()) {
            Object[] stat = messageStats.get(0);
            stats.put("totalMessages", stat[0]);
            stats.put("unreadMessages", stat[1]);
            stats.put("starredMessages", stat[2]);
            stats.put("totalMessageSize", stat[3]);
        } else {
            stats.put("totalMessages", 0L);
            stats.put("unreadMessages", 0L);
            stats.put("starredMessages", 0L);
            stats.put("totalMessageSize", 0L);
        }
        
        // 获取文件夹统计
        List<Object[]> folderStats = folderRepository.getUserFolderStatistics(user);
        Map<String, Map<String, Long>> folderStatMap = new HashMap<>();
        
        for (Object[] folderStat : folderStats) {
            String folderType = ((EmailFolder.FolderType) folderStat[0]).name();
            Map<String, Long> folderData = new HashMap<>();
            folderData.put("folderCount", (Long) folderStat[1]);
            folderData.put("messageCount", (Long) folderStat[2]);
            folderData.put("unreadCount", (Long) folderStat[3]);
            folderStatMap.put(folderType, folderData);
        }
        
        stats.put("folderStatistics", folderStatMap);
        
        return stats;
    }
    
    /**
     * 获取用户的完整邮件地址列表（用于前端显示）
     */
    @Transactional(readOnly = true)
    public List<String> getUserEmailAddresses(User user) {
        List<EmailAlias> aliases = getUserAliases(user);
        return aliases.stream()
            .map(EmailAlias::getFullEmail)
            .collect(Collectors.toList());
    }
    
    /**
     * 更新别名显示信息
     */
    public EmailAlias updateAliasDisplay(User user, Long aliasId, String displayName, String description, String externalAliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        EmailAlias alias = aliasOpt.get();
        
        // 检查外部别名ID是否已被其他别名使用
        if (externalAliasId != null && !externalAliasId.trim().isEmpty()) {
            Optional<EmailAlias> existingAlias = aliasRepository.findByExternalAliasId(externalAliasId);
            if (existingAlias.isPresent() && !existingAlias.get().getId().equals(aliasId)) {
                throw new RuntimeException("外部别名ID已被其他别名使用");
            }
        }
        
        alias.setDisplayName(displayName);
        alias.setDescription(description);
        alias.setExternalAliasId(externalAliasId);
        
        EmailAlias savedAlias = aliasRepository.save(alias);
        
        logger.info("用户 {} 更新了别名 {} 的显示信息", user.getUsername(), alias.getFullEmail());
        
        return savedAlias;
    }
    
    /**
     * 批量同步别名显示名称（与外部平台同步）
     */
    public int syncAliasDisplayNames(User user, List<EmailController.AliasSyncRequest> requests) {
        int updatedCount = 0;
        List<EmailAlias> userAliases = getUserAliases(user);
        
        // 创建邮箱地址到别名的映射
        Map<String, EmailAlias> emailToAliasMap = userAliases.stream()
            .collect(Collectors.toMap(
                alias -> alias.getFullEmail().toLowerCase(),
                alias -> alias
            ));
        
        for (EmailController.AliasSyncRequest request : requests) {
            try {
                EmailAlias alias = emailToAliasMap.get(request.getFullEmail().toLowerCase());
                if (alias != null) {
                    boolean updated = false;
                    
                    // 更新显示名称
                    if (request.getDisplayName() != null && !request.getDisplayName().equals(alias.getDisplayName())) {
                        alias.setDisplayName(request.getDisplayName());
                        updated = true;
                    }
                    
                    // 更新外部别名ID
                    if (request.getExternalAliasId() != null && !request.getExternalAliasId().equals(alias.getExternalAliasId())) {
                        // 检查外部别名ID是否已被其他别名使用
                        Optional<EmailAlias> existingAlias = aliasRepository.findByExternalAliasId(request.getExternalAliasId());
                        if (existingAlias.isEmpty() || existingAlias.get().getId().equals(alias.getId())) {
                            alias.setExternalAliasId(request.getExternalAliasId());
                            updated = true;
                        } else {
                            logger.warn("外部别名ID {} 已被其他别名使用，跳过更新", request.getExternalAliasId());
                        }
                    }
                    
                    // 更新描述
                    if (request.getDescription() != null && !request.getDescription().equals(alias.getDescription())) {
                        alias.setDescription(request.getDescription());
                        updated = true;
                    }
                    
                    if (updated) {
                        aliasRepository.save(alias);
                        updatedCount++;
                        logger.debug("已更新别名 {} 的显示信息", alias.getFullEmail());
                    }
                } else {
                    logger.warn("未找到邮箱地址为 {} 的别名", request.getFullEmail());
                }
            } catch (Exception e) {
                logger.error("同步别名 {} 失败", request.getFullEmail(), e);
            }
        }
        
        logger.info("用户 {} 批量同步了 {} 个别名的显示信息", user.getUsername(), updatedCount);
        
        return updatedCount;
    }
    
    /**
     * 根据外部别名ID查找别名
     */
    @Transactional(readOnly = true)
    public Optional<EmailAlias> findAliasbyExternalId(User user, String externalAliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findByExternalAliasId(externalAliasId);
        if (aliasOpt.isPresent() && aliasOpt.get().getUser().getId().equals(user.getId())) {
            return aliasOpt;
        }
        return Optional.empty();
    }
    
    /**
     * 搜索别名（根据显示名称或别名前缀）
     */
    @Transactional(readOnly = true)
    public List<EmailAlias> searchAliases(User user, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getUserAliases(user);
        }
        return aliasRepository.searchByDisplayNameOrAlias(user, keyword.trim());
    }
}