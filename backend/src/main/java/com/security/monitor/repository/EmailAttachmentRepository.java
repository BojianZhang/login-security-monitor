package com.security.monitor.repository;

import com.security.monitor.model.EmailAttachment;
import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件附件仓库接口
 */
@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {
    
    /**
     * 根据邮件查找附件
     */
    List<EmailAttachment> findByMessage(EmailMessage message);
    
    /**
     * 根据邮件查找附件（排序）
     */
    List<EmailAttachment> findByMessageOrderByFilename(EmailMessage message);
    
    /**
     * 根据内容ID查找内嵌附件
     */
    Optional<EmailAttachment> findByMessageAndContentId(EmailMessage message, String contentId);
    
    /**
     * 查找用户的所有附件
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "ORDER BY a.message.receivedAt DESC")
    Page<EmailAttachment> findByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 搜索用户的附件
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "AND a.message.isDeleted = false " +
           "AND (:filename IS NULL OR LOWER(a.filename) LIKE LOWER(CONCAT('%', :filename, '%'))) " +
           "AND (:contentType IS NULL OR LOWER(a.contentType) LIKE LOWER(CONCAT('%', :contentType, '%'))) " +
           "AND (:minSize IS NULL OR a.fileSize >= :minSize) " +
           "AND (:maxSize IS NULL OR a.fileSize <= :maxSize) " +
           "AND (:dateFrom IS NULL OR a.message.receivedAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR a.message.receivedAt <= :dateTo) " +
           "ORDER BY a.message.receivedAt DESC")
    Page<EmailAttachment> searchByUser(@Param("user") User user,
                                      @Param("filename") String filename,
                                      @Param("contentType") String contentType,
                                      @Param("minSize") Long minSize,
                                      @Param("maxSize") Long maxSize,
                                      @Param("dateFrom") LocalDateTime dateFrom,
                                      @Param("dateTo") LocalDateTime dateTo,
                                      Pageable pageable);
    
    /**
     * 统计用户附件数量
     */
    @Query("SELECT COUNT(a) FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "AND a.message.isDeleted = false")
    long countByUser(@Param("user") User user);
    
    /**
     * 统计用户附件总大小
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "AND a.message.isDeleted = false")
    long sumFileSizeByUser(@Param("user") User user);
    
    /**
     * 根据文件哈希查找附件（用于去重）
     */
    List<EmailAttachment> findByFileHash(String fileHash);
    
    /**
     * 统计邮件的附件数量
     */
    int countByMessage(EmailMessage message);
    
    /**
     * 查找大附件（超过指定大小）
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "AND a.fileSize > :sizeThreshold " +
           "ORDER BY a.fileSize DESC")
    List<EmailAttachment> findLargeAttachments(@Param("user") User user, 
                                              @Param("sizeThreshold") long sizeThreshold,
                                              Pageable pageable);
    
    /**
     * 按文件类型统计附件
     */
    @Query("SELECT a.contentType, COUNT(a), SUM(a.fileSize) " +
           "FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "AND a.message.isDeleted = false " +
           "GROUP BY a.contentType " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> getAttachmentStatsByContentType(@Param("user") User user);
    
    /**
     * 查找孤立的附件（对应的邮件已删除）
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message.isDeleted = true")
    List<EmailAttachment> findOrphanedAttachments();
    
    /**
     * 删除指定邮件的所有附件
     */
    void deleteByMessage(EmailMessage message);
    
    /**
     * 查找内嵌附件
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message = :message " +
           "AND a.isInline = true")
    List<EmailAttachment> findInlineAttachments(@Param("message") EmailMessage message);
    
    /**
     * 查找普通附件（非内嵌）
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message = :message " +
           "AND (a.isInline = false OR a.isInline IS NULL)")
    List<EmailAttachment> findRegularAttachments(@Param("message") EmailMessage message);
    
    /**
     * 根据隔离状态查找附件
     */
    Page<EmailAttachment> findByIsQuarantined(boolean isQuarantined, Pageable pageable);
    
    /**
     * 查找指定用户的隔离附件
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "AND a.isQuarantined = :isQuarantined " +
           "ORDER BY a.lastScannedAt DESC")
    Page<EmailAttachment> findQuarantinedByUser(@Param("user") User user, 
                                               @Param("isQuarantined") boolean isQuarantined, 
                                               Pageable pageable);
    
    /**
     * 统计隔离文件数量
     */
    long countByIsQuarantined(boolean isQuarantined);
    
    /**
     * 根据病毒扫描状态查找附件
     */
    List<EmailAttachment> findByVirusScanStatus(String scanStatus);
    
    /**
     * 查找需要扫描的附件（未扫描或扫描时间过期）
     */
    @Query("SELECT a FROM EmailAttachment a " +
           "WHERE a.virusScanStatus IS NULL " +
           "OR a.virusScanStatus = 'PENDING' " +
           "OR (a.lastScannedAt IS NOT NULL AND a.lastScannedAt < :expiredDate)")
    List<EmailAttachment> findAttachmentsNeedingScan(@Param("expiredDate") LocalDateTime expiredDate);
    
    /**
     * 统计病毒扫描状态分布
     */
    @Query("SELECT a.virusScanStatus, COUNT(a) " +
           "FROM EmailAttachment a " +
           "WHERE a.message.user = :user " +
           "GROUP BY a.virusScanStatus")
    List<Object[]> getScanStatusDistribution(@Param("user") User user);
}