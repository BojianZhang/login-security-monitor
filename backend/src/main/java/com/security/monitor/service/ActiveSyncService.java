package com.security.monitor.service;

import com.security.monitor.model.*;
import com.security.monitor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.DatatypeConverter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ActiveSync服务
 */
@Service
@Transactional
public class ActiveSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActiveSyncService.class);
    
    @Autowired
    private ActiveSyncDeviceRepository deviceRepository;
    
    @Autowired
    private ActiveSyncFolderRepository folderRepository;
    
    @Autowired
    private ActiveSyncLogRepository logRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailFolderRepository emailFolderRepository;
    
    @Autowired
    private EmailMessageRepository messageRepository;
    
    @Value("${activesync.enabled:true}")
    private boolean activeSyncEnabled;
    
    @Value("${activesync.require-device-approval:false}")
    private boolean requireDeviceApproval;
    
    @Value("${activesync.max-devices-per-user:10}")
    private int maxDevicesPerUser;
    
    @Value("${activesync.default-heartbeat-interval:300}")
    private int defaultHeartbeatInterval;
    
    @Value("${activesync.max-sync-items:500}")
    private int maxSyncItems;
    
    /**
     * 注册新设备
     */
    public ActiveSyncDevice registerDevice(String username, String deviceId, String deviceType, 
                                         String userAgent, String clientIP) {
        
        if (!activeSyncEnabled) {
            throw new RuntimeException("ActiveSync服务已禁用");
        }
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查设备数量限制
        long deviceCount = deviceRepository.countByUserAndIsBlockedFalse(user);
        if (deviceCount >= maxDevicesPerUser) {
            throw new RuntimeException("设备数量已达上限");
        }
        
        // 检查设备是否已存在
        Optional<ActiveSyncDevice> existingDevice = deviceRepository.findByDeviceId(deviceId);
        if (existingDevice.isPresent()) {
            ActiveSyncDevice device = existingDevice.get();
            if (!device.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("设备已被其他用户注册");
            }
            // 更新设备信息
            updateDeviceInfo(device, deviceType, userAgent, clientIP);
            return device;
        }
        
        // 创建新设备
        ActiveSyncDevice device = new ActiveSyncDevice(user, deviceId, deviceType);
        device.setDeviceUserAgent(userAgent);
        device.setLastSyncIP(clientIP);
        device.setHeartbeatInterval(defaultHeartbeatInterval);
        device.setMaxItemsToSync(maxSyncItems);
        
        // 解析设备信息
        parseDeviceInfo(device, userAgent);
        
        // 设置初始状态
        if (requireDeviceApproval) {
            device.setStatus(ActiveSyncDevice.DeviceStatus.PENDING);
        } else {
            device.setStatus(ActiveSyncDevice.DeviceStatus.ALLOWED);
        }
        
        // 生成初始密钥
        device.generateNewSyncKey();
        device.generateNewFolderSyncKey();
        device.generateNewPolicyKey();
        
        device = deviceRepository.save(device);
        
        // 创建默认文件夹映射
        createDefaultFolders(device);
        
        // 记录日志
        logSyncActivity(device, null, ActiveSyncLog.SyncType.PROVISION, "DeviceRegistration", 
                       ActiveSyncLog.SyncStatus.SUCCESS, clientIP, userAgent);
        
        logger.info("新设备已注册: deviceId={}, user={}, type={}", deviceId, username, deviceType);
        
        return device;
    }
    
    /**
     * 处理文件夹同步
     */
    public FolderSyncResult processFolderSync(String deviceId, String syncKey, String clientIP) {
        
        ActiveSyncDevice device = getDeviceById(deviceId);
        validateDeviceAccess(device);
        
        FolderSyncResult result = new FolderSyncResult();
        result.setDeviceId(deviceId);
        result.setOldSyncKey(syncKey);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证同步密钥
            if (!validateSyncKey(device, syncKey, true)) {
                result.setStatus(ActiveSyncLog.SyncStatus.SYNC_ERROR);
                result.setErrorMessage("无效的同步密钥");
                return result;
            }
            
            // 获取用户邮件文件夹
            List<EmailFolder> emailFolders = emailFolderRepository.findByUserOrderByFolderName(device.getUser());
            
            // 获取现有ActiveSync文件夹
            List<ActiveSyncFolder> activeSyncFolders = folderRepository.findByDeviceOrderByFolderName(device);
            Map<Long, ActiveSyncFolder> existingFolders = activeSyncFolders.stream()
                .filter(f -> f.getEmailFolder() != null)
                .collect(Collectors.toMap(f -> f.getEmailFolder().getId(), f -> f));
            
            List<FolderInfo> addedFolders = new ArrayList<>();
            List<FolderInfo> changedFolders = new ArrayList<>();
            List<String> deletedFolders = new ArrayList<>();
            
            // 检查新增和修改的文件夹
            for (EmailFolder emailFolder : emailFolders) {
                ActiveSyncFolder activeSyncFolder = existingFolders.get(emailFolder.getId());
                
                if (activeSyncFolder == null) {
                    // 新增文件夹
                    activeSyncFolder = createActiveSyncFolder(device, emailFolder);
                    addedFolders.add(createFolderInfo(activeSyncFolder));
                } else {
                    // 检查是否有变更
                    if (!emailFolder.getFolderName().equals(activeSyncFolder.getFolderName())) {
                        activeSyncFolder.setFolderName(emailFolder.getFolderName());
                        activeSyncFolder.markChanged();
                        folderRepository.save(activeSyncFolder);
                        changedFolders.add(createFolderInfo(activeSyncFolder));
                    }
                    existingFolders.remove(emailFolder.getId());
                }
            }
            
            // 检查删除的文件夹
            for (ActiveSyncFolder deletedFolder : existingFolders.values()) {
                deletedFolders.add(deletedFolder.getFolderId());
                folderRepository.delete(deletedFolder);
            }
            
            // 生成新的同步密钥
            device.generateNewFolderSyncKey();
            device.setLastSyncTime(LocalDateTime.now());
            device.setLastSyncIP(clientIP);
            deviceRepository.save(device);
            
            result.setNewSyncKey(device.getFolderSyncKey());
            result.setAddedFolders(addedFolders);
            result.setChangedFolders(changedFolders);
            result.setDeletedFolders(deletedFolders);
            result.setStatus(ActiveSyncLog.SyncStatus.SUCCESS);
            
        } catch (Exception e) {
            logger.error("文件夹同步失败: deviceId=" + deviceId, e);
            result.setStatus(ActiveSyncLog.SyncStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 记录同步日志
            ActiveSyncLog syncLog = new ActiveSyncLog(device, ActiveSyncLog.SyncType.FOLDER_SYNC, "FolderSync");
            syncLog.setClientIP(clientIP);
            syncLog.setSyncKeySent(syncKey);
            syncLog.setSyncKeyReturned(result.getNewSyncKey());
            syncLog.setStatus(result.getStatus());
            syncLog.setErrorMessage(result.getErrorMessage());
            syncLog.setProcessingTimeMs(processingTime);
            logRepository.save(syncLog);
            
            // 更新设备统计
            device.updateSyncStats(result.getStatus() == ActiveSyncLog.SyncStatus.SUCCESS);
            deviceRepository.save(device);
        }
        
        return result;
    }
    
    /**
     * 处理内容同步
     */
    public SyncResult processSync(String deviceId, String folderId, String syncKey, 
                                SyncOptions options, String clientIP) {
        
        ActiveSyncDevice device = getDeviceById(deviceId);
        validateDeviceAccess(device);
        
        ActiveSyncFolder folder = folderRepository.findByDeviceAndFolderId(device, folderId)
            .orElseThrow(() -> new RuntimeException("文件夹不存在"));
        
        SyncResult result = new SyncResult();
        result.setDeviceId(deviceId);
        result.setFolderId(folderId);
        result.setOldSyncKey(syncKey);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证同步密钥
            if (!validateSyncKey(folder, syncKey)) {
                result.setStatus(ActiveSyncLog.SyncStatus.SYNC_ERROR);
                result.setErrorMessage("无效的文件夹同步密钥");
                return result;
            }
            
            // 处理客户端变更
            if (options.getClientChanges() != null) {
                processClientChanges(folder, options.getClientChanges(), result);
            }
            
            // 获取服务器变更
            List<SyncItem> serverChanges = getServerChanges(folder, options);
            result.setServerChanges(serverChanges);
            
            // 生成新的同步密钥
            folder.generateNewSyncKey();
            folder.setLastSyncTime(LocalDateTime.now());
            
            // 更新统计
            if (folder.getEmailFolder() != null) {
                int totalItems = messageRepository.countByFolder(folder.getEmailFolder());
                folder.updateSyncStatus(totalItems, serverChanges.size());
            }
            
            folderRepository.save(folder);
            
            result.setNewSyncKey(folder.getSyncKey());
            result.setStatus(ActiveSyncLog.SyncStatus.SUCCESS);
            result.setMoreAvailable(serverChanges.size() >= options.getWindowSize());
            
        } catch (Exception e) {
            logger.error("内容同步失败: deviceId=" + deviceId + ", folderId=" + folderId, e);
            result.setStatus(ActiveSyncLog.SyncStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 记录同步日志
            ActiveSyncLog syncLog = new ActiveSyncLog(device, ActiveSyncLog.SyncType.SYNC, "Sync");
            syncLog.setFolder(folder);
            syncLog.setClientIP(clientIP);
            syncLog.setSyncKeySent(syncKey);
            syncLog.setSyncKeyReturned(result.getNewSyncKey());
            syncLog.setItemsFetched(result.getServerChanges() != null ? result.getServerChanges().size() : 0);
            syncLog.setStatus(result.getStatus());
            syncLog.setErrorMessage(result.getErrorMessage());
            syncLog.setProcessingTimeMs(processingTime);
            logRepository.save(syncLog);
            
            // 更新设备统计
            device.updateSyncStats(result.getStatus() == ActiveSyncLog.SyncStatus.SUCCESS);
            deviceRepository.save(device);
        }
        
        return result;
    }
    
    /**
     * 处理心跳
     */
    public PingResult processPing(String deviceId, List<String> folderIds, int heartbeatInterval, String clientIP) {
        
        ActiveSyncDevice device = getDeviceById(deviceId);
        validateDeviceAccess(device);
        
        PingResult result = new PingResult();
        result.setDeviceId(deviceId);
        result.setHeartbeatInterval(heartbeatInterval);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证心跳间隔
            if (heartbeatInterval < 60 || heartbeatInterval > 3600) {
                heartbeatInterval = device.getHeartbeatInterval();
            }
            
            // 检查文件夹变更
            List<String> changedFolders = new ArrayList<>();
            
            for (String folderId : folderIds) {
                ActiveSyncFolder folder = folderRepository.findByDeviceAndFolderId(device, folderId)
                    .orElse(null);
                
                if (folder != null && hasChanges(folder)) {
                    changedFolders.add(folderId);
                }
            }
            
            // 检查策略更新
            if (device.needsPolicyAcknowledgment()) {
                result.setPolicyUpdateRequired(true);
            }
            
            // 检查远程擦除
            if (device.getRemoteWipeRequested() && !device.getRemoteWipeAcknowledged()) {
                result.setRemoteWipeRequired(true);
            }
            
            result.setChangedFolders(changedFolders);
            result.setStatus(ActiveSyncLog.SyncStatus.SUCCESS);
            
            // 更新设备心跳间隔
            device.setHeartbeatInterval(heartbeatInterval);
            device.setLastSyncTime(LocalDateTime.now());
            device.setLastSyncIP(clientIP);
            deviceRepository.save(device);
            
        } catch (Exception e) {
            logger.error("心跳处理失败: deviceId=" + deviceId, e);
            result.setStatus(ActiveSyncLog.SyncStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 记录心跳日志
            ActiveSyncLog pingLog = new ActiveSyncLog(device, ActiveSyncLog.SyncType.PING, "Ping");
            pingLog.setClientIP(clientIP);
            pingLog.setHeartbeatInterval(heartbeatInterval);
            pingLog.setStatus(result.getStatus());
            pingLog.setErrorMessage(result.getErrorMessage());
            pingLog.setProcessingTimeMs(processingTime);
            logRepository.save(pingLog);
        }
        
        return result;
    }
    
    /**
     * 批准设备
     */
    public void approveDevice(Long deviceId, String approvedBy) {
        ActiveSyncDevice device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在"));
        
        device.allowDevice();
        deviceRepository.save(device);
        
        logger.info("设备已批准: deviceId={}, approvedBy={}", device.getDeviceId(), approvedBy);
    }
    
    /**
     * 阻止设备
     */
    public void blockDevice(Long deviceId, String reason, String blockedBy) {
        ActiveSyncDevice device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在"));
        
        device.blockDevice(reason);
        deviceRepository.save(device);
        
        logger.info("设备已阻止: deviceId={}, reason={}, blockedBy={}", device.getDeviceId(), reason, blockedBy);
    }
    
    /**
     * 请求远程擦除
     */
    public void requestRemoteWipe(Long deviceId, String requestedBy) {
        ActiveSyncDevice device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在"));
        
        device.requestRemoteWipe();
        deviceRepository.save(device);
        
        logger.info("已请求远程擦除: deviceId={}, requestedBy={}", device.getDeviceId(), requestedBy);
    }
    
    /**
     * 获取用户设备列表
     */
    public List<ActiveSyncDevice> getUserDevices(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return deviceRepository.findByUserOrderByLastSyncTimeDesc(user);
    }
    
    /**
     * 获取设备同步统计
     */
    public DeviceSyncStatistics getDeviceSyncStatistics(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        ActiveSyncDevice device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在"));
        
        List<ActiveSyncLog> logs = logRepository.findByDeviceAndCreatedAtBetween(device, startTime, endTime);
        
        DeviceSyncStatistics stats = new DeviceSyncStatistics();
        stats.setDeviceId(device.getDeviceId());
        stats.setStartTime(startTime);
        stats.setEndTime(endTime);
        
        // 统计同步次数
        long totalSyncs = logs.size();
        long successfulSyncs = logs.stream().filter(ActiveSyncLog::isSuccess).count();
        
        stats.setTotalSyncs(totalSyncs);
        stats.setSuccessfulSyncs(successfulSyncs);
        stats.setFailedSyncs(totalSyncs - successfulSyncs);
        
        // 统计数据传输
        long totalDataSent = logs.stream().mapToLong(l -> l.getDataSentBytes() != null ? l.getDataSentBytes() : 0).sum();
        long totalDataReceived = logs.stream().mapToLong(l -> l.getDataReceivedBytes() != null ? l.getDataReceivedBytes() : 0).sum();
        
        stats.setTotalDataSent(totalDataSent);
        stats.setTotalDataReceived(totalDataReceived);
        
        // 统计同步项目
        int totalItems = logs.stream().mapToInt(ActiveSyncLog::getTotalSyncItems).sum();
        stats.setTotalSyncItems(totalItems);
        
        // 平均处理时间
        OptionalDouble avgProcessingTime = logs.stream()
            .filter(l -> l.getProcessingTimeMs() != null)
            .mapToLong(ActiveSyncLog::getProcessingTimeMs)
            .average();
        
        stats.setAverageProcessingTime(avgProcessingTime.orElse(0.0));
        
        return stats;
    }
    
    // 私有方法
    
    private ActiveSyncDevice getDeviceById(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在"));
    }
    
    private void validateDeviceAccess(ActiveSyncDevice device) {
        if (!device.canSync()) {
            if (device.getIsBlocked()) {
                throw new RuntimeException("设备已被阻止: " + device.getBlockReason());
            }
            if (device.getRemoteWipeRequested()) {
                throw new RuntimeException("设备已请求远程擦除");
            }
            if (device.getStatus() == ActiveSyncDevice.DeviceStatus.PENDING) {
                throw new RuntimeException("设备等待管理员批准");
            }
            throw new RuntimeException("设备无法同步");
        }
    }
    
    private boolean validateSyncKey(ActiveSyncDevice device, String syncKey, boolean isFolderSync) {
        if (syncKey == null || syncKey.equals("0")) {
            return true; // 初始同步
        }
        
        String expectedKey = isFolderSync ? device.getFolderSyncKey() : device.getSyncKey();
        return syncKey.equals(expectedKey);
    }
    
    private boolean validateSyncKey(ActiveSyncFolder folder, String syncKey) {
        if (syncKey == null || syncKey.equals("0")) {
            return true; // 初始同步
        }
        
        return syncKey.equals(folder.getSyncKey());
    }
    
    private void createDefaultFolders(ActiveSyncDevice device) {
        List<EmailFolder> emailFolders = emailFolderRepository.findByUserOrderByFolderName(device.getUser());
        
        for (EmailFolder emailFolder : emailFolders) {
            createActiveSyncFolder(device, emailFolder);
        }
    }
    
    private ActiveSyncFolder createActiveSyncFolder(ActiveSyncDevice device, EmailFolder emailFolder) {
        String folderId = generateFolderId();
        ActiveSyncFolder.FolderType folderType = mapToActiveSyncFolderType(emailFolder.getFolderType());
        
        ActiveSyncFolder activeSyncFolder = new ActiveSyncFolder(device, folderId, 
                                                               emailFolder.getFolderName(), folderType);
        activeSyncFolder.setEmailFolder(emailFolder);
        activeSyncFolder.setFolderClass("Email");
        
        return folderRepository.save(activeSyncFolder);
    }
    
    private ActiveSyncFolder.FolderType mapToActiveSyncFolderType(EmailFolder.FolderType emailFolderType) {
        switch (emailFolderType) {
            case INBOX: return ActiveSyncFolder.FolderType.INBOX;
            case SENT: return ActiveSyncFolder.FolderType.SENT;
            case DRAFT: return ActiveSyncFolder.FolderType.DRAFTS;
            case TRASH: return ActiveSyncFolder.FolderType.DELETED;
            case SPAM: return ActiveSyncFolder.FolderType.SPAM;
            default: return ActiveSyncFolder.FolderType.CUSTOM;
        }
    }
    
    private String generateFolderId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    private void parseDeviceInfo(ActiveSyncDevice device, String userAgent) {
        if (userAgent == null) return;
        
        // 简化的设备信息解析
        if (userAgent.contains("iPhone")) {
            device.setDeviceType("iPhone");
            device.setDeviceOS("iOS");
        } else if (userAgent.contains("Android")) {
            device.setDeviceType("Android");
            device.setDeviceOS("Android");
        } else if (userAgent.contains("Windows")) {
            device.setDeviceType("Windows");
            device.setDeviceOS("Windows");
        }
    }
    
    private void updateDeviceInfo(ActiveSyncDevice device, String deviceType, String userAgent, String clientIP) {
        device.setDeviceType(deviceType);
        device.setDeviceUserAgent(userAgent);
        device.setLastSyncIP(clientIP);
        device.setLastSyncTime(LocalDateTime.now());
        deviceRepository.save(device);
    }
    
    private FolderInfo createFolderInfo(ActiveSyncFolder folder) {
        FolderInfo info = new FolderInfo();
        info.setFolderId(folder.getFolderId());
        info.setParentId(folder.getParentId());
        info.setDisplayName(folder.getFolderName());
        info.setType(folder.getFolderType().ordinal());
        return info;
    }
    
    private void processClientChanges(ActiveSyncFolder folder, List<SyncItem> clientChanges, SyncResult result) {
        // 处理客户端发送的变更
        for (SyncItem change : clientChanges) {
            try {
                switch (change.getOperation()) {
                    case "Add":
                        // 处理新增项目
                        break;
                    case "Change":
                        // 处理修改项目
                        break;
                    case "Delete":
                        // 处理删除项目
                        break;
                }
            } catch (Exception e) {
                logger.error("处理客户端变更失败: " + change.getOperation(), e);
            }
        }
    }
    
    private List<SyncItem> getServerChanges(ActiveSyncFolder folder, SyncOptions options) {
        List<SyncItem> changes = new ArrayList<>();
        
        if (folder.getEmailFolder() != null) {
            // 获取邮件变更
            LocalDateTime cutoffDate = folder.getSyncCutoffDate();
            
            List<EmailMessage> messages = messageRepository.findByFolderAndUpdatedAtAfter(
                folder.getEmailFolder(), 
                cutoffDate != null ? cutoffDate : folder.getLastSyncTime(),
                options.getWindowSize()
            );
            
            for (EmailMessage message : messages) {
                SyncItem item = new SyncItem();
                item.setServerId(message.getId().toString());
                item.setOperation("Add"); // 简化处理
                item.setData(convertEmailToActiveSyncFormat(message));
                changes.add(item);
            }
        }
        
        return changes;
    }
    
    private boolean hasChanges(ActiveSyncFolder folder) {
        if (folder.getEmailFolder() != null) {
            // 检查是否有新的邮件或变更
            long newMessageCount = messageRepository.countByFolderAndReceivedAtAfter(
                folder.getEmailFolder(), 
                folder.getLastSyncTime() != null ? folder.getLastSyncTime() : LocalDateTime.now().minusDays(1)
            );
            return newMessageCount > 0;
        }
        return false;
    }
    
    private String convertEmailToActiveSyncFormat(EmailMessage message) {
        // 简化的邮件格式转换
        // 实际实现需要转换为ActiveSync XML格式
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
               "<Email>" +
               "<Subject>" + escapeXml(message.getSubject()) + "</Subject>" +
               "<From>" + escapeXml(message.getFromAddress()) + "</From>" +
               "<Body>" + escapeXml(message.getBodyText()) + "</Body>" +
               "</Email>";
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    private void logSyncActivity(ActiveSyncDevice device, ActiveSyncFolder folder, 
                                ActiveSyncLog.SyncType syncType, String command,
                                ActiveSyncLog.SyncStatus status, String clientIP, String userAgent) {
        
        ActiveSyncLog log = new ActiveSyncLog(device, syncType, command);
        log.setFolder(folder);
        log.setClientIP(clientIP);
        log.setUserAgent(userAgent);
        log.setStatus(status);
        logRepository.save(log);
    }
    
    // 结果类
    
    public static class FolderSyncResult {
        private String deviceId;
        private String oldSyncKey;
        private String newSyncKey;
        private List<FolderInfo> addedFolders = new ArrayList<>();
        private List<FolderInfo> changedFolders = new ArrayList<>();
        private List<String> deletedFolders = new ArrayList<>();
        private ActiveSyncLog.SyncStatus status;
        private String errorMessage;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getOldSyncKey() { return oldSyncKey; }
        public void setOldSyncKey(String oldSyncKey) { this.oldSyncKey = oldSyncKey; }
        
        public String getNewSyncKey() { return newSyncKey; }
        public void setNewSyncKey(String newSyncKey) { this.newSyncKey = newSyncKey; }
        
        public List<FolderInfo> getAddedFolders() { return addedFolders; }
        public void setAddedFolders(List<FolderInfo> addedFolders) { this.addedFolders = addedFolders; }
        
        public List<FolderInfo> getChangedFolders() { return changedFolders; }
        public void setChangedFolders(List<FolderInfo> changedFolders) { this.changedFolders = changedFolders; }
        
        public List<String> getDeletedFolders() { return deletedFolders; }
        public void setDeletedFolders(List<String> deletedFolders) { this.deletedFolders = deletedFolders; }
        
        public ActiveSyncLog.SyncStatus getStatus() { return status; }
        public void setStatus(ActiveSyncLog.SyncStatus status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class SyncResult {
        private String deviceId;
        private String folderId;
        private String oldSyncKey;
        private String newSyncKey;
        private List<SyncItem> serverChanges = new ArrayList<>();
        private boolean moreAvailable;
        private ActiveSyncLog.SyncStatus status;
        private String errorMessage;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getFolderId() { return folderId; }
        public void setFolderId(String folderId) { this.folderId = folderId; }
        
        public String getOldSyncKey() { return oldSyncKey; }
        public void setOldSyncKey(String oldSyncKey) { this.oldSyncKey = oldSyncKey; }
        
        public String getNewSyncKey() { return newSyncKey; }
        public void setNewSyncKey(String newSyncKey) { this.newSyncKey = newSyncKey; }
        
        public List<SyncItem> getServerChanges() { return serverChanges; }
        public void setServerChanges(List<SyncItem> serverChanges) { this.serverChanges = serverChanges; }
        
        public boolean isMoreAvailable() { return moreAvailable; }
        public void setMoreAvailable(boolean moreAvailable) { this.moreAvailable = moreAvailable; }
        
        public ActiveSyncLog.SyncStatus getStatus() { return status; }
        public void setStatus(ActiveSyncLog.SyncStatus status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class PingResult {
        private String deviceId;
        private int heartbeatInterval;
        private List<String> changedFolders = new ArrayList<>();
        private boolean policyUpdateRequired;
        private boolean remoteWipeRequired;
        private ActiveSyncLog.SyncStatus status;
        private String errorMessage;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public int getHeartbeatInterval() { return heartbeatInterval; }
        public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
        
        public List<String> getChangedFolders() { return changedFolders; }
        public void setChangedFolders(List<String> changedFolders) { this.changedFolders = changedFolders; }
        
        public boolean isPolicyUpdateRequired() { return policyUpdateRequired; }
        public void setPolicyUpdateRequired(boolean policyUpdateRequired) { this.policyUpdateRequired = policyUpdateRequired; }
        
        public boolean isRemoteWipeRequired() { return remoteWipeRequired; }
        public void setRemoteWipeRequired(boolean remoteWipeRequired) { this.remoteWipeRequired = remoteWipeRequired; }
        
        public ActiveSyncLog.SyncStatus getStatus() { return status; }
        public void setStatus(ActiveSyncLog.SyncStatus status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class FolderInfo {
        private String folderId;
        private String parentId;
        private String displayName;
        private int type;
        
        // Getters and Setters
        public String getFolderId() { return folderId; }
        public void setFolderId(String folderId) { this.folderId = folderId; }
        
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public int getType() { return type; }
        public void setType(int type) { this.type = type; }
    }
    
    public static class SyncItem {
        private String serverId;
        private String operation;
        private String data;
        
        // Getters and Setters
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
    
    public static class SyncOptions {
        private List<SyncItem> clientChanges;
        private int windowSize = 100;
        private boolean getChanges = true;
        
        // Getters and Setters
        public List<SyncItem> getClientChanges() { return clientChanges; }
        public void setClientChanges(List<SyncItem> clientChanges) { this.clientChanges = clientChanges; }
        
        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
        
        public boolean isGetChanges() { return getChanges; }
        public void setGetChanges(boolean getChanges) { this.getChanges = getChanges; }
    }
    
    public static class DeviceSyncStatistics {
        private String deviceId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long totalSyncs;
        private long successfulSyncs;
        private long failedSyncs;
        private long totalDataSent;
        private long totalDataReceived;
        private int totalSyncItems;
        private double averageProcessingTime;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public long getTotalSyncs() { return totalSyncs; }
        public void setTotalSyncs(long totalSyncs) { this.totalSyncs = totalSyncs; }
        
        public long getSuccessfulSyncs() { return successfulSyncs; }
        public void setSuccessfulSyncs(long successfulSyncs) { this.successfulSyncs = successfulSyncs; }
        
        public long getFailedSyncs() { return failedSyncs; }
        public void setFailedSyncs(long failedSyncs) { this.failedSyncs = failedSyncs; }
        
        public long getTotalDataSent() { return totalDataSent; }
        public void setTotalDataSent(long totalDataSent) { this.totalDataSent = totalDataSent; }
        
        public long getTotalDataReceived() { return totalDataReceived; }
        public void setTotalDataReceived(long totalDataReceived) { this.totalDataReceived = totalDataReceived; }
        
        public int getTotalSyncItems() { return totalSyncItems; }
        public void setTotalSyncItems(int totalSyncItems) { this.totalSyncItems = totalSyncItems; }
        
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public void setAverageProcessingTime(double averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
        
        public double getSuccessRate() {
            return totalSyncs > 0 ? (double) successfulSyncs / totalSyncs * 100.0 : 0.0;
        }
    }
}