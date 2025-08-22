package com.security.monitor.repository;

import com.security.monitor.model.SieveFilter;
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

/**
 * Sieve过滤器仓库接口
 */
@Repository
public interface SieveFilterRepository extends JpaRepository<SieveFilter, Long> {
    
    /**
     * 根据用户和状态查找过滤器（按优先级排序）
     */
    List<SieveFilter> findByUserAndIsActiveOrderByPriority(User user, boolean isActive);
    
    /**
     * 根据用户查找过滤器
     */
    Page<SieveFilter> findByUser(User user, Pageable pageable);
    
    /**
     * 根据用户和状态查找过滤器
     */
    Page<SieveFilter> findByUserAndIsActive(User user, boolean isActive, Pageable pageable);
    
    /**
     * 根据过滤器类型查找
     */
    List<SieveFilter> findByFilterTypeAndIsActive(SieveFilter.FilterType filterType, boolean isActive);
    
    /**
     * 根据过滤器名称查找（用户范围内）
     */
    SieveFilter findByUserAndFilterName(User user, String filterName);
    
    /**
     * 统计用户的过滤器数量
     */
    long countByUser(User user);
    
    /**
     * 统计用户的活跃过滤器数量
     */
    long countByUserAndIsActive(User user, boolean isActive);
    
    /**
     * 查找最常使用的过滤器
     */
    @Query("SELECT f FROM SieveFilter f WHERE f.user = :user AND f.isActive = true ORDER BY f.hitCount DESC")
    List<SieveFilter> findMostUsedFilters(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找最近更新的过滤器
     */
    List<SieveFilter> findByUserAndUpdatedAtAfterOrderByUpdatedAtDesc(User user, LocalDateTime after);
    
    /**
     * 查找有错误的过滤器
     */
    @Query("SELECT f FROM SieveFilter f WHERE f.user = :user AND f.errorCount > 0 ORDER BY f.errorCount DESC")
    List<SieveFilter> findFiltersWithErrors(@Param("user") User user);
    
    /**
     * 更新过滤器命中统计
     */
    @Modifying
    @Query("UPDATE SieveFilter f SET f.hitCount = f.hitCount + 1, f.lastHitAt = :hitTime WHERE f.id = :filterId")
    void updateHitStatistics(@Param("filterId") Long filterId, @Param("hitTime") LocalDateTime hitTime);
    
    /**
     * 更新过滤器错误统计
     */
    @Modifying
    @Query("UPDATE SieveFilter f SET f.errorCount = f.errorCount + 1, f.lastError = :errorMessage WHERE f.id = :filterId")
    void updateErrorStatistics(@Param("filterId") Long filterId, @Param("errorMessage") String errorMessage);
    
    /**
     * 重置过滤器错误统计
     */
    @Modifying
    @Query("UPDATE SieveFilter f SET f.errorCount = 0, f.lastError = NULL WHERE f.id = :filterId")
    void resetErrorStatistics(@Param("filterId") Long filterId);
    
    /**
     * 获取用户过滤器统计信息
     */
    @Query("SELECT " +
           "COUNT(f) as totalFilters, " +
           "SUM(CASE WHEN f.isActive = true THEN 1 ELSE 0 END) as activeFilters, " +
           "SUM(f.hitCount) as totalHits, " +
           "SUM(CASE WHEN f.errorCount > 0 THEN 1 ELSE 0 END) as filtersWithErrors " +
           "FROM SieveFilter f WHERE f.user = :user")
    Object[] getUserFilterStatistics(@Param("user") User user);
    
    /**
     * 按过滤器类型统计
     */
    @Query("SELECT f.filterType, COUNT(f), SUM(f.hitCount) FROM SieveFilter f WHERE f.user = :user GROUP BY f.filterType")
    List<Object[]> getFilterTypeStatistics(@Param("user") User user);
    
    /**
     * 查找指定优先级范围的过滤器
     */
    @Query("SELECT f FROM SieveFilter f WHERE f.user = :user AND f.priority BETWEEN :minPriority AND :maxPriority ORDER BY f.priority")
    List<SieveFilter> findByUserAndPriorityRange(@Param("user") User user, 
                                                  @Param("minPriority") int minPriority, 
                                                  @Param("maxPriority") int maxPriority);
    
    /**
     * 查找重复优先级的过滤器
     */
    @Query("SELECT f.priority, COUNT(f) FROM SieveFilter f WHERE f.user = :user AND f.isActive = true GROUP BY f.priority HAVING COUNT(f) > 1")
    List<Object[]> findDuplicatePriorities(@Param("user") User user);
    
    /**
     * 获取下一个可用的优先级
     */
    @Query("SELECT COALESCE(MAX(f.priority), 0) + 1 FROM SieveFilter f WHERE f.user = :user")
    Integer getNextAvailablePriority(@Param("user") User user);
    
    /**
     * 查找包含特定脚本内容的过滤器
     */
    @Query("SELECT f FROM SieveFilter f WHERE f.user = :user AND f.filterScript LIKE %:scriptPattern%")
    List<SieveFilter> findByUserAndFilterScriptContaining(@Param("user") User user, @Param("scriptPattern") String scriptPattern);
    
    /**
     * 批量更新过滤器状态
     */
    @Modifying
    @Query("UPDATE SieveFilter f SET f.isActive = :isActive WHERE f.user = :user AND f.id IN :filterIds")
    void batchUpdateFilterStatus(@Param("user") User user, @Param("filterIds") List<Long> filterIds, @Param("isActive") boolean isActive);
    
    /**
     * 批量更新过滤器优先级
     */
    @Modifying
    @Query("UPDATE SieveFilter f SET f.priority = :priority WHERE f.id = :filterId")
    void updateFilterPriority(@Param("filterId") Long filterId, @Param("priority") Integer priority);
    
    /**
     * 查找长时间未使用的过滤器
     */
    @Query("SELECT f FROM SieveFilter f WHERE f.user = :user AND (f.lastHitAt IS NULL OR f.lastHitAt < :threshold) AND f.isActive = true")
    List<SieveFilter> findUnusedFilters(@Param("user") User user, @Param("threshold") LocalDateTime threshold);
    
    /**
     * 删除用户的所有过滤器
     */
    void deleteByUser(User user);
}