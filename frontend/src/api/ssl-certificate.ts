import request from '@/utils/request'

// SSL证书管理API
export const sslCertificateApi = {
  // 获取域名的所有证书
  getDomainCertificates(domainName: string) {
    return request({
      url: `/ssl/certificates/domain/${domainName}`,
      method: 'get'
    })
  },

  // 获取域名的活跃证书
  getActiveCertificate(domainName: string) {
    return request({
      url: `/ssl/certificates/domain/${domainName}/active`,
      method: 'get'
    })
  },

  // 申请Let's Encrypt免费证书
  requestLetsEncryptCertificate(data: {
    domainName: string
    email: string
    challengeType?: string
  }) {
    return request({
      url: '/ssl/certificates/request/letsencrypt',
      method: 'post',
      data
    })
  },

  // 上传用户证书
  uploadCertificate(formData: FormData) {
    return request({
      url: '/ssl/certificates/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 续期证书
  renewCertificate(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}/renew`,
      method: 'post'
    })
  },

  // 删除证书
  deleteCertificate(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}`,
      method: 'delete'
    })
  },

  // 获取需要续期的证书
  getCertificatesNeedingRenewal() {
    return request({
      url: '/ssl/certificates/renewal/needed',
      method: 'get'
    })
  },

  // 获取即将过期的证书
  getExpiringCertificates(days: number = 30) {
    return request({
      url: '/ssl/certificates/expiring',
      method: 'get',
      params: { days }
    })
  },

  // 获取证书统计信息
  getStatistics() {
    return request({
      url: '/ssl/certificates/statistics',
      method: 'get'
    })
  },

  // 检查域名是否有活跃证书
  checkActiveCertificate(domainName: string) {
    return request({
      url: `/ssl/certificates/domain/${domainName}/check`,
      method: 'get'
    })
  },

  // 获取所有证书列表（分页）
  getAllCertificates(page: number = 0, size: number = 20) {
    return request({
      url: '/ssl/certificates',
      method: 'get',
      params: { page, size }
    })
  },

  // 批量续期证书
  batchRenewCertificates() {
    return request({
      url: '/ssl/certificates/batch/renew',
      method: 'post'
    })
  },

  // 更新证书自动续期设置
  updateAutoRenew(certificateId: number, autoRenew: boolean) {
    return request({
      url: `/ssl/certificates/${certificateId}/auto-renew`,
      method: 'patch',
      data: { autoRenew }
    })
  },

  // 获取证书详情
  getCertificateDetail(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}`,
      method: 'get'
    })
  },

  // 测试证书配置
  testCertificate(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}/test`,
      method: 'post'
    })
  },

  // 导出证书
  exportCertificate(certificateId: number, format: 'pem' | 'pfx' | 'jks' = 'pem') {
    return request({
      url: `/ssl/certificates/${certificateId}/export`,
      method: 'get',
      params: { format },
      responseType: 'blob'
    })
  },

  // 获取证书监控状态
  getMonitoringStatus(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}/monitoring`,
      method: 'get'
    })
  },

  // 更新证书监控设置
  updateMonitoringSettings(certificateId: number, settings: {
    monitoringEnabled: boolean
    checkInterval?: number
    warningThreshold?: number
    criticalThreshold?: number
  }) {
    return request({
      url: `/ssl/certificates/${certificateId}/monitoring`,
      method: 'patch',
      data: settings
    })
  },

  // 手动触发证书检查
  triggerCertificateCheck(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}/check`,
      method: 'post'
    })
  },

  // 获取证书使用记录
  getCertificateUsage(certificateId: number) {
    return request({
      url: `/ssl/certificates/${certificateId}/usage`,
      method: 'get'
    })
  },

  // 添加证书使用记录
  addCertificateUsage(certificateId: number, usage: {
    serviceName: string
    serviceConfigPath?: string
  }) {
    return request({
      url: `/ssl/certificates/${certificateId}/usage`,
      method: 'post',
      data: usage
    })
  },

  // 获取续期日志
  getRenewalLogs(certificateId: number, page: number = 0, size: number = 10) {
    return request({
      url: `/ssl/certificates/${certificateId}/renewal-logs`,
      method: 'get',
      params: { page, size }
    })
  },

  // 获取ACME挑战信息
  getAcmeChallenge(domainName: string, challengeType: string) {
    return request({
      url: `/ssl/certificates/acme/challenge`,
      method: 'get',
      params: { domainName, challengeType }
    })
  },

  // 验证ACME挑战
  verifyAcmeChallenge(domainName: string, challengeType: string, token: string) {
    return request({
      url: `/ssl/certificates/acme/verify`,
      method: 'post',
      data: { domainName, challengeType, token }
    })
  },

  // 获取系统SSL配置
  getSystemSslConfig() {
    return request({
      url: '/ssl/config',
      method: 'get'
    })
  },

  // 更新系统SSL配置
  updateSystemSslConfig(config: {
    autoRenewEnabled?: boolean
    renewalDaysBefore?: number
    schedulerEnabled?: boolean
    batchSize?: number
    maxConcurrent?: number
    notificationsEnabled?: boolean
    adminEmails?: string[]
  }) {
    return request({
      url: '/ssl/config',
      method: 'patch',
      data: config
    })
  },

  // 获取SSL证书模板
  getCertificateTemplates() {
    return request({
      url: '/ssl/certificates/templates',
      method: 'get'
    })
  },

  // 创建证书模板
  createCertificateTemplate(template: {
    name: string
    description?: string
    certificateType: string
    autoRenew: boolean
    renewalDaysBefore: number
    challengeType?: string
  }) {
    return request({
      url: '/ssl/certificates/templates',
      method: 'post',
      data: template
    })
  },

  // 从模板创建证书
  createFromTemplate(templateId: number, data: {
    domainName: string
    certificateName: string
    email?: string
  }) {
    return request({
      url: `/ssl/certificates/templates/${templateId}/create`,
      method: 'post',
      data
    })
  }
}

// SSL证书类型定义
export interface SslCertificate {
  id: number
  domainName: string
  certificateName: string
  certificateType: 'FREE_LETSENCRYPT' | 'FREE_ZEROSSL' | 'USER_UPLOADED' | 'SELF_SIGNED'
  status: 'PENDING' | 'ACTIVE' | 'EXPIRED' | 'REVOKED' | 'ERROR' | 'RENEWAL_NEEDED'
  issuer?: string
  serialNumber?: string
  issuedAt?: string
  expiresAt?: string
  autoRenew: boolean
  renewalDaysBefore: number
  lastRenewalAttempt?: string
  lastError?: string
  challengeType?: string
  acmeAccountEmail?: string
  isActive: boolean
  createdAt: string
  updatedAt: string
  isFreeType: boolean
}

export interface CertificateStatistics {
  totalCertificates: number
  activeCertificates: number
  expiredCertificates: number
  expiringSoonCertificates: number
}

export interface RenewalLog {
  id: number
  certificateId: number
  renewalType: 'MANUAL' | 'AUTOMATIC' | 'FORCED'
  status: 'SUCCESS' | 'FAILED' | 'PENDING'
  oldExpiresAt?: string
  newExpiresAt?: string
  errorMessage?: string
  attemptDurationMs: number
  createdAt: string
}

export interface CertificateUsage {
  id: number
  certificateId: number
  serviceName: string
  serviceConfigPath?: string
  usageStartDate: string
  usageEndDate?: string
  isActive: boolean
}

export interface SslConfig {
  autoRenewEnabled: boolean
  renewalDaysBefore: number
  schedulerEnabled: boolean
  batchSize: number
  maxConcurrent: number
  notificationsEnabled: boolean
  adminEmails: string[]
  storePath: string
}

export default sslCertificateApi