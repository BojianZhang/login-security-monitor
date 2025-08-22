package com.security.monitor.service.protocol;

import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.User;
import com.security.monitor.service.EmailService;
import com.security.monitor.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.*;

/**
 * 优化的SMTP服务器实现
 * 支持连接池、SSL/TLS、认证和反垃圾邮件
 */
@Service
public class OptimizedSmtpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedSmtpServer.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SecurityService securityService;
    
    @Value("${smtp.port:25}")
    private int smtpPort;
    
    @Value("${smtp.ssl.port:465}")
    private int smtpSslPort;
    
    @Value("${smtp.submission.port:587}")
    private int submissionPort;
    
    @Value("${smtp.max-connections:100}")
    private int maxConnections;
    
    @Value("${smtp.connection-timeout:300000}") // 5 minutes
    private int connectionTimeout;
    
    @Value("${smtp.max-message-size:26214400}") // 25MB
    private long maxMessageSize;
    
    @Value("${smtp.enable-auth:true}")
    private boolean enableAuth;
    
    @Value("${smtp.require-tls:false}")
    private boolean requireTls;
    
    private ServerSocket serverSocket;
    private ServerSocket sslServerSocket;
    private ServerSocket submissionServerSocket;
    private ExecutorService connectionPool;
    private final ConcurrentHashMap<String, SmtpConnection> activeConnections = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    
    /**
     * 启动SMTP服务器
     */
    public void start() throws IOException {
        logger.info("启动SMTP服务器...");
        
        // 创建连接池
        connectionPool = Executors.newFixedThreadPool(maxConnections);
        
        // 启动标准SMTP服务器 (端口25)
        serverSocket = new ServerSocket(smtpPort);
        startServerListener(serverSocket, false, "SMTP");
        
        // 启动SSL SMTP服务器 (端口465)
        SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslServerSocket = sslFactory.createServerSocket(smtpSslPort);
        startServerListener(sslServerSocket, true, "SMTPS");
        
        // 启动邮件提交服务器 (端口587)
        submissionServerSocket = new ServerSocket(submissionPort);
        startServerListener(submissionServerSocket, false, "Submission");
        
        running = true;
        logger.info("SMTP服务器启动完成 - 端口: {}, {}, {}", smtpPort, smtpSslPort, submissionPort);
    }
    
    /**
     * 启动服务器监听器
     */
    private void startServerListener(ServerSocket socket, boolean isSSL, String serverType) {
        connectionPool.submit(() -> {
            logger.info("{} 服务器监听端口: {}", serverType, socket.getLocalPort());
            
            while (running && !socket.isClosed()) {
                try {
                    Socket clientSocket = socket.accept();
                    
                    // 检查连接数限制
                    if (activeConnections.size() >= maxConnections) {
                        logger.warn("达到最大连接数限制，拒绝连接: {}", clientSocket.getRemoteSocketAddress());
                        clientSocket.close();
                        continue;
                    }
                    
                    // 创建SMTP连接处理器
                    SmtpConnection connection = new SmtpConnection(clientSocket, isSSL, serverType);
                    activeConnections.put(connection.getId(), connection);
                    
                    // 提交到线程池处理
                    connectionPool.submit(connection);
                    
                } catch (IOException e) {
                    if (running) {
                        logger.error(serverType + " 服务器接受连接时发生错误", e);
                    }
                }
            }
        });
    }
    
    /**
     * 停止SMTP服务器
     */
    public void stop() {
        logger.info("停止SMTP服务器...");
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (sslServerSocket != null && !sslServerSocket.isClosed()) {
                sslServerSocket.close();
            }
            if (submissionServerSocket != null && !submissionServerSocket.isClosed()) {
                submissionServerSocket.close();
            }
            
            // 关闭所有活动连接
            activeConnections.values().forEach(SmtpConnection::close);
            activeConnections.clear();
            
            // 关闭连接池
            if (connectionPool != null) {
                connectionPool.shutdown();
                try {
                    if (!connectionPool.awaitTermination(10, TimeUnit.SECONDS)) {
                        connectionPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    connectionPool.shutdownNow();
                }
            }
            
        } catch (IOException e) {
            logger.error("停止SMTP服务器时发生错误", e);
        }
        
        logger.info("SMTP服务器已停止");
    }
    
    /**
     * SMTP连接处理器
     */
    private class SmtpConnection implements Runnable {
        private final String id;
        private final Socket socket;
        private final boolean isSSL;
        private final String serverType;
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean authenticated = false;
        private String username;
        private String clientIP;
        private LocalDateTime connectTime;
        private SmtpState state = SmtpState.INITIAL;
        private String mailFrom;
        private String[] rcptTo;
        private StringBuilder messageData;
        
        public SmtpConnection(Socket socket, boolean isSSL, String serverType) {
            this.id = "smtp-" + System.currentTimeMillis() + "-" + socket.hashCode();
            this.socket = socket;
            this.isSSL = isSSL;
            this.serverType = serverType;
            this.clientIP = socket.getRemoteSocketAddress().toString();
            this.connectTime = LocalDateTime.now();
            this.messageData = new StringBuilder();
        }
        
        @Override
        public void run() {
            try {
                // 设置连接超时
                socket.setSoTimeout(connectionTimeout);
                
                // 初始化输入输出流
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                
                logger.info("SMTP连接建立: {} from {}", id, clientIP);
                
                // 发送欢迎消息
                sendResponse("220", "mail.example.com ESMTP Ready");
                
                // 处理SMTP命令
                String line;
                while ((line = reader.readLine()) != null && running) {
                    handleCommand(line.trim());
                    
                    if (state == SmtpState.QUIT) {
                        break;
                    }
                }
                
            } catch (SocketTimeoutException e) {
                logger.info("SMTP连接超时: {}", id);
                sendResponse("421", "Timeout, closing connection");
            } catch (IOException e) {
                logger.warn("SMTP连接IO错误: " + id, e);
            } finally {
                close();
            }
        }
        
        /**
         * 处理SMTP命令
         */
        private void handleCommand(String line) {
            logger.debug("SMTP命令: {} - {}", id, line);
            
            String[] parts = line.split("\\s+", 2);
            String command = parts[0].toUpperCase();
            String argument = parts.length > 1 ? parts[1] : "";
            
            try {
                switch (command) {
                    case "EHLO":
                    case "HELO":
                        handleHelo(command, argument);
                        break;
                    case "AUTH":
                        handleAuth(argument);
                        break;
                    case "STARTTLS":
                        handleStartTls();
                        break;
                    case "MAIL":
                        handleMail(argument);
                        break;
                    case "RCPT":
                        handleRcpt(argument);
                        break;
                    case "DATA":
                        handleData();
                        break;
                    case "RSET":
                        handleRset();
                        break;
                    case "NOOP":
                        sendResponse("250", "OK");
                        break;
                    case "QUIT":
                        handleQuit();
                        break;
                    default:
                        sendResponse("500", "Command not recognized");
                        break;
                }
            } catch (Exception e) {
                logger.error("处理SMTP命令时发生错误: " + command, e);
                sendResponse("451", "Requested action aborted: local error in processing");
            }
        }
        
        /**
         * 处理HELO/EHLO命令
         */
        private void handleHelo(String command, String hostname) {
            if ("EHLO".equals(command)) {
                sendResponse("250-mail.example.com Hello " + hostname);
                sendResponse("250-SIZE " + maxMessageSize);
                sendResponse("250-8BITMIME");
                sendResponse("250-PIPELINING");
                
                if (enableAuth && !authenticated) {
                    sendResponse("250-AUTH PLAIN LOGIN");
                }
                
                if (!isSSL && !requireTls) {
                    sendResponse("250-STARTTLS");
                }
                
                sendResponse("250", "HELP");
            } else {
                sendResponse("250", "mail.example.com Hello " + hostname);
            }
            
            state = SmtpState.HELO;
        }
        
        /**
         * 处理AUTH命令
         */
        private void handleAuth(String argument) {
            if (!enableAuth) {
                sendResponse("502", "Authentication not supported");
                return;
            }
            
            if (authenticated) {
                sendResponse("503", "Already authenticated");
                return;
            }
            
            String[] parts = argument.split("\\s+", 2);
            String mechanism = parts[0].toUpperCase();
            
            switch (mechanism) {
                case "PLAIN":
                    handleAuthPlain(parts.length > 1 ? parts[1] : null);
                    break;
                case "LOGIN":
                    handleAuthLogin();
                    break;
                default:
                    sendResponse("504", "Authentication mechanism not supported");
                    break;
            }
        }
        
        /**
         * 处理PLAIN认证
         */
        private void handleAuthPlain(String credentials) {
            try {
                if (credentials == null) {
                    sendResponse("334", "");
                    credentials = reader.readLine();
                }
                
                byte[] decoded = Base64.getDecoder().decode(credentials);
                String[] authData = new String(decoded, StandardCharsets.UTF_8).split("\0");
                
                if (authData.length >= 3) {
                    String authUser = authData[1];
                    String authPass = authData[2];
                    
                    if (authenticateUser(authUser, authPass)) {
                        authenticated = true;
                        username = authUser;
                        sendResponse("235", "Authentication successful");
                        logger.info("SMTP认证成功: {} - {}", id, username);
                    } else {
                        sendResponse("535", "Authentication failed");
                        logger.warn("SMTP认证失败: {} - {}", id, authUser);
                    }
                } else {
                    sendResponse("535", "Authentication failed");
                }
                
            } catch (Exception e) {
                logger.error("PLAIN认证处理错误", e);
                sendResponse("454", "Temporary authentication failure");
            }
        }
        
        /**
         * 处理LOGIN认证
         */
        private void handleAuthLogin() {
            try {
                sendResponse("334", Base64.getEncoder().encodeToString("Username:".getBytes()));
                String userBase64 = reader.readLine();
                String username = new String(Base64.getDecoder().decode(userBase64), StandardCharsets.UTF_8);
                
                sendResponse("334", Base64.getEncoder().encodeToString("Password:".getBytes()));
                String passBase64 = reader.readLine();
                String password = new String(Base64.getDecoder().decode(passBase64), StandardCharsets.UTF_8);
                
                if (authenticateUser(username, password)) {
                    authenticated = true;
                    this.username = username;
                    sendResponse("235", "Authentication successful");
                    logger.info("SMTP LOGIN认证成功: {} - {}", id, username);
                } else {
                    sendResponse("535", "Authentication failed");
                    logger.warn("SMTP LOGIN认证失败: {} - {}", id, username);
                }
                
            } catch (Exception e) {
                logger.error("LOGIN认证处理错误", e);
                sendResponse("454", "Temporary authentication failure");
            }
        }
        
        /**
         * 处理STARTTLS命令
         */
        private void handleStartTls() {
            if (isSSL) {
                sendResponse("454", "TLS not available due to temporary reason");
                return;
            }
            
            sendResponse("220", "Ready to start TLS");
            // 这里应该升级连接到TLS
            // 实际实现需要SSL上下文和证书配置
            logger.info("STARTTLS initiated for connection: {}", id);
        }
        
        /**
         * 处理MAIL FROM命令
         */
        private void handleMail(String argument) {
            if (requireTls && !isSSL) {
                sendResponse("530", "Must issue STARTTLS first");
                return;
            }
            
            if (enableAuth && !authenticated) {
                sendResponse("530", "Authentication required");
                return;
            }
            
            if (!argument.toUpperCase().startsWith("FROM:")) {
                sendResponse("501", "Syntax error in parameters");
                return;
            }
            
            String fromAddress = extractEmailAddress(argument.substring(5));
            if (fromAddress == null) {
                sendResponse("501", "Invalid sender address");
                return;
            }
            
            // 验证发件人权限
            if (authenticated && !canSendFrom(username, fromAddress)) {
                sendResponse("550", "Sender address rejected");
                return;
            }
            
            mailFrom = fromAddress;
            state = SmtpState.MAIL;
            sendResponse("250", "Sender OK");
            
            logger.debug("MAIL FROM: {} - {}", id, fromAddress);
        }
        
        /**
         * 处理RCPT TO命令
         */
        private void handleRcpt(String argument) {
            if (state != SmtpState.MAIL && state != SmtpState.RCPT) {
                sendResponse("503", "Bad sequence of commands");
                return;
            }
            
            if (!argument.toUpperCase().startsWith("TO:")) {
                sendResponse("501", "Syntax error in parameters");
                return;
            }
            
            String toAddress = extractEmailAddress(argument.substring(3));
            if (toAddress == null) {
                sendResponse("501", "Invalid recipient address");
                return;
            }
            
            // 验证收件人地址
            if (!isValidRecipient(toAddress)) {
                sendResponse("550", "Recipient address rejected");
                return;
            }
            
            // 添加到收件人列表
            if (rcptTo == null) {
                rcptTo = new String[]{toAddress};
            } else {
                String[] newRcptTo = new String[rcptTo.length + 1];
                System.arraycopy(rcptTo, 0, newRcptTo, 0, rcptTo.length);
                newRcptTo[rcptTo.length] = toAddress;
                rcptTo = newRcptTo;
            }
            
            state = SmtpState.RCPT;
            sendResponse("250", "Recipient OK");
            
            logger.debug("RCPT TO: {} - {}", id, toAddress);
        }
        
        /**
         * 处理DATA命令
         */
        private void handleData() throws IOException {
            if (state != SmtpState.RCPT) {
                sendResponse("503", "Bad sequence of commands");
                return;
            }
            
            sendResponse("354", "Start mail input; end with <CRLF>.<CRLF>");
            
            messageData.setLength(0);
            String line;
            long totalSize = 0;
            
            while ((line = reader.readLine()) != null) {
                if (".".equals(line)) {
                    break;
                }
                
                // 检查消息大小限制
                totalSize += line.length() + 2; // +2 for CRLF
                if (totalSize > maxMessageSize) {
                    sendResponse("552", "Message size exceeds maximum limit");
                    return;
                }
                
                // 处理点转义
                if (line.startsWith(".")) {
                    line = line.substring(1);
                }
                
                messageData.append(line).append("\r\n");
            }
            
            // 处理邮件消息
            try {
                processMessage(messageData.toString());
                sendResponse("250", "Message accepted for delivery");
                logger.info("邮件处理成功: {} - {} -> {}", id, mailFrom, String.join(", ", rcptTo));
            } catch (Exception e) {
                logger.error("邮件处理失败: " + id, e);
                sendResponse("554", "Transaction failed");
            }
            
            // 重置状态
            handleRset();
        }
        
        /**
         * 处理RSET命令
         */
        private void handleRset() {
            mailFrom = null;
            rcptTo = null;
            messageData.setLength(0);
            state = SmtpState.HELO;
            sendResponse("250", "Reset OK");
        }
        
        /**
         * 处理QUIT命令
         */
        private void handleQuit() {
            sendResponse("221", "Bye");
            state = SmtpState.QUIT;
        }
        
        /**
         * 发送响应
         */
        private void sendResponse(String code, String message) {
            String response = code + " " + message;
            writer.println(response);
            logger.debug("SMTP响应: {} - {}", id, response);
        }
        
        /**
         * 处理邮件消息
         */
        private void processMessage(String messageData) {
            // 创建邮件消息对象
            EmailMessage emailMessage = new EmailMessage();
            emailMessage.setFromAddress(mailFrom);
            emailMessage.setToAddresses(java.util.Arrays.asList(rcptTo));
            emailMessage.setReceivedAt(LocalDateTime.now());
            emailMessage.setMessageSize((long) messageData.length());
            
            // 解析邮件头和正文
            parseMessageData(emailMessage, messageData);
            
            // 保存邮件
            emailService.saveIncomingEmail(emailMessage);
        }
        
        /**
         * 解析邮件数据
         */
        private void parseMessageData(EmailMessage email, String data) {
            String[] lines = data.split("\r\n");
            boolean inHeaders = true;
            StringBuilder bodyText = new StringBuilder();
            
            for (String line : lines) {
                if (inHeaders) {
                    if (line.isEmpty()) {
                        inHeaders = false;
                        continue;
                    }
                    
                    // 解析邮件头
                    if (line.toLowerCase().startsWith("subject:")) {
                        email.setSubject(line.substring(8).trim());
                    } else if (line.toLowerCase().startsWith("message-id:")) {
                        email.setMessageId(line.substring(11).trim());
                    }
                    // 其他邮件头解析...
                } else {
                    bodyText.append(line).append("\n");
                }
            }
            
            email.setBodyText(bodyText.toString());
        }
        
        /**
         * 关闭连接
         */
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                activeConnections.remove(id);
                logger.info("SMTP连接关闭: {}", id);
            } catch (IOException e) {
                logger.warn("关闭SMTP连接时发生错误: " + id, e);
            }
        }
        
        // 辅助方法
        public String getId() { return id; }
        
        private String extractEmailAddress(String input) {
            // 简化的邮件地址提取
            input = input.trim();
            if (input.startsWith("<") && input.endsWith(">")) {
                return input.substring(1, input.length() - 1);
            }
            return input;
        }
        
        private boolean authenticateUser(String username, String password) {
            try {
                User user = securityService.authenticateUser(username, password);
                return user != null && user.getEmailEnabled();
            } catch (Exception e) {
                logger.error("用户认证错误", e);
                return false;
            }
        }
        
        private boolean canSendFrom(String username, String fromAddress) {
            // 检查用户是否有权限从指定地址发送邮件
            return emailService.canUserSendFrom(username, fromAddress);
        }
        
        private boolean isValidRecipient(String address) {
            // 验证收件人地址是否有效
            return emailService.isValidRecipientAddress(address);
        }
    }
    
    /**
     * SMTP状态枚举
     */
    private enum SmtpState {
        INITIAL, HELO, MAIL, RCPT, DATA, QUIT
    }
    
    /**
     * 获取服务器状态
     */
    public SmtpServerStatus getStatus() {
        SmtpServerStatus status = new SmtpServerStatus();
        status.setRunning(running);
        status.setActiveConnections(activeConnections.size());
        status.setMaxConnections(maxConnections);
        status.setSmtpPort(smtpPort);
        status.setSslPort(smtpSslPort);
        status.setSubmissionPort(submissionPort);
        return status;
    }
    
    /**
     * SMTP服务器状态
     */
    public static class SmtpServerStatus {
        private boolean running;
        private int activeConnections;
        private int maxConnections;
        private int smtpPort;
        private int sslPort;
        private int submissionPort;
        
        // Getters and Setters
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }
        
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public int getSmtpPort() { return smtpPort; }
        public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }
        
        public int getSslPort() { return sslPort; }
        public void setSslPort(int sslPort) { this.sslPort = sslPort; }
        
        public int getSubmissionPort() { return submissionPort; }
        public void setSubmissionPort(int submissionPort) { this.submissionPort = submissionPort; }
    }
}