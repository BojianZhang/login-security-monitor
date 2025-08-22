package com.security.monitor.dto;

import com.security.monitor.model.User;
import java.time.LocalDateTime;

/**
 * 登录响应DTO
 */
public class LoginResponse {
    
    private boolean success;
    private String message;
    private String token;
    private UserInfo user;
    private LoginInfo loginInfo;

    public LoginResponse() {}
    
    public static LoginResponse success(User user, String token, Long loginRecordId) {
        LoginResponse response = new LoginResponse();
        response.success = true;
        response.message = "登录成功";
        response.token = token;
        response.user = new UserInfo(user);
        response.loginInfo = new LoginInfo(loginRecordId, LocalDateTime.now());
        return response;
    }
    
    public static LoginResponse failure(String message) {
        LoginResponse response = new LoginResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    // 用户信息内部类
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private boolean isAdmin;
        private LocalDateTime lastLogin;

        public UserInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.isAdmin = user.getIsAdmin();
            this.lastLogin = user.getLastLogin();
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public boolean isAdmin() { return isAdmin; }
        public LocalDateTime getLastLogin() { return lastLogin; }
    }

    // 登录信息内部类
    public static class LoginInfo {
        private Long loginRecordId;
        private LocalDateTime loginTime;

        public LoginInfo(Long loginRecordId, LocalDateTime loginTime) {
            this.loginRecordId = loginRecordId;
            this.loginTime = loginTime;
        }

        // Getters
        public Long getLoginRecordId() { return loginRecordId; }
        public LocalDateTime getLoginTime() { return loginTime; }
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public LoginInfo getLoginInfo() {
        return loginInfo;
    }

    public void setLoginInfo(LoginInfo loginInfo) {
        this.loginInfo = loginInfo;
    }
}