package com.security.monitor.config;

import com.security.monitor.model.EmailAlias;
import com.security.monitor.service.HackerOneIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

/**
 * 邮箱别名实体监听器
 * 自动处理HackerOne格式别名的显示名称设置
 */
@Component
public class EmailAliasEntityListener {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailAliasEntityListener.class);
    
    @Autowired
    private HackerOneIntegrationService hackerOneService;
    
    /**
     * 别名保存后的处理
     */
    @PostPersist
    public void afterPersist(EmailAlias alias) {
        try {
            // 自动检测并设置HackerOne别名
            if (hackerOneService != null && hackerOneService.isHackerOneEmail(alias.getFullEmail())) {
                // 只有在显示名称为空时才自动设置
                if (alias.getDisplayName() == null || alias.getDisplayName().trim().isEmpty()) {
                    hackerOneService.autoSetHackerOneAlias(alias);
                }
            }
        } catch (Exception e) {
            logger.warn("自动设置HackerOne别名显示名称失败: {}", alias.getFullEmail(), e);
        }
    }
    
    /**
     * 别名更新后的处理
     */
    @PostUpdate
    public void afterUpdate(EmailAlias alias) {
        try {
            // 检查是否需要更新HackerOne显示名称
            if (hackerOneService != null && hackerOneService.isHackerOneEmail(alias.getFullEmail())) {
                String expectedDisplayName = hackerOneService.generateHackerOneDisplayName(alias.getFullEmail());
                
                // 如果当前显示名称不是期望的HackerOne格式，则更新
                if (expectedDisplayName != null && !expectedDisplayName.equals(alias.getDisplayName())) {
                    hackerOneService.autoSetHackerOneAlias(alias);
                }
            }
        } catch (Exception e) {
            logger.warn("更新HackerOne别名显示名称失败: {}", alias.getFullEmail(), e);
        }
    }
}