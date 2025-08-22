package com.security.monitor.repository;

import com.security.monitor.model.BackupRecord;
import com.security.monitor.model.BackupTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 备份记录仓库接口
 */
@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
    
    /**
     * 根据任务查找备份记录
     */
    Page<BackupRecord> findByTaskOrderByCreatedAtDesc(BackupTask task, Pageable pageable);
    
    /**
     * 根据任务和状态查找记录
     */
    List<BackupRecord> findByTaskAndBackupStatusOrderByCreatedAtDesc(BackupTask task, BackupTask.BackupStatus status);
    
    /**
     * 查找成功的备份记录
     */
    List<BackupRecord> findByBackupStatusOrderByCreatedAtDesc(BackupTask.BackupStatus status);
    
    /**
     * 查找指定时间范围内的备份记录
     */
    @Query("SELECT r FROM BackupRecord r WHERE r.startTime >= :startTime AND r.startTime <= :endTime ORDER BY r.startTime DESC")
    List<BackupRecord> findByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找任务的最新备份记录
     */
    Optional<BackupRecord> findTopByTaskOrderByCreatedAtDesc(BackupTask task);
    
    /**
     * 查找任务的最新成功备份记录
     */
    Optional<BackupRecord> findTopByTaskAndBackupStatusOrderByCreatedAtDesc(BackupTask task, BackupTask.BackupStatus status);
    
    /**
     * 统计备份记录总数
     */
    @Query("SELECT r.backupStatus, COUNT(r) FROM BackupRecord r GROUP BY r.backupStatus")
    List<Object[]> countRecordsByStatus();
    
    /**
     * 计算总备份大小
     */
    @Query("SELECT SUM(r.backupSize) FROM BackupRecord r WHERE r.backupStatus = 'SUCCESS'")
    Long getTotalBackupSize();
    
    /**
     * 计算压缩后总大小
     */
    @Query("SELECT SUM(r.compressedSize) FROM BackupRecord r WHERE r.backupStatus = 'SUCCESS' AND r.compressedSize IS NOT NULL")
    Long getTotalCompressedSize();
    
    /**
     * 查找可恢复的备份记录
     */
    List<BackupRecord> findByIsRestorableTrueAndBackupStatusOrderByCreatedAtDesc(BackupTask.BackupStatus status);
    
    /**
     * 查找需要验证的备份记录
     */
    @Query("SELECT r FROM BackupRecord r WHERE r.verificationStatus = 'NOT_VERIFIED' OR r.lastVerifiedAt < :threshold ORDER BY r.createdAt ASC")
    List<BackupRecord> findRecordsNeedingVerification(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 查找过期的备份记录（根据保留策略）
     */
    @Query("SELECT r FROM BackupRecord r WHERE r.createdAt < :expiryDate AND r.backupStatus = 'SUCCESS'")
    List<BackupRecord> findExpiredBackups(@Param("expiryDate") LocalDateTime expiryDate);
    
    /**
     * 根据校验和查找备份记录
     */
    Optional<BackupRecord> findByChecksum(String checksum);
    
    /**
     * 查找大小超过限制的备份记录
     */
    @Query("SELECT r FROM BackupRecord r WHERE r.backupSize > :sizeLimit ORDER BY r.backupSize DESC")
    List<BackupRecord> findOversizedBackups(@Param("sizeLimit") Long sizeLimit);
    
    /**
     * 统计任务的备份历史
     */
    @Query("SELECT DATE(r.createdAt) as date, COUNT(r) as count, SUM(r.backupSize) as totalSize " +
           "FROM BackupRecord r WHERE r.task = :task AND r.createdAt >= :startDate " +
           "GROUP BY DATE(r.createdAt) ORDER BY date DESC")
    List<Object[]> getBackupHistoryByTask(@Param("task") BackupTask task, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 查找恢复次数最多的备份记录
     */
    @Query("SELECT r FROM BackupRecord r WHERE r.restoredCount > 0 ORDER BY r.restoredCount DESC")
    List<BackupRecord> findMostRestoredBackups();
}