package com.security.monitor.controller;

import com.security.monitor.dto.LoginRequest;
import com.security.monitor.dto.LoginResponse;
import com.security.monitor.service.LoginService;
import com.security.monitor.service.LoginService.LoginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private LoginService loginService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, 
                                             HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        String username = loginRequest.getUsername();
        
        // 避免在日志中记录敏感信息，只记录用户名和IP
        logger.info("用户登录尝试 - 用户: {}, IP: {}", 
            username != null ? username.replaceAll("[\r\n]", "") : "unknown", 
            clientIp);

        try {
            LoginResult result = loginService.login(
                loginRequest.getUsername(), 
                loginRequest.getPassword(), 
                request
            );

            if (result.isSuccess()) {
                LoginResponse response = LoginResponse.success(
                    result.getUser(),
                    result.getToken(),
                    result.getLoginRecord().getId()
                );
                
                logger.info("用户 {} 登录成功", loginRequest.getUsername());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("用户 {} 登录失败: {}", loginRequest.getUsername(), result.getMessage());
                return ResponseEntity.badRequest()
                        .body(LoginResponse.failure(result.getMessage()));
            }

        } catch (Exception e) {
            logger.error("登录过程中发生错误", e);
            return ResponseEntity.internalServerError()
                    .body(LoginResponse.failure("登录过程中发生内部错误"));
        }
    }

    /**
     * 检查令牌有效性
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("缺少认证令牌"));
        }

        // 这里可以添加令牌验证逻辑
        return ResponseEntity.ok(ApiResponse.success("令牌有效", null));
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 通用API响应类
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse() {}

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}