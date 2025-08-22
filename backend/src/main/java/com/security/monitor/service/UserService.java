package com.security.monitor.service;

import com.security.monitor.model.User;
import com.security.monitor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Spring Security UserDetailsService接口实现
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    /**
     * 创建用户
     */
    public User createUser(String username, String email, String password, String fullName) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }

        // 创建新用户
        User user = new User(username, email, passwordEncoder.encode(password), fullName);
        user = userRepository.save(user);

        logger.info("创建新用户: {}", username);
        return user;
    }

    /**
     * 根据ID查找用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 获取所有活跃用户
     */
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    /**
     * 获取所有管理员用户
     */
    public List<User> findAdminUsers() {
        return userRepository.findByIsAdminTrue();
    }

    /**
     * 更新用户信息
     */
    public User updateUser(Long userId, String email, String fullName, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 检查邮箱是否被其他用户使用
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被其他用户使用");
        }

        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);

        return userRepository.save(user);
    }

    /**
     * 更改密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("用户 {} 更改了密码", user.getUsername());
    }

    /**
     * 重置密码（管理员功能）
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("管理员重置了用户 {} 的密码", user.getUsername());
    }

    /**
     * 激活/停用用户
     */
    public void toggleUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        user.setIsActive(isActive);
        userRepository.save(user);

        logger.info("用户 {} 状态已{}为: {}", user.getUsername(), 
                isActive ? "激活" : "停用", isActive);
    }

    /**
     * 设置管理员权限
     */
    public void setAdminRole(Long userId, boolean isAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        user.setIsAdmin(isAdmin);
        userRepository.save(user);

        logger.info("用户 {} 管理员权限已设置为: {}", user.getUsername(), isAdmin);
    }

    /**
     * 删除用户
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        userRepository.delete(user);
        logger.info("删除用户: {}", user.getUsername());
    }

    /**
     * 获取用户统计信息
     */
    public UserStats getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long adminUsers = userRepository.findByIsAdminTrue().size();

        return new UserStats(totalUsers, activeUsers, adminUsers);
    }

    /**
     * 用户统计信息类
     */
    public static class UserStats {
        private final long totalUsers;
        private final long activeUsers;
        private final long adminUsers;

        public UserStats(long totalUsers, long activeUsers, long adminUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.adminUsers = adminUsers;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getActiveUsers() {
            return activeUsers;
        }

        public long getAdminUsers() {
            return adminUsers;
        }
    }
}