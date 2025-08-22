package com.security.monitor.service.protocol;

import com.security.monitor.model.EmailFolder;
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
import java.util.*;
import java.util.concurrent.*;

/**
 * 优化的IMAP服务器实现
 * 支持IMAP4rev1协议、连接池、缓存和高性能
 */
@Service
public class OptimizedImapServer {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedImapServer.class);
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SecurityService securityService;
    
    @Value("${imap.port:143}")
    private int imapPort;
    
    @Value("${imap.ssl.port:993}")
    private int imapSslPort;
    
    @Value("${imap.max-connections:200}")
    private int maxConnections;
    
    @Value("${imap.connection-timeout:1800000}") // 30 minutes
    private int connectionTimeout;
    
    @Value("${imap.enable-idle:true}")
    private boolean enableIdle;
    
    @Value("${imap.cache-size:1000}")
    private int cacheSize;
    
    private ServerSocket serverSocket;
    private ServerSocket sslServerSocket;
    private ExecutorService connectionPool;
    private final ConcurrentHashMap<String, ImapConnection> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, List<EmailMessage>> folderCache = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    
    /**
     * 启动IMAP服务器
     */
    public void start() throws IOException {
        logger.info("启动IMAP服务器...");
        
        // 创建连接池
        connectionPool = Executors.newFixedThreadPool(maxConnections);
        
        // 启动标准IMAP服务器 (端口143)
        serverSocket = new ServerSocket(imapPort);
        startServerListener(serverSocket, false, "IMAP");
        
        // 启动SSL IMAP服务器 (端口993)
        SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslServerSocket = sslFactory.createServerSocket(imapSslPort);
        startServerListener(sslServerSocket, true, "IMAPS");
        
        running = true;
        logger.info("IMAP服务器启动完成 - 端口: {}, {}", imapPort, imapSslPort);
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
                    
                    // 创建IMAP连接处理器
                    ImapConnection connection = new ImapConnection(clientSocket, isSSL, serverType);
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
     * 停止IMAP服务器
     */
    public void stop() {
        logger.info("停止IMAP服务器...");
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (sslServerSocket != null && !sslServerSocket.isClosed()) {
                sslServerSocket.close();
            }
            
            // 关闭所有活动连接
            activeConnections.values().forEach(ImapConnection::close);
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
            logger.error("停止IMAP服务器时发生错误", e);
        }
        
        logger.info("IMAP服务器已停止");
    }
    
    /**
     * IMAP连接处理器
     */
    private class ImapConnection implements Runnable {
        private final String id;
        private final Socket socket;
        private final boolean isSSL;
        private final String serverType;
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean authenticated = false;
        private User currentUser;
        private String selectedFolder;
        private ImapState state = ImapState.NOT_AUTHENTICATED;
        private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
        private LocalDateTime lastActivity;
        private boolean idling = false;
        
        public ImapConnection(Socket socket, boolean isSSL, String serverType) {
            this.id = "imap-" + System.currentTimeMillis() + "-" + socket.hashCode();
            this.socket = socket;
            this.isSSL = isSSL;
            this.serverType = serverType;
            this.lastActivity = LocalDateTime.now();
        }
        
        @Override
        public void run() {
            try {
                // 设置连接超时
                socket.setSoTimeout(connectionTimeout);
                
                // 初始化输入输出流
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                
                logger.info("IMAP连接建立: {} from {}", id, socket.getRemoteSocketAddress());
                
                // 发送欢迎消息
                sendResponse("* OK [CAPABILITY IMAP4rev1 STARTTLS AUTH=PLAIN AUTH=LOGIN IDLE] IMAP server ready");
                
                // 处理IMAP命令
                String line;
                while ((line = reader.readLine()) != null && running) {
                    lastActivity = LocalDateTime.now();
                    
                    if (idling && !line.toUpperCase().startsWith("DONE")) {
                        continue; // 在IDLE状态下只处理DONE命令
                    }
                    
                    handleCommand(line.trim());
                    
                    if (state == ImapState.LOGOUT) {
                        break;
                    }
                }
                
            } catch (SocketTimeoutException e) {
                logger.info("IMAP连接超时: {}", id);
                sendResponse("* BYE Timeout, closing connection");
            } catch (IOException e) {
                logger.warn("IMAP连接IO错误: " + id, e);
            } finally {
                close();
            }
        }
        
        /**
         * 处理IMAP命令
         */
        private void handleCommand(String line) {
            logger.debug("IMAP命令: {} - {}", id, line);
            
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 2) {
                sendResponse("* BAD Syntax error");
                return;
            }
            
            String tag = parts[0];
            String command = parts[1].toUpperCase();
            String arguments = parts.length > 2 ? parts[2] : "";
            
            try {
                switch (command) {
                    case "CAPABILITY":
                        handleCapability(tag);
                        break;
                    case "STARTTLS":
                        handleStartTls(tag);
                        break;
                    case "LOGIN":
                        handleLogin(tag, arguments);
                        break;
                    case "AUTHENTICATE":
                        handleAuthenticate(tag, arguments);
                        break;
                    case "SELECT":
                        handleSelect(tag, arguments);
                        break;
                    case "EXAMINE":
                        handleExamine(tag, arguments);
                        break;
                    case "LIST":
                        handleList(tag, arguments);
                        break;
                    case "LSUB":
                        handleLsub(tag, arguments);
                        break;
                    case "STATUS":
                        handleStatus(tag, arguments);
                        break;
                    case "CREATE":
                        handleCreate(tag, arguments);
                        break;
                    case "DELETE":
                        handleDelete(tag, arguments);
                        break;
                    case "RENAME":
                        handleRename(tag, arguments);
                        break;
                    case "SUBSCRIBE":
                        handleSubscribe(tag, arguments);
                        break;
                    case "UNSUBSCRIBE":
                        handleUnsubscribe(tag, arguments);
                        break;
                    case "FETCH":
                        handleFetch(tag, arguments);
                        break;
                    case "STORE":
                        handleStore(tag, arguments);
                        break;
                    case "COPY":
                        handleCopy(tag, arguments);
                        break;
                    case "SEARCH":
                        handleSearch(tag, arguments);
                        break;
                    case "UID":
                        handleUid(tag, arguments);
                        break;
                    case "EXPUNGE":
                        handleExpunge(tag);
                        break;
                    case "CLOSE":
                        handleClose(tag);
                        break;
                    case "IDLE":
                        handleIdle(tag);
                        break;
                    case "DONE":
                        handleDone();
                        break;
                    case "NOOP":
                        handleNoop(tag);
                        break;
                    case "LOGOUT":
                        handleLogout(tag);
                        break;
                    default:
                        sendResponse(tag + " BAD Command not recognized");
                        break;
                }
            } catch (Exception e) {
                logger.error("处理IMAP命令时发生错误: " + command, e);
                sendResponse(tag + " BAD Internal server error");
            }
        }
        
        /**
         * 处理CAPABILITY命令
         */
        private void handleCapability(String tag) {
            sendResponse("* CAPABILITY IMAP4rev1 STARTTLS AUTH=PLAIN AUTH=LOGIN IDLE NAMESPACE QUOTA");
            sendResponse(tag + " OK CAPABILITY completed");
        }
        
        /**
         * 处理STARTTLS命令
         */
        private void handleStartTls(String tag) {
            if (isSSL) {
                sendResponse(tag + " BAD TLS already active");
                return;
            }
            
            sendResponse(tag + " OK Begin TLS negotiation now");
            // 这里应该升级连接到TLS
            logger.info("STARTTLS initiated for connection: {}", id);
        }
        
        /**
         * 处理LOGIN命令
         */
        private void handleLogin(String tag, String arguments) {
            if (authenticated) {
                sendResponse(tag + " BAD Already authenticated");
                return;
            }
            
            String[] parts = parseQuotedArguments(arguments);
            if (parts.length < 2) {
                sendResponse(tag + " BAD LOGIN expects username and password");
                return;
            }
            
            String username = parts[0];
            String password = parts[1];
            
            try {
                User user = securityService.authenticateUser(username, password);
                if (user != null && user.getEmailEnabled()) {
                    authenticated = true;
                    currentUser = user;
                    state = ImapState.AUTHENTICATED;
                    sendResponse(tag + " OK LOGIN completed");
                    logger.info("IMAP登录成功: {} - {}", id, username);
                } else {
                    sendResponse(tag + " NO LOGIN failed");
                    logger.warn("IMAP登录失败: {} - {}", id, username);
                }
            } catch (Exception e) {
                logger.error("IMAP登录处理错误", e);
                sendResponse(tag + " NO LOGIN failed");
            }
        }
        
        /**
         * 处理AUTHENTICATE命令
         */
        private void handleAuthenticate(String tag, String mechanism) {
            if (authenticated) {
                sendResponse(tag + " BAD Already authenticated");
                return;
            }
            
            switch (mechanism.toUpperCase()) {
                case "PLAIN":
                    handleAuthPlain(tag);
                    break;
                case "LOGIN":
                    handleAuthLogin(tag);
                    break;
                default:
                    sendResponse(tag + " NO Unsupported authentication mechanism");
                    break;
            }
        }
        
        /**
         * 处理PLAIN认证
         */
        private void handleAuthPlain(String tag) {
            try {
                sendResponse("+ ");
                String credentials = reader.readLine();
                
                byte[] decoded = Base64.getDecoder().decode(credentials);
                String[] authData = new String(decoded, StandardCharsets.UTF_8).split("\0");
                
                if (authData.length >= 3) {
                    String username = authData[1];
                    String password = authData[2];
                    
                    User user = securityService.authenticateUser(username, password);
                    if (user != null && user.getEmailEnabled()) {
                        authenticated = true;
                        currentUser = user;
                        state = ImapState.AUTHENTICATED;
                        sendResponse(tag + " OK AUTHENTICATE completed");
                        logger.info("IMAP PLAIN认证成功: {} - {}", id, username);
                    } else {
                        sendResponse(tag + " NO AUTHENTICATE failed");
                        logger.warn("IMAP PLAIN认证失败: {} - {}", id, username);
                    }
                } else {
                    sendResponse(tag + " NO AUTHENTICATE failed");
                }
                
            } catch (Exception e) {
                logger.error("PLAIN认证处理错误", e);
                sendResponse(tag + " NO AUTHENTICATE failed");
            }
        }
        
        /**
         * 处理SELECT命令
         */
        private void handleSelect(String tag, String folderName) {
            if (!authenticated) {
                sendResponse(tag + " NO Must be authenticated");
                return;
            }
            
            folderName = unquote(folderName);
            
            try {
                EmailFolder folder = getFolderByName(folderName);
                if (folder == null) {
                    sendResponse(tag + " NO Folder not found");
                    return;
                }
                
                selectedFolder = folderName;
                state = ImapState.SELECTED;
                
                // 发送文件夹状态信息
                sendResponse("* " + folder.getMessageCount() + " EXISTS");
                sendResponse("* " + folder.getUnreadCount() + " RECENT");
                sendResponse("* OK [UNSEEN " + folder.getUnreadCount() + "] Message " + folder.getUnreadCount() + " is first unseen");
                sendResponse("* OK [UIDVALIDITY " + System.currentTimeMillis() + "] UIDs valid");
                sendResponse("* OK [UIDNEXT " + (folder.getMessageCount() + 1) + "] Predicted next UID");
                sendResponse("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
                sendResponse("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft \\*)] Limited");
                
                sendResponse(tag + " OK [READ-WRITE] SELECT completed");
                logger.debug("IMAP文件夹选择: {} - {}", id, folderName);
                
            } catch (Exception e) {
                logger.error("SELECT处理错误", e);
                sendResponse(tag + " NO SELECT failed");
            }
        }
        
        /**
         * 处理LIST命令
         */
        private void handleList(String tag, String arguments) {
            if (!authenticated) {
                sendResponse(tag + " NO Must be authenticated");
                return;
            }
            
            String[] parts = parseQuotedArguments(arguments);
            if (parts.length < 2) {
                sendResponse(tag + " BAD LIST expects reference and mailbox");
                return;
            }
            
            try {
                List<EmailFolder> folders = getFoldersForUser(currentUser.getId());
                
                for (EmailFolder folder : folders) {
                    String attributes = "()";
                    String delimiter = "/";
                    String name = "\"" + folder.getFolderName() + "\"";
                    
                    sendResponse("* LIST " + attributes + " \"" + delimiter + "\" " + name);
                }
                
                sendResponse(tag + " OK LIST completed");
                
            } catch (Exception e) {
                logger.error("LIST处理错误", e);
                sendResponse(tag + " NO LIST failed");
            }
        }
        
        /**
         * 处理FETCH命令
         */
        private void handleFetch(String tag, String arguments) {
            if (state != ImapState.SELECTED) {
                sendResponse(tag + " NO Must select a folder first");
                return;
            }
            
            try {
                String[] parts = arguments.split("\\s+", 2);
                if (parts.length < 2) {
                    sendResponse(tag + " BAD FETCH expects sequence set and items");
                    return;
                }
                
                String sequenceSet = parts[0];
                String items = parts[1];
                
                List<EmailMessage> messages = getMessagesInFolder(selectedFolder);
                List<Integer> messageNumbers = parseSequenceSet(sequenceSet, messages.size());
                
                for (int msgNum : messageNumbers) {
                    if (msgNum > 0 && msgNum <= messages.size()) {
                        EmailMessage message = messages.get(msgNum - 1);
                        sendFetchResponse(msgNum, message, items);
                    }
                }
                
                sendResponse(tag + " OK FETCH completed");
                
            } catch (Exception e) {
                logger.error("FETCH处理错误", e);
                sendResponse(tag + " NO FETCH failed");
            }
        }
        
        /**
         * 处理IDLE命令
         */
        private void handleIdle(String tag) {
            if (!enableIdle) {
                sendResponse(tag + " BAD IDLE not supported");
                return;
            }
            
            if (state != ImapState.SELECTED) {
                sendResponse(tag + " NO Must select a folder first");
                return;
            }
            
            sendResponse("+ idling");
            idling = true;
            
            // 在实际实现中，这里应该监听文件夹变化并推送更新
            logger.debug("IMAP进入IDLE状态: {}", id);
        }
        
        /**
         * 处理DONE命令
         */
        private void handleDone() {
            if (idling) {
                idling = false;
                // 这里应该有对应的tag，简化处理
                sendResponse("OK IDLE terminated");
                logger.debug("IMAP退出IDLE状态: {}", id);
            }
        }
        
        /**
         * 处理NOOP命令
         */
        private void handleNoop(String tag) {
            // 发送任何待处理的更新
            sendResponse(tag + " OK NOOP completed");
        }
        
        /**
         * 处理LOGOUT命令
         */
        private void handleLogout(String tag) {
            sendResponse("* BYE IMAP4rev1 Server logging out");
            sendResponse(tag + " OK LOGOUT completed");
            state = ImapState.LOGOUT;
        }
        
        // 占位方法，实际实现需要更多细节
        private void handleExamine(String tag, String arguments) { /* 实现 */ }
        private void handleLsub(String tag, String arguments) { /* 实现 */ }
        private void handleStatus(String tag, String arguments) { /* 实现 */ }
        private void handleCreate(String tag, String arguments) { /* 实现 */ }
        private void handleDelete(String tag, String arguments) { /* 实现 */ }
        private void handleRename(String tag, String arguments) { /* 实现 */ }
        private void handleSubscribe(String tag, String arguments) { /* 实现 */ }
        private void handleUnsubscribe(String tag, String arguments) { /* 实现 */ }
        private void handleStore(String tag, String arguments) { /* 实现 */ }
        private void handleCopy(String tag, String arguments) { /* 实现 */ }
        private void handleSearch(String tag, String arguments) { /* 实现 */ }
        private void handleUid(String tag, String arguments) { /* 实现 */ }
        private void handleExpunge(String tag) { /* 实现 */ }
        private void handleClose(String tag) { /* 实现 */ }
        
        /**
         * 发送响应
         */
        private void sendResponse(String response) {
            writer.println(response);
            logger.debug("IMAP响应: {} - {}", id, response);
        }
        
        /**
         * 发送FETCH响应
         */
        private void sendFetchResponse(int messageNumber, EmailMessage message, String items) {
            StringBuilder response = new StringBuilder();
            response.append("* ").append(messageNumber).append(" FETCH (");
            
            if (items.toUpperCase().contains("UID")) {
                response.append("UID ").append(message.getId()).append(" ");
            }
            
            if (items.toUpperCase().contains("FLAGS")) {
                response.append("FLAGS (");
                if (message.getIsRead()) response.append("\\Seen ");
                if (message.getIsStarred()) response.append("\\Flagged ");
                response.append(") ");
            }
            
            if (items.toUpperCase().contains("ENVELOPE")) {
                response.append("ENVELOPE (");
                response.append("\"").append(formatDate(message.getReceivedAt())).append("\" ");
                response.append("\"").append(escapeString(message.getSubject())).append("\" ");
                // 其他envelope字段...
                response.append(") ");
            }
            
            if (items.toUpperCase().contains("RFC822.SIZE")) {
                response.append("RFC822.SIZE ").append(message.getMessageSize()).append(" ");
            }
            
            response.append(")");
            sendResponse(response.toString());
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
                logger.info("IMAP连接关闭: {}", id);
            } catch (IOException e) {
                logger.warn("关闭IMAP连接时发生错误: " + id, e);
            }
        }
        
        // 辅助方法
        public String getId() { return id; }
        
        private String[] parseQuotedArguments(String arguments) {
            // 简化的引号参数解析
            List<String> result = new ArrayList<>();
            String[] parts = arguments.split("\\s+");
            for (String part : parts) {
                result.add(unquote(part));
            }
            return result.toArray(new String[0]);
        }
        
        private String unquote(String str) {
            if (str.startsWith("\"") && str.endsWith("\"")) {
                return str.substring(1, str.length() - 1);
            }
            return str;
        }
        
        private String escapeString(String str) {
            if (str == null) return "";
            return str.replace("\"", "\\\"");
        }
        
        private String formatDate(LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss ZZZ"));
        }
        
        private List<Integer> parseSequenceSet(String sequenceSet, int maxSeq) {
            List<Integer> result = new ArrayList<>();
            String[] parts = sequenceSet.split(",");
            
            for (String part : parts) {
                if (part.contains(":")) {
                    String[] range = part.split(":");
                    int start = "*".equals(range[0]) ? maxSeq : Integer.parseInt(range[0]);
                    int end = "*".equals(range[1]) ? maxSeq : Integer.parseInt(range[1]);
                    
                    for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                        result.add(i);
                    }
                } else {
                    int seq = "*".equals(part) ? maxSeq : Integer.parseInt(part);
                    result.add(seq);
                }
            }
            
            return result;
        }
        
        @Cacheable("folders")
        private EmailFolder getFolderByName(String folderName) {
            return emailService.getFolderByName(currentUser.getId(), folderName);
        }
        
        @Cacheable("userFolders")
        private List<EmailFolder> getFoldersForUser(Long userId) {
            return emailService.getFoldersForUser(userId);
        }
        
        @Cacheable("folderMessages")
        private List<EmailMessage> getMessagesInFolder(String folderName) {
            return emailService.getMessagesInFolder(currentUser.getId(), folderName);
        }
    }
    
    /**
     * IMAP状态枚举
     */
    private enum ImapState {
        NOT_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }
    
    /**
     * 获取服务器状态
     */
    public ImapServerStatus getStatus() {
        ImapServerStatus status = new ImapServerStatus();
        status.setRunning(running);
        status.setActiveConnections(activeConnections.size());
        status.setMaxConnections(maxConnections);
        status.setImapPort(imapPort);
        status.setSslPort(imapSslPort);
        status.setIdleEnabled(enableIdle);
        return status;
    }
    
    /**
     * IMAP服务器状态
     */
    public static class ImapServerStatus {
        private boolean running;
        private int activeConnections;
        private int maxConnections;
        private int imapPort;
        private int sslPort;
        private boolean idleEnabled;
        
        // Getters and Setters
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }
        
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public int getImapPort() { return imapPort; }
        public void setImapPort(int imapPort) { this.imapPort = imapPort; }
        
        public int getSslPort() { return sslPort; }
        public void setSslPort(int sslPort) { this.sslPort = sslPort; }
        
        public boolean isIdleEnabled() { return idleEnabled; }
        public void setIdleEnabled(boolean idleEnabled) { this.idleEnabled = idleEnabled; }
    }
}