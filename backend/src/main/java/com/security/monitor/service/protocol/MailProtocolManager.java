package com.security.monitor.service.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 邮件协议服务管理器
 * 统一管理SMTP、IMAP、POP3服务器的启动、监控和优化
 */
@Service
public class MailProtocolManager implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MailProtocolManager.class);
    
    @Autowired
    private OptimizedSmtpServer smtpServer;
    
    @Autowired
    private OptimizedImapServer imapServer;
    
    @Autowired
    private OptimizedPop3Server pop3Server;
    
    private ScheduledExecutorService monitoringExecutor;
    private volatile boolean running = false;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("启动邮件协议服务管理器...");
        
        // 创建监控线程池
        monitoringExecutor = Executors.newScheduledThreadPool(3);
        
        // 异步启动各个服务器
        CompletableFuture<Void> smtpFuture = CompletableFuture.runAsync(() -> {
            try {
                smtpServer.start();
                logger.info("SMTP服务器启动成功");
            } catch (Exception e) {
                logger.error("SMTP服务器启动失败", e);
            }
        });
        
        CompletableFuture<Void> imapFuture = CompletableFuture.runAsync(() -> {
            try {
                imapServer.start();
                logger.info("IMAP服务器启动成功");
            } catch (Exception e) {
                logger.error("IMAP服务器启动失败", e);
            }
        });
        
        CompletableFuture<Void> pop3Future = CompletableFuture.runAsync(() -> {
            try {
                pop3Server.start();
                logger.info("POP3服务器启动成功");
            } catch (Exception e) {
                logger.error("POP3服务器启动失败", e);
            }
        });
        
        // 等待所有服务器启动完成
        CompletableFuture.allOf(smtpFuture, imapFuture, pop3Future)
            .thenRun(() -> {
                running = true;
                startMonitoring();
                logger.info("所有邮件协议服务器启动完成");
            })
            .exceptionally(throwable -> {
                logger.error("邮件协议服务器启动失败", throwable);
                return null;
            });
    }
    
    /**
     * 启动服务器监控
     */
    private void startMonitoring() {
        // 定期监控服务器状态
        monitoringExecutor.scheduleWithFixedDelay(this::monitorServerStatus, 30, 30, TimeUnit.SECONDS);
        
        // 定期报告性能统计
        monitoringExecutor.scheduleWithFixedDelay(this::reportPerformanceStats, 5, 5, TimeUnit.MINUTES);
        
        // 定期清理空闲连接
        monitoringExecutor.scheduleWithFixedDelay(this::cleanupIdleConnections, 10, 10, TimeUnit.MINUTES);
        
        logger.info("邮件协议服务监控已启动");
    }
    
    /**
     * 监控服务器状态
     */
    private void monitorServerStatus() {
        try {
            // 监控SMTP服务器
            OptimizedSmtpServer.SmtpServerStatus smtpStatus = smtpServer.getStatus();
            if (!smtpStatus.isRunning()) {
                logger.warn("SMTP服务器未运行，尝试重启...");
                restartSmtpServer();
            }
            
            // 监控IMAP服务器
            OptimizedImapServer.ImapServerStatus imapStatus = imapServer.getStatus();
            if (!imapStatus.isRunning()) {
                logger.warn("IMAP服务器未运行，尝试重启...");
                restartImapServer();
            }
            
            // 监控POP3服务器
            OptimizedPop3Server.Pop3ServerStatus pop3Status = pop3Server.getStatus();
            if (!pop3Status.isRunning()) {
                logger.warn("POP3服务器未运行，尝试重启...");
                restartPop3Server();
            }
            
            // 检查连接数是否接近限制
            checkConnectionLimits(smtpStatus, imapStatus, pop3Status);
            
        } catch (Exception e) {
            logger.error("监控服务器状态时发生错误", e);
        }
    }
    
    /**
     * 检查连接数限制
     */
    private void checkConnectionLimits(OptimizedSmtpServer.SmtpServerStatus smtpStatus,
                                     OptimizedImapServer.ImapServerStatus imapStatus,
                                     OptimizedPop3Server.Pop3ServerStatus pop3Status) {
        
        // SMTP连接数检查
        double smtpUsage = (double) smtpStatus.getActiveConnections() / smtpStatus.getMaxConnections();
        if (smtpUsage > 0.8) {
            logger.warn("SMTP服务器连接数接近限制: {}/{} ({}%)", 
                       smtpStatus.getActiveConnections(), 
                       smtpStatus.getMaxConnections(), 
                       Math.round(smtpUsage * 100));
        }
        
        // IMAP连接数检查
        double imapUsage = (double) imapStatus.getActiveConnections() / imapStatus.getMaxConnections();
        if (imapUsage > 0.8) {
            logger.warn("IMAP服务器连接数接近限制: {}/{} ({}%)", 
                       imapStatus.getActiveConnections(), 
                       imapStatus.getMaxConnections(), 
                       Math.round(imapUsage * 100));
        }
        
        // POP3连接数检查
        double pop3Usage = (double) pop3Status.getActiveConnections() / pop3Status.getMaxConnections();
        if (pop3Usage > 0.8) {
            logger.warn("POP3服务器连接数接近限制: {}/{} ({}%)", 
                       pop3Status.getActiveConnections(), 
                       pop3Status.getMaxConnections(), 
                       Math.round(pop3Usage * 100));
        }
    }
    
    /**
     * 报告性能统计
     */
    private void reportPerformanceStats() {
        try {
            OptimizedSmtpServer.SmtpServerStatus smtpStatus = smtpServer.getStatus();
            OptimizedImapServer.ImapServerStatus imapStatus = imapServer.getStatus();
            OptimizedPop3Server.Pop3ServerStatus pop3Status = pop3Server.getStatus();
            
            logger.info("=== 邮件协议服务器性能统计 ===");
            logger.info("SMTP: 运行状态={}, 活动连接={}/{}, 端口={}/{}/{}", 
                       smtpStatus.isRunning(),
                       smtpStatus.getActiveConnections(),
                       smtpStatus.getMaxConnections(),
                       smtpStatus.getSmtpPort(),
                       smtpStatus.getSslPort(),
                       smtpStatus.getSubmissionPort());
            
            logger.info("IMAP: 运行状态={}, 活动连接={}/{}, 端口={}/{}, IDLE={}", 
                       imapStatus.isRunning(),
                       imapStatus.getActiveConnections(),
                       imapStatus.getMaxConnections(),
                       imapStatus.getImapPort(),
                       imapStatus.getSslPort(),
                       imapStatus.isIdleEnabled());
            
            logger.info("POP3: 运行状态={}, 活动连接={}/{}, 端口={}/{}, 删除模式={}", 
                       pop3Status.isRunning(),
                       pop3Status.getActiveConnections(),
                       pop3Status.getMaxConnections(),
                       pop3Status.getPop3Port(),
                       pop3Status.getSslPort(),
                       pop3Status.isDeleteOnRetr());
            
            logger.info("==============================");
            
        } catch (Exception e) {
            logger.error("报告性能统计时发生错误", e);
        }
    }
    
    /**
     * 清理空闲连接
     */
    private void cleanupIdleConnections() {
        try {
            // 这里可以实现空闲连接清理逻辑
            // 实际实现需要在各个服务器中添加连接时间跟踪
            logger.debug("执行空闲连接清理");
            
        } catch (Exception e) {
            logger.error("清理空闲连接时发生错误", e);
        }
    }
    
    /**
     * 重启SMTP服务器
     */
    private void restartSmtpServer() {
        try {
            logger.info("重启SMTP服务器...");
            smtpServer.stop();
            Thread.sleep(2000); // 等待2秒
            smtpServer.start();
            logger.info("SMTP服务器重启成功");
        } catch (Exception e) {
            logger.error("重启SMTP服务器失败", e);
        }
    }
    
    /**
     * 重启IMAP服务器
     */
    private void restartImapServer() {
        try {
            logger.info("重启IMAP服务器...");
            imapServer.stop();
            Thread.sleep(2000); // 等待2秒
            imapServer.start();
            logger.info("IMAP服务器重启成功");
        } catch (Exception e) {
            logger.error("重启IMAP服务器失败", e);
        }
    }
    
    /**
     * 重启POP3服务器
     */
    private void restartPop3Server() {
        try {
            logger.info("重启POP3服务器...");
            pop3Server.stop();
            Thread.sleep(2000); // 等待2秒
            pop3Server.start();
            logger.info("POP3服务器重启成功");
        } catch (Exception e) {
            logger.error("重启POP3服务器失败", e);
        }
    }
    
    /**
     * 获取总体状态
     */
    public MailProtocolStatus getOverallStatus() {
        MailProtocolStatus status = new MailProtocolStatus();
        status.setRunning(running);
        
        try {
            status.setSmtpStatus(smtpServer.getStatus());
            status.setImapStatus(imapServer.getStatus());
            status.setPop3Status(pop3Server.getStatus());
            
            // 计算总连接数
            int totalConnections = status.getSmtpStatus().getActiveConnections() +
                                 status.getImapStatus().getActiveConnections() +
                                 status.getPop3Status().getActiveConnections();
            status.setTotalActiveConnections(totalConnections);
            
            int totalMaxConnections = status.getSmtpStatus().getMaxConnections() +
                                    status.getImapStatus().getMaxConnections() +
                                    status.getPop3Status().getMaxConnections();
            status.setTotalMaxConnections(totalMaxConnections);
            
        } catch (Exception e) {
            logger.error("获取总体状态时发生错误", e);
        }
        
        return status;
    }
    
    /**
     * 优雅关闭所有服务器
     */
    @PreDestroy
    public void shutdown() {
        logger.info("关闭邮件协议服务管理器...");
        running = false;
        
        try {
            // 停止监控
            if (monitoringExecutor != null) {
                monitoringExecutor.shutdown();
                if (!monitoringExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    monitoringExecutor.shutdownNow();
                }
            }
            
            // 并行关闭所有服务器
            CompletableFuture<Void> smtpShutdown = CompletableFuture.runAsync(() -> {
                try {
                    smtpServer.stop();
                    logger.info("SMTP服务器已停止");
                } catch (Exception e) {
                    logger.error("停止SMTP服务器时发生错误", e);
                }
            });
            
            CompletableFuture<Void> imapShutdown = CompletableFuture.runAsync(() -> {
                try {
                    imapServer.stop();
                    logger.info("IMAP服务器已停止");
                } catch (Exception e) {
                    logger.error("停止IMAP服务器时发生错误", e);
                }
            });
            
            CompletableFuture<Void> pop3Shutdown = CompletableFuture.runAsync(() -> {
                try {
                    pop3Server.stop();
                    logger.info("POP3服务器已停止");
                } catch (Exception e) {
                    logger.error("停止POP3服务器时发生错误", e);
                }
            });
            
            // 等待所有服务器关闭
            CompletableFuture.allOf(smtpShutdown, imapShutdown, pop3Shutdown)
                .get(30, TimeUnit.SECONDS);
            
            logger.info("所有邮件协议服务器已关闭");
            
        } catch (Exception e) {
            logger.error("关闭邮件协议服务器时发生错误", e);
        }
    }
    
    /**
     * 邮件协议总体状态
     */
    public static class MailProtocolStatus {
        private boolean running;
        private OptimizedSmtpServer.SmtpServerStatus smtpStatus;
        private OptimizedImapServer.ImapServerStatus imapStatus;
        private OptimizedPop3Server.Pop3ServerStatus pop3Status;
        private int totalActiveConnections;
        private int totalMaxConnections;
        
        // Getters and Setters
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }
        
        public OptimizedSmtpServer.SmtpServerStatus getSmtpStatus() { return smtpStatus; }
        public void setSmtpStatus(OptimizedSmtpServer.SmtpServerStatus smtpStatus) { this.smtpStatus = smtpStatus; }
        
        public OptimizedImapServer.ImapServerStatus getImapStatus() { return imapStatus; }
        public void setImapStatus(OptimizedImapServer.ImapServerStatus imapStatus) { this.imapStatus = imapStatus; }
        
        public OptimizedPop3Server.Pop3ServerStatus getPop3Status() { return pop3Status; }
        public void setPop3Status(OptimizedPop3Server.Pop3ServerStatus pop3Status) { this.pop3Status = pop3Status; }
        
        public int getTotalActiveConnections() { return totalActiveConnections; }
        public void setTotalActiveConnections(int totalActiveConnections) { this.totalActiveConnections = totalActiveConnections; }
        
        public int getTotalMaxConnections() { return totalMaxConnections; }
        public void setTotalMaxConnections(int totalMaxConnections) { this.totalMaxConnections = totalMaxConnections; }
    }
}