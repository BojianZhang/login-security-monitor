package com.security.monitor.service;

import com.security.monitor.model.LoginRecord;
import com.security.monitor.model.User;
import com.security.monitor.repository.LoginRecordRepository;
import com.security.monitor.repository.UserRepository;
import com.security.monitor.service.GeoLocationService.GeoLocationInfo;
import eu.bitwalker.useragentutils.UserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 登录服务
 */
@Service
@Transactional
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginRecordRepository loginRecordRepository;

    @Autowired
    private GeoLocationService geoLocationService;

    @Autowired
    private AnomalousLoginDetectionService anomalousLoginDetectionService;

    @Autowired
    private JwtService jwtService;

    /**
     * 用户登录
     */
    public LoginResult login(String username, String password, HttpServletRequest request) {
        logger.info("用户 {} 尝试登录", username);
        
        try {
            // 执行身份验证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            User user = (User) authentication.getPrincipal();
            
            // 记录成功登录
            LoginRecord loginRecord = recordLogin(user, request, LoginRecord.LoginStatus.SUCCESS);
            
            // 更新用户最后登录时间
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // 生成JWT令牌
            String token = jwtService.generateToken(user);

            // 执行异地登录检测
            anomalousLoginDetectionService.detectAnomalousLogin(loginRecord);

            logger.info("用户 {} 登录成功", username);
            
            return LoginResult.success(user, token, loginRecord);

        } catch (AuthenticationException e) {
            logger.warn("用户 {} 登录失败: {}", username, e.getMessage());
            
            // 查找用户（即使登录失败也要记录）
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                recordLogin(user, request, LoginRecord.LoginStatus.FAILED);
            }
            
            return LoginResult.failure("用户名或密码错误");
        } catch (Exception e) {
            logger.error("用户 {} 登录过程中发生错误", username, e);
            return LoginResult.failure("登录过程中发生错误");
        }
    }

    /**
     * 记录登录信息
     */
    private LoginRecord recordLogin(User user, HttpServletRequest request, LoginRecord.LoginStatus status) {
        String ipAddress = getClientIpAddress(request);
        String userAgentString = request.getHeader("User-Agent");
        
        logger.debug("记录用户 {} 的登录信息，IP: {}", user.getUsername(), ipAddress);

        // 创建登录记录
        LoginRecord loginRecord = new LoginRecord(user, ipAddress, userAgentString);
        loginRecord.setLoginStatus(status);
        loginRecord.setSessionId(UUID.randomUUID().toString());

        // 解析User-Agent
        parseUserAgent(loginRecord, userAgentString);

        // 获取地理位置信息
        enrichWithGeoLocation(loginRecord, ipAddress);

        // 保存登录记录（基础信息先保存）
        loginRecord = loginRecordRepository.save(loginRecord);

        return loginRecord;
    }

    /**
     * 解析User-Agent信息
     */
    private void parseUserAgent(LoginRecord loginRecord, String userAgentString) {
        if (userAgentString == null || userAgentString.isEmpty()) {
            return;
        }

        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
            
            loginRecord.setBrowser(userAgent.getBrowser().getName());
            loginRecord.setOs(userAgent.getOperatingSystem().getName());
            
            // 设备类型判断
            if (userAgent.getOperatingSystem().isMobileDevice()) {
                loginRecord.setDeviceType("Mobile");
            } else {
                loginRecord.setDeviceType("Desktop");
            }

        } catch (Exception e) {
            logger.debug("解析User-Agent失败: {}", userAgentString, e);
        }
    }

    /**
     * 获取地理位置信息并填充到登录记录
     */
    private void enrichWithGeoLocation(LoginRecord loginRecord, String ipAddress) {
        try {
            GeoLocationInfo geoInfo = geoLocationService.getGeoLocation(ipAddress);
            
            if (geoInfo != null) {
                loginRecord.setCountry(geoInfo.getCountry());
                loginRecord.setRegion(geoInfo.getRegion());
                loginRecord.setCity(geoInfo.getCity());
                loginRecord.setLatitude(geoInfo.getLatitude());
                loginRecord.setLongitude(geoInfo.getLongitude());
                loginRecord.setTimezone(geoInfo.getTimezone());
                loginRecord.setIsp(geoInfo.getIsp());
            }

        } catch (Exception e) {
            logger.debug("获取IP地理位置信息失败: {}", ipAddress, e);
        }
    }

    /**
     * 获取客户端真实IP地址
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
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 登录结果对象
     */
    public static class LoginResult {
        private final boolean success;
        private final User user;
        private final String token;
        private final LoginRecord loginRecord;
        private final String message;

        private LoginResult(boolean success, User user, String token, LoginRecord loginRecord, String message) {
            this.success = success;
            this.user = user;
            this.token = token;
            this.loginRecord = loginRecord;
            this.message = message;
        }

        public static LoginResult success(User user, String token, LoginRecord loginRecord) {
            return new LoginResult(true, user, token, loginRecord, "登录成功");
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, null, null, null, message);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public User getUser() {
            return user;
        }

        public String getToken() {
            return token;
        }

        public LoginRecord getLoginRecord() {
            return loginRecord;
        }

        public String getMessage() {
            return message;
        }
    }
}