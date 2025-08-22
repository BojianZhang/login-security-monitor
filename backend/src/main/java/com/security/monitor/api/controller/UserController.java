package com.security.monitor.api.controller;

import com.security.monitor.api.dto.*;
import com.security.monitor.model.User;
import com.security.monitor.service.UserService;
import com.security.monitor.service.EmailAliasService;
import com.security.monitor.service.EmailFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理API控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "用户管理API")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailAliasService aliasService;
    
    @Autowired
    private EmailFolderService folderService;
    
    /**
     * 获取用户列表
     */
    @GetMapping
    @Operation(summary = "获取用户列表", description = "分页获取所有用户信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsers(
            @Parameter(description = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向", example = "desc") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDir);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }
        
        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        PaginationInfo pagination = new PaginationInfo(page, size, userPage.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(userDTOs, pagination));
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取详细信息")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        
        User user = userService.getUserById(id);
        UserDTO userDTO = convertToDTO(user);
        
        return ResponseEntity.ok(ApiResponse.success(userDTO));
    }
    
    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名获取用户", description = "根据用户名获取用户信息")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.principal.username")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUsername(
            @Parameter(description = "用户名", required = true) @PathVariable String username) {
        
        User user = userService.getUserByUsername(username);
        UserDTO userDTO = convertToDTO(user);
        
        return ResponseEntity.ok(ApiResponse.success(userDTO));
    }
    
    /**
     * 创建新用户
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "创建新的用户账户")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "用户创建成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "409", description = "用户名或邮箱已存在")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(
            @Parameter(description = "用户创建请求", required = true) @Valid @RequestBody CreateUserRequest request) {
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setIsAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : false);
        user.setIsEmailAdmin(request.getIsEmailAdmin() != null ? request.getIsEmailAdmin() : false);
        user.setStorageQuota(request.getStorageQuota() != null ? request.getStorageQuota() : 1073741824L); // 1GB
        user.setEmailEnabled(request.getEmailEnabled() != null ? request.getEmailEnabled() : true);
        
        User createdUser = userService.createUser(user, request.getPassword());
        UserDTO userDTO = convertToDTO(createdUser);
        
        return ResponseEntity.ok(ApiResponse.success(userDTO, "用户创建成功"));
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @Parameter(description = "用户更新请求", required = true) @Valid @RequestBody UpdateUserRequest request) {
        
        User existingUser = userService.getUserById(id);
        
        if (request.getEmail() != null) existingUser.setEmail(request.getEmail());
        if (request.getFullName() != null) existingUser.setFullName(request.getFullName());
        if (request.getPhone() != null) existingUser.setPhone(request.getPhone());
        if (request.getIsActive() != null) existingUser.setIsActive(request.getIsActive());
        if (request.getIsAdmin() != null) existingUser.setIsAdmin(request.getIsAdmin());
        if (request.getIsEmailAdmin() != null) existingUser.setIsEmailAdmin(request.getIsEmailAdmin());
        if (request.getStorageQuota() != null) existingUser.setStorageQuota(request.getStorageQuota());
        if (request.getEmailEnabled() != null) existingUser.setEmailEnabled(request.getEmailEnabled());
        
        User updatedUser = userService.updateUser(existingUser);
        UserDTO userDTO = convertToDTO(updatedUser);
        
        return ResponseEntity.ok(ApiResponse.success(userDTO, "用户更新成功"));
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        
        userService.deleteUser(id);
        
        return ResponseEntity.ok(ApiResponse.success(null, "用户删除成功"));
    }
    
    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        
        userService.resetPassword(id, newPassword);
        
        return ResponseEntity.ok(ApiResponse.success(null, "密码重置成功"));
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/{id}/change-password")
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    @PreAuthorize("#id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @Parameter(description = "旧密码") @RequestParam String oldPassword,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        
        userService.changePassword(id, oldPassword, newPassword);
        
        return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
    }
    
    /**
     * 启用/禁用用户
     */
    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "切换用户状态", description = "启用或禁用用户账户")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> toggleUserStatus(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        
        User user = userService.toggleUserStatus(id);
        UserDTO userDTO = convertToDTO(user);
        
        return ResponseEntity.ok(ApiResponse.success(userDTO, "用户状态已更新"));
    }
    
    /**
     * 获取用户存储使用情况
     */
    @GetMapping("/{id}/storage")
    @Operation(summary = "获取存储使用情况", description = "获取用户的存储空间使用情况")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<StorageUsageDTO>> getStorageUsage(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        
        User user = userService.getUserById(id);
        StorageUsageDTO usage = new StorageUsageDTO();
        usage.setUserId(user.getId());
        usage.setUsername(user.getUsername());
        usage.setStorageQuota(user.getStorageQuota());
        usage.setStorageUsed(user.getStorageUsed());
        usage.setUsagePercentage(user.getStorageUsed() * 100.0 / user.getStorageQuota());
        usage.setRemainingStorage(user.getStorageQuota() - user.getStorageUsed());
        
        return ResponseEntity.ok(ApiResponse.success(usage));
    }
    
    /**
     * 获取用户的邮件别名
     */
    @GetMapping("/{id}/aliases")
    @Operation(summary = "获取用户别名", description = "获取用户的所有邮件别名")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<List<EmailAliasDTO>>> getUserAliases(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        
        User user = userService.getUserById(id);
        List<EmailAliasDTO> aliases = aliasService.getUserAliases(user).stream()
                .map(this::convertAliasToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(aliases));
    }
    
    /**
     * 获取用户的邮件文件夹
     */
    @GetMapping("/{id}/folders")
    @Operation(summary = "获取用户文件夹", description = "获取用户的所有邮件文件夹")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<List<EmailFolderDTO>>> getUserFolders(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        
        User user = userService.getUserById(id);
        List<EmailFolderDTO> folders = folderService.getUserFolders(user).stream()
                .map(this::convertFolderToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(folders));
    }
    
    // 私有方法：DTO转换
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setIsActive(user.getIsActive());
        dto.setIsAdmin(user.getIsAdmin());
        dto.setIsEmailAdmin(user.getIsEmailAdmin());
        dto.setStorageQuota(user.getStorageQuota());
        dto.setStorageUsed(user.getStorageUsed());
        dto.setLastLogin(user.getLastLogin());
        dto.setEmailEnabled(user.getEmailEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        return dto;
    }
    
    private EmailAliasDTO convertAliasToDTO(EmailAlias alias) {
        EmailAliasDTO dto = new EmailAliasDTO();
        dto.setId(alias.getId());
        dto.setAliasEmail(alias.getAliasEmail());
        dto.setDomainName(alias.getDomain().getDomainName());
        dto.setIsActive(alias.getIsActive());
        dto.setForwardTo(alias.getForwardTo());
        dto.setDisplayName(alias.getDisplayName());
        dto.setDescription(alias.getDescription());
        dto.setCreatedAt(alias.getCreatedAt());
        
        return dto;
    }
    
    private EmailFolderDTO convertFolderToDTO(EmailFolder folder) {
        EmailFolderDTO dto = new EmailFolderDTO();
        dto.setId(folder.getId());
        dto.setFolderName(folder.getFolderName());
        dto.setFolderType(folder.getFolderType().toString());
        dto.setMessageCount(folder.getMessageCount());
        dto.setUnreadCount(folder.getUnreadCount());
        dto.setCreatedAt(folder.getCreatedAt());
        
        return dto;
    }
}

/**
 * 存储使用情况DTO
 */
class StorageUsageDTO {
    private Long userId;
    private String username;
    private Long storageQuota;
    private Long storageUsed;
    private Double usagePercentage;
    private Long remainingStorage;
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public Long getStorageQuota() { return storageQuota; }
    public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
    
    public Long getStorageUsed() { return storageUsed; }
    public void setStorageUsed(Long storageUsed) { this.storageUsed = storageUsed; }
    
    public Double getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(Double usagePercentage) { this.usagePercentage = usagePercentage; }
    
    public Long getRemainingStorage() { return remainingStorage; }
    public void setRemainingStorage(Long remainingStorage) { this.remainingStorage = remainingStorage; }
}