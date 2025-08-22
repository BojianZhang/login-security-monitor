package com.security.monitor.repository;

import com.security.monitor.model.EmailAlias;
import com.security.monitor.model.EmailDomain;
import com.security.monitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 邮箱别名仓库接口 - 性能优化版
 */
@Repository
public interface EmailAliasRepository extends JpaRepository<EmailAlias, Long> {
    
    /**
     * 根据用户查找所有别名 - 优化版本，避免N+1查询
     */
    @Query("SELECT a FROM EmailAlias a JOIN FETCH a.domain d " +
           "WHERE a.user = :user AND a.isActive = true " +
           "ORDER BY a.createdAt DESC")
    List<EmailAlias> findByUserWithDomainFetch(@Param("user") User user);
    
    /**
     * 根据用户查找所有别名
     */
    List<EmailAlias> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 根据用户和活跃状态查找别名
     */
    List<EmailAlias> findByUserAndIsActiveOrderByCreatedAtDesc(User user, Boolean isActive);
    
    /**
     * 根据别名邮箱和域名查找
     */
    Optional<EmailAlias> findByAliasEmailAndDomain(String aliasEmail, EmailDomain domain);
    
    /**
     * 检查别名是否存在
     */
    boolean existsByAliasEmailAndDomain(String aliasEmail, EmailDomain domain);
    
    /**
     * 查找用户在指定域名下的别名
     */
    List<EmailAlias> findByUserAndDomainOrderByCreatedAtDesc(User user, EmailDomain domain);
    
    /**
     * 查找catch-all别名
     */
    List<EmailAlias> findByDomainAndIsCatchAllTrueAndIsActiveTrue(EmailDomain domain);
    
    /**
     * 查找用户的活跃别名数量
     */
    @Query("SELECT COUNT(a) FROM EmailAlias a WHERE a.user = :user AND a.isActive = true")
    long countActiveAliasesByUser(@Param("user") User user);
    
    /**
     * 查找完整邮箱地址
     */
    @Query("SELECT a FROM EmailAlias a JOIN FETCH a.domain d " +
           "WHERE CONCAT(a.aliasEmail, '@', d.domainName) = :fullEmail " +
           "AND a.isActive = true")
    Optional<EmailAlias> findByFullEmailAddress(@Param("fullEmail") String fullEmail);
    
    /**
     * 获取用户别名统计
     */
    @Query("SELECT d.domainName, COUNT(a.id) " +
           "FROM EmailAlias a JOIN a.domain d " +
           "WHERE a.user = :user AND a.isActive = true " +
           "GROUP BY d.domainName")
    List<Object[]> getUserAliasStatistics(@Param("user") User user);
    
    /**
     * 根据外部别名ID查找别名
     */
    Optional<EmailAlias> findByExternalAliasId(String externalAliasId);
    
    /**
     * 根据显示名称搜索别名（模糊匹配）
     */
    @Query("SELECT a FROM EmailAlias a WHERE a.user = :user " +
           "AND (LOWER(a.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.aliasEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND a.isActive = true")
    List<EmailAlias> searchByDisplayNameOrAlias(@Param("user") User user, @Param("keyword") String keyword);
}