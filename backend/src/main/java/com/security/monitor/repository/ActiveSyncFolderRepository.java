package com.security.monitor.repository;

import com.security.monitor.model.ActiveSyncFolder;
import com.security.monitor.model.ActiveSyncDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ActiveSync文件夹Repository
 */
@Repository
public interface ActiveSyncFolderRepository extends JpaRepository<ActiveSyncFolder, Long> {
    
    /**
     * 根据设备查找文件夹列表
     */
    List<ActiveSyncFolder> findByDeviceOrderByFolderName(ActiveSyncDevice device);
    
    /**
     * 根据设备和文件夹ID查找文件夹
     */
    Optional<ActiveSyncFolder> findByDeviceAndFolderId(ActiveSyncDevice device, String folderId);
    
    /**
     * 根据设备和文件夹类型查找文件夹
     */
    List<ActiveSyncFolder> findByDeviceAndFolderType(ActiveSyncDevice device, ActiveSyncFolder.FolderType folderType);
    
    /**
     * 查找启用同步的文件夹
     */
    List<ActiveSyncFolder> findByDeviceAndSyncEnabledTrue(ActiveSyncDevice device);
    
    /**
     * 查找有变更的文件夹
     */
    List<ActiveSyncFolder> findByDeviceAndHasChangesTrue(ActiveSyncDevice device);
    
    /**
     * 根据文件夹类别查找文件夹
     */
    List<ActiveSyncFolder> findByFolderClassOrderByFolderName(String folderClass);
    
    /**
     * 查找最近同步的文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.lastSyncTime >= :sinceTime " +
           "ORDER BY f.lastSyncTime DESC")
    List<ActiveSyncFolder> findRecentlySynced(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * 查找长时间未同步的文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.lastSyncTime < :beforeTime " +
           "OR f.lastSyncTime IS NULL ORDER BY f.lastSyncTime ASC")
    List<ActiveSyncFolder> findStaleFolder(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 统计文件夹数量按类型
     */
    @Query("SELECT f.folderType, COUNT(f) FROM ActiveSyncFolder f GROUP BY f.folderType")
    List<Object[]> countFoldersByType();
    
    /**
     * 统计文件夹数量按同步状态
     */
    @Query("SELECT f.syncEnabled, COUNT(f) FROM ActiveSyncFolder f GROUP BY f.syncEnabled")
    List<Object[]> countFoldersBySyncEnabled();
    
    /**
     * 查找大数据量文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.estimatedDataSize > :sizeThreshold " +
           "ORDER BY f.estimatedDataSize DESC")
    List<ActiveSyncFolder> findLargeFolders(@Param("sizeThreshold") long sizeThreshold);
    
    /**
     * 查找项目数量多的文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.totalItemCount > :itemThreshold " +
           "ORDER BY f.totalItemCount DESC")
    List<ActiveSyncFolder> findFoldersWithManyItems(@Param("itemThreshold") int itemThreshold);
    
    /**
     * 查找同步进度较低的文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.totalItemCount > 0 " +
           "AND (f.syncedItemCount * 100.0 / f.totalItemCount) < :progressThreshold " +
           "ORDER BY (f.syncedItemCount * 100.0 / f.totalItemCount) ASC")
    List<ActiveSyncFolder> findFoldersWithLowSyncProgress(@Param("progressThreshold") double progressThreshold);
    
    /**
     * 根据同步过滤类型查找文件夹
     */
    List<ActiveSyncFolder> findBySyncFilterTypeOrderByFolderName(ActiveSyncFolder.SyncFilterType syncFilterType);
    
    /**
     * 查找有父文件夹的子文件夹
     */
    List<ActiveSyncFolder> findByParentIdIsNotNullOrderByParentIdAscFolderNameAsc();
    
    /**
     * 根据父文件夹ID查找子文件夹
     */
    List<ActiveSyncFolder> findByParentIdOrderByFolderName(String parentId);
    
    /**
     * 查找顶级文件夹（无父文件夹）
     */
    List<ActiveSyncFolder> findByParentIdIsNullOrderByFolderName();
    
    /**
     * 统计设备的文件夹数量
     */
    long countByDevice(ActiveSyncDevice device);
    
    /**
     * 统计启用同步的文件夹数量
     */
    long countByDeviceAndSyncEnabledTrue(ActiveSyncDevice device);
    
    /**
     * 查找特定邮件文件夹关联的ActiveSync文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.emailFolder.id = :emailFolderId")
    List<ActiveSyncFolder> findByEmailFolderId(@Param("emailFolderId") Long emailFolderId);
    
    /**
     * 查找冲突解决策略为特定类型的文件夹
     */
    List<ActiveSyncFolder> findBySyncConflictResolution(ActiveSyncFolder.ConflictResolution conflictResolution);
    
    /**
     * 查找有截断大小设置的文件夹
     */
    List<ActiveSyncFolder> findBySyncTruncationSizeIsNotNullOrderBySyncTruncationSizeDesc();
    
    /**
     * 查找最近有项目同步的文件夹
     */
    @Query("SELECT f FROM ActiveSyncFolder f WHERE f.lastItemSyncTime >= :sinceTime " +
           "ORDER BY f.lastItemSyncTime DESC")
    List<ActiveSyncFolder> findRecentlyItemSynced(@Param("sinceTime") LocalDateTime sinceTime);
    
    /**
     * 查找指定时间范围内创建的文件夹
     */
    List<ActiveSyncFolder> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计总的文件夹数据大小
     */
    @Query("SELECT SUM(f.estimatedDataSize) FROM ActiveSyncFolder f WHERE f.device = :device")
    Long getTotalDataSizeByDevice(@Param("device") ActiveSyncDevice device);
    
    /**
     * 统计总的文件夹项目数
     */
    @Query("SELECT SUM(f.totalItemCount) FROM ActiveSyncFolder f WHERE f.device = :device")
    Long getTotalItemCountByDevice(@Param("device") ActiveSyncDevice device);
    
    /**
     * 查找同步密钥为特定值的文件夹
     */
    Optional<ActiveSyncFolder> findBySyncKey(String syncKey);
}