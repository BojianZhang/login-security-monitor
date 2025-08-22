package com.security.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 邮件投递日志实体
 */
@Entity
@Table(name = "email_delivery_logs")
public class EmailDeliveryLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private EmailMessage message;
    
    @Column(name = "message_id_string", length = 255)
    private String messageIdString; // 邮件Message-ID
    
    @Column(name = "queue_id", length = 100)
    private String queueId; // 队列ID
    
    @Column(name = "from_address", nullable = false, length = 320)
    private String fromAddress;
    
    @Column(name = "to_address", nullable = false, length = 320)
    private String toAddress;
    
    @Column(name = "delivery_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;
    
    @Column(name = "smtp_response_code")
    private Integer smtpResponseCode;
    
    @Column(name = "smtp_response_message", length = 1000)
    private String smtpResponseMessage;
    
    @Column(name = "delivery_server", length = 255)
    private String deliveryServer; // 投递服务器
    
    @Column(name = "relay_host", length = 255)
    private String relayHost; // 中继主机
    
    @Column(name = "delivery_attempts", nullable = false)
    private Integer deliveryAttempts = 1;
    
    @Column(name = "delivery_delay_seconds")
    private Long deliveryDelaySeconds; // 投递延迟
    
    @Column(name = "message_size")
    private Long messageSize; // 邮件大小
    
    @Column(name = "delivery_method", length = 20)
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod = DeliveryMethod.SMTP;
    
    @Column(name = "transport_security", length = 20)
    @Enumerated(EnumType.STRING)
    private TransportSecurity transportSecurity;
    
    @Column(name = "sender_ip", length = 45)
    private String senderIp;
    
    @Column(name = "recipient_domain", length = 255)
    private String recipientDomain;
    
    @Column(name = "bounce_reason", length = 500)
    private String bounceReason; // 退信原因
    
    @Column(name = "retry_until")
    private LocalDateTime retryUntil; // 重试截止时间
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // 投递状态枚举
    public enum DeliveryStatus {
        QUEUED("队列中"),
        SENDING("发送中"),
        DELIVERED("已投递"),
        BOUNCED("已退回"),
        DEFERRED("已延迟"),
        FAILED("失败"),
        REJECTED("已拒绝"),
        EXPIRED("已过期");
        
        private final String description;
        
        DeliveryStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 投递方法枚举
    public enum DeliveryMethod {
        SMTP("SMTP"),
        LOCAL("本地投递"),
        PIPE("管道"),
        MAILDIR("Maildir"),
        MBOX("mbox");
        
        private final String description;
        
        DeliveryMethod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 传输安全枚举
    public enum TransportSecurity {
        NONE("无加密"),
        STARTTLS("STARTTLS"),
        TLS("TLS/SSL"),
        SMTPS("SMTPS");
        
        private final String description;
        
        TransportSecurity(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 构造函数
    public EmailDeliveryLog() {
        this.createdAt = LocalDateTime.now();
    }
    
    public EmailDeliveryLog(String fromAddress, String toAddress, DeliveryStatus status) {
        this();
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.deliveryStatus = status;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public EmailMessage getMessage() {
        return message;
    }
    
    public void setMessage(EmailMessage message) {
        this.message = message;
    }
    
    public String getMessageIdString() {
        return messageIdString;
    }
    
    public void setMessageIdString(String messageIdString) {
        this.messageIdString = messageIdString;
    }
    
    public String getQueueId() {
        return queueId;
    }
    
    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }
    
    public String getFromAddress() {
        return fromAddress;
    }
    
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    public String getToAddress() {
        return toAddress;
    }
    
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
    
    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    
    public Integer getSmtpResponseCode() {
        return smtpResponseCode;
    }
    
    public void setSmtpResponseCode(Integer smtpResponseCode) {
        this.smtpResponseCode = smtpResponseCode;
    }
    
    public String getSmtpResponseMessage() {
        return smtpResponseMessage;
    }
    
    public void setSmtpResponseMessage(String smtpResponseMessage) {
        this.smtpResponseMessage = smtpResponseMessage;
    }
    
    public String getDeliveryServer() {
        return deliveryServer;
    }
    
    public void setDeliveryServer(String deliveryServer) {
        this.deliveryServer = deliveryServer;
    }
    
    public String getRelayHost() {
        return relayHost;
    }
    
    public void setRelayHost(String relayHost) {
        this.relayHost = relayHost;
    }
    
    public Integer getDeliveryAttempts() {
        return deliveryAttempts;
    }
    
    public void setDeliveryAttempts(Integer deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }
    
    public Long getDeliveryDelaySeconds() {
        return deliveryDelaySeconds;
    }
    
    public void setDeliveryDelaySeconds(Long deliveryDelaySeconds) {
        this.deliveryDelaySeconds = deliveryDelaySeconds;
    }
    
    public Long getMessageSize() {
        return messageSize;
    }
    
    public void setMessageSize(Long messageSize) {
        this.messageSize = messageSize;
    }
    
    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public TransportSecurity getTransportSecurity() {
        return transportSecurity;
    }
    
    public void setTransportSecurity(TransportSecurity transportSecurity) {
        this.transportSecurity = transportSecurity;
    }
    
    public String getSenderIp() {
        return senderIp;
    }
    
    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }
    
    public String getRecipientDomain() {
        return recipientDomain;
    }
    
    public void setRecipientDomain(String recipientDomain) {
        this.recipientDomain = recipientDomain;
    }
    
    public String getBounceReason() {
        return bounceReason;
    }
    
    public void setBounceReason(String bounceReason) {
        this.bounceReason = bounceReason;
    }
    
    public LocalDateTime getRetryUntil() {
        return retryUntil;
    }
    
    public void setRetryUntil(LocalDateTime retryUntil) {
        this.retryUntil = retryUntil;
    }
    
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 增加投递尝试次数
     */
    public void incrementDeliveryAttempts() {
        this.deliveryAttempts++;
    }
    
    /**
     * 标记为已投递
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    /**
     * 标记为退回
     */
    public void markAsBounced(String reason) {
        this.deliveryStatus = DeliveryStatus.BOUNCED;
        this.bounceReason = reason;
    }
    
    /**
     * 标记为延迟
     */
    public void markAsDeferred(LocalDateTime retryUntil) {
        this.deliveryStatus = DeliveryStatus.DEFERRED;
        this.retryUntil = retryUntil;
    }
    
    /**
     * 检查是否为成功投递
     */
    public boolean isSuccessfulDelivery() {
        return deliveryStatus == DeliveryStatus.DELIVERED;
    }
    
    /**
     * 检查是否为失败投递
     */
    public boolean isFailedDelivery() {
        return deliveryStatus == DeliveryStatus.FAILED || 
               deliveryStatus == DeliveryStatus.BOUNCED ||
               deliveryStatus == DeliveryStatus.REJECTED ||
               deliveryStatus == DeliveryStatus.EXPIRED;
    }
    
    /**
     * 提取收件人域名
     */
    public void extractRecipientDomain() {
        if (toAddress != null && toAddress.contains("@")) {
            this.recipientDomain = toAddress.substring(toAddress.indexOf("@") + 1);
        }
    }
}