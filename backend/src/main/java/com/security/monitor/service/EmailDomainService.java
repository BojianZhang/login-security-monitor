package com.security.monitor.service;

import com.security.monitor.model.EmailDomain;
import com.security.monitor.repository.EmailDomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 邮件域名服务 - 带缓存优化
 */
@Service
@Transactional
public class EmailDomainService {
    
    @Autowired
    private EmailDomainRepository domainRepository;
    
    /**
     * 获取活跃域名列表 - 缓存1小时
     */
    @Cacheable(value = "activeDomains", unless = "#result.size() == 0")
    @Transactional(readOnly = true)
    public List<EmailDomain> getActiveDomains() {
        return domainRepository.findByIsActiveTrue();
    }
    
    /**
     * 根据ID获取域名 - 缓存30分钟
     */
    @Cacheable(value = "domains", key = "#domainId", unless = "#result == null")
    @Transactional(readOnly = true)
    public Optional<EmailDomain> getDomainById(Long domainId) {
        return domainRepository.findById(domainId);
    }
    
    /**
     * 根据域名获取 - 缓存30分钟
     */
    @Cacheable(value = "domains", key = "#domainName", unless = "#result == null")
    @Transactional(readOnly = true)
    public Optional<EmailDomain> getDomainByName(String domainName) {
        return domainRepository.findByDomainName(domainName);
    }
    
    /**
     * 清除域名缓存
     */
    @CacheEvict(value = {"domains", "activeDomains"}, allEntries = true)
    public void clearDomainCache() {
        // 缓存清除
    }
    
    /**
     * 更新域名后清除相关缓存
     */
    @CacheEvict(value = {"domains", "activeDomains"}, allEntries = true)
    public EmailDomain updateDomain(EmailDomain domain) {
        return domainRepository.save(domain);
    }
}