package com.security.monitor.service;

import com.security.monitor.model.LoginRecord;
import com.security.monitor.model.SecurityAlert;
import com.security.monitor.model.User;
import com.security.monitor.model.UserLocation;
import com.security.monitor.repository.LoginRecordRepository;
import com.security.monitor.repository.SecurityAlertRepository;
import com.security.monitor.repository.UserLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 异地登录检测服务
 */
@Service
@Transactional
public class AnomalousLoginDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalousLoginDetectionService.class);

    @Autowired
    private LoginRecordRepository loginRecordRepository;

    @Autowired
    private UserLocationRepository userLocationRepository;

    @Autowired
    private SecurityAlertRepository securityAlertRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${security.monitor.geo.max-distance-km:500}")
    private double maxDistanceKm;

    @Value("${security.monitor.risk.time-window-minutes:60}")
    private int timeWindowMinutes;

    @Value("${security.monitor.risk.threshold-high:70}")
    private int highRiskThreshold;

    /**
     * 检测异地登录
     */
    public void detectAnomalousLogin(LoginRecord loginRecord) {
        User user = loginRecord.getUser();
        logger.info("开始检测用户 {} 的异地登录行为", user.getUsername());

        try {
            // 计算风险评分
            int riskScore = calculateRiskScore(loginRecord);
            loginRecord.setRiskScore(riskScore);

            // 检测是否为可疑登录
            boolean isSuspicious = riskScore >= highRiskThreshold;
            loginRecord.setIsSuspicious(isSuspicious);

            // 更新用户位置信息
            updateUserLocation(loginRecord);

            // 如果是可疑登录，创建安全警报
            if (isSuspicious) {
                createSecurityAlert(loginRecord);
            }

            // 保存登录记录
            loginRecordRepository.save(loginRecord);

            logger.info("用户 {} 登录检测完成，风险评分: {}, 是否可疑: {}", 
                    user.getUsername(), riskScore, isSuspicious);

        } catch (Exception e) {
            logger.error("检测用户 {} 异地登录时发生错误", user.getUsername(), e);
        }
    }

    /**
     * 计算登录风险评分
     */
    private int calculateRiskScore(LoginRecord loginRecord) {
        int riskScore = 0;
        User user = loginRecord.getUser();

        // 1. 地理位置异常检测
        riskScore += calculateLocationRisk(loginRecord);

        // 2. 时间窗口内多地登录检测
        riskScore += calculateMultipleLocationRisk(loginRecord);

        // 3. 设备异常检测
        riskScore += calculateDeviceRisk(loginRecord);

        // 4. 登录频率异常检测
        riskScore += calculateFrequencyRisk(loginRecord);

        // 5. IP风险评估
        riskScore += calculateIpRisk(loginRecord);

        return Math.min(riskScore, 100); // 最大值限制为100
    }

    /**
     * 计算地理位置风险
     */
    private int calculateLocationRisk(LoginRecord loginRecord) {
        int risk = 0;
        User user = loginRecord.getUser();

        // 获取用户常用位置
        List<UserLocation> userLocations = userLocationRepository.findByUserOrderByLoginCountDesc(user);
        
        if (userLocations.isEmpty()) {
            // 新用户，风险较低
            return 10;
        }

        // 检查是否在常用位置附近
        boolean isNearKnownLocation = false;
        double minDistance = Double.MAX_VALUE;

        for (UserLocation location : userLocations) {
            if (loginRecord.getLatitude() != null && loginRecord.getLongitude() != null) {
                double distance = location.calculateDistance(
                    loginRecord.getLatitude(), 
                    loginRecord.getLongitude()
                );
                
                minDistance = Math.min(minDistance, distance);
                
                if (distance <= maxDistanceKm) {
                    isNearKnownLocation = true;
                    break;
                }
            }
        }

        if (!isNearKnownLocation) {
            // 不在常用位置附近
            if (minDistance > maxDistanceKm * 2) {
                risk += 40; // 距离很远
            } else if (minDistance > maxDistanceKm) {
                risk += 25; // 距离较远
            }
        }

        return risk;
    }

    /**
     * 计算时间窗口内多地登录风险
     */
    private int calculateMultipleLocationRisk(LoginRecord loginRecord) {
        int risk = 0;
        User user = loginRecord.getUser();
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(timeWindowMinutes);

        // 获取时间窗口内的登录记录
        List<LoginRecord> recentLogins = loginRecordRepository.findRecentLoginsByUser(user, timeWindow);

        if (recentLogins.size() > 1) {
            // 检查是否从多个地理位置登录
            List<Object[]> locations = loginRecordRepository.findDistinctLocationsByUserSince(user, timeWindow);
            
            if (locations.size() > 1) {
                risk += 30; // 多地登录
                
                // 计算最大距离
                double maxDistance = 0;
                for (LoginRecord record : recentLogins) {
                    if (record.getLatitude() != null && record.getLongitude() != null &&
                        loginRecord.getLatitude() != null && loginRecord.getLongitude() != null) {
                        
                        double distance = calculateDistance(
                            record.getLatitude(), record.getLongitude(),
                            loginRecord.getLatitude(), loginRecord.getLongitude()
                        );
                        maxDistance = Math.max(maxDistance, distance);
                    }
                }
                
                // 根据距离增加风险
                if (maxDistance > 1000) {
                    risk += 20; // 跨国登录
                } else if (maxDistance > 500) {
                    risk += 10; // 跨省登录
                }
            }
        }

        return risk;
    }

    /**
     * 计算设备风险
     */
    private int calculateDeviceRisk(LoginRecord loginRecord) {
        int risk = 0;
        User user = loginRecord.getUser();

        // 获取用户最近的登录记录
        List<LoginRecord> recentLogins = loginRecordRepository.findSuccessfulLoginsByUser(
            user, PageRequest.of(0, 10)
        );

        if (!recentLogins.isEmpty()) {
            boolean isKnownDevice = false;
            
            for (LoginRecord record : recentLogins) {
                if (isSimilarDevice(loginRecord, record)) {
                    isKnownDevice = true;
                    break;
                }
            }

            if (!isKnownDevice) {
                risk += 20; // 新设备
            }
        }

        return risk;
    }

    /**
     * 计算登录频率风险
     */
    private int calculateFrequencyRisk(LoginRecord loginRecord) {
        int risk = 0;
        User user = loginRecord.getUser();
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);

        // 统计过去一小时的登录次数
        long loginCount = loginRecordRepository.countLoginsByUserSince(user, hourAgo);

        if (loginCount > 10) {
            risk += 30; // 高频登录
        } else if (loginCount > 5) {
            risk += 15; // 频繁登录
        }

        return risk;
    }

    /**
     * 计算IP风险
     */
    private int calculateIpRisk(LoginRecord loginRecord) {
        int risk = 0;
        String ipAddress = loginRecord.getIpAddress();
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);

        // 统计该IP的登录次数
        long ipLoginCount = loginRecordRepository.countLoginsByIpSince(ipAddress, dayAgo);

        if (ipLoginCount > 50) {
            risk += 25; // 高频IP
        } else if (ipLoginCount > 20) {
            risk += 10; // 频繁IP
        }

        // 检查是否为已知的高风险IP（这里可以集成威胁情报）
        if (isHighRiskIp(ipAddress)) {
            risk += 35;
        }

        return risk;
    }

    /**
     * 更新用户位置信息
     */
    private void updateUserLocation(LoginRecord loginRecord) {
        if (loginRecord.getCountry() == null || loginRecord.getRegion() == null || 
            loginRecord.getCity() == null) {
            return;
        }

        User user = loginRecord.getUser();
        
        Optional<UserLocation> existingLocation = userLocationRepository
            .findByUserAndCountryAndRegionAndCity(
                user, 
                loginRecord.getCountry(),
                loginRecord.getRegion(),
                loginRecord.getCity()
            );

        if (existingLocation.isPresent()) {
            // 更新现有位置
            UserLocation location = existingLocation.get();
            location.incrementLoginCount();
            location.setLatitude(loginRecord.getLatitude());
            location.setLongitude(loginRecord.getLongitude());
            userLocationRepository.save(location);
        } else {
            // 创建新位置
            UserLocation newLocation = new UserLocation(
                user,
                loginRecord.getCountry(),
                loginRecord.getRegion(),
                loginRecord.getCity(),
                loginRecord.getLatitude(),
                loginRecord.getLongitude()
            );
            userLocationRepository.save(newLocation);
        }
    }

    /**
     * 创建安全警报
     */
    private void createSecurityAlert(LoginRecord loginRecord) {
        User user = loginRecord.getUser();
        
        SecurityAlert alert = new SecurityAlert();
        alert.setUser(user);
        alert.setLoginRecord(loginRecord);
        alert.setRiskScore(loginRecord.getRiskScore());
        
        // 确定警报类型和严重程度
        if (loginRecord.getRiskScore() >= 90) {
            alert.setSeverity(SecurityAlert.Severity.CRITICAL);
            alert.setAlertType(SecurityAlert.AlertType.HIGH_RISK_IP);
            alert.setTitle("检测到极高风险登录活动");
        } else if (loginRecord.getRiskScore() >= 70) {
            alert.setSeverity(SecurityAlert.Severity.HIGH);
            alert.setAlertType(SecurityAlert.AlertType.ANOMALOUS_LOCATION);
            alert.setTitle("检测到异地登录活动");
        } else {
            alert.setSeverity(SecurityAlert.Severity.MEDIUM);
            alert.setAlertType(SecurityAlert.AlertType.SUSPICIOUS_DEVICE);
            alert.setTitle("检测到可疑登录活动");
        }

        // 设置描述
        alert.setDescription(buildAlertDescription(loginRecord));

        // 保存警报
        securityAlertRepository.save(alert);

        // 发送通知
        notificationService.sendSecurityAlert(alert);

        logger.info("为用户 {} 创建了安全警报: {}", user.getUsername(), alert.getTitle());
    }

    /**
     * 构建警报描述
     */
    private String buildAlertDescription(LoginRecord loginRecord) {
        StringBuilder description = new StringBuilder();
        description.append("用户 ").append(loginRecord.getUser().getUsername())
                  .append(" 在 ").append(loginRecord.getLoginTime())
                  .append(" 从以下位置登录:\n");
        
        if (loginRecord.getCountry() != null) {
            description.append("国家/地区: ").append(loginRecord.getCountry()).append("\n");
        }
        if (loginRecord.getRegion() != null) {
            description.append("省/州: ").append(loginRecord.getRegion()).append("\n");
        }
        if (loginRecord.getCity() != null) {
            description.append("城市: ").append(loginRecord.getCity()).append("\n");
        }
        
        description.append("IP地址: ").append(loginRecord.getIpAddress()).append("\n");
        description.append("风险评分: ").append(loginRecord.getRiskScore()).append("/100\n");
        
        if (loginRecord.getUserAgent() != null) {
            description.append("设备信息: ").append(loginRecord.getUserAgent());
        }

        return description.toString();
    }

    /**
     * 检查是否为相似设备
     */
    private boolean isSimilarDevice(LoginRecord current, LoginRecord previous) {
        if (current.getUserAgent() == null || previous.getUserAgent() == null) {
            return false;
        }

        // 简单的设备相似度检测
        return current.getBrowser() != null && current.getBrowser().equals(previous.getBrowser()) &&
               current.getOs() != null && current.getOs().equals(previous.getOs());
    }

    /**
     * 检查是否为高风险IP
     */
    private boolean isHighRiskIp(String ipAddress) {
        // 这里可以集成威胁情报数据库
        // 暂时返回false，实际实现中可以查询威胁情报API
        return false;
    }

    /**
     * 计算两点间距离
     */
    private double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0;
        }

        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lonDistance = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) 
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371 * c; // 地球半径6371公里
    }
}