package com.security.monitor.repository;

import com.security.monitor.model.DnsBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DNS黑名单仓库接口
 */
@Repository
public interface DnsBlacklistRepository extends JpaRepository<DnsBlacklist, Long> {
    
    /**
     * 查找活跃的DNS黑名单
     */
    List<DnsBlacklist> findByIsActiveOrderByWeightDesc(boolean isActive);
    
    /**
     * 根据主机名查找
     */
    DnsBlacklist findByHostname(String hostname);
    
    /**
     * 分页查询黑名单
     */
    Page<DnsBlacklist> findByIsActive(boolean isActive, Pageable pageable);
    
    /**
     * 查找最常用的黑名单
     */
    @Query("SELECT d FROM DnsBlacklist d WHERE d.isActive = true ORDER BY d.positiveHits DESC")
    List<DnsBlacklist> findMostUsedBlacklists(Pageable pageable);
    
    /**
     * 统计活跃黑名单数量
     */
    long countByIsActive(boolean isActive);
    
    /**
     * 更新查询统计
     */
    @Modifying
    @Query("UPDATE DnsBlacklist d SET d.totalQueries = d.totalQueries + 1, d.lastQueryAt = :queryTime WHERE d.id = :blacklistId")
    void updateQueryStatistics(@Param("blacklistId") Long blacklistId, @Param("queryTime") LocalDateTime queryTime);
    
    /**
     * 更新命中统计
     */
    @Modifying
    @Query("UPDATE DnsBlacklist d SET d.positiveHits = d.positiveHits + 1 WHERE d.id = :blacklistId")
    void updateHitStatistics(@Param("blacklistId") Long blacklistId);
    
    /**
     * 获取黑名单统计信息
     */
    @Query("SELECT " +
           "COUNT(d) as totalBlacklists, " +
           "SUM(CASE WHEN d.isActive = true THEN 1 ELSE 0 END) as activeBlacklists, " +
           "SUM(d.totalQueries) as totalQueries, " +
           "SUM(d.positiveHits) as totalHits " +
           "FROM DnsBlacklist d")
    Object[] getBlacklistStatistics();
    
    /**
     * 查找最近查询的黑名单
     */
    List<DnsBlacklist> findByLastQueryAtAfterOrderByLastQueryAtDesc(LocalDateTime after);
}