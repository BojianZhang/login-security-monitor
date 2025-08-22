package com.security.monitor.service;

import com.security.monitor.model.BackupRecord;
import com.security.monitor.model.BackupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库备份服务
 */
@Service
public class DatabaseBackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    
    @Value("${spring.datasource.password}")
    private String databasePassword;
    
    @Value("${backup.mysql.bin-path:/usr/bin}")
    private String mysqlBinPath;
    
    /**
     * 执行完整数据库备份
     */
    public void executeFullBackup(BackupTask task, BackupRecord record) throws Exception {
        logger.info("开始执行数据库完整备份: {}", task.getTaskName());
        
        String backupFile = generateDatabaseBackupPath(record, "full");
        
        // 使用mysqldump执行备份
        executeMysqlDump(task, record, backupFile, null);
        
        // 统计数据库信息
        collectDatabaseStatistics(record);
        
        logger.info("数据库完整备份完成: {}", backupFile);
    }
    
    /**
     * 执行增量数据库备份
     */
    public void executeIncrementalBackup(BackupTask task, BackupRecord record, LocalDateTime lastBackupTime) throws Exception {
        logger.info("开始执行数据库增量备份: {}, 上次备份时间: {}", task.getTaskName(), lastBackupTime);
        
        String backupFile = generateDatabaseBackupPath(record, "incremental");
        
        // 构建增量备份的WHERE条件
        String whereCondition = String.format("WHERE created_at > '%s' OR updated_at > '%s'", 
                                            lastBackupTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                            lastBackupTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        executeMysqlDump(task, record, backupFile, whereCondition);
        
        collectDatabaseStatistics(record);
        
        logger.info("数据库增量备份完成: {}", backupFile);
    }
    
    /**
     * 执行差异数据库备份
     */
    public void executeDifferentialBackup(BackupTask task, BackupRecord record, LocalDateTime baseTime) throws Exception {
        logger.info("开始执行数据库差异备份: {}, 基准时间: {}", task.getTaskName(), baseTime);
        
        String backupFile = generateDatabaseBackupPath(record, "differential");
        
        // 构建差异备份的WHERE条件
        String whereCondition = String.format("WHERE created_at > '%s' OR updated_at > '%s'", 
                                            baseTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                            baseTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        executeMysqlDump(task, record, backupFile, whereCondition);
        
        collectDatabaseStatistics(record);
        
        logger.info("数据库差异备份完成: {}", backupFile);
    }
    
    /**
     * 执行MySQL数据库转储
     */
    private void executeMysqlDump(BackupTask task, BackupRecord record, String backupFile, String whereCondition) throws Exception {
        
        // 从数据库URL中提取数据库名
        String databaseName = extractDatabaseName(databaseUrl);
        
        List<String> command = new ArrayList<>();
        command.add(Paths.get(mysqlBinPath, "mysqldump").toString());
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");
        command.add("--hex-blob");
        command.add("--complete-insert");
        
        // 添加认证参数
        if (databaseUsername != null && !databaseUsername.isEmpty()) {
            command.add("-u");
            command.add(databaseUsername);
        }
        
        if (databasePassword != null && !databasePassword.isEmpty()) {
            command.add("-p" + databasePassword);
        }
        
        // 根据备份范围添加参数
        switch (task.getBackupScope()) {
            case SYSTEM:
                command.add("--all-databases");
                break;
            case CUSTOM:
                command.add(databaseName);
                
                // 如果有WHERE条件，需要为每个表添加
                if (whereCondition != null) {
                    addTableSpecificConditions(command, databaseName, whereCondition);
                }
                break;
            default:
                command.add(databaseName);
                break;
        }
        
        logger.info("执行MySQL备份命令: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(new File(backupFile));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("MySQL备份失败，退出代码: " + exitCode);
        }
        
        // 更新备份记录
        Path backupPath = Paths.get(backupFile);
        record.setBackupPath(backupFile);
        record.setBackupSize(Files.size(backupPath));
    }
    
    /**
     * 为表添加特定的WHERE条件
     */
    private void addTableSpecificConditions(List<String> command, String databaseName, String whereCondition) throws SQLException {
        // 获取所有表名
        List<String> tables = getDatabaseTables(databaseName);
        
        for (String table : tables) {
            // 检查表是否有created_at或updated_at字段
            if (hasTimestampFields(databaseName, table)) {
                command.add("--where=" + whereCondition.replace("WHERE ", ""));
            }
        }
    }
    
    /**
     * 获取数据库中的所有表
     */
    private List<String> getDatabaseTables(String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SHOW TABLES FROM " + databaseName);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        
        return tables;
    }
    
    /**
     * 检查表是否有时间戳字段
     */
    private boolean hasTimestampFields(String databaseName, String tableName) throws SQLException {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                    "AND COLUMN_NAME IN ('created_at', 'updated_at')";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, databaseName);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * 收集数据库统计信息
     */
    private void collectDatabaseStatistics(BackupRecord record) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            
            // 统计表数量
            int tableCount = countTables(conn);
            record.setDatabaseTables(tableCount);
            
            // 统计记录数量
            long recordCount = countRecords(conn);
            record.setDatabaseRecords(recordCount);
            
            logger.info("数据库统计完成 - 表数: {}, 记录数: {}", tableCount, recordCount);
        }
    }
    
    /**
     * 统计表数量
     */
    private int countTables(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
        String databaseName = extractDatabaseName(databaseUrl);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
    
    /**
     * 统计记录数量
     */
    private long countRecords(Connection conn) throws SQLException {
        List<String> tables = getDatabaseTables(extractDatabaseName(databaseUrl));
        long totalRecords = 0;
        
        for (String table : tables) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    totalRecords += rs.getLong(1);
                }
            }
        }
        
        return totalRecords;
    }
    
    /**
     * 从数据库URL中提取数据库名
     */
    private String extractDatabaseName(String url) {
        // 从jdbc:mysql://localhost:3306/database_name?params提取database_name
        String[] parts = url.split("/");
        if (parts.length > 3) {
            String dbPart = parts[parts.length - 1];
            int paramIndex = dbPart.indexOf('?');
            return paramIndex > 0 ? dbPart.substring(0, paramIndex) : dbPart;
        }
        return "secure_email_system";
    }
    
    /**
     * 生成数据库备份文件路径
     */
    private String generateDatabaseBackupPath(BackupRecord record, String type) {
        String baseDir = new File(record.getBackupPath()).getParent();
        String fileName = String.format("database_%s_%s.sql", 
                                      type, 
                                      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        return Paths.get(baseDir, fileName).toString();
    }
    
    /**
     * 恢复数据库备份
     */
    public void restoreDatabase(BackupRecord record) throws Exception {
        logger.info("开始恢复数据库备份: {}", record.getBackupName());
        
        String databaseName = extractDatabaseName(databaseUrl);
        
        List<String> command = new ArrayList<>();
        command.add(Paths.get(mysqlBinPath, "mysql").toString());
        
        // 添加认证参数
        if (databaseUsername != null && !databaseUsername.isEmpty()) {
            command.add("-u");
            command.add(databaseUsername);
        }
        
        if (databasePassword != null && !databasePassword.isEmpty()) {
            command.add("-p" + databasePassword);
        }
        
        command.add(databaseName);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectInput(new File(record.getBackupPath()));
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("数据库恢复失败，退出代码: " + exitCode);
        }
        
        // 增加恢复计数
        record.incrementRestoredCount();
        
        logger.info("数据库恢复完成: {}", record.getBackupName());
    }
}