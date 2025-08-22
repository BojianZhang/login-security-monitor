package com.security.monitor.repository;

import com.security.monitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 查找活跃用户
     */
    List<User> findByIsActiveTrue();

    /**
     * 查找管理员用户
     */
    List<User> findByIsAdminTrue();

    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin >= :since ORDER BY u.lastLogin DESC")
    List<User> findRecentlyLoggedInUsers(@Param("since") LocalDateTime since);

    /**
     * 查找从未登录的用户
     */
    List<User> findByLastLoginIsNull();

    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
}