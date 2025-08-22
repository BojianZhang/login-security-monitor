package com.security.monitor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 数据保护服务 - 攻击防护和数据备份
 */
@Service
public class DataProtectionService {

    private static final Logger logger = LoggerFactory.getLogger(DataProtectionService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${app.backup.directory:/app/backups}")
    private String backupDirectory;

    @Value("${app.backup.encrypt:true}")
    private boolean encryptBackups;

    @Value("${app.backup.max-files:10}")
    private int maxBackupFiles;

    @Value("${app.security.emergency-mode:false}")
    private boolean emergencyModeEnabled;

    @Autowired
    private NotificationService notificationService;

    private final Map<String, Integer> threatLevels = new ConcurrentHashMap<>();
    private volatile boolean systemUnderAttack = false;
    private Path backupPath;

    @PostConstruct
    public void initialize() {
        try {
            // 创建备份目录
            backupPath = Paths.get(backupDirectory);
            Files.createDirectories(backupPath);
            
            // 创建数据保护目录结构
            Files.createDirectories(backupPath.resolve("emergency"));
            Files.createDirectories(backupPath.resolve("scheduled"));
            Files.createDirectories(backupPath.resolve("attack-triggered"));
            
            securityLogger.info("数据保护服务已初始化 - 备份目录: {}", backupPath.toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("初始化数据保护服务失败", e);
        }
    }

    /**
     * 处理安全威胁
     */
    @Async
    public void handleSecurityThreat(String sourceIP, String threatType, String details) {
        securityLogger.warn("🚨 安全威胁检测 - IP: {}, 类型: {}, 详情: {}", sourceIP, threatType, details);
        
        // 增加威胁等级
        threatLevels.merge(sourceIP, 1, Integer::sum);
        
        // 检查是否需要进入紧急模式
        int totalThreats = threatLevels.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalThreats > 50) { // 总威胁超过50次
            activateEmergencyMode("高威胁等级检测");
        }
        
        // 发送实时警报
        CompletableFuture.runAsync(() -> {
            try {
                notificationService.sendSecurityAlert(
                    "安全威胁检测",
                    String.format("检测到安全威胁 - IP: %s, 类型: %s, 当前威胁等级: %d", 
                        sourceIP, threatType, threatLevels.get(sourceIP))
                );
            } catch (Exception e) {
                logger.error("发送安全警报失败", e);
            }
        });
    }

    /**
     * 激活紧急模式
     */
    public void activateEmergencyMode(String reason) {
        if (!systemUnderAttack) {
            systemUnderAttack = true;
            securityLogger.error("🚨🚨🚨 系统进入紧急模式 - 原因: {}", reason);
            
            // 立即执行紧急数据备份
            triggerEmergencyBackup(reason);
            
            // 发送紧急通知
            CompletableFuture.runAsync(() -> {
                try {
                    notificationService.sendEmergencyAlert(
                        "系统紧急模式激活",
                        String.format("系统检测到严重安全威胁并已进入紧急保护模式。原因: %s。已自动执行数据备份。请立即检查系统安全状态。", reason)
                    );
                } catch (Exception e) {
                    logger.error("发送紧急通知失败", e);
                }
            });
        }
    }

    /**
     * 触发紧急备份
     */
    @Async
    public CompletableFuture<Boolean> triggerEmergencyBackup(String reason) {
        if (!backupEnabled) {
            logger.warn("备份功能已禁用，跳过紧急备份");
            return CompletableFuture.completedFuture(false);
        }

        try {
            securityLogger.info("🔄 开始紧急数据备份 - 原因: {}", reason);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("emergency_backup_%s.sql", timestamp);
            Path emergencyBackupPath = backupPath.resolve("emergency").resolve(backupFileName);
            
            // 执行数据库备份
            boolean success = performDatabaseBackup(emergencyBackupPath, true);
            
            if (success) {
                // 创建备份元数据
                createBackupMetadata(emergencyBackupPath, reason, "EMERGENCY");
                
                // 如果启用加密，加密备份文件
                if (encryptBackups) {
                    encryptBackupFile(emergencyBackupPath);
                }
                
                securityLogger.info("✅ 紧急数据备份完成 - 文件: {}", backupFileName);
                
                // 发送备份完成通知
                notificationService.sendBackupNotification(
                    "紧急数据备份完成",
                    String.format("紧急数据备份已完成。文件: %s, 原因: %s", backupFileName, reason)
                );
                
                return CompletableFuture.completedFuture(true);
            } else {
                securityLogger.error("❌ 紧急数据备份失败");
                return CompletableFuture.completedFuture(false);
            }
            
        } catch (Exception e) {
            securityLogger.error("紧急数据备份过程中发生错误", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 定时数据备份（每4小时执行一次）
     */
    @Scheduled(fixedRate = 14400000) // 4小时
    public void scheduledBackup() {
        if (!backupEnabled) return;

        try {
            securityLogger.info("🔄 开始定时数据备份");
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("scheduled_backup_%s.sql", timestamp);
            Path scheduledBackupPath = backupPath.resolve("scheduled").resolve(backupFileName);
            
            boolean success = performDatabaseBackup(scheduledBackupPath, false);
            
            if (success) {
                createBackupMetadata(scheduledBackupPath, "定时备份", "SCHEDULED");
                
                if (encryptBackups) {
                    encryptBackupFile(scheduledBackupPath);
                }
                
                // 清理旧备份文件
                cleanupOldBackups();
                
                securityLogger.info("✅ 定时数据备份完成 - 文件: {}", backupFileName);
            }
            
        } catch (Exception e) {
            logger.error("定时备份失败", e);
        }
    }

    /**
     * 执行数据库备份
     */
    private boolean performDatabaseBackup(Path backupFilePath, boolean isEmergency) {
        try {
            // 构建mysqldump命令
            String[] command = {
                "docker-compose", "exec", "-T", "mysql",
                "mysqldump",
                "-u", "root",
                "-p" + System.getenv("DB_ROOT_PASSWORD"),
                "--single-transaction",
                "--routines",
                "--triggers",
                System.getenv("DB_NAME")
            };

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // 读取备份数据并写入文件
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedWriter writer = Files.newBufferedWriter(backupFilePath)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 计算文件校验和
                String checksum = calculateFileChecksum(backupFilePath);
                securityLogger.info("备份文件校验和: {} - {}", backupFilePath.getFileName(), checksum);
                return true;
            } else {
                logger.error("数据库备份失败，退出码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("执行数据库备份时发生错误", e);
            return false;
        }
    }

    /**
     * 创建备份元数据
     */
    private void createBackupMetadata(Path backupFilePath, String reason, String type) {
        try {
            Path metadataPath = backupFilePath.resolveSibling(backupFilePath.getFileName() + ".meta");
            
            Properties metadata = new Properties();
            metadata.setProperty("backup.timestamp", LocalDateTime.now().toString());
            metadata.setProperty("backup.reason", reason);
            metadata.setProperty("backup.type", type);
            metadata.setProperty("backup.size", String.valueOf(Files.size(backupFilePath)));
            metadata.setProperty("backup.checksum", calculateFileChecksum(backupFilePath));
            metadata.setProperty("system.version", getSystemVersion());
            
            try (OutputStream out = Files.newOutputStream(metadataPath)) {
                metadata.store(out, "Backup Metadata");
            }
            
        } catch (Exception e) {
            logger.error("创建备份元数据失败", e);
        }
    }

    /**
     * 加密备份文件
     */
    private void encryptBackupFile(Path backupFilePath) {
        try {
            Path encryptedPath = backupFilePath.resolveSibling(backupFilePath.getFileName() + ".enc");
            
            // 使用GZIP压缩（简单的混淆，实际生产中应使用AES等强加密）
            try (InputStream in = Files.newInputStream(backupFilePath);
                 OutputStream out = new GZIPOutputStream(Files.newOutputStream(encryptedPath))) {
                
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            }
            
            // 删除原文件，保留加密文件
            Files.deleteIfExists(backupFilePath);
            Files.move(encryptedPath, backupFilePath);
            
            securityLogger.info("备份文件已加密: {}", backupFilePath.getFileName());
            
        } catch (Exception e) {
            logger.error("加密备份文件失败", e);
        }
    }

    /**
     * 计算文件校验和
     */
    private String calculateFileChecksum(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            try (InputStream in = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    md.update(buffer, 0, length);
                }
            }
            
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("计算文件校验和失败", e);
            return "unknown";
        }
    }

    /**
     * 清理旧备份文件
     */
    private void cleanupOldBackups() {
        try {
            // 清理定时备份文件
            cleanupBackupDirectory(backupPath.resolve("scheduled"));
            
            // 保留更多紧急备份文件
            cleanupBackupDirectory(backupPath.resolve("emergency"), maxBackupFiles * 2);
            
        } catch (Exception e) {
            logger.error("清理旧备份文件失败", e);
        }
    }

    private void cleanupBackupDirectory(Path directory) {
        cleanupBackupDirectory(directory, maxBackupFiles);
    }

    private void cleanupBackupDirectory(Path directory, int maxFiles) {
        try {
            if (!Files.exists(directory)) return;
            
            List<Path> backupFiles = Files.list(directory)
                    .filter(path -> path.toString().endsWith(".sql"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();
            
            if (backupFiles.size() > maxFiles) {
                for (int i = maxFiles; i < backupFiles.size(); i++) {
                    Path fileToDelete = backupFiles.get(i);
                    Files.deleteIfExists(fileToDelete);
                    Files.deleteIfExists(fileToDelete.resolveSibling(fileToDelete.getFileName() + ".meta"));
                    securityLogger.info("清理旧备份文件: {}", fileToDelete.getFileName());
                }
            }
            
        } catch (Exception e) {
            logger.error("清理备份目录失败: {}", directory, e);
        }
    }

    /**
     * 获取系统版本信息
     */
    private String getSystemVersion() {
        return "Login Security Monitor v1.0.0";
    }

    /**
     * 系统状态检查
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("backupEnabled", backupEnabled);
        status.put("emergencyMode", systemUnderAttack);
        status.put("threatLevels", new HashMap<>(threatLevels));
        status.put("backupDirectory", backupPath.toString());
        
        try {
            status.put("backupCount", Files.list(backupPath).count());
        } catch (IOException e) {
            status.put("backupCount", -1);
        }
        
        return status;
    }

    /**
     * 手动触发数据备份
     */
    @Async
    public CompletableFuture<Boolean> manualBackup(String reason) {
        securityLogger.info("🔄 开始手动数据备份 - 原因: {}", reason);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = String.format("manual_backup_%s.sql", timestamp);
        Path manualBackupPath = backupPath.resolve(backupFileName);
        
        boolean success = performDatabaseBackup(manualBackupPath, false);
        
        if (success) {
            createBackupMetadata(manualBackupPath, reason, "MANUAL");
            if (encryptBackups) {
                encryptBackupFile(manualBackupPath);
            }
            securityLogger.info("✅ 手动数据备份完成 - 文件: {}", backupFileName);
        }
        
        return CompletableFuture.completedFuture(success);
    }

    /**
     * 退出紧急模式
     */
    public void deactivateEmergencyMode() {
        if (systemUnderAttack) {
            systemUnderAttack = false;
            threatLevels.clear();
            securityLogger.info("系统已退出紧急模式");
        }
    }

    @PreDestroy
    public void cleanup() {
        securityLogger.info("数据保护服务正在关闭...");
    }
}