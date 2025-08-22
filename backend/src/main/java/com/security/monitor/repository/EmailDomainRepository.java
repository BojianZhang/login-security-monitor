package com.security.monitor.repository;

import com.security.monitor.model.EmailDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 邮件域名仓库接口
 */
@Repository
public interface EmailDomainRepository extends JpaRepository<EmailDomain, Long> {
    
    /**
     * 根据域名查找
     */
    Optional<EmailDomain> findByDomainName(String domainName);
    
    /**
     * 查找活跃的域名
     */
    List<EmailDomain> findByIsActiveTrue();
    
    /**
     * 查找主域名
     */
    Optional<EmailDomain> findByIsPrimaryTrue();
    
    /**
     * 检查域名是否存在且活跃
     */
    boolean existsByDomainNameAndIsActiveTrue(String domainName);
    
    /**
     * 查找活跃域名及其别名数量
     */
    @Query("SELECT d FROM EmailDomain d LEFT JOIN FETCH d.aliases a WHERE d.isActive = true")
    List<EmailDomain> findActiveDomainsWithAliases();
    
    /**
     * 获取域名统计信息
     */
    @Query("SELECT d.domainName, COUNT(a.id) as aliasCount " +
           "FROM EmailDomain d LEFT JOIN d.aliases a " +
           "WHERE d.isActive = true " +
           "GROUP BY d.id, d.domainName")
    List<Object[]> getDomainStatistics();
}