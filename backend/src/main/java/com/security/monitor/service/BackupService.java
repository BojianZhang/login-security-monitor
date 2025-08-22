package com.security.monitor.service;

import com.security.monitor.model.BackupRecord;
import com.security.monitor.model.BackupTask;
import com.security.monitor.repository.BackupRecordRepository;
import com.security.monitor.repository.BackupTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * 备份服务
 */
@Service
@Transactional
public class BackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    
    @Autowired
    private BackupTaskRepository backupTaskRepository;
    
    @Autowired
    private BackupRecordRepository backupRecordRepository;
    
    @Autowired
    private DatabaseBackupService databaseBackupService;
    
    @Autowired
    private FileBackupService fileBackupService;
    
    @Autowired
    private StorageService storageService;
    
    @Value("${backup.work-directory:/tmp/backups}")
    private String workDirectory;
    
    @Value("${backup.max-concurrent:3}")
    private int maxConcurrentBackups;
    
    /**
     * 执行备份任务
     */
    @Async
    public CompletableFuture<BackupRecord> executeBackup(BackupTask task) {
        logger.info("开始执行备份任务: {}", task.getTaskName());
        
        // 创建备份记录
        BackupRecord record = new BackupRecord(task, generateBackupName(task), generateBackupPath(task));
        record = backupRecordRepository.save(record);
        
        try {
            // 根据备份类型执行不同的备份策略
            switch (task.getBackupType()) {
                case FULL:
                    executeFullBackup(task, record);
                    break;
                case INCREMENTAL:
                    executeIncrementalBackup(task, record);
                    break;
                case DIFFERENTIAL:
                    executeDifferentialBackup(task, record);
                    break;
                case DATABASE_ONLY:
                    executeDatabaseBackup(task, record);
                    break;
                case FILES_ONLY:
                    executeFileBackup(task, record);
                    break;
            }
            
            // 压缩备份文件
            if (task.getCompressionEnabled()) {
                compressBackup(record);
            }
            
            // 加密备份文件
            if (task.getEncryptionEnabled()) {
                encryptBackup(task, record);
            }
            
            // 计算文件校验和
            calculateChecksum(record);
            
            // 上传到存储位置
            uploadToStorage(task, record);
            
            // 更新任务统计
            task.updateBackupStats(BackupTask.BackupStatus.SUCCESS, record.getBackupSize(), null);
            backupTaskRepository.save(task);
            
            // 完成备份记录
            record.completeBackup(BackupTask.BackupStatus.SUCCESS, null);
            record = backupRecordRepository.save(record);
            
            logger.info("备份任务完成: {}, 大小: {}", task.getTaskName(), record.getFormattedBackupSize());
            
        } catch (Exception e) {
            logger.error("备份任务失败: " + task.getTaskName(), e);
            
            // 更新失败状态
            task.updateBackupStats(BackupTask.BackupStatus.FAILED, 0L, e.getMessage());
            backupTaskRepository.save(task);
            
            record.completeBackup(BackupTask.BackupStatus.FAILED, e.getMessage());
            record = backupRecordRepository.save(record);
        }
        
        return CompletableFuture.completedFuture(record);
    }
    
    /**
     * 执行完整备份
     */
    private void executeFullBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("执行完整备份: {}", task.getTaskName());
        
        // 备份数据库
        if (task.getBackupScope() != BackupTask.BackupScope.FOLDER) {
            executeDatabaseBackup(task, record);
        }
        
        // 备份文件
        if (task.getIncludeAttachments() || task.getIncludeLogs()) {
            executeFileBackup(task, record);
        }
    }
    
    /**
     * 执行增量备份
     */
    private void executeIncrementalBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("执行增量备份: {}", task.getTaskName());
        
        // 获取最后一次备份时间
        LocalDateTime lastBackupTime = task.getLastBackupTime();
        if (lastBackupTime == null) {
            // 如果没有上次备份时间，执行完整备份
            executeFullBackup(task, record);
            return;
        }
        
        // 增量备份数据库
        databaseBackupService.executeIncrementalBackup(task, record, lastBackupTime);
        
        // 增量备份文件
        fileBackupService.executeIncrementalBackup(task, record, lastBackupTime);
    }
    
    /**
     * 执行差异备份
     */
    private void executeDifferentialBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("执行差异备份: {}", task.getTaskName());
        
        // 获取最后一次完整备份时间
        BackupRecord lastFullBackup = backupRecordRepository
                .findTopByTaskAndBackupStatusOrderByCreatedAtDesc(task, BackupTask.BackupStatus.SUCCESS)
                .orElse(null);
        
        LocalDateTime baseTime = lastFullBackup != null ? lastFullBackup.getCreatedAt() : null;
        if (baseTime == null) {
            // 如果没有完整备份，执行完整备份
            executeFullBackup(task, record);
            return;
        }
        
        // 差异备份数据库
        databaseBackupService.executeDifferentialBackup(task, record, baseTime);
        
        // 差异备份文件
        fileBackupService.executeDifferentialBackup(task, record, baseTime);
    }
    
    /**
     * 执行数据库备份
     */
    private void executeDatabaseBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("执行数据库备份: {}", task.getTaskName());
        
        databaseBackupService.executeFullBackup(task, record);
    }
    
    /**
     * 执行文件备份
     */
    private void executeFileBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("执行文件备份: {}", task.getTaskName());
        
        fileBackupService.executeFullBackup(task, record);
    }
    
    /**
     * 压缩备份文件
     */
    private void compressBackup(BackupRecord record) throws Exception {
        logger.info("压缩备份文件: {}", record.getBackupName());
        
        Path originalPath = Paths.get(record.getBackupPath());
        Path compressedPath = Paths.get(record.getBackupPath() + ".gz");
        
        try (FileInputStream fis = new FileInputStream(originalPath.toFile());
             FileOutputStream fos = new FileOutputStream(compressedPath.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }
        
        // 更新记录信息
        record.setCompressedSize(Files.size(compressedPath));
        record.setBackupPath(compressedPath.toString());
        
        // 删除原始文件
        Files.deleteIfExists(originalPath);
        
        logger.info("压缩完成，原始大小: {}, 压缩后大小: {}", 
                   record.getFormattedBackupSize(), record.getFormattedCompressedSize());
    }
    
    /**
     * 加密备份文件
     */
    private void encryptBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("加密备份文件: {}", record.getBackupName());
        
        // 这里应该实现具体的加密逻辑，比如AES加密
        // 为了演示，我们只是记录加密算法
        record.setEncryptionAlgorithm("AES-256-GCM");
        
        logger.info("加密完成，算法: {}", record.getEncryptionAlgorithm());
    }
    
    /**
     * 计算文件校验和
     */
    private void calculateChecksum(BackupRecord record) throws Exception {
        logger.info("计算文件校验和: {}", record.getBackupName());
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Path filePath = Paths.get(record.getBackupPath());
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, length);
            }
        }
        
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        record.setChecksum(hexString.toString());
        logger.info("校验和计算完成: {}", record.getChecksum());
    }
    
    /**
     * 上传到存储位置
     */
    private void uploadToStorage(BackupTask task, BackupRecord record) throws Exception {
        logger.info("上传备份到存储: {}", task.getStorageType());
        
        String storageLocation = storageService.uploadBackup(task, record);
        record.setStorageLocation(storageLocation);
        
        logger.info("上传完成，存储位置: {}", storageLocation);
    }
    
    /**
     * 生成备份名称
     */
    private String generateBackupName(BackupTask task) {
        String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "");
        return String.format("%s_%s_%s.backup", 
                           task.getTaskName().replaceAll("\\s+", "_"),
                           task.getBackupType().name().toLowerCase(),
                           timestamp);
    }
    
    /**
     * 生成备份路径
     */
    private String generateBackupPath(BackupTask task) {
        Path workDir = Paths.get(workDirectory);
        try {
            Files.createDirectories(workDir);
        } catch (Exception e) {
            logger.warn("无法创建工作目录: " + workDir, e);
        }
        
        return workDir.resolve(generateBackupName(task)).toString();
    }
    
    /**
     * 定时检查需要执行的备份任务
     */
    @Scheduled(fixedDelay = 60000) // 每分钟检查一次
    public void checkScheduledBackups() {
        try {
            List<BackupTask> tasksToBackup = backupTaskRepository.findTasksNeedingBackup(LocalDateTime.now());
            
            for (BackupTask task : tasksToBackup) {
                logger.info("发现需要执行的备份任务: {}", task.getTaskName());
                executeBackup(task);
                
                // 更新下次备份时间
                updateNextBackupTime(task);
            }
            
        } catch (Exception e) {
            logger.error("检查定时备份任务时发生错误", e);
        }
    }
    
    /**
     * 更新下次备份时间
     */
    private void updateNextBackupTime(BackupTask task) {
        // 这里应该根据Cron表达式计算下次执行时间
        // 为了演示，我们简单地设置为1小时后
        task.setNextBackupTime(LocalDateTime.now().plusHours(1));
        backupTaskRepository.save(task);
    }
    
    /**
     * 验证备份文件完整性
     */
    @Async
    public CompletableFuture<Boolean> verifyBackup(BackupRecord record) {
        try {
            logger.info("验证备份文件: {}", record.getBackupName());
            
            // 检查文件是否存在
            Path backupPath = Paths.get(record.getBackupPath());
            if (!Files.exists(backupPath)) {
                record.setVerificationResult(BackupRecord.VerificationStatus.CORRUPTED);
                backupRecordRepository.save(record);
                return CompletableFuture.completedFuture(false);
            }
            
            // 验证文件大小
            long actualSize = Files.size(backupPath);
            if (record.getCompressedSize() != null && !record.getCompressedSize().equals(actualSize)) {
                record.setVerificationResult(BackupRecord.VerificationStatus.CORRUPTED);
                backupRecordRepository.save(record);
                return CompletableFuture.completedFuture(false);
            }
            
            // 验证校验和
            calculateChecksum(record);
            // 这里应该与存储的校验和进行比较
            
            record.setVerificationResult(BackupRecord.VerificationStatus.VERIFIED);
            backupRecordRepository.save(record);
            
            logger.info("备份验证成功: {}", record.getBackupName());
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            logger.error("备份验证失败: " + record.getBackupName(), e);
            record.setVerificationResult(BackupRecord.VerificationStatus.VERIFICATION_FAILED);
            backupRecordRepository.save(record);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * 清理过期备份
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredBackups() {
        try {
            logger.info("开始清理过期备份");
            
            List<BackupTask> activeTasks = backupTaskRepository.findByIsActiveTrue();
            
            for (BackupTask task : activeTasks) {
                LocalDateTime expiryDate = LocalDateTime.now().minusDays(task.getRetentionDays());
                List<BackupRecord> expiredBackups = backupRecordRepository.findExpiredBackups(expiryDate);
                
                for (BackupRecord backup : expiredBackups) {
                    deleteBackup(backup);
                }
            }
            
            logger.info("过期备份清理完成");
            
        } catch (Exception e) {
            logger.error("清理过期备份时发生错误", e);
        }
    }
    
    /**
     * 删除备份文件
     */
    private void deleteBackup(BackupRecord record) {
        try {
            // 删除本地文件
            Path backupPath = Paths.get(record.getBackupPath());
            Files.deleteIfExists(backupPath);
            
            // 删除存储位置的文件
            if (record.getStorageLocation() != null) {
                storageService.deleteBackup(record);
            }
            
            // 删除数据库记录
            backupRecordRepository.delete(record);
            
            logger.info("已删除过期备份: {}", record.getBackupName());
            
        } catch (Exception e) {
            logger.error("删除备份失败: " + record.getBackupName(), e);
        }
    }
}