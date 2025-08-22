package com.security.monitor.api.controller;

import com.security.monitor.api.dto.ApiResponse;
import com.security.monitor.model.BackupRecord;
import com.security.monitor.model.BackupTask;
import com.security.monitor.repository.BackupRecordRepository;
import com.security.monitor.repository.BackupTaskRepository;
import com.security.monitor.service.BackupService;
import com.security.monitor.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 备份管理API控制器
 */
@RestController
@RequestMapping("/api/v1/backup")
@Tag(name = "备份管理", description = "备份任务和记录管理API")
public class BackupController {
    
    @Autowired
    private BackupService backupService;
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private BackupTaskRepository backupTaskRepository;
    
    @Autowired
    private BackupRecordRepository backupRecordRepository;
    
    /**
     * 获取所有备份任务
     */
    @GetMapping("/tasks")
    @Operation(summary = "获取备份任务列表", description = "获取所有备份任务的分页列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BackupTask>>> getTasks(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<BackupTask> tasks = backupTaskRepository.findAll(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
    
    /**
     * 获取活跃的备份任务
     */
    @GetMapping("/tasks/active")
    @Operation(summary = "获取活跃备份任务", description = "获取所有启用状态的备份任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BackupTask>>> getActiveTasks() {
        List<BackupTask> tasks = backupTaskRepository.findByIsActiveTrue();
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
    
    /**
     * 创建备份任务
     */
    @PostMapping("/tasks")
    @Operation(summary = "创建备份任务", description = "创建新的备份任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupTask>> createTask(@RequestBody BackupTask task) {
        task.setCreatedBy("admin"); // 实际应用中应该从认证上下文获取
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        BackupTask savedTask = backupTaskRepository.save(task);
        
        return ResponseEntity.ok(ApiResponse.success(savedTask, "备份任务创建成功"));
    }
    
    /**
     * 更新备份任务
     */
    @PutMapping("/tasks/{id}")
    @Operation(summary = "更新备份任务", description = "更新指定的备份任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BackupTask>> updateTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @RequestBody BackupTask task) {
        
        return backupTaskRepository.findById(id)
                .map(existingTask -> {
                    // 更新任务信息
                    existingTask.setTaskName(task.getTaskName());
                    existingTask.setBackupType(task.getBackupType());
                    existingTask.setBackupScope(task.getBackupScope());
                    existingTask.setScheduleExpression(task.getScheduleExpression());
                    existingTask.setStorageType(task.getStorageType());
                    existingTask.setStoragePath(task.getStoragePath());
                    existingTask.setCompressionEnabled(task.getCompressionEnabled());
                    existingTask.setEncryptionEnabled(task.getEncryptionEnabled());
                    existingTask.setRetentionDays(task.getRetentionDays());
                    existingTask.setMaxBackupSize(task.getMaxBackupSize());
                    existingTask.setIncludeAttachments(task.getIncludeAttachments());
                    existingTask.setIncludeLogs(task.getIncludeLogs());
                    existingTask.setExcludePatterns(task.getExcludePatterns());
                    existingTask.setIsActive(task.getIsActive());
                    existingTask.setUpdatedAt(LocalDateTime.now());
                    
                    BackupTask savedTask = backupTaskRepository.save(existingTask);
                    return ResponseEntity.ok(ApiResponse.success(savedTask, "备份任务更新成功"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 删除备份任务
     */
    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除备份任务", description = "删除指定的备份任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        return backupTaskRepository.findById(id)
                .map(task -> {
                    backupTaskRepository.delete(task);
                    return ResponseEntity.ok(ApiResponse.<Void>success(null, "备份任务删除成功"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 立即执行备份任务
     */
    @PostMapping("/tasks/{id}/execute")
    @Operation(summary = "执行备份任务", description = "立即执行指定的备份任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> executeTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        return backupTaskRepository.findById(id)
                .map(task -> {
                    CompletableFuture<BackupRecord> future = backupService.executeBackup(task);
                    return ResponseEntity.ok(ApiResponse.success("", "备份任务已开始执行"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 测试存储连接
     */
    @PostMapping("/tasks/{id}/test-connection")
    @Operation(summary = "测试存储连接", description = "测试备份任务的存储连接")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> testConnection(@Parameter(description = "任务ID") @PathVariable Long id) {
        return backupTaskRepository.findById(id)
                .map(task -> {
                    boolean success = storageService.testConnection(task.getStorageType(), task.getStoragePath());
                    String message = success ? "存储连接测试成功" : "存储连接测试失败";
                    return ResponseEntity.ok(ApiResponse.success(success, message));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取备份记录
     */
    @GetMapping("/records")
    @Operation(summary = "获取备份记录", description = "获取备份记录的分页列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BackupRecord>>> getRecords(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<BackupRecord> records = backupRecordRepository.findAll(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(records));
    }
    
    /**
     * 获取任务的备份记录
     */
    @GetMapping("/tasks/{taskId}/records")
    @Operation(summary = "获取任务备份记录", description = "获取指定任务的备份记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BackupRecord>>> getTaskRecords(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        
        return backupTaskRepository.findById(taskId)
                .map(task -> {
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    Page<BackupRecord> records = backupRecordRepository.findByTaskOrderByCreatedAtDesc(task, pageable);
                    return ResponseEntity.ok(ApiResponse.success(records));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 验证备份记录
     */
    @PostMapping("/records/{id}/verify")
    @Operation(summary = "验证备份记录", description = "验证指定备份记录的完整性")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> verifyRecord(@Parameter(description = "记录ID") @PathVariable Long id) {
        return backupRecordRepository.findById(id)
                .map(record -> {
                    CompletableFuture<Boolean> future = backupService.verifyBackup(record);
                    return ResponseEntity.ok(ApiResponse.success("", "备份验证已开始"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 删除备份记录
     */
    @DeleteMapping("/records/{id}")
    @Operation(summary = "删除备份记录", description = "删除指定的备份记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@Parameter(description = "记录ID") @PathVariable Long id) {
        return backupRecordRepository.findById(id)
                .map(record -> {
                    try {
                        storageService.deleteBackup(record);
                        backupRecordRepository.delete(record);
                        return ResponseEntity.ok(ApiResponse.<Void>success(null, "备份记录删除成功"));
                    } catch (Exception e) {
                        return ResponseEntity.ok(ApiResponse.<Void>error("删除备份记录失败: " + e.getMessage(), "BACKUP_001"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 获取备份统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取备份统计", description = "获取备份系统的统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        
        // 任务统计
        long totalTasks = backupTaskRepository.count();
        long activeTasks = backupTaskRepository.findByIsActiveTrue().size();
        
        // 记录统计
        long totalRecords = backupRecordRepository.count();
        Long totalBackupSize = backupRecordRepository.getTotalBackupSize();
        Long totalCompressedSize = backupRecordRepository.getTotalCompressedSize();
        
        // 状态统计
        List<Object[]> statusCounts = backupRecordRepository.countRecordsByStatus();
        
        Map<String, Object> statistics = Map.of(
            "totalTasks", totalTasks,
            "activeTasks", activeTasks,
            "inactiveTasks", totalTasks - activeTasks,
            "totalRecords", totalRecords,
            "totalBackupSize", totalBackupSize != null ? totalBackupSize : 0L,
            "totalCompressedSize", totalCompressedSize != null ? totalCompressedSize : 0L,
            "statusCounts", statusCounts
        );
        
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
    
    /**
     * 获取最近失败的备份任务
     */
    @GetMapping("/tasks/failed")
    @Operation(summary = "获取失败任务", description = "获取最近失败的备份任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BackupTask>>> getFailedTasks() {
        List<BackupTask> failedTasks = backupTaskRepository.findRecentFailedTasks();
        return ResponseEntity.ok(ApiResponse.success(failedTasks));
    }
    
    /**
     * 获取可恢复的备份记录
     */
    @GetMapping("/records/restorable")
    @Operation(summary = "获取可恢复备份", description = "获取可用于恢复的备份记录")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BackupRecord>>> getRestorableRecords() {
        List<BackupRecord> restorableRecords = backupRecordRepository
                .findByIsRestorableTrueAndBackupStatusOrderByCreatedAtDesc(BackupTask.BackupStatus.SUCCESS);
        return ResponseEntity.ok(ApiResponse.success(restorableRecords));
    }
}