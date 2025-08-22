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

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * DMARC报告生成服务
 */
@Service
@Transactional
public class DmarcReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(DmarcReportService.class);
    
    @Autowired
    private DmarcReportRepository dmarcReportRepository;
    
    @Autowired
    private DmarcReportRecordRepository dmarcReportRecordRepository;
    
    @Autowired
    private EmailValidationLogRepository validationLogRepository;
    
    @Autowired
    private EmailDeliveryLogRepository deliveryLogRepository;
    
    @Autowired
    private EmailDomainRepository domainRepository;
    
    @Autowired
    private EmailQueueService emailQueueService;
    
    @Value("${dmarc.report.org-name:Secure Email System}")
    private String reportOrgName;
    
    @Value("${dmarc.report.contact-email:postmaster@example.com}")
    private String reportContactEmail;
    
    @Value("${dmarc.report.storage-path:/var/mail/dmarc-reports}")
    private String reportStoragePath;
    
    @Value("${dmarc.report.enabled:true}")
    private boolean dmarcReportEnabled;
    
    @Value("${dmarc.report.generation-interval-hours:24}")
    private int generationIntervalHours;
    
    /**
     * 生成DMARC聚合报告
     */
    public DmarcReport generateAggregateReport(String domain, LocalDateTime startTime, LocalDateTime endTime) {
        
        if (!dmarcReportEnabled) {
            logger.info("DMARC报告生成已禁用");
            return null;
        }
        
        logger.info("开始生成DMARC聚合报告: domain={}, period={} to {}", domain, startTime, endTime);
        
        // 检查是否已存在相同时间段的报告
        Optional<DmarcReport> existingReport = dmarcReportRepository
            .findByDomainAndBeginTimeAndEndTime(domain, startTime, endTime);
        
        if (existingReport.isPresent()) {
            logger.warn("DMARC报告已存在: domain={}, period={} to {}", domain, startTime, endTime);
            return existingReport.get();
        }
        
        // 创建新报告
        DmarcReport report = new DmarcReport(domain, reportOrgName, startTime, endTime);
        report.setEmail(reportContactEmail);
        
        // 获取域名配置
        EmailDomain emailDomain = domainRepository.findByDomainNameAndIsActive(domain, true)
            .orElse(null);
        
        if (emailDomain != null) {
            // 设置DMARC策略信息
            String dmarcPolicy = emailDomain.getDmarcPolicy();
            if (dmarcPolicy != null) {
                switch (dmarcPolicy.toLowerCase()) {
                    case "quarantine":
                        report.setPolicyP(DmarcReport.DispositionType.QUARANTINE);
                        break;
                    case "reject":
                        report.setPolicyP(DmarcReport.DispositionType.REJECT);
                        break;
                    default:
                        report.setPolicyP(DmarcReport.DispositionType.NONE);
                }
            }
        }
        
        // 保存报告基本信息
        report = dmarcReportRepository.save(report);
        
        // 收集验证日志数据
        List<EmailValidationLog> validationLogs = validationLogRepository
            .findByValidatedAtBetweenAndValidationStatus(startTime, endTime, "COMPLETED");
        
        // 按源IP和From域分组数据
        Map<String, Map<String, List<EmailValidationLog>>> groupedLogs = validationLogs.stream()
            .filter(log -> log.getSenderIp() != null)
            .collect(Collectors.groupingBy(
                EmailValidationLog::getSenderIp,
                Collectors.groupingBy(log -> extractDomainFromEmail(log.getMessageId()))
            ));
        
        // 生成报告记录
        long totalMessages = 0;
        long compliantMessages = 0;
        long failedMessages = 0;
        
        List<DmarcReportRecord> records = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, List<EmailValidationLog>>> ipEntry : groupedLogs.entrySet()) {
            String sourceIp = ipEntry.getKey();
            
            for (Map.Entry<String, List<EmailValidationLog>> domainEntry : ipEntry.getValue().entrySet()) {
                String headerFrom = domainEntry.getKey();
                List<EmailValidationLog> logs = domainEntry.getValue();
                
                DmarcReportRecord record = createReportRecord(report, sourceIp, headerFrom, logs);
                records.add(record);
                
                totalMessages += record.getCount();
                if (record.isDmarcCompliant()) {
                    compliantMessages += record.getCount();
                } else {
                    failedMessages += record.getCount();
                }
            }
        }
        
        // 保存报告记录
        if (!records.isEmpty()) {
            dmarcReportRecordRepository.saveAll(records);
        }
        
        // 更新报告统计
        report.addMessageStats(totalMessages, compliantMessages, failedMessages);
        
        // 生成报告文件
        try {
            generateReportFile(report, records);
        } catch (Exception e) {
            logger.error("生成DMARC报告文件失败", e);
            report.setErrorMessage("报告文件生成失败: " + e.getMessage());
        }
        
        report = dmarcReportRepository.save(report);
        
        logger.info("DMARC聚合报告生成完成: reportId={}, totalMessages={}, compliantRate={}%",
            report.getReportId(), totalMessages, report.getComplianceRate());
        
        return report;
    }
    
    /**
     * 发送DMARC报告
     */
    @Async
    public void sendDmarcReport(Long reportId) {
        
        DmarcReport report = dmarcReportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("DMARC报告不存在"));
        
        if (report.getIsSent()) {
            logger.warn("DMARC报告已发送: reportId={}", report.getReportId());
            return;
        }
        
        try {
            // 获取DMARC接收方信息
            List<String> dmarcRua = getDmarcRuaAddresses(report.getDomain());
            
            if (dmarcRua.isEmpty()) {
                logger.warn("未找到DMARC RUA地址: domain={}", report.getDomain());
                return;
            }
            
            for (String ruaAddress : dmarcRua) {
                sendReportToRecipient(report, ruaAddress);
            }
            
            report.markAsSent();
            dmarcReportRepository.save(report);
            
            logger.info("DMARC报告发送成功: reportId={}", report.getReportId());
            
        } catch (Exception e) {
            logger.error("发送DMARC报告失败: reportId=" + report.getReportId(), e);
            report.markSendFailed(e.getMessage());
            dmarcReportRepository.save(report);
        }
    }
    
    /**
     * 自动生成定期报告
     */
    @Transactional
    public void generatePeriodicReports() {
        
        if (!dmarcReportEnabled) {
            return;
        }
        
        logger.info("开始生成定期DMARC报告");
        
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(generationIntervalHours);
        
        // 获取所有活跃域名
        List<EmailDomain> domains = domainRepository.findByIsActiveAndDmarcPolicyIsNotNull(true);
        
        for (EmailDomain domain : domains) {
            try {
                DmarcReport report = generateAggregateReport(domain.getDomainName(), startTime, endTime);
                
                if (report != null && report.getTotalMessages() > 0) {
                    // 异步发送报告
                    sendDmarcReport(report.getId());
                }
                
            } catch (Exception e) {
                logger.error("生成域名 {} 的DMARC报告失败", domain.getDomainName(), e);
            }
        }
        
        logger.info("定期DMARC报告生成完成");
    }
    
    /**
     * 重试发送失败的报告
     */
    @Transactional
    public void retryFailedReports() {
        
        List<DmarcReport> failedReports = dmarcReportRepository.findReportsNeedingRetry();
        
        for (DmarcReport report : failedReports) {
            if (report.needsRetry()) {
                logger.info("重试发送DMARC报告: reportId={}", report.getReportId());
                sendDmarcReport(report.getId());
            }
        }
    }
    
    /**
     * 清理过期的DMARC报告
     */
    @Transactional
    public int cleanupExpiredReports(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        List<DmarcReport> expiredReports = dmarcReportRepository.findByCreatedAtBefore(cutoffDate);
        
        int deletedCount = 0;
        for (DmarcReport report : expiredReports) {
            try {
                // 删除报告文件
                if (report.getReportPath() != null) {
                    Files.deleteIfExists(Paths.get(report.getReportPath()));
                }
                
                // 删除数据库记录
                dmarcReportRepository.delete(report);
                deletedCount++;
                
            } catch (Exception e) {
                logger.error("删除过期DMARC报告失败: reportId=" + report.getReportId(), e);
            }
        }
        
        logger.info("清理了 {} 个过期的DMARC报告", deletedCount);
        return deletedCount;
    }
    
    // 私有方法
    
    /**
     * 创建报告记录
     */
    private DmarcReportRecord createReportRecord(DmarcReport report, String sourceIp, 
                                               String headerFrom, List<EmailValidationLog> logs) {
        
        DmarcReportRecord record = new DmarcReportRecord(report, sourceIp, headerFrom);
        record.setCount((long) logs.size());
        
        // 分析验证结果
        for (EmailValidationLog log : logs) {
            // SPF结果
            if (log.getSpfStatus() != null) {
                record.setSpfResult(convertToAuthResult(log.getSpfStatus()));
                record.setSpfDomain(extractDomainFromSpfRecord(log.getSpfRecord()));
            }
            
            // DKIM结果
            if (log.getDkimStatus() != null) {
                record.setDkimResult(convertToAuthResult(log.getDkimStatus()));
                record.setDkimDomain(log.getDkimDomain());
                record.setDkimSelector(log.getDkimSelector());
            }
            
            // DMARC结果
            if (log.getDmarcStatus() != null) {
                record.setDmarcResult(convertToDmarcResult(log.getDmarcStatus()));
            }
            
            // 设置处置结果
            record.setDisposition(determineDmarcDisposition(log));
            
            // 只需要设置一次，使用第一条日志的信息
            break;
        }
        
        return record;
    }
    
    /**
     * 生成报告文件
     */
    private void generateReportFile(DmarcReport report, List<DmarcReportRecord> records) throws IOException {
        
        // 创建存储目录
        Path storagePath = Paths.get(reportStoragePath);
        Files.createDirectories(storagePath);
        
        // 生成文件名
        String fileName = String.format("%s!%s!%d!%d.xml",
            reportOrgName.replaceAll("\\s+", "_"),
            report.getDomain(),
            report.getBeginTime().toEpochSecond(java.time.ZoneOffset.UTC),
            report.getEndTime().toEpochSecond(java.time.ZoneOffset.UTC)
        );
        
        Path xmlFile = storagePath.resolve(fileName);
        Path gzipFile = storagePath.resolve(fileName + ".gz");
        
        // 生成XML内容
        String xmlContent = generateXmlReport(report, records);
        
        // 写入XML文件
        Files.write(xmlFile, xmlContent.getBytes("UTF-8"));
        
        // 压缩文件
        try (FileOutputStream fos = new FileOutputStream(gzipFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write(xmlContent.getBytes("UTF-8"));
        }
        
        // 更新报告信息
        report.setReportPath(gzipFile.toString());
        report.setReportSize(Files.size(gzipFile));
        report.setCompressionType(DmarcReport.CompressionType.GZIP);
        
        // 删除临时XML文件
        Files.deleteIfExists(xmlFile);
        
        logger.info("DMARC报告文件已生成: {}", gzipFile);
    }
    
    /**
     * 生成XML报告内容
     */
    private String generateXmlReport(DmarcReport report, List<DmarcReportRecord> records) {
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<feedback>\n");
        
        // 报告元数据
        xml.append("  <report_metadata>\n");
        xml.append("    <org_name>").append(escapeXml(report.getOrgName())).append("</org_name>\n");
        xml.append("    <email>").append(escapeXml(report.getEmail())).append("</email>\n");
        xml.append("    <report_id>").append(escapeXml(report.getReportId())).append("</report_id>\n");
        xml.append("    <date_range>\n");
        xml.append("      <begin>").append(report.getBeginTime().toEpochSecond(java.time.ZoneOffset.UTC)).append("</begin>\n");
        xml.append("      <end>").append(report.getEndTime().toEpochSecond(java.time.ZoneOffset.UTC)).append("</end>\n");
        xml.append("    </date_range>\n");
        xml.append("  </report_metadata>\n");
        
        // 策略发布
        xml.append("  <policy_published>\n");
        xml.append("    <domain>").append(escapeXml(report.getPolicyDomain())).append("</domain>\n");
        xml.append("    <adkim>").append(report.getPolicyAdkim().getValue()).append("</adkim>\n");
        xml.append("    <aspf>").append(report.getPolicyAspf().getValue()).append("</aspf>\n");
        xml.append("    <p>").append(report.getPolicyP().getValue()).append("</p>\n");
        if (report.getPolicySp() != null) {
            xml.append("    <sp>").append(report.getPolicySp().getValue()).append("</sp>\n");
        }
        xml.append("    <pct>").append(report.getPolicyPct()).append("</pct>\n");
        xml.append("  </policy_published>\n");
        
        // 记录数据
        for (DmarcReportRecord record : records) {
            xml.append("  <record>\n");
            xml.append("    <row>\n");
            xml.append("      <source_ip>").append(escapeXml(record.getSourceIp())).append("</source_ip>\n");
            xml.append("      <count>").append(record.getCount()).append("</count>\n");
            xml.append("      <policy_evaluated>\n");
            if (record.getDisposition() != null) {
                xml.append("        <disposition>").append(record.getDisposition().getValue()).append("</disposition>\n");
            }
            if (record.getDmarcResult() != null) {
                xml.append("        <dkim>").append(record.getDmarcResult().getValue()).append("</dkim>\n");
                xml.append("        <spf>").append(record.getDmarcResult().getValue()).append("</spf>\n");
            }
            xml.append("      </policy_evaluated>\n");
            xml.append("    </row>\n");
            
            xml.append("    <identifiers>\n");
            xml.append("      <header_from>").append(escapeXml(record.getHeaderFrom())).append("</header_from>\n");
            if (record.getEnvelopeFrom() != null) {
                xml.append("      <envelope_from>").append(escapeXml(record.getEnvelopeFrom())).append("</envelope_from>\n");
            }
            xml.append("    </identifiers>\n");
            
            xml.append("    <auth_results>\n");
            
            // SPF结果
            if (record.getSpfResult() != null) {
                xml.append("      <spf>\n");
                xml.append("        <domain>").append(escapeXml(record.getSpfDomain())).append("</domain>\n");
                xml.append("        <result>").append(record.getSpfResult().getValue()).append("</result>\n");
                xml.append("      </spf>\n");
            }
            
            // DKIM结果
            if (record.getDkimResult() != null) {
                xml.append("      <dkim>\n");
                xml.append("        <domain>").append(escapeXml(record.getDkimDomain())).append("</domain>\n");
                xml.append("        <result>").append(record.getDkimResult().getValue()).append("</result>\n");
                if (record.getDkimSelector() != null) {
                    xml.append("        <selector>").append(escapeXml(record.getDkimSelector())).append("</selector>\n");
                }
                xml.append("      </dkim>\n");
            }
            
            xml.append("    </auth_results>\n");
            xml.append("  </record>\n");
        }
        
        xml.append("</feedback>\n");
        
        return xml.toString();
    }
    
    /**
     * 发送报告给接收方
     */
    private void sendReportToRecipient(DmarcReport report, String recipientEmail) throws Exception {
        
        String subject = String.format("Report Domain: %s Submitter: %s Report-ID: %s",
            report.getDomain(), reportOrgName, report.getReportId());
        
        String body = String.format(
            "This is a DMARC aggregate report for %s\n\n" +
            "Report Period: %s to %s\n" +
            "Total Messages: %d\n" +
            "Compliant Messages: %d\n" +
            "Compliance Rate: %.2f%%\n\n" +
            "Please find the detailed report in the attached file.",
            report.getDomain(),
            report.getBeginTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            report.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            report.getTotalMessages(),
            report.getCompliantMessages(),
            report.getComplianceRate()
        );
        
        // 发送邮件
        emailQueueService.sendEmailWithAttachment(
            reportContactEmail,
            recipientEmail,
            subject,
            body,
            report.getReportPath(),
            "application/gzip"
        );
        
        report.setRecipientUri(recipientEmail);
    }
    
    /**
     * 获取DMARC RUA地址
     */
    private List<String> getDmarcRuaAddresses(String domain) {
        // 这里应该查询DNS TXT记录获取DMARC策略
        // 简化实现，返回配置的默认地址
        return Arrays.asList("dmarc-reports@" + domain);
    }
    
    /**
     * 从邮箱地址提取域名
     */
    private String extractDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.lastIndexOf("@") + 1);
    }
    
    /**
     * 从SPF记录提取域名
     */
    private String extractDomainFromSpfRecord(String spfRecord) {
        if (spfRecord == null) return null;
        
        // 简化实现，实际应该解析SPF记录
        if (spfRecord.contains("include:")) {
            String[] parts = spfRecord.split("include:");
            if (parts.length > 1) {
                return parts[1].split("\\s+")[0];
            }
        }
        return null;
    }
    
    /**
     * 转换认证结果
     */
    private DmarcReportRecord.AuthResult convertToAuthResult(String status) {
        if (status == null) return null;
        
        switch (status.toLowerCase()) {
            case "pass": return DmarcReportRecord.AuthResult.PASS;
            case "fail": return DmarcReportRecord.AuthResult.FAIL;
            case "softfail": return DmarcReportRecord.AuthResult.SOFT_FAIL;
            case "neutral": return DmarcReportRecord.AuthResult.NEUTRAL;
            case "temperror": return DmarcReportRecord.AuthResult.TEMP_ERROR;
            case "permerror": return DmarcReportRecord.AuthResult.PERM_ERROR;
            default: return DmarcReportRecord.AuthResult.NONE;
        }
    }
    
    /**
     * 转换DMARC结果
     */
    private DmarcReportRecord.DMARCResult convertToDmarcResult(String status) {
        if (status == null) return null;
        
        switch (status.toLowerCase()) {
            case "pass": return DmarcReportRecord.DMARCResult.PASS;
            default: return DmarcReportRecord.DMARCResult.FAIL;
        }
    }
    
    /**
     * 确定DMARC处置结果
     */
    private DmarcReportRecord.DispositionType determineDmarcDisposition(EmailValidationLog log) {
        String dmarcStatus = log.getDmarcStatus();
        String dmarcPolicy = log.getDmarcPolicy();
        
        if ("pass".equalsIgnoreCase(dmarcStatus)) {
            return DmarcReportRecord.DispositionType.NONE;
        }
        
        if (dmarcPolicy != null) {
            switch (dmarcPolicy.toLowerCase()) {
                case "quarantine": return DmarcReportRecord.DispositionType.QUARANTINE;
                case "reject": return DmarcReportRecord.DispositionType.REJECT;
            }
        }
        
        return DmarcReportRecord.DispositionType.NONE;
    }
    
    /**
     * XML转义
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}