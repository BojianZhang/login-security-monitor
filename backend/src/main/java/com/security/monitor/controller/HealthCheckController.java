package com.security.monitor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查控制器
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 系统整体健康检查
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        boolean isHealthy = true;

        // 检查数据库连接
        Map<String, Object> dbHealth = checkDatabase();
        health.put("database", dbHealth);
        if (!"UP".equals(dbHealth.get("status"))) {
            isHealthy = false;
        }

        // 检查Redis连接
        Map<String, Object> redisHealth = checkRedis();
        health.put("redis", redisHealth);
        if (!"UP".equals(redisHealth.get("status"))) {
            isHealthy = false;
        }

        // 检查应用状态
        health.put("application", Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        ));

        health.put("overall", isHealthy ? "UP" : "DOWN");

        return isHealthy ? 
            ResponseEntity.ok(health) : 
            ResponseEntity.status(503).body(health);
    }

    /**
     * 数据库健康检查
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> health = checkDatabase();
        return "UP".equals(health.get("status")) ? 
            ResponseEntity.ok(health) : 
            ResponseEntity.status(503).body(health);
    }

    /**
     * Redis健康检查
     */
    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> redisHealth() {
        Map<String, Object> health = checkRedis();
        return "UP".equals(health.get("status")) ? 
            ResponseEntity.ok(health) : 
            ResponseEntity.status(503).body(health);
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> health = new HashMap<>();
        try {
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(5); // 5秒超时
            connection.close();
            
            if (isValid) {
                health.put("status", "UP");
                health.put("database", "MySQL");
            } else {
                health.put("status", "DOWN");
                health.put("error", "Database connection invalid");
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> health = new HashMap<>();
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            String pong = connection.ping();
            connection.close();
            
            if ("PONG".equals(pong)) {
                health.put("status", "UP");
                health.put("redis", "Connected");
            } else {
                health.put("status", "DOWN");
                health.put("error", "Redis ping failed");
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }
}