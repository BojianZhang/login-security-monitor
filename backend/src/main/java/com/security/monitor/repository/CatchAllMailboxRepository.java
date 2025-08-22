package com.security.monitor.repository;

import com.security.monitor.model.CatchAllMailbox;
import com.security.monitor.model.EmailDomain;
import com.security.monitor.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Catch-All邮箱仓库接口
 */
@Repository
public interface CatchAllMailboxRepository extends JpaRepository<CatchAllMailbox, Long> {
    
    /**
     * 根据域名和状态查找Catch-All邮箱（按优先级排序）
     */
    List<CatchAllMailbox> findByDomainAndIsActiveOrderByPriority(EmailDomain domain, boolean isActive);
    
    /**
     * 根据域名名称和状态查找Catch-All邮箱（按优先级排序）
     */
    @Query("SELECT c FROM CatchAllMailbox c WHERE c.domain.domainName = :domainName AND c.isActive = :isActive ORDER BY c.priority ASC")
    List<CatchAllMailbox> findByDomainNameAndIsActiveOrderByPriority(@Param("domainName") String domainName, @Param("isActive") boolean isActive);
    
    /**
     * 根据域名查找Catch-All邮箱
     */
    Page<CatchAllMailbox> findByDomain(EmailDomain domain, Pageable pageable);
    
    /**
     * 根据目标用户查找Catch-All邮箱
     */
    List<CatchAllMailbox> findByTargetUserAndIsActive(User targetUser, boolean isActive);
    
    /**
     * 根据目标邮箱查找Catch-All邮箱
     */
    List<CatchAllMailbox> findByTargetEmailAndIsActive(String targetEmail, boolean isActive);
    
    /**
     * 根据Catch-All类型查找
     */
    List<CatchAllMailbox> findByDomainAndCatchAllTypeAndIsActive(EmailDomain domain, CatchAllMailbox.CatchAllType catchAllType, boolean isActive);
    
    /**
     * 统计域名下的Catch-All邮箱数量
     */
    long countByDomain(EmailDomain domain);
    
    /**
     * 统计域名下活跃的Catch-All邮箱数量
     */
    long countByDomainAndIsActive(EmailDomain domain, boolean isActive);
    
    /**
     * 获取域名下的最大优先级
     */
    @Query("SELECT MAX(c.priority) FROM CatchAllMailbox c WHERE c.domain = :domain")
    Integer getMaxPriorityByDomain(@Param("domain") EmailDomain domain);
    
    /**
     * 查找最活跃的Catch-All邮箱
     */
    @Query("SELECT c FROM CatchAllMailbox c WHERE c.domain = :domain AND c.isActive = true ORDER BY c.messageCount DESC")
    List<CatchAllMailbox> findMostActiveCatchAll(@Param("domain") EmailDomain domain, Pageable pageable);
    
    /**
     * 查找最近有消息的Catch-All邮箱
     */
    @Query("SELECT c FROM CatchAllMailbox c WHERE c.domain = :domain AND c.lastMessageDate IS NOT NULL ORDER BY c.lastMessageDate DESC")
    List<CatchAllMailbox> findRecentlyActiveCatchAll(@Param("domain") EmailDomain domain, Pageable pageable);
    
    /**
     * 查找达到每日限制的Catch-All邮箱
     */
    @Query("SELECT c FROM CatchAllMailbox c WHERE c.domain = :domain AND c.isActive = true AND " +
           "c.maxDailyMessages IS NOT NULL AND c.dailyMessageCount >= c.maxDailyMessages AND " +
           "DATE(c.lastMessageDate) = CURRENT_DATE")
    List<CatchAllMailbox> findCatchAllAtDailyLimit(@Param("domain") EmailDomain domain);
    
    /**
     * 按Catch-All类型统计
     */
    @Query("SELECT c.catchAllType, COUNT(c), SUM(c.messageCount) FROM CatchAllMailbox c WHERE c.domain = :domain GROUP BY c.catchAllType")
    List<Object[]> getCatchAllTypeStatistics(@Param("domain") EmailDomain domain);
    
    /**
     * 获取Catch-All统计信息
     */
    @Query("SELECT " +
           "COUNT(c) as totalCatchAll, " +
           "SUM(CASE WHEN c.isActive = true THEN 1 ELSE 0 END) as activeCatchAll, " +
           "SUM(c.messageCount) as totalMessages, " +
           "AVG(c.messageCount) as avgMessagesPerCatchAll " +
           "FROM CatchAllMailbox c WHERE c.domain = :domain")
    Object[] getDomainCatchAllStatistics(@Param("domain") EmailDomain domain);
    
    /**
     * 查找启用自动回复的Catch-All邮箱
     */
    List<CatchAllMailbox> findByDomainAndAutoReplyEnabledAndIsActive(EmailDomain domain, boolean autoReplyEnabled, boolean isActive);
    
    /**
     * 查找需要重置每日计数的Catch-All邮箱
     */
    @Query("SELECT c FROM CatchAllMailbox c WHERE c.dailyMessageCount > 0 AND " +
           "(c.lastMessageDate IS NULL OR DATE(c.lastMessageDate) < CURRENT_DATE)")
    List<CatchAllMailbox> findCatchAllNeedingDailyReset();
    
    /**
     * 搜索Catch-All邮箱（按目标邮箱）
     */
    @Query("SELECT c FROM CatchAllMailbox c WHERE c.domain = :domain AND " +
           "(LOWER(c.targetEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(c.targetUser IS NOT NULL AND (LOWER(c.targetUser.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.targetUser.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))))")
    Page<CatchAllMailbox> searchCatchAll(@Param("domain") EmailDomain domain, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 获取每日Catch-All活动统计
     */
    @Query("SELECT DATE(c.lastMessageDate) as activityDate, COUNT(DISTINCT c.id) as activeCatchAll, SUM(c.dailyMessageCount) as totalMessages " +
           "FROM CatchAllMailbox c WHERE c.domain = :domain AND c.lastMessageDate BETWEEN :start AND :end " +
           "GROUP BY DATE(c.lastMessageDate) ORDER BY activityDate DESC")
    List<Object[]> getDailyCatchAllActivity(@Param("domain") EmailDomain domain, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 查找重复优先级的Catch-All邮箱
     */
    @Query("SELECT c.priority, COUNT(c) FROM CatchAllMailbox c WHERE c.domain = :domain AND c.isActive = true GROUP BY c.priority HAVING COUNT(c) > 1")
    List<Object[]> findDuplicatePriorities(@Param("domain") EmailDomain domain);
    
    /**
     * 批量更新优先级
     */
    @Query("UPDATE CatchAllMailbox c SET c.priority = :priority WHERE c.id = :catchAllId")
    void updatePriority(@Param("catchAllId") Long catchAllId, @Param("priority") Integer priority);
    
    /**
     * 重置每日消息计数
     */
    @Query("UPDATE CatchAllMailbox c SET c.dailyMessageCount = 0 WHERE c.id IN :catchAllIds")
    void resetDailyMessageCount(@Param("catchAllIds") List<Long> catchAllIds);
    
    /**
     * 删除域名下的所有Catch-All邮箱
     */
    void deleteByDomain(EmailDomain domain);
    
    /**
     * 删除目标用户的所有Catch-All邮箱
     */
    void deleteByTargetUser(User targetUser);
}