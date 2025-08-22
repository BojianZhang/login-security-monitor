package com.security.monitor.service.protocol;

import com.security.monitor.model.EmailMessage;
import com.security.monitor.model.User;
import com.security.monitor.service.EmailService;
import com.security.monitor.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.*;

/**
 * 优化的POP3服务器实现
 * 支持POP3协议、SSL/TLS、认证和连接池
 */
@Service
public class OptimizedPop3Server {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedPop3Server.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SecurityService securityService;
    
    @Value("${pop3.port:110}")
    private int pop3Port;
    
    @Value("${pop3.ssl.port:995}")
    private int pop3SslPort;
    
    @Value("${pop3.max-connections:100}")
    private int maxConnections;
    
    @Value("${pop3.connection-timeout:600000}") // 10 minutes
    private int connectionTimeout;
    
    @Value("${pop3.delete-on-retr:false}")
    private boolean deleteOnRetr;
    
    private ServerSocket serverSocket;
    private ServerSocket sslServerSocket;
    private ExecutorService connectionPool;
    private final ConcurrentHashMap<String, Pop3Connection> activeConnections = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    
    /**
     * 启动POP3服务器
     */
    public void start() throws IOException {
        logger.info("启动POP3服务器...");
        
        // 创建连接池
        connectionPool = Executors.newFixedThreadPool(maxConnections);
        
        // 启动标准POP3服务器 (端口110)
        serverSocket = new ServerSocket(pop3Port);
        startServerListener(serverSocket, false, "POP3");
        
        // 启动SSL POP3服务器 (端口995)
        SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslServerSocket = sslFactory.createServerSocket(pop3SslPort);
        startServerListener(sslServerSocket, true, "POP3S");
        
        running = true;
        logger.info("POP3服务器启动完成 - 端口: {}, {}", pop3Port, pop3SslPort);
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
                    
                    // 创建POP3连接处理器
                    Pop3Connection connection = new Pop3Connection(clientSocket, isSSL, serverType);
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
     * 停止POP3服务器
     */
    public void stop() {
        logger.info("停止POP3服务器...");
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (sslServerSocket != null && !sslServerSocket.isClosed()) {
                sslServerSocket.close();
            }
            
            // 关闭所有活动连接
            activeConnections.values().forEach(Pop3Connection::close);
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
            logger.error("停止POP3服务器时发生错误", e);
        }
        
        logger.info("POP3服务器已停止");
    }
    
    /**
     * POP3连接处理器
     */
    private class Pop3Connection implements Runnable {
        private final String id;
        private final Socket socket;
        private final boolean isSSL;
        private final String serverType;
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean authenticated = false;
        private User currentUser;
        private Pop3State state = Pop3State.AUTHORIZATION;
        private List<EmailMessage> messages;
        private boolean[] markedForDeletion;
        private LocalDateTime connectTime;
        
        public Pop3Connection(Socket socket, boolean isSSL, String serverType) {
            this.id = "pop3-" + System.currentTimeMillis() + "-" + socket.hashCode();
            this.socket = socket;
            this.isSSL = isSSL;
            this.serverType = serverType;
            this.connectTime = LocalDateTime.now();
        }
        
        @Override
        public void run() {
            try {
                // 设置连接超时
                socket.setSoTimeout(connectionTimeout);
                
                // 初始化输入输出流
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                
                logger.info("POP3连接建立: {} from {}", id, socket.getRemoteSocketAddress());
                
                // 发送欢迎消息
                sendResponse("+OK POP3 server ready");
                
                // 处理POP3命令
                String line;
                while ((line = reader.readLine()) != null && running) {
                    handleCommand(line.trim());
                    
                    if (state == Pop3State.QUIT) {
                        break;
                    }
                }
                
            } catch (SocketTimeoutException e) {
                logger.info("POP3连接超时: {}", id);
                sendResponse("-ERR Timeout, closing connection");
            } catch (IOException e) {
                logger.warn("POP3连接IO错误: " + id, e);
            } finally {
                close();
            }
        }
        
        /**
         * 处理POP3命令
         */
        private void handleCommand(String line) {
            logger.debug("POP3命令: {} - {}", id, line);
            
            String[] parts = line.split("\\s+", 2);
            String command = parts[0].toUpperCase();
            String argument = parts.length > 1 ? parts[1] : "";
            
            try {
                switch (command) {
                    case "CAPA":
                        handleCapa();
                        break;
                    case "USER":
                        handleUser(argument);
                        break;
                    case "PASS":
                        handlePass(argument);
                        break;
                    case "APOP":
                        handleApop(argument);
                        break;
                    case "STLS":
                        handleStls();
                        break;
                    case "STAT":
                        handleStat();
                        break;
                    case "LIST":
                        handleList(argument);
                        break;
                    case "RETR":
                        handleRetr(argument);
                        break;
                    case "DELE":
                        handleDele(argument);
                        break;
                    case "NOOP":
                        handleNoop();
                        break;
                    case "RSET":
                        handleRset();
                        break;
                    case "TOP":
                        handleTop(argument);
                        break;
                    case "UIDL":
                        handleUidl(argument);
                        break;
                    case "QUIT":
                        handleQuit();
                        break;
                    default:
                        sendResponse("-ERR Command not recognized");
                        break;
                }
            } catch (Exception e) {
                logger.error("处理POP3命令时发生错误: " + command, e);
                sendResponse("-ERR Internal server error");
            }
        }
        
        /**
         * 处理CAPA命令
         */
        private void handleCapa() {
            sendResponse("+OK Capability list follows");
            sendResponse("USER");
            sendResponse("RESP-CODES");
            sendResponse("LOGIN-DELAY 900");
            sendResponse("PIPELINING");
            sendResponse("EXPIRE 60");
            sendResponse("UIDL");
            sendResponse("TOP");
            
            if (!isSSL) {
                sendResponse("STLS");
            }
            
            sendResponse(".");
        }
        
        /**
         * 处理USER命令
         */
        private void handleUser(String username) {
            if (state != Pop3State.AUTHORIZATION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            if (username.isEmpty()) {
                sendResponse("-ERR Username required");
                return;
            }
            
            // 存储用户名，等待密码
            state = Pop3State.USER_PROVIDED;
            sendResponse("+OK User name accepted, password please");
            
            logger.debug("POP3用户名: {} - {}", id, username);
        }
        
        /**
         * 处理PASS命令
         */
        private void handlePass(String password) {
            if (state != Pop3State.USER_PROVIDED) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            try {
                // 从前一个命令获取用户名（简化实现）
                String username = "user"; // 实际应该从session中获取
                
                User user = securityService.authenticateUser(username, password);
                if (user != null && user.getEmailEnabled()) {
                    authenticated = true;
                    currentUser = user;
                    state = Pop3State.TRANSACTION;
                    
                    // 加载用户邮件
                    loadUserMessages();
                    
                    sendResponse("+OK Mailbox open, " + messages.size() + " messages");
                    logger.info("POP3认证成功: {} - {}", id, username);
                } else {
                    sendResponse("-ERR Authentication failed");
                    state = Pop3State.AUTHORIZATION;
                    logger.warn("POP3认证失败: {} - {}", id, username);
                }
                
            } catch (Exception e) {
                logger.error("POP3认证处理错误", e);
                sendResponse("-ERR Authentication failed");
                state = Pop3State.AUTHORIZATION;
            }
        }
        
        /**
         * 处理APOP命令
         */
        private void handleApop(String argument) {
            // APOP认证实现
            sendResponse("-ERR APOP not supported");
        }
        
        /**
         * 处理STLS命令
         */
        private void handleStls() {
            if (isSSL) {
                sendResponse("-ERR TLS already active");
                return;
            }
            
            sendResponse("+OK Begin TLS negotiation");
            // 这里应该升级连接到TLS
            logger.info("STLS initiated for connection: {}", id);
        }
        
        /**
         * 处理STAT命令
         */
        private void handleStat() {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            int messageCount = 0;
            long totalSize = 0;
            
            for (int i = 0; i < messages.size(); i++) {
                if (!markedForDeletion[i]) {
                    messageCount++;
                    totalSize += messages.get(i).getMessageSize();
                }
            }
            
            sendResponse("+OK " + messageCount + " " + totalSize);
        }
        
        /**
         * 处理LIST命令
         */
        private void handleList(String argument) {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            if (argument.isEmpty()) {
                // 列出所有消息
                sendResponse("+OK");
                for (int i = 0; i < messages.size(); i++) {
                    if (!markedForDeletion[i]) {
                        sendResponse((i + 1) + " " + messages.get(i).getMessageSize());
                    }
                }
                sendResponse(".");
            } else {
                // 列出指定消息
                try {
                    int messageNum = Integer.parseInt(argument);
                    if (messageNum < 1 || messageNum > messages.size()) {
                        sendResponse("-ERR Invalid message number");
                    } else if (markedForDeletion[messageNum - 1]) {
                        sendResponse("-ERR Message deleted");
                    } else {
                        sendResponse("+OK " + messageNum + " " + messages.get(messageNum - 1).getMessageSize());
                    }
                } catch (NumberFormatException e) {
                    sendResponse("-ERR Invalid message number");
                }
            }
        }
        
        /**
         * 处理RETR命令
         */
        private void handleRetr(String argument) {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            try {
                int messageNum = Integer.parseInt(argument);
                if (messageNum < 1 || messageNum > messages.size()) {
                    sendResponse("-ERR Invalid message number");
                    return;
                }
                
                if (markedForDeletion[messageNum - 1]) {
                    sendResponse("-ERR Message deleted");
                    return;
                }
                
                EmailMessage message = messages.get(messageNum - 1);
                
                sendResponse("+OK " + message.getMessageSize() + " octets");
                
                // 发送邮件内容
                sendEmailContent(message);
                sendResponse(".");
                
                // 如果配置为检索后删除，标记删除
                if (deleteOnRetr) {
                    markedForDeletion[messageNum - 1] = true;
                }
                
                logger.debug("POP3检索邮件: {} - 消息{}", id, messageNum);
                
            } catch (NumberFormatException e) {
                sendResponse("-ERR Invalid message number");
            }
        }
        
        /**
         * 处理DELE命令
         */
        private void handleDele(String argument) {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            try {
                int messageNum = Integer.parseInt(argument);
                if (messageNum < 1 || messageNum > messages.size()) {
                    sendResponse("-ERR Invalid message number");
                } else if (markedForDeletion[messageNum - 1]) {
                    sendResponse("-ERR Message already deleted");
                } else {
                    markedForDeletion[messageNum - 1] = true;
                    sendResponse("+OK Message " + messageNum + " deleted");
                    logger.debug("POP3标记删除: {} - 消息{}", id, messageNum);
                }
            } catch (NumberFormatException e) {
                sendResponse("-ERR Invalid message number");
            }
        }
        
        /**
         * 处理NOOP命令
         */
        private void handleNoop() {
            sendResponse("+OK");
        }
        
        /**
         * 处理RSET命令
         */
        private void handleRset() {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            // 取消所有删除标记
            for (int i = 0; i < markedForDeletion.length; i++) {
                markedForDeletion[i] = false;
            }
            
            sendResponse("+OK");
            logger.debug("POP3重置删除标记: {}", id);
        }
        
        /**
         * 处理TOP命令
         */
        private void handleTop(String argument) {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            String[] parts = argument.split("\\s+");
            if (parts.length < 2) {
                sendResponse("-ERR TOP requires message number and line count");
                return;
            }
            
            try {
                int messageNum = Integer.parseInt(parts[0]);
                int lineCount = Integer.parseInt(parts[1]);
                
                if (messageNum < 1 || messageNum > messages.size()) {
                    sendResponse("-ERR Invalid message number");
                    return;
                }
                
                if (markedForDeletion[messageNum - 1]) {
                    sendResponse("-ERR Message deleted");
                    return;
                }
                
                EmailMessage message = messages.get(messageNum - 1);
                
                sendResponse("+OK");
                sendEmailHeaders(message);
                sendResponse(""); // 空行分隔头部和正文
                sendEmailBodyLines(message, lineCount);
                sendResponse(".");
                
            } catch (NumberFormatException e) {
                sendResponse("-ERR Invalid arguments");
            }
        }
        
        /**
         * 处理UIDL命令
         */
        private void handleUidl(String argument) {
            if (state != Pop3State.TRANSACTION) {
                sendResponse("-ERR Command not valid in this state");
                return;
            }
            
            if (argument.isEmpty()) {
                // 列出所有消息的UID
                sendResponse("+OK");
                for (int i = 0; i < messages.size(); i++) {
                    if (!markedForDeletion[i]) {
                        sendResponse((i + 1) + " " + generateUID(messages.get(i)));
                    }
                }
                sendResponse(".");
            } else {
                // 列出指定消息的UID
                try {
                    int messageNum = Integer.parseInt(argument);
                    if (messageNum < 1 || messageNum > messages.size()) {
                        sendResponse("-ERR Invalid message number");
                    } else if (markedForDeletion[messageNum - 1]) {
                        sendResponse("-ERR Message deleted");
                    } else {
                        sendResponse("+OK " + messageNum + " " + generateUID(messages.get(messageNum - 1)));
                    }
                } catch (NumberFormatException e) {
                    sendResponse("-ERR Invalid message number");
                }
            }
        }
        
        /**
         * 处理QUIT命令
         */
        private void handleQuit() {
            if (state == Pop3State.TRANSACTION) {
                // 执行实际删除操作
                int deletedCount = 0;
                for (int i = 0; i < messages.size(); i++) {
                    if (markedForDeletion[i]) {
                        try {
                            emailService.deleteMessage(messages.get(i).getId());
                            deletedCount++;
                        } catch (Exception e) {
                            logger.error("删除邮件失败: " + messages.get(i).getId(), e);
                        }
                    }
                }
                
                sendResponse("+OK POP3 server signing off (" + deletedCount + " messages deleted)");
                logger.info("POP3会话结束: {} - 删除{}条消息", id, deletedCount);
            } else {
                sendResponse("+OK POP3 server signing off");
            }
            
            state = Pop3State.QUIT;
        }
        
        /**
         * 发送响应
         */
        private void sendResponse(String response) {
            writer.println(response);
            logger.debug("POP3响应: {} - {}", id, response);
        }
        
        /**
         * 发送邮件内容
         */
        private void sendEmailContent(EmailMessage message) {
            // 发送邮件头
            sendEmailHeaders(message);
            
            // 空行分隔头部和正文
            sendResponse("");
            
            // 发送邮件正文
            String body = message.getBodyText();
            if (body != null) {
                String[] lines = body.split("\n");
                for (String line : lines) {
                    // POP3点转义
                    if (line.startsWith(".")) {
                        line = "." + line;
                    }
                    sendResponse(line);
                }
            }
        }
        
        /**
         * 发送邮件头
         */
        private void sendEmailHeaders(EmailMessage message) {
            sendResponse("Message-ID: " + (message.getMessageId() != null ? message.getMessageId() : generateMessageId()));
            sendResponse("From: " + message.getFromAddress());
            
            if (message.getToAddresses() != null && !message.getToAddresses().isEmpty()) {
                sendResponse("To: " + String.join(", ", message.getToAddresses()));
            }
            
            if (message.getSubject() != null) {
                sendResponse("Subject: " + message.getSubject());
            }
            
            sendResponse("Date: " + formatDate(message.getReceivedAt()));
            
            // 其他标准头部
            sendResponse("Content-Type: text/plain; charset=UTF-8");
            sendResponse("Content-Transfer-Encoding: 8bit");
        }
        
        /**
         * 发送邮件正文行
         */
        private void sendEmailBodyLines(EmailMessage message, int lineCount) {
            String body = message.getBodyText();
            if (body != null) {
                String[] lines = body.split("\n");
                int count = Math.min(lineCount, lines.length);
                
                for (int i = 0; i < count; i++) {
                    String line = lines[i];
                    if (line.startsWith(".")) {
                        line = "." + line;
                    }
                    sendResponse(line);
                }
            }
        }
        
        /**
         * 加载用户邮件
         */
        private void loadUserMessages() {
            messages = getInboxMessagesForUser(currentUser.getId());
            markedForDeletion = new boolean[messages.size()];
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
                logger.info("POP3连接关闭: {}", id);
            } catch (IOException e) {
                logger.warn("关闭POP3连接时发生错误: " + id, e);
            }
        }
        
        // 辅助方法
        public String getId() { return id; }
        
        private String generateUID(EmailMessage message) {
            return "msg" + message.getId() + "@" + System.currentTimeMillis();
        }
        
        private String generateMessageId() {
            return "<" + System.currentTimeMillis() + "@pop3.example.com>";
        }
        
        private String formatDate(LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss ZZZ"));
        }
        
        @Cacheable("inboxMessages")
        private List<EmailMessage> getInboxMessagesForUser(Long userId) {
            return emailService.getInboxMessagesForUser(userId);
        }
    }
    
    /**
     * POP3状态枚举
     */
    private enum Pop3State {
        AUTHORIZATION, USER_PROVIDED, TRANSACTION, QUIT
    }
    
    /**
     * 获取服务器状态
     */
    public Pop3ServerStatus getStatus() {
        Pop3ServerStatus status = new Pop3ServerStatus();
        status.setRunning(running);
        status.setActiveConnections(activeConnections.size());
        status.setMaxConnections(maxConnections);
        status.setPop3Port(pop3Port);
        status.setSslPort(pop3SslPort);
        status.setDeleteOnRetr(deleteOnRetr);
        return status;
    }
    
    /**
     * POP3服务器状态
     */
    public static class Pop3ServerStatus {
        private boolean running;
        private int activeConnections;
        private int maxConnections;
        private int pop3Port;
        private int sslPort;
        private boolean deleteOnRetr;
        
        // Getters and Setters
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }
        
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public int getPop3Port() { return pop3Port; }
        public void setPop3Port(int pop3Port) { this.pop3Port = pop3Port; }
        
        public int getSslPort() { return sslPort; }
        public void setSslPort(int sslPort) { this.sslPort = sslPort; }
        
        public boolean isDeleteOnRetr() { return deleteOnRetr; }
        public void setDeleteOnRetr(boolean deleteOnRetr) { this.deleteOnRetr = deleteOnRetr; }
    }
}