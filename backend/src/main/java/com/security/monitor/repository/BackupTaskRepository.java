package com.security.monitor.repository;

import com.security.monitor.model.BackupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 备份任务仓库接口
 */
@Repository
public interface BackupTaskRepository extends JpaRepository<BackupTask, Long> {
    
    /**
     * 查找所有活跃的备份任务
     */
    List<BackupTask> findByIsActiveTrue();
    
    /**
     * 查找需要执行备份的任务
     */
    @Query("SELECT t FROM BackupTask t WHERE t.isActive = true AND t.nextBackupTime <= :currentTime")
    List<BackupTask> findTasksNeedingBackup(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 根据备份类型查找任务
     */
    List<BackupTask> findByBackupTypeAndIsActiveTrue(BackupTask.BackupType backupType);
    
    /**
     * 根据存储类型查找任务
     */
    List<BackupTask> findByStorageTypeAndIsActiveTrue(BackupTask.StorageType storageType);
    
    /**
     * 根据创建者查找任务
     */
    List<BackupTask> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    /**
     * 查找最近失败的任务
     */
    @Query("SELECT t FROM BackupTask t WHERE t.lastBackupStatus = 'FAILED' AND t.isActive = true ORDER BY t.lastBackupTime DESC")
    List<BackupTask> findRecentFailedTasks();
    
    /**
     * 根据任务名称查找
     */
    Optional<BackupTask> findByTaskNameIgnoreCase(String taskName);
    
    /**
     * 统计不同状态的任务数量
     */
    @Query("SELECT t.lastBackupStatus, COUNT(t) FROM BackupTask t WHERE t.isActive = true GROUP BY t.lastBackupStatus")
    List<Object[]> countTasksByStatus();
    
    /**
     * 查找存储使用量最大的任务
     */
    @Query("SELECT t FROM BackupTask t WHERE t.isActive = true ORDER BY t.lastBackupSize DESC")
    List<BackupTask> findTasksByStorageUsage();
    
    /**
     * 查找成功率较低的任务
     */
    @Query("SELECT t FROM BackupTask t WHERE t.isActive = true AND (CAST(t.successfulCount AS DOUBLE) / t.backupCount) < :threshold ORDER BY (CAST(t.successfulCount AS DOUBLE) / t.backupCount) ASC")
    List<BackupTask> findTasksWithLowSuccessRate(@Param("threshold") double threshold);
}