package com.security.monitor.repository;

import com.security.monitor.model.EmailGroup;
import com.security.monitor.model.EmailDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件群组仓库接口
 */
@Repository
public interface EmailGroupRepository extends JpaRepository<EmailGroup, Long> {
    
    /**
     * 根据群组邮箱地址和状态查找群组
     */
    EmailGroup findByGroupEmailAndIsActive(String groupEmail, boolean isActive);
    
    /**
     * 根据域名查找群组
     */
    Page<EmailGroup> findByDomain(EmailDomain domain, Pageable pageable);
    
    /**
     * 根据域名和状态查找群组
     */
    Page<EmailGroup> findByDomainAndIsActive(EmailDomain domain, boolean isActive, Pageable pageable);
    
    /**
     * 根据群组类型查找群组
     */
    List<EmailGroup> findByGroupTypeAndIsActive(EmailGroup.GroupType groupType, boolean isActive);
    
    /**
     * 检查群组名是否已存在（域名范围内）
     */
    boolean existsByDomainAndGroupName(EmailDomain domain, String groupName);
    
    /**
     * 根据群组名查找（域名范围内）
     */
    EmailGroup findByDomainAndGroupName(EmailDomain domain, String groupName);
    
    /**
     * 统计域名下的群组数量
     */
    long countByDomain(EmailDomain domain);
    
    /**
     * 统计域名下活跃群组数量
     */
    long countByDomainAndIsActive(EmailDomain domain, boolean isActive);
    
    /**
     * 查找最活跃的群组
     */
    @Query("SELECT g FROM EmailGroup g WHERE g.domain = :domain AND g.isActive = true ORDER BY g.messageCount DESC")
    List<EmailGroup> findMostActiveGroups(@Param("domain") EmailDomain domain, Pageable pageable);
    
    /**
     * 查找最近创建的群组
     */
    List<EmailGroup> findByDomainAndCreatedAtAfterOrderByCreatedAtDesc(EmailDomain domain, LocalDateTime after);
    
    /**
     * 查找最近有消息的群组
     */
    @Query("SELECT g FROM EmailGroup g WHERE g.domain = :domain AND g.lastMessageAt IS NOT NULL ORDER BY g.lastMessageAt DESC")
    List<EmailGroup> findRecentlyActiveGroups(@Param("domain") EmailDomain domain, Pageable pageable);
    
    /**
     * 查找长时间无活动的群组
     */
    @Query("SELECT g FROM EmailGroup g WHERE g.domain = :domain AND g.isActive = true AND (g.lastMessageAt IS NULL OR g.lastMessageAt < :threshold)")
    List<EmailGroup> findInactiveGroups(@Param("domain") EmailDomain domain, @Param("threshold") LocalDateTime threshold);
    
    /**
     * 按群组类型统计
     */
    @Query("SELECT g.groupType, COUNT(g), SUM(g.messageCount) FROM EmailGroup g WHERE g.domain = :domain GROUP BY g.groupType")
    List<Object[]> getGroupTypeStatistics(@Param("domain") EmailDomain domain);
    
    /**
     * 获取群组统计信息
     */
    @Query("SELECT " +
           "COUNT(g) as totalGroups, " +
           "SUM(CASE WHEN g.isActive = true THEN 1 ELSE 0 END) as activeGroups, " +
           "SUM(g.messageCount) as totalMessages, " +
           "AVG(g.messageCount) as avgMessagesPerGroup " +
           "FROM EmailGroup g WHERE g.domain = :domain")
    Object[] getDomainGroupStatistics(@Param("domain") EmailDomain domain);
    
    /**
     * 查找需要审核的群组
     */
    List<EmailGroup> findByDomainAndRequireModerationAndIsActive(EmailDomain domain, boolean requireModeration, boolean isActive);
    
    /**
     * 查找允许外部发件人的群组
     */
    List<EmailGroup> findByDomainAndAllowExternalSendersAndIsActive(EmailDomain domain, boolean allowExternalSenders, boolean isActive);
    
    /**
     * 搜索群组（按名称、显示名称、描述）
     */
    @Query("SELECT g FROM EmailGroup g WHERE g.domain = :domain AND " +
           "(LOWER(g.groupName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<EmailGroup> searchGroups(@Param("domain") EmailDomain domain, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 查找成员数量超过限制的群组
     */
    @Query("SELECT g FROM EmailGroup g WHERE g.domain = :domain AND g.maxMembers IS NOT NULL AND " +
           "(SELECT COUNT(m) FROM EmailGroupMember m WHERE m.group = g AND m.isActive = true) >= g.maxMembers")
    List<EmailGroup> findGroupsAtCapacity(@Param("domain") EmailDomain domain);
    
    /**
     * 获取每日群组活动统计
     */
    @Query("SELECT DATE(g.lastMessageAt) as activityDate, COUNT(DISTINCT g.id) as activeGroups, SUM(g.messageCount) as totalMessages " +
           "FROM EmailGroup g WHERE g.domain = :domain AND g.lastMessageAt BETWEEN :start AND :end " +
           "GROUP BY DATE(g.lastMessageAt) ORDER BY activityDate DESC")
    List<Object[]> getDailyGroupActivity(@Param("domain") EmailDomain domain, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 删除域名下的所有群组
     */
    void deleteByDomain(EmailDomain domain);
}