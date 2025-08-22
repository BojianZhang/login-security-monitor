package com.security.monitor.scheduler;

import com.security.monitor.model.User;
import com.security.monitor.repository.UserRepository;
import com.security.monitor.service.HackerOneIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HackerOne 集成定时任务
 * 定期同步和维护HackerOne别名的显示名称
 */
@Component
@ConditionalOnProperty(
    value = "app.integrations.hackerone.auto-sync.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class HackerOneSyncScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(HackerOneSyncScheduler.class);
    
    @Autowired
    private HackerOneIntegrationService hackerOneService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 每小时同步一次HackerOne别名
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void syncHackerOneAliasesHourly() {
        logger.info("开始定时同步 HackerOne 别名...");
        
        try {
            List<User> allUsers = userRepository.findAll();
            int totalSyncedCount = 0;
            int processedUsers = 0;
            
            for (User user : allUsers) {
                try {
                    int userSyncedCount = hackerOneService.syncUserHackerOneAliases(user);
                    totalSyncedCount += userSyncedCount;
                    processedUsers++;
                    
                    if (userSyncedCount > 0) {
                        logger.debug("用户 {} 同步了 {} 个 HackerOne 别名", user.getUsername(), userSyncedCount);
                    }
                    
                } catch (Exception e) {
                    logger.error("同步用户 {} 的 HackerOne 别名失败", user.getUsername(), e);
                }
            }
            
            logger.info("HackerOne 别名定时同步完成: 处理用户 {}, 同步别名 {}", processedUsers, totalSyncedCount);
            
        } catch (Exception e) {
            logger.error("HackerOne 别名定时同步任务异常", e);
        }
    }
    
    /**
     * 每天生成HackerOne别名统计报告
     */
    @Scheduled(cron = "0 0 8 * * ?") // 每天早上8点
    public void generateHackerOneReport() {
        logger.info("开始生成 HackerOne 别名统计报告...");
        
        try {
            List<User> allUsers = userRepository.findAll();
            
            long totalUsers = allUsers.size();
            long usersWithHackerOne = 0;
            long totalHackerOneAliases = 0;
            long activatedHackerOneAliases = 0;
            long syncedHackerOneAliases = 0;
            
            for (User user : allUsers) {
                try {
                    HackerOneIntegrationService.HackerOneAliasStats stats = hackerOneService.getHackerOneAliasStats(user);
                    
                    if (stats.getTotalCount() > 0) {
                        usersWithHackerOne++;
                        totalHackerOneAliases += stats.getTotalCount();
                        activatedHackerOneAliases += stats.getActiveCount();
                        syncedHackerOneAliases += stats.getSyncedCount();
                    }
                    
                } catch (Exception e) {
                    logger.error("获取用户 {} 的 HackerOne 统计失败", user.getUsername(), e);
                }
            }
            
            // 生成报告
            StringBuilder report = new StringBuilder();
            report.append("HackerOne 别名统计报告\n");
            report.append("=".repeat(50)).append("\n");
            report.append(String.format("总用户数: %d\n", totalUsers));
            report.append(String.format("拥有HackerOne别名的用户: %d (%.1f%%)\n", 
                usersWithHackerOne, usersWithHackerOne * 100.0 / totalUsers));
            report.append(String.format("HackerOne别名总数: %d\n", totalHackerOneAliases));
            report.append(String.format("活跃的HackerOne别名: %d (%.1f%%)\n", 
                activatedHackerOneAliases, totalHackerOneAliases > 0 ? activatedHackerOneAliases * 100.0 / totalHackerOneAliases : 0));
            report.append(String.format("已同步的HackerOne别名: %d (%.1f%%)\n", 
                syncedHackerOneAliases, totalHackerOneAliases > 0 ? syncedHackerOneAliases * 100.0 / totalHackerOneAliases : 0));
            report.append("=".repeat(50));
            
            logger.info("HackerOne 别名统计报告:\n{}", report);
            
        } catch (Exception e) {
            logger.error("生成 HackerOne 别名统计报告失败", e);
        }
    }
    
    /**
     * 手动触发全量同步（可通过管理接口调用）
     */
    public void forceFullSync() {
        logger.info("开始强制全量同步 HackerOne 别名...");
        syncHackerOneAliasesHourly(); // 复用定时同步逻辑
    }
}