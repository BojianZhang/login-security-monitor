package com.security.monitor.repository;

import com.security.monitor.model.EmailFolder;
import com.security.monitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 邮件文件夹仓库接口
 */
@Repository
public interface EmailFolderRepository extends JpaRepository<EmailFolder, Long> {
    
    /**
     * 根据用户查找所有文件夹
     */
    List<EmailFolder> findByUserOrderByCreatedAtAsc(User user);
    
    /**
     * 根据用户和文件夹名称查找
     */
    Optional<EmailFolder> findByUserAndFolderName(User user, String folderName);
    
    /**
     * 根据用户和文件夹类型查找
     */
    List<EmailFolder> findByUserAndFolderType(User user, EmailFolder.FolderType folderType);
    
    /**
     * 根据用户和文件夹类型查找单个
     */
    Optional<EmailFolder> findByUserAndFolderTypeAndParentIsNull(User user, EmailFolder.FolderType folderType);
    
    /**
     * 查找用户的根级文件夹
     */
    List<EmailFolder> findByUserAndParentIsNullOrderByCreatedAtAsc(User user);
    
    /**
     * 查找子文件夹
     */
    List<EmailFolder> findByParentOrderByCreatedAtAsc(EmailFolder parent);
    
    /**
     * 获取用户文件夹统计
     */
    @Query("SELECT f.folderType, COUNT(f), SUM(f.messageCount), SUM(f.unreadCount) " +
           "FROM EmailFolder f " +
           "WHERE f.user = :user " +
           "GROUP BY f.folderType")
    List<Object[]> getUserFolderStatistics(@Param("user") User user);
    
    /**
     * 查找用户的收件箱
     */
    @Query("SELECT f FROM EmailFolder f " +
           "WHERE f.user = :user AND f.folderType = 'INBOX' " +
           "AND f.parent IS NULL")
    Optional<EmailFolder> findInboxByUser(@Param("user") User user);
    
    /**
     * 查找用户的发件箱
     */
    @Query("SELECT f FROM EmailFolder f " +
           "WHERE f.user = :user AND f.folderType = 'SENT' " +
           "AND f.parent IS NULL")
    Optional<EmailFolder> findSentBoxByUser(@Param("user") User user);
    
    /**
     * 查找用户的草稿箱
     */
    @Query("SELECT f FROM EmailFolder f " +
           "WHERE f.user = :user AND f.folderType = 'DRAFT' " +
           "AND f.parent IS NULL")
    Optional<EmailFolder> findDraftBoxByUser(@Param("user") User user);
    
    /**
     * 查找用户的垃圾箱
     */
    @Query("SELECT f FROM EmailFolder f " +
           "WHERE f.user = :user AND f.folderType = 'TRASH' " +
           "AND f.parent IS NULL")
    Optional<EmailFolder> findTrashByUser(@Param("user") User user);
    
    /**
     * 查找用户的垃圾邮件箱
     */
    @Query("SELECT f FROM EmailFolder f " +
           "WHERE f.user = :user AND f.folderType = 'SPAM' " +
           "AND f.parent IS NULL")
    Optional<EmailFolder> findSpamBoxByUser(@Param("user") User user);
}