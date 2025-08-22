package com.security.monitor.repository;

import com.security.monitor.model.LoginRecord;
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
 * 登录记录Repository
 */
@Repository
public interface LoginRecordRepository extends JpaRepository<LoginRecord, Long> {

    /**
     * 根据用户ID分页查询登录记录
     */
    Page<LoginRecord> findByUserIdOrderByLoginTimeDesc(Long userId, Pageable pageable);

    /**
     * 查询用户的登录记录
     */
    List<LoginRecord> findByUserOrderByLoginTimeDesc(User user);

    /**
     * 查询指定时间范围内的登录记录
     */
    List<LoginRecord> findByUserAndLoginTimeBetween(User user, LocalDateTime start, LocalDateTime end);

    /**
     * 查询可疑登录记录
     */
    List<LoginRecord> findByIsSuspiciousTrue();

    /**
     * 查询指定用户的可疑登录记录
     */
    List<LoginRecord> findByUserAndIsSuspiciousTrue(User user);

    /**
     * 查询高风险登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.riskScore >= :threshold ORDER BY lr.loginTime DESC")
    List<LoginRecord> findHighRiskLogins(@Param("threshold") int threshold);

    /**
     * 查询用户在指定时间窗口内的登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.user = :user AND lr.loginTime >= :since ORDER BY lr.loginTime DESC")
    List<LoginRecord> findRecentLoginsByUser(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * 查询指定IP地址的登录记录
     */
    List<LoginRecord> findByIpAddressOrderByLoginTimeDesc(String ipAddress);

    /**
     * 查询用户最近的成功登录记录
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.user = :user AND lr.loginStatus = 'SUCCESS' ORDER BY lr.loginTime DESC")
    List<LoginRecord> findSuccessfulLoginsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 统计用户在指定时间内的登录次数
     */
    @Query("SELECT COUNT(lr) FROM LoginRecord lr WHERE lr.user = :user AND lr.loginTime >= :since")
    long countLoginsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * 统计指定IP在指定时间内的登录次数
     */
    @Query("SELECT COUNT(lr) FROM LoginRecord lr WHERE lr.ipAddress = :ip AND lr.loginTime >= :since")
    long countLoginsByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);

    /**
     * 查询用户在指定时间内从多个不同地理位置的登录
     */
    @Query("SELECT DISTINCT lr.country, lr.region, lr.city FROM LoginRecord lr " +
           "WHERE lr.user = :user AND lr.loginTime >= :since AND lr.country IS NOT NULL")
    List<Object[]> findDistinctLocationsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * 查询用户最近的登录记录（用于异地检测）
     */
    @Query("SELECT lr FROM LoginRecord lr WHERE lr.user = :user AND lr.latitude IS NOT NULL AND lr.longitude IS NOT NULL " +
           "ORDER BY lr.loginTime DESC")
    List<LoginRecord> findRecentLoginsWithLocation(@Param("user") User user, Pageable pageable);
}