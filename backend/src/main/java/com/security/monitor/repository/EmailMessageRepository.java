package com.security.monitor.repository;

import com.security.monitor.model.EmailFolder;
import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件消息仓库接口
 */
@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {
    
    /**
     * 根据用户和文件夹分页查询消息
     */
    Page<EmailMessage> findByUserAndFolderAndIsDeletedFalseOrderByReceivedAtDesc(
        User user, EmailFolder folder, Pageable pageable);
    
    /**
     * 查找用户的未读消息
     */
    Page<EmailMessage> findByUserAndIsReadFalseAndIsDeletedFalseOrderByReceivedAtDesc(
        User user, Pageable pageable);
    
    /**
     * 根据消息ID查找
     */
    Optional<EmailMessage> findByMessageId(String messageId);
    
    /**
     * 根据线程ID查找消息
     */
    List<EmailMessage> findByThreadIdOrderByReceivedAtAsc(String threadId);
    
    /**
     * 查找用户的星标消息
     */
    Page<EmailMessage> findByUserAndIsStarredTrueAndIsDeletedFalseOrderByReceivedAtDesc(
        User user, Pageable pageable);
    
    /**
     * 搜索消息（全文搜索）
     */
    @Query("SELECT m FROM EmailMessage m " +
           "WHERE m.user = :user " +
           "AND m.isDeleted = false " +
           "AND (LOWER(m.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(m.bodyText) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(m.fromAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY m.receivedAt DESC")
    Page<EmailMessage> searchMessages(@Param("user") User user, 
                                     @Param("keyword") String keyword, 
                                     Pageable pageable);
    
    /**
     * 查找用户某个时间段的消息
     */
    List<EmailMessage> findByUserAndReceivedAtBetweenOrderByReceivedAtDesc(
        User user, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 统计用户未读消息数量
     */
    @Query("SELECT COUNT(m) FROM EmailMessage m " +
           "WHERE m.user = :user AND m.isRead = false AND m.isDeleted = false")
    long countUnreadMessagesByUser(@Param("user") User user);
    
    /**
     * 统计文件夹中的消息数量
     */
    @Query("SELECT COUNT(m) FROM EmailMessage m " +
           "WHERE m.folder = :folder AND m.isDeleted = false")
    long countMessagesByFolder(@Param("folder") EmailFolder folder);
    
    /**
     * 统计文件夹中的未读消息数量
     */
    @Query("SELECT COUNT(m) FROM EmailMessage m " +
           "WHERE m.folder = :folder AND m.isRead = false AND m.isDeleted = false")
    long countUnreadMessagesByFolder(@Param("folder") EmailFolder folder);
    
    /**
     * 批量标记消息为已读
     */
    @Modifying
    @Query("UPDATE EmailMessage m SET m.isRead = true " +
           "WHERE m.user = :user AND m.folder = :folder AND m.isRead = false")
    void markFolderMessagesAsRead(@Param("user") User user, @Param("folder") EmailFolder folder);
    
    /**
     * 批量删除消息（软删除）
     */
    @Modifying
    @Query("UPDATE EmailMessage m SET m.isDeleted = true " +
           "WHERE m.user = :user AND m.id IN :messageIds")
    void softDeleteMessages(@Param("user") User user, @Param("messageIds") List<Long> messageIds);
    
    /**
     * 移动消息到指定文件夹
     */
    @Modifying
    @Query("UPDATE EmailMessage m SET m.folder = :targetFolder " +
           "WHERE m.user = :user AND m.id IN :messageIds")
    void moveMessages(@Param("user") User user, 
                     @Param("messageIds") List<Long> messageIds, 
                     @Param("targetFolder") EmailFolder targetFolder);
    
    /**
     * 查找需要清理的旧消息
     */
    @Query("SELECT m FROM EmailMessage m " +
           "WHERE m.receivedAt < :cutoffDate " +
           "AND m.folder.folderType IN ('TRASH', 'SPAM') " +
           "AND m.isDeleted = true")
    List<EmailMessage> findOldDeletedMessages(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 根据收件人地址查找消息（支持按别名查询）
     */
    Page<EmailMessage> findByToAddressesContainingOrderByReceivedAtDesc(String toAddress, Pageable pageable);
    
    /**
     * 统计指定收件人地址的消息数量
     */
    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.toAddresses LIKE CONCAT('%', :toAddress, '%') AND m.isDeleted = false")
    long countByToAddressesContaining(@Param("toAddress") String toAddress);
    
    /**
     * 统计指定收件人地址的未读消息数量
     */
    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.toAddresses LIKE CONCAT('%', :toAddress, '%') AND m.isRead = false AND m.isDeleted = false")
    long countByToAddressesContainingAndIsReadFalse(@Param("toAddress") String toAddress);
    
    /**
     * 统计指定时间后收到的消息数量
     */
    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.toAddresses LIKE CONCAT('%', :toAddress, '%') AND m.receivedAt > :after AND m.isDeleted = false")
    long countByToAddressesContainingAndReceivedAtAfter(@Param("toAddress") String toAddress, @Param("after") LocalDateTime after);
    
    /**
     * 批量标记指定收件人地址的消息为已读
     */
    @Modifying
    @Query("UPDATE EmailMessage m SET m.isRead = true WHERE m.toAddresses LIKE CONCAT('%', :toAddress, '%') AND m.isRead = false")
    void markMessagesAsReadByToAddress(@Param("toAddress") String toAddress);
    
    /**
     * 获取用户邮件统计信息
     */
    @Query("SELECT " +
           "COUNT(m) as totalCount, " +
           "SUM(CASE WHEN m.isRead = false THEN 1 ELSE 0 END) as unreadCount, " +
           "SUM(CASE WHEN m.isStarred = true THEN 1 ELSE 0 END) as starredCount, " +
           "SUM(m.messageSize) as totalSize " +
           "FROM EmailMessage m " +
           "WHERE m.user = :user AND m.isDeleted = false")
    List<Object[]> getUserMessageStatistics(@Param("user") User user);
    
    // ======= 搜索相关方法 =======
    
    /**
     * 全文搜索（使用MySQL FULLTEXT索引）
     */
    @Query(value = "SELECT * FROM email_messages m " +
                   "WHERE m.user_id = :userId " +
                   "AND m.is_deleted = false " +
                   "AND (:dateFrom IS NULL OR m.received_at >= :dateFrom) " +
                   "AND (:dateTo IS NULL OR m.received_at <= :dateTo) " +
                   "AND MATCH(m.subject, m.body_text, m.body_html) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
                   "ORDER BY m.received_at DESC",
           nativeQuery = true)
    Page<EmailMessage> searchByUser(@Param("query") String query,
                                   @Param("userId") Long userId,
                                   @Param("dateFrom") LocalDateTime dateFrom,
                                   @Param("dateTo") LocalDateTime dateTo,
                                   Pageable pageable);
    
    /**
     * 在指定文件夹内全文搜索
     */
    @Query(value = "SELECT * FROM email_messages m " +
                   "WHERE m.user_id = :userId " +
                   "AND m.folder_id = :folderId " +
                   "AND m.is_deleted = false " +
                   "AND (:dateFrom IS NULL OR m.received_at >= :dateFrom) " +
                   "AND (:dateTo IS NULL OR m.received_at <= :dateTo) " +
                   "AND MATCH(m.subject, m.body_text, m.body_html) AGAINST(:query IN NATURAL LANGUAGE MODE) " +
                   "ORDER BY m.received_at DESC",
           nativeQuery = true)
    Page<EmailMessage> searchInFolder(@Param("query") String query,
                                     @Param("userId") Long userId,
                                     @Param("folderId") Long folderId,
                                     @Param("dateFrom") LocalDateTime dateFrom,
                                     @Param("dateTo") LocalDateTime dateTo,
                                     Pageable pageable);
    
    /**
     * 高级搜索（精确匹配）
     */
    @Query("SELECT m FROM EmailMessage m " +
           "WHERE m.user = :user " +
           "AND m.isDeleted = false " +
           "AND (:fromAddress IS NULL OR LOWER(m.fromAddress) LIKE LOWER(CONCAT('%', :fromAddress, '%'))) " +
           "AND (:toAddress IS NULL OR LOWER(m.toAddresses) LIKE LOWER(CONCAT('%', :toAddress, '%'))) " +
           "AND (:subject IS NULL OR LOWER(m.subject) LIKE LOWER(CONCAT('%', :subject, '%'))) " +
           "AND (:bodyText IS NULL OR LOWER(m.bodyText) LIKE LOWER(CONCAT('%', :bodyText, '%'))) " +
           "AND (:folderId IS NULL OR m.folder.id = :folderId) " +
           "AND (:dateFrom IS NULL OR m.receivedAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR m.receivedAt <= :dateTo) " +
           "AND (:isRead IS NULL OR m.isRead = :isRead) " +
           "AND (:hasAttachments IS NULL OR " +
           "     (:hasAttachments = true AND EXISTS(SELECT a FROM EmailAttachment a WHERE a.message = m)) OR " +
           "     (:hasAttachments = false AND NOT EXISTS(SELECT a FROM EmailAttachment a WHERE a.message = m))) " +
           "AND (:priorityLevel IS NULL OR m.priorityLevel = :priorityLevel) " +
           "ORDER BY m.receivedAt DESC")
    Page<EmailMessage> advancedSearch(@Param("user") User user,
                                     @Param("fromAddress") String fromAddress,
                                     @Param("toAddress") String toAddress,
                                     @Param("subject") String subject,
                                     @Param("bodyText") String bodyText,
                                     @Param("folderId") Long folderId,
                                     @Param("dateFrom") LocalDateTime dateFrom,
                                     @Param("dateTo") LocalDateTime dateTo,
                                     @Param("isRead") Boolean isRead,
                                     @Param("hasAttachments") Boolean hasAttachments,
                                     @Param("priorityLevel") Integer priorityLevel,
                                     Pageable pageable);
    
    /**
     * 获取发件人建议
     */
    @Query("SELECT DISTINCT m.fromAddress FROM EmailMessage m " +
           "WHERE m.user = :user " +
           "AND m.isDeleted = false " +
           "AND LOWER(m.fromAddress) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.fromAddress")
    List<String> findSenderSuggestions(@Param("user") User user, 
                                      @Param("query") String query,
                                      Pageable pageable);
    
    /**
     * 获取主题建议
     */
    @Query("SELECT DISTINCT m.subject FROM EmailMessage m " +
           "WHERE m.user = :user " +
           "AND m.isDeleted = false " +
           "AND m.subject IS NOT NULL " +
           "AND LOWER(m.subject) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.subject")
    List<String> findSubjectSuggestions(@Param("user") User user, 
                                       @Param("query") String query,
                                       Pageable pageable);
    
    /**
     * 统计用户邮件数量
     */
    long countByUser(User user);
    
    /**
     * 统计用户未读邮件数量
     */
    long countByUserAndIsRead(User user, boolean isRead);
    
    /**
     * 统计用户指定时间后的邮件数量
     */
    long countByUserAndReceivedAtAfter(User user, LocalDateTime after);
    
    /**
     * 统计文件夹邮件数量
     */
    int countByFolder(EmailFolder folder);
    
    /**
     * 统计文件夹未读邮件数量
     */
    int countByFolderAndIsRead(EmailFolder folder, boolean isRead);
    
    /**
     * 获取用户热门发件人统计
     */
    @Query("SELECT m.fromAddress, COUNT(m) as messageCount " +
           "FROM EmailMessage m " +
           "WHERE m.user = :user " +
           "AND m.isDeleted = false " +
           "AND m.fromAddress IS NOT NULL " +
           "GROUP BY m.fromAddress " +
           "ORDER BY messageCount DESC")
    List<Object[]> findTopSendersByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 检查消息ID是否存在
     */
    boolean existsByMessageId(String messageId);
}