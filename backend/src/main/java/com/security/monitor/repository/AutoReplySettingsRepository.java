package com.security.monitor.repository;

import com.security.monitor.model.AutoReplySettings;
import com.security.monitor.model.EmailAlias;
import com.security.monitor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 自动回复设置仓库接口
 */
@Repository
public interface AutoReplySettingsRepository extends JpaRepository<AutoReplySettings, Long> {
    
    /**
     * 根据别名查找自动回复设置
     */
    Optional<AutoReplySettings> findByAlias(EmailAlias alias);
    
    /**
     * 查找用户的所有自动回复设置
     */
    @Query("SELECT s FROM AutoReplySettings s WHERE s.alias.user = :user")
    List<AutoReplySettings> findByAliasUser(@Param("user") User user);
    
    /**
     * 查找启用的自动回复设置
     */
    List<AutoReplySettings> findByIsEnabledTrue();
    
    /**
     * 查找特定域名下的自动回复设置
     */
    @Query("SELECT s FROM AutoReplySettings s WHERE s.alias.domain.id = :domainId")
    List<AutoReplySettings> findByAliasDomainId(@Param("domainId") Long domainId);
    
    /**
     * 统计用户启用的自动回复数量
     */
    @Query("SELECT COUNT(s) FROM AutoReplySettings s WHERE s.alias.user = :user AND s.isEnabled = true")
    long countEnabledByUser(@Param("user") User user);
}