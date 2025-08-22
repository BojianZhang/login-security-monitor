package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件接收和处理服务
 * 支持IMAP/POP3协议接收邮件以及邮件处理
 */
@Service
@Transactional
public class EmailReceiveService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailReceiveService.class);
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private EmailAliasRepository aliasRepository;
    
    @Autowired
    private EmailFolderRepository folderRepository;
    
    @Autowired
    private EmailAttachmentRepository attachmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AutoReplyService autoReplyService;
    
    @Autowired
    private EmailForwardingService forwardingService;
    
    @Value("${app.mail.storage.path:/opt/mail-storage}")
    private String mailStoragePath;
    
    @Value("${app.mail.attachment.max-size:52428800}") // 50MB
    private long maxAttachmentSize;
    
    /**
     * 根据别名ID获取该别名收到的邮件
     */
    @Transactional(readOnly = true)
    public Page<EmailMessage> getMessagesByAlias(Long aliasId, Pageable pageable) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty()) {
            throw new RuntimeException("别名不存在");
        }
        
        EmailAlias alias = aliasOpt.get();
        String fullEmail = alias.getFullEmail(); // 获取完整邮箱地址
        
        // 查询发送到此别名的邮件
        return messageRepository.findByToAddressesContainingOrderByReceivedAtDesc(
            fullEmail, pageable
        );
    }
    
    /**
     * 根据别名获取邮件统计
     */
    @Transactional(readOnly = true)
    public EmailAliasStats getAliasStats(Long aliasId) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty()) {
            throw new RuntimeException("别名不存在");
        }
        
        EmailAlias alias = aliasOpt.get();
        String fullEmail = alias.getFullEmail();
        
        // 统计该别名的邮件数据
        long totalCount = messageRepository.countByToAddressesContaining(fullEmail);
        long unreadCount = messageRepository.countByToAddressesContainingAndIsReadFalse(fullEmail);
        long todayCount = messageRepository.countByToAddressesContainingAndReceivedAtAfter(
            fullEmail, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        );
        
        return new EmailAliasStats(alias, totalCount, unreadCount, todayCount);
    }
    
    /**
     * 处理收到的邮件（模拟邮件接收）
     */
    public EmailMessage receiveEmail(EmailReceiveRequest request) {
        logger.info("收到新邮件: {} -> {}", request.getFromAddress(), request.getToAddress());
        
        // 查找目标别名
        Optional<EmailAlias> aliasOpt = aliasRepository.findByFullEmailAddress(request.getToAddress());
        if (aliasOpt.isEmpty()) {
            logger.warn("未找到目标别名: {}", request.getToAddress());
            return null;
        }
        
        EmailAlias alias = aliasOpt.get();
        User user = alias.getUser();
        
        // 获取用户收件箱
        Optional<EmailFolder> inboxOpt = folderRepository.findInboxByUser(user);
        if (inboxOpt.isEmpty()) {
            logger.error("用户 {} 没有收件箱", user.getUsername());
            return null;
        }
        
        EmailFolder inbox = inboxOpt.get();
        
        // 创建邮件记录
        EmailMessage message = new EmailMessage(user, inbox, generateMessageId());
        message.setSubject(request.getSubject());
        message.setFromAddress(request.getFromAddress());
        message.setToAddresses(request.getToAddress()); // 实际应该是JSON格式，这里简化
        message.setBodyText(request.getBodyText());
        message.setBodyHtml(request.getBodyHtml());
        message.setReceivedAt(LocalDateTime.now());
        message.setMessageSize((long) (request.getBodyText().length() + request.getBodyHtml().length()));
        
        // 检查是否需要转发
        if (alias.getForwardTo() != null && !alias.getForwardTo().trim().isEmpty()) {
            logger.info("转发邮件从 {} 到 {}", request.getToAddress(), alias.getForwardTo());
            // TODO: 实现邮件转发逻辑
        }
        
        EmailMessage savedMessage = messageRepository.save(message);
        
        // 更新文件夹统计
        inbox.incrementMessageCount();
        inbox.incrementUnreadCount();
        folderRepository.save(inbox);
        
        // 处理邮件转发
        try {
            forwardingService.processForwarding(savedMessage);
        } catch (Exception e) {
            logger.error("邮件转发处理失败", e);
            // 不影响邮件接收的主流程
        }
        
        // 处理自动回复
        try {
            autoReplyService.processAutoReply(savedMessage);
        } catch (Exception e) {
            logger.error("自动回复处理失败", e);
            // 不影响邮件接收的主流程
        }
        
        logger.info("邮件已保存: ID={}, 标题={}", savedMessage.getId(), savedMessage.getSubject());
        
        return savedMessage;
    }
    
    /**
     * 通过IMAP接收邮件
     */
    @Async
    @Transactional
    public CompletableFuture<ReceiveResult> receiveEmailsViaIMAP(User user, IMAPConfig config) {
        logger.info("开始通过IMAP接收用户邮件: user={}, host={}", user.getUsername(), config.getHost());
        
        int receivedCount = 0;
        int errorCount = 0;
        
        try {
            // 连接IMAP服务器
            Store store = connectToIMAPStore(config);
            
            // 获取收件箱
            Folder inboxFolder = store.getFolder("INBOX");
            inboxFolder.open(Folder.READ_ONLY);
            
            // 获取未读邮件
            Message[] messages = inboxFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            
            logger.info("找到 {} 封未读邮件", messages.length);
            
            // 获取用户的收件箱文件夹
            EmailFolder userInbox = getUserInboxFolder(user);
            
            // 处理每封邮件
            for (Message message : messages) {
                try {
                    EmailMessage emailMessage = processIncomingMessage(message, user, userInbox);
                    if (emailMessage != null) {
                        receivedCount++;
                        logger.debug("成功接收邮件: subject={}", emailMessage.getSubject());
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("处理邮件失败: subject=" + getMessageSubject(message), e);
                }
            }
            
            // 关闭连接
            inboxFolder.close(false);
            store.close();
            
            logger.info("IMAP邮件接收完成: user={}, 成功={}, 失败={}", 
                user.getUsername(), receivedCount, errorCount);
            
            return CompletableFuture.completedFuture(
                new ReceiveResult(true, receivedCount, errorCount, "IMAP接收完成"));
            
        } catch (Exception e) {
            logger.error("IMAP邮件接收失败: user=" + user.getUsername(), e);
            return CompletableFuture.completedFuture(
                new ReceiveResult(false, receivedCount, errorCount, "IMAP接收失败: " + e.getMessage()));
        }
    }
    
    /**
     * 通过POP3接收邮件
     */
    @Async
    @Transactional
    public CompletableFuture<ReceiveResult> receiveEmailsViaPOP3(User user, POP3Config config) {
        logger.info("开始通过POP3接收用户邮件: user={}, host={}", user.getUsername(), config.getHost());
        
        int receivedCount = 0;
        int errorCount = 0;
        
        try {
            // 连接POP3服务器
            Store store = connectToPOP3Store(config);
            
            // 获取收件箱
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            
            // 获取所有邮件
            Message[] messages = folder.getMessages();
            
            logger.info("找到 {} 封邮件", messages.length);
            
            // 获取用户的收件箱文件夹
            EmailFolder userInbox = getUserInboxFolder(user);
            
            // 处理每封邮件
            for (Message message : messages) {
                try {
                    // 检查邮件是否已存在
                    String messageId = getMessageId(message);
                    if (!messageRepository.existsByMessageId(messageId)) {
                        EmailMessage emailMessage = processIncomingMessage(message, user, userInbox);
                        if (emailMessage != null) {
                            receivedCount++;
                            logger.debug("成功接收邮件: subject={}", emailMessage.getSubject());
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("处理邮件失败: subject=" + getMessageSubject(message), e);
                }
            }
            
            // 关闭连接
            folder.close(false);
            store.close();
            
            logger.info("POP3邮件接收完成: user={}, 成功={}, 失败={}", 
                user.getUsername(), receivedCount, errorCount);
            
            return CompletableFuture.completedFuture(
                new ReceiveResult(true, receivedCount, errorCount, "POP3接收完成"));
            
        } catch (Exception e) {
            logger.error("POP3邮件接收失败: user=" + user.getUsername(), e);
            return CompletableFuture.completedFuture(
                new ReceiveResult(false, receivedCount, errorCount, "POP3接收失败: " + e.getMessage()));
        }
    }
    
    /**
     * 连接到IMAP服务器
     */
    private Store connectToIMAPStore(IMAPConfig config) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", config.getHost());
        props.put("mail.imaps.port", config.getPort());
        props.put("mail.imaps.ssl.enable", config.isUseSsl());
        props.put("mail.imaps.ssl.trust", "*");
        
        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(config.getHost(), config.getUsername(), config.getPassword());
        
        return store;
    }
    
    /**
     * 连接到POP3服务器
     */
    private Store connectToPOP3Store(POP3Config config) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "pop3s");
        props.put("mail.pop3s.host", config.getHost());
        props.put("mail.pop3s.port", config.getPort());
        props.put("mail.pop3s.ssl.enable", config.isUseSsl());
        props.put("mail.pop3s.ssl.trust", "*");
        
        Session session = Session.getInstance(props);
        Store store = session.getStore("pop3s");
        store.connect(config.getHost(), config.getUsername(), config.getPassword());
        
        return store;
    }
    
    /**
     * 处理接收到的邮件消息
     */
    private EmailMessage processIncomingMessage(Message message, User user, EmailFolder folder) 
            throws MessagingException, IOException {
        
        // 创建邮件消息实体
        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setUser(user);
        emailMessage.setFolder(folder);
        
        // 设置消息ID
        String messageId = getMessageId(message);
        emailMessage.setMessageId(messageId);
        
        // 设置主题
        String subject = message.getSubject();
        if (subject != null) {
            subject = MimeUtility.decodeText(subject);
        }
        emailMessage.setSubject(subject);
        
        // 设置发件人
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            emailMessage.setFromAddress(fromAddresses[0].toString());
        }
        
        // 设置收件人
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null) {
            emailMessage.setToAddresses(addressesToJson(toAddresses));
        }
        
        // 设置抄送
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
        if (ccAddresses != null) {
            emailMessage.setCcAddresses(addressesToJson(ccAddresses));
        }
        
        // 设置密送
        Address[] bccAddresses = message.getRecipients(Message.RecipientType.BCC);
        if (bccAddresses != null) {
            emailMessage.setBccAddresses(addressesToJson(bccAddresses));
        }
        
        // 设置回复地址
        Address[] replyToAddresses = message.getReplyTo();
        if (replyToAddresses != null && replyToAddresses.length > 0) {
            emailMessage.setReplyTo(replyToAddresses[0].toString());
        }
        
        // 设置时间
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            emailMessage.setSentAt(LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault()));
        }
        
        Date receivedDate = message.getReceivedDate();
        if (receivedDate != null) {
            emailMessage.setReceivedAt(LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault()));
        } else {
            emailMessage.setReceivedAt(LocalDateTime.now());
        }
        
        // 设置优先级
        String[] priority = message.getHeader("X-Priority");
        if (priority != null && priority.length > 0) {
            try {
                emailMessage.setPriorityLevel(Integer.parseInt(priority[0]));
            } catch (NumberFormatException e) {
                emailMessage.setPriorityLevel(3); // 默认普通优先级
            }
        }
        
        // 设置邮件大小
        emailMessage.setMessageSize((long) message.getSize());
        
        // 处理邮件内容
        processMessageContent(message, emailMessage);
        
        // 保存邮件
        emailMessage = messageRepository.save(emailMessage);
        
        // 处理附件
        processAttachments(message, emailMessage);
        
        // 更新文件夹统计
        updateFolderStatistics(folder);
        
        return emailMessage;
    }
    
    /**
     * 处理邮件内容
     */
    private void processMessageContent(Message message, EmailMessage emailMessage) 
            throws MessagingException, IOException {
        
        Object content = message.getContent();
        
        if (content instanceof String) {
            // 纯文本邮件
            if (message.isMimeType("text/plain")) {
                emailMessage.setBodyText((String) content);
            } else if (message.isMimeType("text/html")) {
                emailMessage.setBodyHtml((String) content);
            }
        } else if (content instanceof MimeMultipart) {
            // 多部分邮件
            processMimeMultipart((MimeMultipart) content, emailMessage);
        }
    }
    
    /**
     * 处理多部分MIME邮件
     */
    private void processMimeMultipart(MimeMultipart multipart, EmailMessage emailMessage) 
            throws MessagingException, IOException {
        
        int count = multipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            if (bodyPart.isMimeType("text/plain")) {
                // 纯文本部分
                emailMessage.setBodyText((String) bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                // HTML部分
                emailMessage.setBodyHtml((String) bodyPart.getContent());
            } else if (bodyPart.getDisposition() != null && 
                      bodyPart.getDisposition().equalsIgnoreCase(Part.ATTACHMENT)) {
                // 附件部分（在processAttachments中处理）
                continue;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                // 嵌套的多部分
                processMimeMultipart((MimeMultipart) bodyPart.getContent(), emailMessage);
            }
        }
    }
    
    /**
     * 处理邮件附件
     */
    private void processAttachments(Message message, EmailMessage emailMessage) 
            throws MessagingException, IOException {
        
        if (!(message.getContent() instanceof MimeMultipart)) {
            return;
        }
        
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        int count = multipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                (bodyPart.getFileName() != null && !bodyPart.getFileName().isEmpty())) {
                
                // 处理附件
                processAttachment(bodyPart, emailMessage);
            }
        }
    }
    
    /**
     * 处理单个附件
     */
    private void processAttachment(BodyPart bodyPart, EmailMessage emailMessage) 
            throws MessagingException, IOException {
        
        String filename = bodyPart.getFileName();
        if (filename == null || filename.trim().isEmpty()) {
            return;
        }
        
        // 解码文件名
        filename = MimeUtility.decodeText(filename);
        
        // 检查附件大小
        int size = bodyPart.getSize();
        if (size > maxAttachmentSize) {
            logger.warn("附件过大，跳过: filename={}, size={}", filename, size);
            return;
        }
        
        // 创建附件实体
        EmailAttachment attachment = new EmailAttachment();
        attachment.setMessage(emailMessage);
        attachment.setFilename(filename);
        attachment.setContentType(bodyPart.getContentType());
        attachment.setFileSize((long) size);
        
        // 检查是否为内嵌附件
        String[] contentId = bodyPart.getHeader("Content-ID");
        if (contentId != null && contentId.length > 0) {
            attachment.setContentId(contentId[0]);
            attachment.setIsInline(true);
        }
        
        // 保存附件文件
        String storagePath = saveAttachmentFile(bodyPart, emailMessage, filename);
        attachment.setStoragePath(storagePath);
        
        // 计算文件哈希
        String fileHash = calculateFileHash(storagePath);
        attachment.setFileHash(fileHash);
        
        // 保存附件记录
        attachmentRepository.save(attachment);
        
        logger.debug("附件保存成功: filename={}, size={}", filename, size);
    }
    
    /**
     * 保存附件文件到本地存储
     */
    private String saveAttachmentFile(BodyPart bodyPart, EmailMessage emailMessage, String filename) 
            throws IOException, MessagingException {
        
        // 创建存储目录
        String userDir = "user_" + emailMessage.getUser().getId();
        String messageDir = "msg_" + emailMessage.getId();
        Path attachmentDir = Paths.get(mailStoragePath, "attachments", userDir, messageDir);
        Files.createDirectories(attachmentDir);
        
        // 生成安全的文件名
        String safeFilename = generateSafeFilename(filename);
        Path filePath = attachmentDir.resolve(safeFilename);
        
        // 保存文件
        try (InputStream is = bodyPart.getInputStream();
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        return filePath.toString();
    }
    
    /**
     * 生成安全的文件名
     */
    private String generateSafeFilename(String filename) {
        // 移除危险字符
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // 限制长度
        if (safeFilename.length() > 100) {
            String extension = "";
            int dotIndex = safeFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = safeFilename.substring(dotIndex);
                safeFilename = safeFilename.substring(0, Math.min(100 - extension.length(), dotIndex));
            } else {
                safeFilename = safeFilename.substring(0, 100);
            }
            safeFilename += extension;
        }
        
        // 添加时间戳避免重名
        return System.currentTimeMillis() + "_" + safeFilename;
    }
    
    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(String filePath) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            logger.warn("计算文件哈希失败: " + filePath, e);
            return "";
        }
    }
    
    /**
     * 获取用户收件箱文件夹
     */
    private EmailFolder getUserInboxFolder(User user) {
        return folderRepository.findByUserAndFolderType(user, EmailFolder.FolderType.INBOX)
            .orElseThrow(() -> new RuntimeException("用户收件箱文件夹不存在"));
    }
    
    /**
     * 更新文件夹统计信息
     */
    private void updateFolderStatistics(EmailFolder folder) {
        int messageCount = messageRepository.countByFolder(folder);
        int unreadCount = messageRepository.countByFolderAndIsRead(folder, false);
        
        folder.setMessageCount(messageCount);
        folder.setUnreadCount(unreadCount);
        folderRepository.save(folder);
    }
    
    /**
     * 获取消息ID
     */
    private String getMessageId(Message message) throws MessagingException {
        String[] messageIds = message.getHeader("Message-ID");
        if (messageIds != null && messageIds.length > 0) {
            return messageIds[0];
        }
        
        // 如果没有Message-ID，生成一个
        return "generated-" + System.currentTimeMillis() + "-" + message.hashCode();
    }
    
    /**
     * 获取邮件主题（安全方式）
     */
    private String getMessageSubject(Message message) {
        try {
            return message.getSubject();
        } catch (MessagingException e) {
            return "Unknown Subject";
        }
    }
    
    /**
     * 将地址数组转换为JSON格式
     */
    private String addressesToJson(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < addresses.length; i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(addresses[i].toString().replace("\"", "\\\"")).append("\"");
        }
        json.append("]");
        
        return json.toString();
    }
    
    /**
     * 获取用户所有别名的邮件概览
     */
    @Transactional(readOnly = true)
    public List<EmailAliasOverview> getUserAliasesOverview(User user) {
        List<EmailAlias> aliases = aliasRepository.findByUserWithDomainFetch(user);
        
        return aliases.stream().map(alias -> {
            String fullEmail = alias.getFullEmail();
            long totalMessages = messageRepository.countByToAddressesContaining(fullEmail);
            long unreadMessages = messageRepository.countByToAddressesContainingAndIsReadFalse(fullEmail);
            
            return new EmailAliasOverview(
                alias.getId(),
                alias.getAliasEmail(),
                alias.getDomain().getDomainName(),
                fullEmail,
                alias.getDisplayName(),
                alias.getDescription(),
                alias.getExternalAliasId(),
                alias.getIsActive(),
                totalMessages,
                unreadMessages,
                alias.getCreatedAt()
            );
        }).toList();
    }
    
    /**
     * 标记别名的所有邮件为已读
     */
    public void markAliasMessagesAsRead(Long aliasId, User user) {
        Optional<EmailAlias> aliasOpt = aliasRepository.findById(aliasId);
        if (aliasOpt.isEmpty() || !aliasOpt.get().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("别名不存在或无权限");
        }
        
        String fullEmail = aliasOpt.get().getFullEmail();
        messageRepository.markMessagesAsReadByToAddress(fullEmail);
        
        logger.info("已将别名 {} 的所有邮件标记为已读", fullEmail);
    }
    
    private String generateMessageId() {
        return UUID.randomUUID().toString() + "@secure-email-system";
    }
    
    // 内部数据类
    public static class EmailReceiveRequest {
        private String fromAddress;
        private String toAddress;
        private String subject;
        private String bodyText;
        private String bodyHtml;
        
        // 构造函数
        public EmailReceiveRequest(String fromAddress, String toAddress, String subject, String bodyText, String bodyHtml) {
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.subject = subject;
            this.bodyText = bodyText;
            this.bodyHtml = bodyHtml;
        }
        
        // Getters and Setters
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
        public String getBodyHtml() { return bodyHtml; }
        public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    }
    
    public static class EmailAliasStats {
        private EmailAlias alias;
        private long totalMessages;
        private long unreadMessages;
        private long todayMessages;
        
        public EmailAliasStats(EmailAlias alias, long totalMessages, long unreadMessages, long todayMessages) {
            this.alias = alias;
            this.totalMessages = totalMessages;
            this.unreadMessages = unreadMessages;
            this.todayMessages = todayMessages;
        }
        
        // Getters
        public EmailAlias getAlias() { return alias; }
        public long getTotalMessages() { return totalMessages; }
        public long getUnreadMessages() { return unreadMessages; }
        public long getTodayMessages() { return todayMessages; }
    }
    
    public static class EmailAliasOverview {
        private Long aliasId;
        private String aliasEmail;
        private String domainName;
        private String fullEmail;
        private String displayName; // 自定义显示名称
        private String description; // 别名描述
        private String externalAliasId; // 外部别名ID
        private Boolean isActive;
        private long totalMessages;
        private long unreadMessages;
        private LocalDateTime createdAt;
        
        public EmailAliasOverview(Long aliasId, String aliasEmail, String domainName, String fullEmail,
                                String displayName, String description, String externalAliasId,
                                Boolean isActive, long totalMessages, long unreadMessages, LocalDateTime createdAt) {
            this.aliasId = aliasId;
            this.aliasEmail = aliasEmail;
            this.domainName = domainName;
            this.fullEmail = fullEmail;
            this.displayName = displayName;
            this.description = description;
            this.externalAliasId = externalAliasId;
            this.isActive = isActive;
            this.totalMessages = totalMessages;
            this.unreadMessages = unreadMessages;
            this.createdAt = createdAt;
        }
        
        // Getters
        public Long getAliasId() { return aliasId; }
        public String getAliasEmail() { return aliasEmail; }
        public String getDomainName() { return domainName; }
        public String getFullEmail() { return fullEmail; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getExternalAliasId() { return externalAliasId; }
        public Boolean getIsActive() { return isActive; }
        public long getTotalMessages() { return totalMessages; }
        public long getUnreadMessages() { return unreadMessages; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        /**
         * 获取应该显示的名称
         * 优先使用自定义显示名称，如果没有则使用完整邮箱地址
         */
        public String getDisplayNameOrEmail() {
            return displayName != null && !displayName.trim().isEmpty() 
                ? displayName 
                : fullEmail;
        }
    }
    
    // IMAP/POP3 配置类
    
    /**
     * 接收结果
     */
    public static class ReceiveResult {
        private final boolean success;
        private final int receivedCount;
        private final int errorCount;
        private final String message;
        
        public ReceiveResult(boolean success, int receivedCount, int errorCount, String message) {
            this.success = success;
            this.receivedCount = receivedCount;
            this.errorCount = errorCount;
            this.message = message;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public int getReceivedCount() { return receivedCount; }
        public int getErrorCount() { return errorCount; }
        public String getMessage() { return message; }
    }
    
    /**
     * IMAP配置
     */
    public static class IMAPConfig {
        private String host;
        private int port = 993;
        private String username;
        private String password;
        private boolean useSsl = true;
        
        // Constructors, getters and setters
        public IMAPConfig() {}
        
        public IMAPConfig(String host, String username, String password) {
            this.host = host;
            this.username = username;
            this.password = password;
        }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isUseSsl() { return useSsl; }
        public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
    }
    
    /**
     * POP3配置
     */
    public static class POP3Config {
        private String host;
        private int port = 995;
        private String username;
        private String password;
        private boolean useSsl = true;
        
        // Constructors, getters and setters
        public POP3Config() {}
        
        public POP3Config(String host, String username, String password) {
            this.host = host;
            this.username = username;
            this.password = password;
        }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isUseSsl() { return useSsl; }
        public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
    }
}