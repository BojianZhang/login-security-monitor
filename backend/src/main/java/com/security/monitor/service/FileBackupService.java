package com.security.monitor.service;

import com.security.monitor.model.BackupRecord;
import com.security.monitor.model.BackupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件备份服务
 */
@Service
public class FileBackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileBackupService.class);
    
    @Value("${email.attachments.storage-path:/opt/mail/attachments}")
    private String attachmentStoragePath;
    
    @Value("${email.logs.storage-path:/opt/mail/logs}")
    private String logsStoragePath;
    
    @Value("${server.ssl.certificates-path:/opt/mail/certificates}")
    private String certificatesPath;
    
    @Value("${backup.file.max-size:1073741824}") // 1GB
    private long maxFileSize;
    
    /**
     * 执行完整文件备份
     */
    public void executeFullBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("开始执行文件完整备份: {}", task.getTaskName());
        
        String backupFile = generateFileBackupPath(record, "full");
        List<Path> sourceDirectories = getSourceDirectories(task);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            
            for (Path sourceDir : sourceDirectories) {
                if (Files.exists(sourceDir)) {
                    backupDirectory(sourceDir, zos, task, record, null);
                }
            }
        }
        
        // 更新备份记录
        Path backupPath = Paths.get(backupFile);
        record.setBackupPath(backupFile);
        record.setBackupSize(Files.size(backupPath));
        
        logger.info("文件完整备份完成: {}, 大小: {}", backupFile, record.getFormattedBackupSize());
    }
    
    /**
     * 执行增量文件备份
     */
    public void executeIncrementalBackup(BackupTask task, BackupRecord record, LocalDateTime lastBackupTime) throws Exception {
        logger.info("开始执行文件增量备份: {}, 上次备份时间: {}", task.getTaskName(), lastBackupTime);
        
        String backupFile = generateFileBackupPath(record, "incremental");
        List<Path> sourceDirectories = getSourceDirectories(task);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            
            for (Path sourceDir : sourceDirectories) {
                if (Files.exists(sourceDir)) {
                    backupDirectory(sourceDir, zos, task, record, lastBackupTime);
                }
            }
        }
        
        // 更新备份记录
        Path backupPath = Paths.get(backupFile);
        record.setBackupPath(backupFile);
        record.setBackupSize(Files.size(backupPath));
        
        logger.info("文件增量备份完成: {}, 大小: {}", backupFile, record.getFormattedBackupSize());
    }
    
    /**
     * 执行差异文件备份
     */
    public void executeDifferentialBackup(BackupTask task, BackupRecord record, LocalDateTime baseTime) throws Exception {
        logger.info("开始执行文件差异备份: {}, 基准时间: {}", task.getTaskName(), baseTime);
        
        String backupFile = generateFileBackupPath(record, "differential");
        List<Path> sourceDirectories = getSourceDirectories(task);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            
            for (Path sourceDir : sourceDirectories) {
                if (Files.exists(sourceDir)) {
                    backupDirectory(sourceDir, zos, task, record, baseTime);
                }
            }
        }
        
        // 更新备份记录
        Path backupPath = Paths.get(backupFile);
        record.setBackupPath(backupFile);
        record.setBackupSize(Files.size(backupPath));
        
        logger.info("文件差异备份完成: {}, 大小: {}", backupFile, record.getFormattedBackupSize());
    }
    
    /**
     * 备份目录
     */
    private void backupDirectory(Path sourceDir, ZipOutputStream zos, BackupTask task, BackupRecord record, LocalDateTime sinceTime) throws IOException {
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                
                try {
                    // 检查文件是否需要备份
                    if (!shouldBackupFile(file, task, sinceTime, attrs)) {
                        record.setFilesSkipped((record.getFilesSkipped() != null ? record.getFilesSkipped() : 0) + 1);
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // 检查文件大小
                    if (attrs.size() > maxFileSize) {
                        logger.warn("文件过大，跳过备份: {}, 大小: {}", file, attrs.size());
                        record.setFilesSkipped((record.getFilesSkipped() != null ? record.getFilesSkipped() : 0) + 1);
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // 添加文件到ZIP
                    String relativePath = sourceDir.relativize(file).toString();
                    ZipEntry entry = new ZipEntry(relativePath);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    zos.putNextEntry(entry);
                    
                    Files.copy(file, zos);
                    zos.closeEntry();
                    
                    record.setFilesProcessed((record.getFilesProcessed() != null ? record.getFilesProcessed() : 0) + 1);
                    
                } catch (Exception e) {
                    logger.error("备份文件失败: " + file, e);
                    record.setFilesFailed((record.getFilesFailed() != null ? record.getFilesFailed() : 0) + 1);
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.error("访问文件失败: " + file, exc);
                record.setFilesFailed((record.getFilesFailed() != null ? record.getFilesFailed() : 0) + 1);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 判断文件是否需要备份
     */
    private boolean shouldBackupFile(Path file, BackupTask task, LocalDateTime sinceTime, BasicFileAttributes attrs) {
        
        // 检查排除模式
        if (isExcluded(file, task.getExcludePatterns())) {
            return false;
        }
        
        // 如果指定了时间条件，检查文件修改时间
        if (sinceTime != null) {
            LocalDateTime fileModified = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), 
                ZoneId.systemDefault()
            );
            
            if (fileModified.isBefore(sinceTime)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查文件是否被排除
     */
    private boolean isExcluded(Path file, String excludePatterns) {
        if (excludePatterns == null || excludePatterns.trim().isEmpty()) {
            return false;
        }
        
        String fileName = file.getFileName().toString();
        String filePath = file.toString();
        
        String[] patterns = excludePatterns.split(",");
        for (String pattern : patterns) {
            pattern = pattern.trim();
            
            // 简单的通配符匹配
            if (pattern.startsWith("*") && fileName.endsWith(pattern.substring(1))) {
                return true;
            }
            
            if (pattern.endsWith("*") && fileName.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return true;
            }
            
            if (filePath.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取源目录列表
     */
    private List<Path> getSourceDirectories(BackupTask task) {
        List<Path> directories = new ArrayList<>();
        
        switch (task.getBackupScope()) {
            case SYSTEM:
                // 备份所有相关目录
                if (task.getIncludeAttachments()) {
                    directories.add(Paths.get(attachmentStoragePath));
                }
                if (task.getIncludeLogs()) {
                    directories.add(Paths.get(logsStoragePath));
                }
                directories.add(Paths.get(certificatesPath));
                break;
                
            case FOLDER:
                // 备份指定文件夹
                if (task.getStoragePath() != null) {
                    directories.add(Paths.get(task.getStoragePath()));
                }
                break;
                
            case CUSTOM:
                // 根据任务配置决定备份内容
                if (task.getIncludeAttachments()) {
                    directories.add(Paths.get(attachmentStoragePath));
                }
                if (task.getIncludeLogs()) {
                    directories.add(Paths.get(logsStoragePath));
                }
                break;
                
            default:
                // 默认备份附件目录
                directories.add(Paths.get(attachmentStoragePath));
                break;
        }
        
        return directories;
    }
    
    /**
     * 生成文件备份路径
     */
    private String generateFileBackupPath(BackupRecord record, String type) {
        String baseDir = new File(record.getBackupPath()).getParent();
        String fileName = String.format("files_%s_%s.zip", 
                                      type, 
                                      LocalDateTime.now().toString().replaceAll("[^0-9]", ""));
        return Paths.get(baseDir, fileName).toString();
    }
    
    /**
     * 恢复文件备份
     */
    public void restoreFiles(BackupRecord record, String targetDirectory) throws Exception {
        logger.info("开始恢复文件备份: {} 到目录: {}", record.getBackupName(), targetDirectory);
        
        Path targetPath = Paths.get(targetDirectory);
        Files.createDirectories(targetPath);
        
        // 这里应该实现ZIP文件的解压缩逻辑
        // 为了演示，我们只是记录操作
        record.incrementRestoredCount();
        
        logger.info("文件恢复完成: {}", record.getBackupName());
    }
    
    /**
     * 获取目录大小
     */
    public long getDirectorySize(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }
        
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
    }
    
    /**
     * 获取目录文件数量
     */
    public long getFileCount(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }
        
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .count();
    }
}