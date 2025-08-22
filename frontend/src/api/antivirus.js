import axios from 'axios'

const API_BASE = '/api/antivirus'

export const antivirusAPI = {
  /**
   * 扫描指定邮件的附件
   */
  scanEmailMessage(messageId) {
    return axios.post(`${API_BASE}/scan/message/${messageId}`)
  },

  /**
   * 扫描单个附件
   */
  scanAttachment(attachmentId) {
    return axios.post(`${API_BASE}/scan/attachment/${attachmentId}`)
  },

  /**
   * 批量扫描附件
   */
  batchScanAttachments(attachmentIds) {
    return axios.post(`${API_BASE}/scan/batch`, attachmentIds)
  },

  /**
   * 获取扫描统计信息
   */
  getStatistics(params) {
    return axios.get(`${API_BASE}/statistics`, { params })
  },

  /**
   * 更新病毒定义库
   */
  updateDefinitions() {
    return axios.post(`${API_BASE}/update-definitions`)
  },

  /**
   * 获取隔离文件列表
   */
  getQuarantinedFiles(params = {}) {
    return axios.get(`${API_BASE}/quarantine`, { params })
  },

  /**
   * 从隔离区恢复文件
   */
  restoreFromQuarantine(attachmentId) {
    return axios.post(`${API_BASE}/quarantine/${attachmentId}/restore`)
  },

  /**
   * 删除隔离文件
   */
  deleteQuarantinedFile(attachmentId) {
    return axios.delete(`${API_BASE}/quarantine/${attachmentId}`)
  },

  /**
   * 获取扫描引擎状态
   */
  getStatus() {
    return axios.get(`${API_BASE}/status`)
  },

  /**
   * 获取最近的扫描日志
   */
  getRecentLogs(params = {}) {
    return axios.get(`${API_BASE}/logs/recent`, { params })
  },

  /**
   * 更新扫描设置
   */
  updateSettings(settings) {
    return axios.post(`${API_BASE}/settings`, settings)
  },

  /**
   * 获取病毒定义列表
   */
  getVirusDefinitions(params = {}) {
    return axios.get(`${API_BASE}/definitions`, { params })
  },

  /**
   * 添加病毒定义
   */
  addVirusDefinition(definition) {
    return axios.post(`${API_BASE}/definitions`, definition)
  },

  /**
   * 更新病毒定义
   */
  updateVirusDefinition(id, definition) {
    return axios.put(`${API_BASE}/definitions/${id}`, definition)
  },

  /**
   * 删除病毒定义
   */
  deleteVirusDefinition(id) {
    return axios.delete(`${API_BASE}/definitions/${id}`)
  },

  /**
   * 启用/禁用病毒定义
   */
  toggleVirusDefinition(id, isActive) {
    return axios.patch(`${API_BASE}/definitions/${id}/toggle`, { isActive })
  },

  /**
   * 获取扫描历史
   */
  getScanHistory(params = {}) {
    return axios.get(`${API_BASE}/history`, { params })
  },

  /**
   * 获取威胁检测趋势
   */
  getThreatTrend(params = {}) {
    return axios.get(`${API_BASE}/trend`, { params })
  },

  /**
   * 导出扫描报告
   */
  exportScanReport(params = {}) {
    return axios.get(`${API_BASE}/report/export`, { 
      params,
      responseType: 'blob'
    })
  },

  /**
   * 测试病毒检测（使用EICAR测试文件）
   */
  testVirusDetection() {
    return axios.post(`${API_BASE}/test`)
  },

  /**
   * 获取隔离区统计
   */
  getQuarantineStatistics() {
    return axios.get(`${API_BASE}/quarantine/statistics`)
  },

  /**
   * 清空隔离区
   */
  clearQuarantine() {
    return axios.delete(`${API_BASE}/quarantine/clear`)
  },

  /**
   * 扫描配置验证
   */
  validateConfiguration() {
    return axios.post(`${API_BASE}/validate-config`)
  }
}

export default antivirusAPI