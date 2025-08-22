package com.security.monitor.repository;

import com.security.monitor.model.EmailGroupMember;
import com.security.monitor.model.EmailGroup;
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
 * 邮件群组成员仓库接口
 */
@Repository
public interface EmailGroupMemberRepository extends JpaRepository<EmailGroupMember, Long> {
    
    /**
     * 根据群组查找成员
     */
    List<EmailGroupMember> findByGroup(EmailGroup group);
    
    /**
     * 根据群组和状态查找成员
     */
    List<EmailGroupMember> findByGroupAndIsActive(EmailGroup group, boolean isActive);
    
    /**
     * 根据群组查找活跃且可接收邮件的成员
     */
    List<EmailGroupMember> findByGroupAndIsActiveAndCanReceive(EmailGroup group, boolean isActive, boolean canReceive);
    
    /**
     * 根据群组查找审核员
     */
    List<EmailGroupMember> findByGroupAndIsModeratorAndIsActive(EmailGroup group, boolean isModerator, boolean isActive);
    
    /**
     * 根据用户查找群组成员关系
     */
    Page<EmailGroupMember> findByUser(User user, Pageable pageable);
    
    /**
     * 根据用户和状态查找群组成员关系
     */
    List<EmailGroupMember> findByUserAndIsActive(User user, boolean isActive);
    
    /**
     * 检查用户是否已是群组成员
     */
    boolean existsByGroupAndUser(EmailGroup group, User user);
    
    /**
     * 检查外部邮箱是否已是群组成员
     */
    boolean existsByGroupAndExternalEmail(EmailGroup group, String externalEmail);
    
    /**
     * 根据群组和用户查找成员
     */
    EmailGroupMember findByGroupAndUser(EmailGroup group, User user);
    
    /**
     * 根据群组和外部邮箱查找成员
     */
    EmailGroupMember findByGroupAndExternalEmail(EmailGroup group, String externalEmail);
    
    /**
     * 检查发件人是否为群组成员（支持内部和外部邮箱）
     */
    @Query("SELECT COUNT(m) > 0 FROM EmailGroupMember m WHERE m.group = :group AND m.isActive = true AND " +
           "((m.user IS NOT NULL AND m.user.email = :email) OR m.externalEmail = :email)")
    boolean existsByGroupAndUserEmailOrExternalEmail(@Param("group") EmailGroup group, @Param("email") String email, @Param("email2") String email2);
    
    /**
     * 统计群组成员数量
     */
    long countByGroup(EmailGroup group);
    
    /**
     * 统计群组活跃成员数量
     */
    long countByGroupAndIsActive(EmailGroup group, boolean isActive);
    
    /**
     * 根据成员角色查找成员
     */
    List<EmailGroupMember> findByGroupAndMemberRoleAndIsActive(EmailGroup group, EmailGroupMember.MemberRole memberRole, boolean isActive);
    
    /**
     * 根据订阅类型查找成员
     */
    List<EmailGroupMember> findByGroupAndSubscriptionTypeAndIsActive(EmailGroup group, EmailGroupMember.SubscriptionType subscriptionType, boolean isActive);
    
    /**
     * 查找最近加入的成员
     */
    List<EmailGroupMember> findByGroupAndJoinedAtAfterOrderByJoinedAtDesc(EmailGroup group, LocalDateTime after);
    
    /**
     * 查找最近活跃的成员
     */
    @Query("SELECT m FROM EmailGroupMember m WHERE m.group = :group AND m.lastActivityAt IS NOT NULL ORDER BY m.lastActivityAt DESC")
    List<EmailGroupMember> findRecentlyActiveMembers(@Param("group") EmailGroup group, Pageable pageable);
    
    /**
     * 查找长时间无活动的成员
     */
    @Query("SELECT m FROM EmailGroupMember m WHERE m.group = :group AND m.isActive = true AND " +
           "(m.lastActivityAt IS NULL OR m.lastActivityAt < :threshold)")
    List<EmailGroupMember> findInactiveMembers(@Param("group") EmailGroup group, @Param("threshold") LocalDateTime threshold);
    
    /**
     * 按成员角色统计
     */
    @Query("SELECT m.memberRole, COUNT(m) FROM EmailGroupMember m WHERE m.group = :group AND m.isActive = true GROUP BY m.memberRole")
    List<Object[]> getMemberRoleStatistics(@Param("group") EmailGroup group);
    
    /**
     * 按订阅类型统计
     */
    @Query("SELECT m.subscriptionType, COUNT(m) FROM EmailGroupMember m WHERE m.group = :group AND m.isActive = true GROUP BY m.subscriptionType")
    List<Object[]> getSubscriptionTypeStatistics(@Param("group") EmailGroup group);
    
    /**
     * 获取成员统计信息
     */
    @Query("SELECT " +
           "COUNT(m) as totalMembers, " +
           "SUM(CASE WHEN m.isActive = true THEN 1 ELSE 0 END) as activeMembers, " +
           "SUM(CASE WHEN m.user IS NOT NULL THEN 1 ELSE 0 END) as internalMembers, " +
           "SUM(CASE WHEN m.externalEmail IS NOT NULL THEN 1 ELSE 0 END) as externalMembers " +
           "FROM EmailGroupMember m WHERE m.group = :group")
    Object[] getGroupMemberStatistics(@Param("group") EmailGroup group);
    
    /**
     * 查找用户参与的所有群组
     */
    @Query("SELECT m.group FROM EmailGroupMember m WHERE m.user = :user AND m.isActive = true")
    List<EmailGroup> findGroupsByUser(@Param("user") User user);
    
    /**
     * 查找用户管理的群组
     */
    @Query("SELECT m.group FROM EmailGroupMember m WHERE m.user = :user AND m.isActive = true AND " +
           "m.memberRole IN ('OWNER', 'ADMIN')")
    List<EmailGroup> findManagedGroupsByUser(@Param("user") User user);
    
    /**
     * 搜索成员（按名称、邮箱）
     */
    @Query("SELECT m FROM EmailGroupMember m WHERE m.group = :group AND " +
           "(LOWER(m.memberName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.externalEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(m.user IS NOT NULL AND (LOWER(m.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))))")
    Page<EmailGroupMember> searchMembers(@Param("group") EmailGroup group, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 批量更新成员状态
     */
    @Query("UPDATE EmailGroupMember m SET m.isActive = :isActive WHERE m.group = :group AND m.id IN :memberIds")
    void batchUpdateMemberStatus(@Param("group") EmailGroup group, @Param("memberIds") List<Long> memberIds, @Param("isActive") boolean isActive);
    
    /**
     * 删除群组的所有成员
     */
    void deleteByGroup(EmailGroup group);
    
    /**
     * 删除用户的所有群组成员关系
     */
    void deleteByUser(User user);
}