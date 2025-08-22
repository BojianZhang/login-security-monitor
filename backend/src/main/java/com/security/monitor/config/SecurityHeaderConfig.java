package com.security.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全头配置
 */
@Configuration
public class SecurityHeaderConfig implements WebMvcConfigurer {

    @Bean
    public Filter securityHeaderFilter() {
        return new SecurityHeaderFilter();
    }

    public static class SecurityHeaderFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 防止XSS攻击
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // 防止点击劫持
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // 内容安全策略
            httpResponse.setHeader("Content-Security-Policy", 
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'");
            
            // 引用策略
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // 强制HTTPS
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            // 权限策略
            httpResponse.setHeader("Permissions-Policy", 
                "camera=(), microphone=(), geolocation=(), payment=()");
            
            chain.doFilter(request, response);
        }
    }
}