package com.security.monitor.config;

import com.security.monitor.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 检查Authorization头是否存在且以Bearer开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取JWT令牌
        jwt = authHeader.substring(7);
        
        try {
            // 从JWT中提取用户名
            username = jwtService.extractUsername(jwt);

            // 如果用户名存在且当前没有认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 加载用户详细信息
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // 验证JWT令牌
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    
                    // 创建认证令牌
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // 设置认证详细信息
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 将认证信息设置到SecurityContext中
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    logger.debug("用户 {} 通过JWT认证", username);
                }
            }
        } catch (Exception e) {
            logger.debug("JWT认证失败: {}", e.getMessage());
            // 不需要抛出异常，让请求继续处理
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 对于登录和其他公开端点，不进行JWT过滤
        return path.equals("/auth/login") || 
               path.equals("/auth/register") ||
               path.startsWith("/public/") ||
               path.equals("/error") ||
               path.equals("/") ||
               path.startsWith("/actuator/health");
    }
}