package com.security.monitor.repository;

import com.security.monitor.model.EmailSearchHistory;
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
 * 邮件搜索历史仓库
 */
@Repository
public interface EmailSearchHistoryRepository extends JpaRepository<EmailSearchHistory, Long> {
    
    /**
     * 查找用户的搜索历史
     */
    Page<EmailSearchHistory> findByUser(User user, Pageable pageable);
    
    /**
     * 查找用户的搜索历史（按时间倒序）
     */
    List<EmailSearchHistory> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 查找最近的搜索记录
     */
    Optional<EmailSearchHistory> findByUserAndQueryAndCreatedAtAfter(
        User user, String query, LocalDateTime after);
    
    /**
     * 查找用户的热门搜索词
     */
    @Query("SELECT h.query, COUNT(h) as searchCount " +
           "FROM EmailSearchHistory h " +
           "WHERE h.user = :user " +
           "AND h.createdAt >= :since " +
           "GROUP BY h.query " +
           "ORDER BY searchCount DESC")
    List<Object[]> findPopularQueries(@Param("user") User user, 
                                     @Param("since") LocalDateTime since,
                                     Pageable pageable);
    
    /**
     * 统计用户搜索次数
     */
    long countByUser(User user);
    
    /**
     * 统计用户在指定时间段的搜索次数
     */
    long countByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);
    
    /**
     * 删除用户的所有搜索历史
     */
    @Modifying
    @Query("DELETE FROM EmailSearchHistory h WHERE h.user = :user")
    void deleteByUser(@Param("user") User user);
    
    /**
     * 删除过期的搜索历史
     */
    @Modifying
    @Query("DELETE FROM EmailSearchHistory h WHERE h.createdAt < :expiredDate")
    void deleteExpiredHistory(@Param("expiredDate") LocalDateTime expiredDate);
    
    /**
     * 获取搜索统计信息
     */
    @Query("SELECT " +
           "COUNT(h) as totalSearches, " +
           "COUNT(DISTINCT h.query) as uniqueQueries, " +
           "AVG(h.executionTimeMs) as avgExecutionTime, " +
           "AVG(h.resultCount) as avgResultCount " +
           "FROM EmailSearchHistory h " +
           "WHERE h.user = :user " +
           "AND h.createdAt >= :since")
    Object[] getSearchStatistics(@Param("user") User user, @Param("since") LocalDateTime since);
}