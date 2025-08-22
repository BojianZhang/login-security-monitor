package com.security.monitor.service;

import com.security.monitor.model.SecurityAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 通知服务 - 增强版安全通知
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.security.notification.enabled:true}")
    private boolean notificationEnabled;

    @Value("${app.security.admin-emails:admin@example.com}")
    private List<String> adminEmails;

    @Value("${app.mail.from:Security Monitor <noreply@example.com>}")
    private String fromEmail;

    /**
     * 发送安全警报通知
     */
    @Async("notificationExecutor")
    public void sendSecurityAlert(SecurityAlert alert) {
        if (!notificationEnabled) {
            logger.info("通知功能已禁用，跳过发送警报通知");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String[] recipients = adminEmails.toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(fromEmail);
            helper.setSubject("🚨 安全警报 - " + getSeverityEmoji(alert.getSeverity()) + " " + alert.getAlertType());
            
            String htmlContent = buildSecurityAlertEmail(alert);
            helper.setText(htmlContent, true);
            
            // 根据严重程度设置邮件优先级
            if (alert.getSeverity() == SecurityAlert.Severity.CRITICAL || alert.getSeverity() == SecurityAlert.Severity.HIGH) {
                message.setHeader("X-Priority", "1");
                message.setHeader("Importance", "High");
            }
            
            mailSender.send(message);
            logger.info("安全警报通知已发送: {} - {}", alert.getAlertType(), alert.getSeverity());
            
        } catch (Exception e) {
            logger.error("发送安全警报通知失败", e);
        }
    }

    /**
     * 发送安全警报（字符串参数版本）
     */
    @Async("notificationExecutor")
    public void sendSecurityAlert(String subject, String content) {
        if (!notificationEnabled) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String[] recipients = adminEmails.toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(fromEmail);
            helper.setSubject("🚨 安全警报 - " + subject);
            
            String htmlContent = buildSimpleAlertEmail(subject, content);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("安全警报通知已发送: {}", subject);
            
        } catch (Exception e) {
            logger.error("发送安全警报通知失败", e);
        }
    }

    /**
     * 发送紧急安全警报
     */
    @Async("notificationExecutor")
    public void sendEmergencyAlert(String subject, String content) {
        logger.warn("🚨 发送紧急安全警报: {}", subject);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 设置收件人（所有管理员）
            String[] recipients = adminEmails.toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(fromEmail);
            helper.setSubject("🚨 紧急安全警报 - " + subject);
            
            // 构建紧急警报邮件内容
            String htmlContent = buildEmergencyAlertEmail(subject, content);
            helper.setText(htmlContent, true);
            
            // 设置高优先级
            message.setHeader("X-Priority", "1");
            message.setHeader("Importance", "High");
            
            mailSender.send(message);
            logger.info("紧急安全警报已发送给 {} 位管理员", recipients.length);
            
        } catch (Exception e) {
            logger.error("发送紧急安全警报失败", e);
        }
    }

    /**
     * 发送备份完成通知
     */
    @Async("notificationExecutor")
    public void sendBackupNotification(String subject, String content) {
        logger.info("📧 发送备份通知: {}", subject);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String[] recipients = adminEmails.toArray(new String[0]);
            helper.setTo(recipients);
            helper.setFrom(fromEmail);
            helper.setSubject("📦 数据备份通知 - " + subject);
            
            String htmlContent = buildBackupNotificationEmail(subject, content);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("备份通知已发送");
            
        } catch (Exception e) {
            logger.error("发送备份通知失败", e);
        }
    }

    private String buildSecurityAlertEmail(SecurityAlert alert) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        String severityColor = getSeverityColor(alert.getSeverity());
        String severityEmoji = getSeverityEmoji(alert.getSeverity());
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>安全警报</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, %s, %s); color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .alert-icon { font-size: 48px; margin-bottom: 10px; }
                    .content { padding: 30px 20px; }
                    .alert-box { background: #fff5f5; border: 2px solid #fed7d7; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .alert-box h2 { color: #c53030; margin-top: 0; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table th, .info-table td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
                    .info-table th { background-color: #f8f9fa; font-weight: bold; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; border-top: 1px solid #eee; }
                    .severity-%s { color: %s; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="alert-icon">%s</div>
                        <h1>安全警报</h1>
                        <p>检测到安全威胁事件</p>
                    </div>
                    
                    <div class="content">
                        <div class="alert-box">
                            <h2>⚠️ 警报详情</h2>
                            <p><strong>警报类型：</strong>%s</p>
                            <p><strong>严重程度：</strong><span class="severity-%s">%s %s</span></p>
                            <p><strong>警报消息：</strong>%s</p>
                        </div>
                        
                        <table class="info-table">
                            <tr>
                                <th>用户</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>发生时间</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>当前状态</th>
                                <td>%s</td>
                            </tr>
                        </table>
                        
                        %s
                    </div>
                    
                    <div class="footer">
                        <p>此邮件由登录安全监控系统自动发送</p>
                        <p>如需处理此警报，请登录管理后台</p>
                        <p style="font-size: 12px; color: #999;">
                            Login Security Monitor - Security Alert System<br>
                            Alert ID: %d | Generated at %s
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            severityColor, darkenColor(severityColor), 
            alert.getSeverity().name().toLowerCase(), severityColor,
            severityEmoji,
            getAlertTypeName(alert.getAlertType()),
            alert.getSeverity().name().toLowerCase(), severityEmoji, alert.getSeverity(),
            alert.getMessage(),
            alert.getUser().getUsername(),
            alert.getCreatedAt().format(formatter),
            getStatusName(alert.getStatus()),
            alert.getDetails() != null ? "<div style='margin-top: 15px;'><strong>详细信息：</strong><br>" + alert.getDetails() + "</div>" : "",
            alert.getId(),
            now.format(formatter)
        );
    }

    private String buildEmergencyAlertEmail(String subject, String content) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>紧急安全警报</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #ff4757, #ff3838); color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .alert-icon { font-size: 48px; margin-bottom: 10px; animation: pulse 2s infinite; }
                    .content { padding: 30px 20px; }
                    .alert-box { background: #fff5f5; border: 2px solid #fed7d7; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .alert-box h2 { color: #c53030; margin-top: 0; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table th, .info-table td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
                    .info-table th { background-color: #f8f9fa; font-weight: bold; }
                    .action-required { background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 6px; padding: 15px; margin: 20px 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; border-top: 1px solid #eee; }
                    @keyframes pulse { 0%% { transform: scale(1); } 50%% { transform: scale(1.1); } 100%% { transform: scale(1); } }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="alert-icon">🚨</div>
                        <h1>紧急安全警报</h1>
                        <p>系统检测到严重安全威胁</p>
                    </div>
                    
                    <div class="content">
                        <div class="alert-box">
                            <h2>⚠️ 警报详情</h2>
                            <p><strong>警报主题：</strong>%s</p>
                            <p><strong>详细描述：</strong>%s</p>
                        </div>
                        
                        <table class="info-table">
                            <tr>
                                <th>发生时间</th>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <th>系统状态</th>
                                <td><span style="color: #e74c3c; font-weight: bold;">紧急模式已激活</span></td>
                            </tr>
                            <tr>
                                <th>数据保护</th>
                                <td><span style="color: #27ae60; font-weight: bold;">自动备份已执行</span></td>
                            </tr>
                        </table>
                        
                        <div class="action-required">
                            <h3>🔧 需要立即处理的操作：</h3>
                            <ul>
                                <li>立即检查系统安全日志</li>
                                <li>验证最新数据备份完整性</li>
                                <li>评估攻击影响范围</li>
                                <li>必要时联系安全团队</li>
                                <li>确认系统恢复正常后退出紧急模式</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>此邮件由登录安全监控系统自动发送</p>
                        <p>如需帮助，请联系系统管理员</p>
                        <p style="font-size: 12px; color: #999;">
                            Login Security Monitor - Emergency Alert System<br>
                            Generated at %s
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, subject, content, now.format(formatter), now.format(formatter));
    }

    private String buildBackupNotificationEmail(String subject, String content) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>数据备份通知</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #00b894, #00a085); color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .backup-icon { font-size: 48px; margin-bottom: 10px; }
                    .content { padding: 30px 20px; }
                    .success-box { background: #f0f9ff; border: 2px solid #bfdbfe; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .success-box h2 { color: #1e40af; margin-top: 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="backup-icon">📦</div>
                        <h1>数据备份通知</h1>
                        <p>系统数据备份操作完成</p>
                    </div>
                    
                    <div class="content">
                        <div class="success-box">
                            <h2>✅ 备份完成</h2>
                            <p><strong>备份类型：</strong>%s</p>
                            <p><strong>备份详情：</strong>%s</p>
                            <p><strong>完成时间：</strong>%s</p>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>此邮件由登录安全监控系统自动发送</p>
                        <p style="font-size: 12px; color: #999;">
                            Login Security Monitor - Backup Notification<br>
                            Generated at %s
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, subject, content, now.format(formatter), now.format(formatter));
    }

    private String buildSimpleAlertEmail(String subject, String content) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>安全警报</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #e17055, #d63031); color: white; padding: 30px 20px; text-align: center; }
                    .content { padding: 30px 20px; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚨 安全警报</h1>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <p>%s</p>
                        <p><strong>时间：</strong>%s</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由登录安全监控系统自动发送</p>
                    </div>
                </div>
            </body>
            </html>
            """, subject, content, now.format(formatter));
    }

    private String getSeverityColor(SecurityAlert.Severity severity) {
        return switch (severity) {
            case CRITICAL -> "#dc2626";
            case HIGH -> "#ea580c";
            case MEDIUM -> "#d97706";
            case LOW -> "#059669";
        };
    }

    private String getSeverityEmoji(SecurityAlert.Severity severity) {
        return switch (severity) {
            case CRITICAL -> "🔴";
            case HIGH -> "🟠";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
        };
    }

    private String darkenColor(String color) {
        // 简单的颜色加深逻辑
        return color.replace("#dc2626", "#991b1b")
                   .replace("#ea580c", "#c2410c")
                   .replace("#d97706", "#b45309")
                   .replace("#059669", "#047857");
    }

    private String getAlertTypeName(SecurityAlert.AlertType alertType) {
        return switch (alertType) {
            case LOCATION_ANOMALY -> "异地登录";
            case MULTIPLE_LOCATIONS -> "多地登录";
            case HIGH_RISK_SCORE -> "高风险登录";
            case SUSPICIOUS_DEVICE -> "可疑设备";
            case BRUTE_FORCE -> "暴力破解";
        };
    }

    private String getStatusName(SecurityAlert.Status status) {
        return switch (status) {
            case OPEN -> "待处理";
            case ACKNOWLEDGED -> "已确认";
            case RESOLVED -> "已解决";
            case FALSE_POSITIVE -> "误报";
        };
    }
}
                sendAlertEmail(alert, recipient);
            }

            logger.info("安全警报通知已发送，警报ID: {}", alert.getId());

        } catch (Exception e) {
            logger.error("发送安全警报通知时发生错误，警报ID: {}", alert.getId(), e);
        }
    }

    /**
     * 发送警报邮件
     */
    private void sendAlertEmail(SecurityAlert alert, String recipient) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject(buildAlertSubject(alert));
            message.setText(buildAlertMessage(alert));

            mailSender.send(message);
            logger.debug("警报邮件已发送到: {}", recipient);

        } catch (Exception e) {
            logger.error("发送警报邮件失败，收件人: {}", recipient, e);
        }
    }

    /**
     * 构建警报邮件主题
     */
    private String buildAlertSubject(SecurityAlert alert) {
        return String.format("[安全警报] %s - %s", 
            alert.getSeverity(), 
            alert.getTitle()
        );
    }

    /**
     * 构建警报邮件内容
     */
    private String buildAlertMessage(SecurityAlert alert) {
        StringBuilder message = new StringBuilder();
        
        message.append("检测到安全警报\n\n");
        
        message.append("警报详情:\n");
        message.append("用户: ").append(alert.getUser().getUsername()).append("\n");
        message.append("类型: ").append(alert.getAlertType()).append("\n");
        message.append("严重程度: ").append(alert.getSeverity()).append("\n");
        message.append("风险评分: ").append(alert.getRiskScore()).append("/100\n");
        message.append("创建时间: ").append(
            alert.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ).append("\n\n");
        
        message.append("描述:\n");
        message.append(alert.getDescription()).append("\n\n");
        
        if (alert.getLoginRecord() != null) {
            message.append("登录信息:\n");
            message.append("IP地址: ").append(alert.getLoginRecord().getIpAddress()).append("\n");
            
            if (alert.getLoginRecord().getCountry() != null) {
                message.append("位置: ")
                       .append(alert.getLoginRecord().getCity()).append(", ")
                       .append(alert.getLoginRecord().getRegion()).append(", ")
                       .append(alert.getLoginRecord().getCountry()).append("\n");
            }
            
            if (alert.getLoginRecord().getUserAgent() != null) {
                message.append("设备: ").append(alert.getLoginRecord().getUserAgent()).append("\n");
            }
        }
        
        message.append("\n请及时处理此安全警报。\n");
        message.append("登录系统查看详细信息: http://localhost:8081/alerts/").append(alert.getId());
        
        return message.toString();
    }

    /**
     * 获取管理员邮箱列表
     */
    private List<String> getAdminEmails() {
        return Arrays.asList(adminEmails.split(","));
    }

    /**
     * 发送测试通知
     */
    @Async
    public void sendTestNotification(String recipient) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject("[测试] 登录安全监控系统通知测试");
            message.setText("这是一条测试通知消息。\n\n如果您收到此消息，说明通知系统工作正常。\n\n发送时间: " 
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            mailSender.send(message);
            logger.info("测试通知已发送到: {}", recipient);

        } catch (Exception e) {
            logger.error("发送测试通知失败，收件人: {}", recipient, e);
            throw new RuntimeException("发送测试通知失败: " + e.getMessage());
        }
    }

    /**
     * 批量发送周期性报告
     */
    @Async
    public void sendPeriodicReport(String reportContent) {
        if (!notificationEnabled) {
            return;
        }

        try {
            List<String> recipients = getAdminEmails();
            
            for (String recipient : recipients) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipient);
                message.setSubject("[安全报告] 登录安全监控周期报告");
                message.setText(reportContent);

                mailSender.send(message);
            }

            logger.info("周期性安全报告已发送");

        } catch (Exception e) {
            logger.error("发送周期性报告时发生错误", e);
        }
    }
}