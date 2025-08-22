package com.security.monitor.repository;

import com.security.monitor.model.AutoReplyHistory;
import com.security.monitor.model.AutoReplySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动回复历史仓库接口
 */
@Repository
public interface AutoReplyHistoryRepository extends JpaRepository<AutoReplyHistory, Long> {
    
    /**
     * 根据设置查找回复历史
     */
    List<AutoReplyHistory> findBySettingsOrderByCreatedAtDesc(AutoReplySettings settings);
    
    /**
     * 统计指定设置和发件人的回复次数
     */
    long countBySettingsAndFromAddress(AutoReplySettings settings, String fromAddress);
    
    /**
     * 查找指定时间前的历史记录
     */
    List<AutoReplyHistory> findByCreatedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * 查找发送成功的回复历史
     */
    List<AutoReplyHistory> findByReplySentTrueOrderByCreatedAtDesc();
    
    /**
     * 查找发送失败的回复历史
     */
    List<AutoReplyHistory> findByReplySentFalseOrderByCreatedAtDesc();
    
    /**
     * 统计指定时间段内的回复数量
     */
    @Query("SELECT COUNT(h) FROM AutoReplyHistory h WHERE h.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * 统计指定设置的总回复数
     */
    long countBySettings(AutoReplySettings settings);
    
    /**
     * 统计指定设置的成功回复数
     */
    long countBySettingsAndReplySentTrue(AutoReplySettings settings);
}