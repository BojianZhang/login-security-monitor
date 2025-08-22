<template>
  <div class="antivirus-management">
    <div class="page-header">
      <h1><i class="fas fa-shield-virus"></i> 防病毒管理</h1>
      <div class="header-actions">
        <button @click="updateVirusDefinitions" class="btn btn-primary" :disabled="updating">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': updating }"></i>
          更新病毒库
        </button>
        <button @click="startBatchScan" class="btn btn-warning" :disabled="scanning">
          <i class="fas fa-search" :class="{ 'fa-spin': scanning }"></i>
          批量扫描
        </button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon threat">
          <i class="fas fa-exclamation-triangle"></i>
        </div>
        <div class="stat-content">
          <h3>{{ statistics.threatsFound || 0 }}</h3>
          <p>发现威胁</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon scanned">
          <i class="fas fa-shield-alt"></i>
        </div>
        <div class="stat-content">
          <h3>{{ statistics.totalScans || 0 }}</h3>
          <p>总扫描次数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon quarantine">
          <i class="fas fa-lock"></i>
        </div>
        <div class="stat-content">
          <h3>{{ quarantineCount }}</h3>
          <p>隔离文件</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon rate">
          <i class="fas fa-percentage"></i>
        </div>
        <div class="stat-content">
          <h3>{{ (statistics.threatRate || 0).toFixed(2) }}%</h3>
          <p>威胁检出率</p>
        </div>
      </div>
    </div>

    <!-- 扫描引擎状态 -->
    <div class="engine-status">
      <h2><i class="fas fa-cogs"></i> 扫描引擎状态</h2>
      <div class="status-grid">
        <div class="status-item">
          <label>引擎状态:</label>
          <span class="badge badge-success">
            <i class="fas fa-check-circle"></i> 运行中
          </span>
        </div>
        <div class="status-item">
          <label>引擎版本:</label>
          <span>{{ engineStatus.version || '1.0.0' }}</span>
        </div>
        <div class="status-item">
          <label>病毒库数量:</label>
          <span>{{ engineStatus.definitionsCount || 0 }} 个</span>
        </div>
        <div class="status-item">
          <label>最后更新:</label>
          <span>{{ formatDateTime(engineStatus.lastUpdate) }}</span>
        </div>
      </div>
    </div>

    <!-- 扫描日志 -->
    <div class="scan-logs">
      <h2><i class="fas fa-list-alt"></i> 最近扫描日志</h2>
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>时间</th>
              <th>邮件ID</th>
              <th>扫描状态</th>
              <th>发现威胁</th>
              <th>扫描文件数</th>
              <th>处理时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in scanLogs" :key="log.id">
              <td>{{ formatDateTime(log.scannedAt) }}</td>
              <td>{{ log.messageId }}</td>
              <td>
                <span class="badge" :class="getScanStatusClass(log.scanStatus)">
                  {{ getScanStatusText(log.scanStatus) }}
                </span>
              </td>
              <td>{{ log.threatFound ? '是' : '否' }}</td>
              <td>{{ log.filesScanned }}</td>
              <td>{{ log.processingTimeMs }}ms</td>
              <td>
                <button @click="viewDetails(log)" class="btn btn-sm btn-outline-primary">
                  详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 隔离文件管理 -->
    <div class="quarantine-section">
      <h2><i class="fas fa-lock"></i> 隔离文件管理</h2>
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>文件名</th>
              <th>文件大小</th>
              <th>隔离原因</th>
              <th>隔离时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="file in quarantinedFiles" :key="file.id">
              <td>
                <i class="fas fa-file"></i>
                {{ file.filename }}
              </td>
              <td>{{ formatFileSize(file.fileSize) }}</td>
              <td>{{ file.quarantineReason }}</td>
              <td>{{ formatDateTime(file.lastScannedAt) }}</td>
              <td>
                <button @click="restoreFile(file)" class="btn btn-sm btn-success me-1">
                  <i class="fas fa-undo"></i> 恢复
                </button>
                <button @click="deleteFile(file)" class="btn btn-sm btn-danger">
                  <i class="fas fa-trash"></i> 删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 扫描设置 -->
    <div class="scan-settings">
      <h2><i class="fas fa-cog"></i> 扫描设置</h2>
      <div class="settings-grid">
        <div class="setting-item">
          <label for="maxFileSize">最大扫描文件大小 (MB):</label>
          <input 
            id="maxFileSize" 
            v-model="settings.maxFileSize" 
            type="number" 
            class="form-control"
            min="1"
            max="1024"
          >
        </div>
        <div class="setting-item">
          <label for="scanTimeout">扫描超时时间 (秒):</label>
          <input 
            id="scanTimeout" 
            v-model="settings.scanTimeout" 
            type="number" 
            class="form-control"
            min="5"
            max="300"
          >
        </div>
        <div class="setting-item">
          <label for="autoScan">自动扫描新邮件:</label>
          <input 
            id="autoScan" 
            v-model="settings.autoScan" 
            type="checkbox" 
            class="form-check-input"
          >
        </div>
        <div class="setting-item">
          <label for="quarantinePath">隔离目录:</label>
          <input 
            id="quarantinePath" 
            v-model="settings.quarantinePath" 
            type="text" 
            class="form-control"
            readonly
          >
        </div>
      </div>
      <div class="settings-actions">
        <button @click="saveSettings" class="btn btn-primary">
          <i class="fas fa-save"></i> 保存设置
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useToast } from '@/composables/useToast'
import { antivirusAPI } from '@/api/antivirus'

export default {
  name: 'AntiVirusManagement',
  setup() {
    const { showToast } = useToast()
    
    // 响应式数据
    const statistics = ref({})
    const engineStatus = ref({})
    const scanLogs = ref([])
    const quarantinedFiles = ref([])
    const updating = ref(false)
    const scanning = ref(false)
    
    const settings = ref({
      maxFileSize: 100,
      scanTimeout: 30,
      autoScan: true,
      quarantinePath: '/opt/quarantine'
    })

    // 计算属性
    const quarantineCount = computed(() => quarantinedFiles.value.length)

    // 生命周期
    onMounted(() => {
      loadData()
    })

    // 方法
    const loadData = async () => {
      try {
        await Promise.all([
          loadStatistics(),
          loadEngineStatus(),
          loadScanLogs(),
          loadQuarantinedFiles()
        ])
      } catch (error) {
        console.error('加载数据失败:', error)
        showToast('加载数据失败', 'error')
      }
    }

    const loadStatistics = async () => {
      try {
        const endDate = new Date()
        const startDate = new Date(endDate.getTime() - 30 * 24 * 60 * 60 * 1000) // 30天前
        
        const response = await antivirusAPI.getStatistics({
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString()
        })
        statistics.value = response.data
      } catch (error) {
        console.error('加载统计信息失败:', error)
      }
    }

    const loadEngineStatus = async () => {
      try {
        const response = await antivirusAPI.getStatus()
        engineStatus.value = response.data
      } catch (error) {
        console.error('加载引擎状态失败:', error)
      }
    }

    const loadScanLogs = async () => {
      try {
        const response = await antivirusAPI.getRecentLogs({ limit: 10 })
        scanLogs.value = response.data
      } catch (error) {
        console.error('加载扫描日志失败:', error)
      }
    }

    const loadQuarantinedFiles = async () => {
      try {
        const response = await antivirusAPI.getQuarantinedFiles({
          page: 0,
          size: 20
        })
        quarantinedFiles.value = response.data.content || []
      } catch (error) {
        console.error('加载隔离文件失败:', error)
      }
    }

    const updateVirusDefinitions = async () => {
      updating.value = true
      try {
        await antivirusAPI.updateDefinitions()
        showToast('病毒库更新成功', 'success')
        await loadEngineStatus()
      } catch (error) {
        console.error('更新病毒库失败:', error)
        showToast('更新病毒库失败', 'error')
      } finally {
        updating.value = false
      }
    }

    const startBatchScan = async () => {
      scanning.value = true
      try {
        // 这里需要实现批量扫描逻辑
        showToast('批量扫描已启动', 'info')
        // 模拟扫描过程
        setTimeout(() => {
          scanning.value = false
          showToast('批量扫描完成', 'success')
          loadData()
        }, 3000)
      } catch (error) {
        console.error('批量扫描失败:', error)
        showToast('批量扫描失败', 'error')
        scanning.value = false
      }
    }

    const restoreFile = async (file) => {
      try {
        await antivirusAPI.restoreFromQuarantine(file.id)
        showToast(`文件 ${file.filename} 已恢复`, 'success')
        await loadQuarantinedFiles()
      } catch (error) {
        console.error('恢复文件失败:', error)
        showToast('恢复文件失败', 'error')
      }
    }

    const deleteFile = async (file) => {
      if (!confirm(`确定要删除文件 ${file.filename} 吗？此操作不可恢复。`)) {
        return
      }
      
      try {
        await antivirusAPI.deleteQuarantinedFile(file.id)
        showToast(`文件 ${file.filename} 已删除`, 'success')
        await loadQuarantinedFiles()
      } catch (error) {
        console.error('删除文件失败:', error)
        showToast('删除文件失败', 'error')
      }
    }

    const saveSettings = async () => {
      try {
        await antivirusAPI.updateSettings(settings.value)
        showToast('设置已保存', 'success')
      } catch (error) {
        console.error('保存设置失败:', error)
        showToast('保存设置失败', 'error')
      }
    }

    const viewDetails = (log) => {
      // 实现查看详情功能
      console.log('查看扫描详情:', log)
    }

    // 工具方法
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-'
      return new Date(dateTime).toLocaleString('zh-CN')
    }

    const formatFileSize = (size) => {
      if (!size) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let unitIndex = 0
      let fileSize = size
      
      while (fileSize >= 1024 && unitIndex < units.length - 1) {
        fileSize /= 1024
        unitIndex++
      }
      
      return `${fileSize.toFixed(1)} ${units[unitIndex]}`
    }

    const getScanStatusClass = (status) => {
      const statusMap = {
        'CLEAN': 'badge-success',
        'INFECTED': 'badge-danger',
        'SUSPICIOUS': 'badge-warning',
        'ERROR': 'badge-secondary'
      }
      return statusMap[status] || 'badge-secondary'
    }

    const getScanStatusText = (status) => {
      const statusMap = {
        'CLEAN': '清洁',
        'INFECTED': '感染',
        'SUSPICIOUS': '可疑',
        'ERROR': '错误'
      }
      return statusMap[status] || status
    }

    return {
      statistics,
      engineStatus,
      scanLogs,
      quarantinedFiles,
      updating,
      scanning,
      settings,
      quarantineCount,
      loadData,
      updateVirusDefinitions,
      startBatchScan,
      restoreFile,
      deleteFile,
      saveSettings,
      viewDetails,
      formatDateTime,
      formatFileSize,
      getScanStatusClass,
      getScanStatusText
    }
  }
}
</script>

<style scoped>
.antivirus-management {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e0e0e0;
}

.page-header h1 {
  color: #333;
  font-size: 24px;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.stat-icon.threat { background: #dc3545; }
.stat-icon.scanned { background: #28a745; }
.stat-icon.quarantine { background: #ffc107; }
.stat-icon.rate { background: #17a2b8; }

.stat-content h3 {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.engine-status {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.engine-status h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 20px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.status-item {
  display: flex;
  flex-direction: column;
}

.status-item label {
  font-weight: bold;
  color: #666;
  margin-bottom: 5px;
}

.scan-logs, .quarantine-section, .scan-settings {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.scan-logs h2, .quarantine-section h2, .scan-settings h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 20px;
}

.table-responsive {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th, .table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.table th {
  background: #f8f9fa;
  font-weight: bold;
  color: #333;
}

.badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.badge-success { background: #d4edda; color: #155724; }
.badge-danger { background: #f8d7da; color: #721c24; }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-secondary { background: #e2e3e5; color: #383d41; }

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.setting-item {
  display: flex;
  flex-direction: column;
}

.setting-item label {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.form-control {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-check-input {
  margin-top: 5px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.btn-primary { background: #007bff; color: white; }
.btn-success { background: #28a745; color: white; }
.btn-warning { background: #ffc107; color: #212529; }
.btn-danger { background: #dc3545; color: white; }
.btn-outline-primary { background: transparent; color: #007bff; border: 1px solid #007bff; }

.btn:hover {
  opacity: 0.9;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-sm {
  padding: 4px 8px;
  font-size: 12px;
}

.me-1 {
  margin-right: 5px;
}

.fa-spin {
  animation: fa-spin 1s infinite linear;
}

@keyframes fa-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>