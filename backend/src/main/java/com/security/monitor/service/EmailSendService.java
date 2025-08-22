package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件发送服务
 * 支持SMTP协议发送邮件，包括HTML邮件和附件
 */
@Service
public class EmailSendService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailSendService.class);
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Autowired
    private EmailFolderRepository folderRepository;
    
    @Autowired
    private EmailAttachmentRepository attachmentRepository;
    
    @Autowired
    private EmailQueueRepository queueRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.mail.smtp.host:localhost}")
    private String smtpHost;
    
    @Value("${app.mail.smtp.port:587}")
    private int smtpPort;
    
    @Value("${app.mail.smtp.username:}")
    private String smtpUsername;
    
    @Value("${app.mail.smtp.password:}")
    private String smtpPassword;
    
    @Value("${app.mail.smtp.ssl.enable:false}")
    private boolean smtpSslEnable;
    
    @Value("${app.mail.smtp.starttls.enable:true}")
    private boolean smtpStartTlsEnable;
    
    @Value("${app.mail.storage.path:/opt/mail-storage}")
    private String mailStoragePath;
    
    @Value("${app.mail.attachment.max-size:52428800}") // 50MB
    private long maxAttachmentSize;
    
    /**
     * 发送邮件
     */
    @Transactional
    public EmailMessage sendEmail(User sender, EmailSendRequest request) {
        logger.info("用户 {} 发送邮件: {} -> {}", sender.getUsername(), request.getFromAddress(), request.getToAddresses());
        
        try {
            // 验证发送权限
            validateSendPermission(sender, request.getFromAddress());
            
            // 创建邮件记录
            EmailMessage message = createEmailMessage(sender, request);
            
            // 保存到发件箱
            EmailFolder sentFolder = getSentFolder(sender);
            message.setFolder(sentFolder);
            message = messageRepository.save(message);
            
            // 处理附件
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                processOutgoingAttachments(message, request.getAttachments());
            }
            
            // 发送邮件
            boolean sendSuccess = sendEmailViaSMTP(message, request);
            
            if (sendSuccess) {
                message.setSentAt(LocalDateTime.now());
                logger.info("邮件发送成功: ID={}", message.getId());
            } else {
                // 发送失败，加入重试队列
                addToRetryQueue(message, request);
                logger.warn("邮件发送失败，已加入重试队列: ID={}", message.getId());
            }
            
            // 更新文件夹统计
            updateFolderStatistics(sentFolder);
            
            return messageRepository.save(message);
            
        } catch (Exception e) {
            logger.error("邮件发送异常", e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 异步发送邮件
     */
    @Async
    @Transactional
    public CompletableFuture<SendResult> sendEmailAsync(User sender, EmailSendRequest request) {
        try {
            EmailMessage message = sendEmail(sender, request);
            boolean success = message.getSentAt() != null;
            
            return CompletableFuture.completedFuture(
                new SendResult(success, message.getId(), success ? "发送成功" : "发送失败"));
                
        } catch (Exception e) {
            logger.error("异步邮件发送失败", e);
            return CompletableFuture.completedFuture(
                new SendResult(false, null, "发送失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量发送邮件
     */
    @Async
    @Transactional
    public CompletableFuture<BatchSendResult> sendBatchEmails(User sender, List<EmailSendRequest> requests) {
        logger.info("用户 {} 批量发送 {} 封邮件", sender.getUsername(), requests.size());
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (EmailSendRequest request : requests) {
            try {
                EmailMessage message = sendEmail(sender, request);
                if (message.getSentAt() != null) {
                    successCount++;
                } else {
                    failCount++;
                    errors.add("发送到 " + request.getToAddresses() + " 失败");
                }
            } catch (Exception e) {
                failCount++;
                errors.add("发送到 " + request.getToAddresses() + " 异常: " + e.getMessage());
                logger.error("批量发送邮件失败", e);
            }
        }
        
        logger.info("批量发送完成: 成功={}, 失败={}", successCount, failCount);
        
        return CompletableFuture.completedFuture(
            new BatchSendResult(successCount, failCount, errors));
    }
    
    /**
     * 通过SMTP发送邮件
     */
    private boolean sendEmailViaSMTP(EmailMessage message, EmailSendRequest request) {
        try {
            // 创建SMTP会话
            Session session = createSMTPSession();
            
            // 创建邮件消息
            MimeMessage mimeMessage = createMimeMessage(session, message, request);
            
            // 发送邮件
            Transport.send(mimeMessage);
            
            logger.debug("SMTP邮件发送成功: subject={}", message.getSubject());
            return true;
            
        } catch (Exception e) {
            logger.error("SMTP邮件发送失败: subject=" + message.getSubject(), e);
            return false;
        }
    }
    
    /**
     * 创建SMTP会话
     */
    private Session createSMTPSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", smtpSslEnable);
        props.put("mail.smtp.starttls.enable", smtpStartTlsEnable);
        props.put("mail.smtp.ssl.trust", "*");
        
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
    }
    
    /**
     * 创建MIME邮件消息
     */
    private MimeMessage createMimeMessage(Session session, EmailMessage message, EmailSendRequest request) 
            throws MessagingException, IOException {
        
        MimeMessage mimeMessage = new MimeMessage(session);
        
        // 设置发件人
        mimeMessage.setFrom(new InternetAddress(request.getFromAddress()));
        
        // 设置收件人
        String[] toAddresses = request.getToAddresses().split("[,;]");
        for (String address : toAddresses) {
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(address.trim()));
        }
        
        // 设置抄送
        if (request.getCcAddresses() != null && !request.getCcAddresses().trim().isEmpty()) {
            String[] ccAddresses = request.getCcAddresses().split("[,;]");
            for (String address : ccAddresses) {
                mimeMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(address.trim()));
            }
        }
        
        // 设置密送
        if (request.getBccAddresses() != null && !request.getBccAddresses().trim().isEmpty()) {
            String[] bccAddresses = request.getBccAddresses().split("[,;]");
            for (String address : bccAddresses) {
                mimeMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(address.trim()));
            }
        }
        
        // 设置主题
        mimeMessage.setSubject(request.getSubject(), "UTF-8");
        
        // 设置回复地址
        if (request.getReplyTo() != null && !request.getReplyTo().trim().isEmpty()) {
            mimeMessage.setReplyTo(InternetAddress.parse(request.getReplyTo()));
        }
        
        // 设置优先级
        if (request.getPriorityLevel() != null) {
            switch (request.getPriorityLevel()) {
                case 1:
                    mimeMessage.setHeader("X-Priority", "1");
                    mimeMessage.setHeader("X-MSMail-Priority", "High");
                    break;
                case 5:
                    mimeMessage.setHeader("X-Priority", "5");
                    mimeMessage.setHeader("X-MSMail-Priority", "Low");
                    break;
                default:
                    mimeMessage.setHeader("X-Priority", "3");
                    mimeMessage.setHeader("X-MSMail-Priority", "Normal");
            }
        }
        
        // 设置内容
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            // 有附件的邮件
            createMultipartContent(mimeMessage, request);
        } else {
            // 无附件的邮件
            createSimpleContent(mimeMessage, request);
        }
        
        // 设置发送时间
        mimeMessage.setSentDate(new Date());
        
        return mimeMessage;
    }
    
    /**
     * 创建简单邮件内容（无附件）
     */
    private void createSimpleContent(MimeMessage mimeMessage, EmailSendRequest request) 
            throws MessagingException {
        
        if (request.getBodyHtml() != null && !request.getBodyHtml().trim().isEmpty()) {
            // HTML邮件
            mimeMessage.setContent(request.getBodyHtml(), "text/html; charset=UTF-8");
        } else {
            // 纯文本邮件
            mimeMessage.setText(request.getBodyText(), "UTF-8");
        }
    }
    
    /**
     * 创建多部分邮件内容（有附件）
     */
    private void createMultipartContent(MimeMessage mimeMessage, EmailSendRequest request) 
            throws MessagingException, IOException {
        
        MimeMultipart multipart = new MimeMultipart();
        
        // 创建邮件正文部分
        MimeBodyPart textPart = new MimeBodyPart();
        if (request.getBodyHtml() != null && !request.getBodyHtml().trim().isEmpty()) {
            textPart.setContent(request.getBodyHtml(), "text/html; charset=UTF-8");
        } else {
            textPart.setText(request.getBodyText(), "UTF-8");
        }
        multipart.addBodyPart(textPart);
        
        // 添加附件
        for (MultipartFile attachment : request.getAttachments()) {
            if (!attachment.isEmpty()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new javax.activation.DataHandler(
                    new javax.activation.DataSource() {
                        @Override
                        public java.io.InputStream getInputStream() throws IOException {
                            return attachment.getInputStream();
                        }
                        
                        @Override
                        public java.io.OutputStream getOutputStream() throws IOException {
                            throw new UnsupportedOperationException();
                        }
                        
                        @Override
                        public String getContentType() {
                            return attachment.getContentType();
                        }
                        
                        @Override
                        public String getName() {
                            return attachment.getOriginalFilename();
                        }
                    }
                ));
                attachmentPart.setFileName(MimeUtility.encodeText(attachment.getOriginalFilename(), "UTF-8", null));
                multipart.addBodyPart(attachmentPart);
            }
        }
        
        mimeMessage.setContent(multipart);
    }
    
    /**
     * 创建邮件消息记录
     */
    private EmailMessage createEmailMessage(User sender, EmailSendRequest request) {
        EmailMessage message = new EmailMessage();
        message.setUser(sender);
        message.setMessageId(generateMessageId());
        message.setSubject(request.getSubject());
        message.setFromAddress(request.getFromAddress());
        message.setToAddresses(request.getToAddresses());
        message.setCcAddresses(request.getCcAddresses());
        message.setBccAddresses(request.getBccAddresses());
        message.setReplyTo(request.getReplyTo());
        message.setBodyText(request.getBodyText());
        message.setBodyHtml(request.getBodyHtml());
        message.setPriorityLevel(request.getPriorityLevel() != null ? request.getPriorityLevel() : 3);
        message.setReceivedAt(LocalDateTime.now());
        
        // 计算邮件大小
        long size = (request.getBodyText() != null ? request.getBodyText().length() : 0) +
                   (request.getBodyHtml() != null ? request.getBodyHtml().length() : 0);
        if (request.getAttachments() != null) {
            for (MultipartFile attachment : request.getAttachments()) {
                size += attachment.getSize();
            }
        }
        message.setMessageSize(size);
        
        return message;
    }
    
    /**
     * 处理发送邮件的附件
     */
    private void processOutgoingAttachments(EmailMessage message, List<MultipartFile> attachments) 
            throws IOException {
        
        for (MultipartFile file : attachments) {
            if (file.isEmpty()) continue;
            
            // 检查附件大小
            if (file.getSize() > maxAttachmentSize) {
                logger.warn("附件过大，跳过: filename={}, size={}", file.getOriginalFilename(), file.getSize());
                continue;
            }
            
            // 保存附件文件
            String storagePath = saveOutgoingAttachment(message, file);
            
            // 创建附件记录
            EmailAttachment attachment = new EmailAttachment();
            attachment.setMessage(message);
            attachment.setFilename(file.getOriginalFilename());
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(storagePath);
            attachment.setIsInline(false);
            
            // 计算文件哈希
            String fileHash = calculateFileHash(storagePath);
            attachment.setFileHash(fileHash);
            
            attachmentRepository.save(attachment);
            
            logger.debug("发送附件保存成功: filename={}, size={}", file.getOriginalFilename(), file.getSize());
        }
    }
    
    /**
     * 保存发送邮件的附件
     */
    private String saveOutgoingAttachment(EmailMessage message, MultipartFile file) throws IOException {
        // 创建存储目录
        String userDir = "user_" + message.getUser().getId();
        String messageDir = "msg_" + message.getId();
        Path attachmentDir = Paths.get(mailStoragePath, "attachments", userDir, messageDir);
        Files.createDirectories(attachmentDir);
        
        // 生成安全的文件名
        String safeFilename = generateSafeFilename(file.getOriginalFilename());
        Path filePath = attachmentDir.resolve(safeFilename);
        
        // 保存文件
        file.transferTo(filePath.toFile());
        
        return filePath.toString();
    }
    
    /**
     * 验证发送权限
     */
    private void validateSendPermission(User sender, String fromAddress) {
        // 检查用户是否有权限使用该发件地址
        if (!sender.getEmail().equals(fromAddress)) {
            // 检查是否是用户的别名
            boolean isValidAlias = sender.getEmailAliases().stream()
                .anyMatch(alias -> alias.getFullEmail().equals(fromAddress) && alias.getIsActive());
            
            if (!isValidAlias) {
                throw new RuntimeException("无权限使用发件地址: " + fromAddress);
            }
        }
    }
    
    /**
     * 获取用户发件箱
     */
    private EmailFolder getSentFolder(User user) {
        return folderRepository.findByUserAndFolderType(user, EmailFolder.FolderType.SENT)
            .orElseThrow(() -> new RuntimeException("用户发件箱文件夹不存在"));
    }
    
    /**
     * 添加到重试队列
     */
    private void addToRetryQueue(EmailMessage message, EmailSendRequest request) {
        EmailQueue queueItem = new EmailQueue();
        queueItem.setFromAddress(request.getFromAddress());
        queueItem.setToAddress(request.getToAddresses());
        queueItem.setSubject(request.getSubject());
        queueItem.setBodyText(request.getBodyText());
        queueItem.setBodyHtml(request.getBodyHtml());
        queueItem.setPriority(request.getPriorityLevel() != null ? request.getPriorityLevel() : 3);
        queueItem.setStatus(EmailQueue.QueueStatus.PENDING);
        queueItem.setScheduledAt(LocalDateTime.now().plusMinutes(5)); // 5分钟后重试
        
        queueRepository.save(queueItem);
    }
    
    /**
     * 更新文件夹统计
     */
    private void updateFolderStatistics(EmailFolder folder) {
        int messageCount = messageRepository.countByFolder(folder);
        folder.setMessageCount(messageCount);
        folderRepository.save(folder);
    }
    
    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return UUID.randomUUID().toString() + "@secure-email-system";
    }
    
    /**
     * 生成安全的文件名
     */
    private String generateSafeFilename(String filename) {
        if (filename == null) return "attachment_" + System.currentTimeMillis();
        
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
            try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
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
    
    // 内部数据类
    
    /**
     * 邮件发送请求
     */
    public static class EmailSendRequest {
        private String fromAddress;
        private String toAddresses;
        private String ccAddresses;
        private String bccAddresses;
        private String replyTo;
        private String subject;
        private String bodyText;
        private String bodyHtml;
        private Integer priorityLevel;
        private List<MultipartFile> attachments;
        
        // Constructors
        public EmailSendRequest() {}
        
        public EmailSendRequest(String fromAddress, String toAddresses, String subject, String bodyText) {
            this.fromAddress = fromAddress;
            this.toAddresses = toAddresses;
            this.subject = subject;
            this.bodyText = bodyText;
        }
        
        // Getters and Setters
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        
        public String getToAddresses() { return toAddresses; }
        public void setToAddresses(String toAddresses) { this.toAddresses = toAddresses; }
        
        public String getCcAddresses() { return ccAddresses; }
        public void setCcAddresses(String ccAddresses) { this.ccAddresses = ccAddresses; }
        
        public String getBccAddresses() { return bccAddresses; }
        public void setBccAddresses(String bccAddresses) { this.bccAddresses = bccAddresses; }
        
        public String getReplyTo() { return replyTo; }
        public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
        
        public String getBodyHtml() { return bodyHtml; }
        public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
        
        public Integer getPriorityLevel() { return priorityLevel; }
        public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
        
        public List<MultipartFile> getAttachments() { return attachments; }
        public void setAttachments(List<MultipartFile> attachments) { this.attachments = attachments; }
    }
    
    /**
     * 发送结果
     */
    public static class SendResult {
        private final boolean success;
        private final Long messageId;
        private final String message;
        
        public SendResult(boolean success, Long messageId, String message) {
            this.success = success;
            this.messageId = messageId;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public Long getMessageId() { return messageId; }
        public String getMessage() { return message; }
    }
    
    /**
     * 批量发送结果
     */
    public static class BatchSendResult {
        private final int successCount;
        private final int failCount;
        private final List<String> errors;
        
        public BatchSendResult(int successCount, int failCount, List<String> errors) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.errors = errors;
        }
        
        public int getSuccessCount() { return successCount; }
        public int getFailCount() { return failCount; }
        public List<String> getErrors() { return errors; }
        public int getTotalCount() { return successCount + failCount; }
    }
}