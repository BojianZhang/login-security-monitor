package com.security.monitor.repository;

import com.security.monitor.model.VirusDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 病毒定义仓库接口
 */
@Repository
public interface VirusDefinitionRepository extends JpaRepository<VirusDefinition, Long> {
    
    /**
     * 根据病毒名称查找
     */
    Optional<VirusDefinition> findByVirusName(String virusName);
    
    /**
     * 检查病毒名称是否存在
     */
    boolean existsByVirusName(String virusName);
    
    /**
     * 根据哈希特征查找
     */
    Optional<VirusDefinition> findByHashSignature(String hashSignature);
    
    /**
     * 根据特征类型查找活跃的定义
     */
    List<VirusDefinition> findBySignatureTypeAndIsActive(VirusDefinition.SignatureType signatureType, boolean isActive);
    
    /**
     * 查找所有活跃的病毒定义
     */
    List<VirusDefinition> findByIsActiveOrderByUpdatedAtDesc(boolean isActive);
    
    /**
     * 分页查询病毒定义
     */
    Page<VirusDefinition> findByIsActive(boolean isActive, Pageable pageable);
    
    /**
     * 根据严重程度查找
     */
    List<VirusDefinition> findBySeverityAndIsActive(VirusDefinition.Severity severity, boolean isActive);
    
    /**
     * 查找模式特征定义
     */
    @Query("SELECT v FROM VirusDefinition v WHERE v.signatureType = 'PATTERN' AND v.isActive = true ORDER BY v.severity DESC")
    List<VirusDefinition> findActivePatternDefinitions();
    
    /**
     * 查找哈希特征定义
     */
    @Query("SELECT v FROM VirusDefinition v WHERE v.signatureType = 'HASH' AND v.isActive = true")
    List<VirusDefinition> findActiveHashDefinitions();
    
    /**
     * 统计活跃定义数量
     */
    long countByIsActive(boolean isActive);
    
    /**
     * 统计按严重程度分组的定义数量
     */
    @Query("SELECT v.severity, COUNT(v) FROM VirusDefinition v WHERE v.isActive = true GROUP BY v.severity")
    List<Object[]> countDefinitionsBySeverity();
    
    /**
     * 更新检测统计
     */
    @Modifying
    @Query("UPDATE VirusDefinition v SET v.detectionCount = v.detectionCount + 1, v.lastDetectedAt = :detectedAt WHERE v.id = :definitionId")
    void updateDetectionStatistics(@Param("definitionId") Long definitionId, @Param("detectedAt") LocalDateTime detectedAt);
    
    /**
     * 查找最常检测到的病毒
     */
    @Query("SELECT v FROM VirusDefinition v WHERE v.isActive = true ORDER BY v.detectionCount DESC")
    List<VirusDefinition> findMostDetectedViruses(Pageable pageable);
    
    /**
     * 查找最近更新的定义
     */
    List<VirusDefinition> findByUpdatedAtAfterOrderByUpdatedAtDesc(LocalDateTime after);
    
    /**
     * 根据模式特征搜索（模糊匹配）
     */
    @Query("SELECT v FROM VirusDefinition v WHERE v.signatureType = 'PATTERN' AND v.patternSignature LIKE %:pattern% AND v.isActive = true")
    List<VirusDefinition> findByPatternSignatureContaining(@Param("pattern") String pattern);
    
    /**
     * 获取病毒定义统计信息
     */
    @Query("SELECT " +
           "COUNT(v) as totalDefinitions, " +
           "SUM(CASE WHEN v.isActive = true THEN 1 ELSE 0 END) as activeDefinitions, " +
           "SUM(v.detectionCount) as totalDetections, " +
           "MAX(v.updatedAt) as lastUpdated " +
           "FROM VirusDefinition v")
    Object[] getDefinitionStatistics();
    
    /**
     * 删除过期的未使用定义
     */
    @Modifying
    @Query("DELETE FROM VirusDefinition v WHERE v.isActive = false AND v.detectionCount = 0 AND v.updatedAt < :expiredDate")
    void deleteUnusedDefinitions(@Param("expiredDate") LocalDateTime expiredDate);
}