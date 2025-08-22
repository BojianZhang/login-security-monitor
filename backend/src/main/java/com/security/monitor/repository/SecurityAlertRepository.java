package com.security.monitor.repository;

import com.security.monitor.model.SecurityAlert;
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
 * 安全警报Repository
 */
@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    /**
     * 分页查询所有警报
     */
    Page<SecurityAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据用户查询警报
     */
    Page<SecurityAlert> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 查询未处理的警报
     */
    List<SecurityAlert> findByStatusOrderByCreatedAtDesc(SecurityAlert.Status status);

    /**
     * 查询指定严重程度的警报
     */
    List<SecurityAlert> findBySeverityOrderByCreatedAtDesc(SecurityAlert.Severity severity);

    /**
     * 查询高风险警报
     */
    @Query("SELECT sa FROM SecurityAlert sa WHERE sa.riskScore >= :threshold ORDER BY sa.createdAt DESC")
    List<SecurityAlert> findHighRiskAlerts(@Param("threshold") int threshold);

    /**
     * 查询指定时间范围内的警报
     */
    @Query("SELECT sa FROM SecurityAlert sa WHERE sa.createdAt BETWEEN :start AND :end ORDER BY sa.createdAt DESC")
    List<SecurityAlert> findAlertsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查询指定类型的警报
     */
    List<SecurityAlert> findByAlertTypeOrderByCreatedAtDesc(SecurityAlert.AlertType alertType);

    /**
     * 查询用户的未处理警报
     */
    @Query("SELECT sa FROM SecurityAlert sa WHERE sa.user = :user AND sa.status = 'OPEN' ORDER BY sa.createdAt DESC")
    List<SecurityAlert> findOpenAlertsByUser(@Param("user") User user);

    /**
     * 查询需要立即关注的关键警报
     */
    @Query("SELECT sa FROM SecurityAlert sa WHERE sa.severity = 'CRITICAL' AND sa.status IN ('OPEN', 'INVESTIGATING') ORDER BY sa.createdAt DESC")
    List<SecurityAlert> findCriticalOpenAlerts();

    /**
     * 统计指定状态的警报数量
     */
    long countByStatus(SecurityAlert.Status status);

    /**
     * 统计指定严重程度的警报数量
     */
    long countBySeverity(SecurityAlert.Severity severity);

    /**
     * 统计指定时间内创建的警报数量
     */
    @Query("SELECT COUNT(sa) FROM SecurityAlert sa WHERE sa.createdAt >= :since")
    long countAlertsSince(@Param("since") LocalDateTime since);

    /**
     * 查询用户在指定时间内的警报数量
     */
    @Query("SELECT COUNT(sa) FROM SecurityAlert sa WHERE sa.user = :user AND sa.createdAt >= :since")
    long countAlertsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * 查询处理时间最长的警报
     */
    @Query("SELECT sa FROM SecurityAlert sa WHERE sa.handledAt IS NOT NULL " +
           "ORDER BY (sa.handledAt - sa.createdAt) DESC")
    List<SecurityAlert> findLongestHandledAlerts(Pageable pageable);

    /**
     * 查询指定用户处理的警报
     */
    List<SecurityAlert> findByHandledByOrderByHandledAtDesc(User handledBy);

    /**
     * 查询最近24小时内的警报统计
     */
    @Query("SELECT sa.alertType, COUNT(sa) FROM SecurityAlert sa " +
           "WHERE sa.createdAt >= :since GROUP BY sa.alertType")
    List<Object[]> getAlertStatisticsSince(@Param("since") LocalDateTime since);
}