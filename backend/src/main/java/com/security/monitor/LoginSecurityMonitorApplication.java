package com.security.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业级安全邮件管理系统主应用类
 * 集成登录安全监控和邮件服务管理
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class LoginSecurityMonitorApplication {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  企业级安全邮件管理系统启动中...");
        System.out.println("  Enterprise Secure Email System");
        System.out.println("========================================");
        SpringApplication.run(LoginSecurityMonitorApplication.class, args);
    }
}