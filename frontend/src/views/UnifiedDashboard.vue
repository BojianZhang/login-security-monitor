<template>
  <div class="unified-dashboard">
    <!-- 页面头部 -->
    <div class="dashboard-header">
      <h1 class="page-title">
        <el-icon><Monitor /></el-icon>
        企业级安全邮件管理系统
      </h1>
      <p class="page-subtitle">集成邮件服务管理与登录安全监控</p>
    </div>

    <!-- 快速概览卡片 -->
    <div class="overview-section">
      <el-row :gutter="24">
        <!-- 邮件系统概览 -->
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="overview-card email-card" shadow="hover">
            <div class="card-content">
              <div class="card-icon email-icon">
                <el-icon><Message /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ emailStats.totalMessages || 0 }}</div>
                <div class="card-label">总邮件数</div>
                <div class="card-meta">未读: {{ emailStats.unreadMessages || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="overview-card alias-card" shadow="hover">
            <div class="card-content">
              <div class="card-icon alias-icon">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ emailStats.totalAliases || 0 }}</div>
                <div class="card-label">邮箱别名</div>
                <div class="card-meta">已激活</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <!-- 安全监控概览 -->
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="overview-card security-card" shadow="hover">
            <div class="card-content">
              <div class="card-icon security-icon">
                <el-icon><Shield /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ securityStats.totalLogins || 0 }}</div>
                <div class="card-label">24h登录</div>
                <div class="card-meta">安全状态</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="overview-card alerts-card" shadow="hover">
            <div class="card-content">
              <div class="card-icon alerts-icon">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ securityStats.activeAlerts || 0 }}</div>
                <div class="card-label">活跃警报</div>
                <div class="card-meta">需处理</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 主要功能区域 -->
    <div class="main-content">
      <el-row :gutter="24">
        <!-- 左侧：邮件管理 -->
        <el-col :xs="24" :lg="12">
          <el-card class="function-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Message /></el-icon>
                <span>邮件管理</span>
                <el-button type="primary" size="small" @click="goToEmailManagement">
                  管理邮件
                </el-button>
              </div>
            </template>
            
            <div class="email-management-preview">
              <!-- 邮箱别名列表 -->
              <div class="section-title">我的邮箱别名</div>
              <div class="aliases-list">
                <div 
                  v-for="alias in emailAliases.slice(0, 5)" 
                  :key="alias.id" 
                  class="alias-item"
                >
                  <div class="alias-email">
                    <el-icon><User /></el-icon>
                    {{ alias.aliasEmail }}@{{ alias.domain?.domainName }}
                  </div>
                  <div class="alias-status">
                    <el-tag 
                      :type="alias.isActive ? 'success' : 'info'"
                      size="small"
                    >
                      {{ alias.isActive ? '已激活' : '已禁用' }}
                    </el-tag>
                  </div>
                </div>
                <div v-if="emailAliases.length === 0" class="empty-state">
                  <el-empty description="暂无邮箱别名" :image-size="80">
                    <el-button type="primary" @click="createNewAlias">
                      创建别名
                    </el-button>
                  </el-empty>
                </div>
              </div>
              
              <!-- 快速操作 -->
              <div class="quick-actions">
                <el-button-group>
                  <el-button size="small" @click="createNewAlias">
                    <el-icon><Plus /></el-icon>
                    新建别名
                  </el-button>
                  <el-button size="small" @click="viewMessages">
                    <el-icon><View /></el-icon>
                    查看邮件
                  </el-button>
                  <el-button size="small" @click="manageSettings">
                    <el-icon><Setting /></el-icon>
                    邮件设置
                  </el-button>
                </el-button-group>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <!-- 右侧：安全监控 -->
        <el-col :xs="24" :lg="12">
          <el-card class="function-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><Shield /></el-icon>
                <span>安全监控</span>
                <el-button type="warning" size="small" @click="goToSecurityMonitor">
                  安全中心
                </el-button>
              </div>
            </template>
            
            <div class="security-monitoring-preview">
              <!-- 最近登录记录 -->
              <div class="section-title">最近登录记录</div>
              <div class="login-records">
                <div 
                  v-for="record in recentLogins.slice(0, 4)" 
                  :key="record.id"
                  class="login-record"
                >
                  <div class="login-info">
                    <div class="login-location">
                      <el-icon><MapLocation /></el-icon>
                      {{ record.city }}, {{ record.country }}
                    </div>
                    <div class="login-time">{{ formatTime(record.loginTime) }}</div>
                  </div>
                  <div class="login-status">
                    <el-tag 
                      :type="getLoginStatusType(record.loginStatus, record.riskScore)"
                      size="small"
                    >
                      {{ getLoginStatusText(record.loginStatus, record.riskScore) }}
                    </el-tag>
                  </div>
                </div>
              </div>
              
              <!-- 安全警报 -->
              <div class="section-title">
                活跃安全警报
                <el-badge :value="activeAlerts.length" class="alerts-badge" />
              </div>
              <div class="active-alerts">
                <div 
                  v-for="alert in activeAlerts.slice(0, 3)" 
                  :key="alert.id"
                  class="alert-item"
                  :class="'alert-' + alert.severity.toLowerCase()"
                >
                  <div class="alert-content">
                    <div class="alert-title">{{ alert.title }}</div>
                    <div class="alert-time">{{ formatTime(alert.createdAt) }}</div>
                  </div>
                  <div class="alert-severity">
                    <el-tag 
                      :type="getSeverityType(alert.severity)"
                      size="small"
                    >
                      {{ alert.severity }}
                    </el-tag>
                  </div>
                </div>
                <div v-if="activeAlerts.length === 0" class="no-alerts">
                  <el-icon class="safe-icon"><CircleCheckFilled /></el-icon>
                  <span>系统安全，无活跃警报</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 系统状态 -->
    <div class="system-status">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>系统状态</span>
            <div class="status-indicator">
              <el-tag :type="systemStatus.type" size="small">
                {{ systemStatus.text }}
              </el-tag>
            </div>
          </div>
        </template>
        
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8">
            <div class="status-item">
              <div class="status-label">邮件服务</div>
              <div class="status-value">
                <el-tag type="success" size="small">正常运行</el-tag>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="8">
            <div class="status-item">
              <div class="status-label">安全监控</div>
              <div class="status-value">
                <el-tag type="success" size="small">正常运行</el-tag>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="8">
            <div class="status-item">
              <div class="status-label">数据备份</div>
              <div class="status-value">
                <el-tag type="success" size="small">已备份</el-tag>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Monitor, Message, Shield, Warning, User, UserFilled,
  Plus, View, Setting, MapLocation, CircleCheckFilled
} from '@element-plus/icons-vue'
import api from '@/utils/api'

export default {
  name: 'UnifiedDashboard',
  components: {
    Monitor, Message, Shield, Warning, User, UserFilled,
    Plus, View, Setting, MapLocation, CircleCheckFilled
  },
  setup() {
    const router = useRouter()
    const loading = ref(true)
    
    // 响应式数据
    const emailStats = ref({})
    const emailAliases = ref([])
    const securityStats = ref({})
    const recentLogins = ref([])
    const activeAlerts = ref([])
    
    // 计算属性
    const systemStatus = computed(() => {
      const alertCount = activeAlerts.value.length
      if (alertCount === 0) {
        return { type: 'success', text: '系统正常' }
      } else if (alertCount <= 5) {
        return { type: 'warning', text: '注意安全' }
      } else {
        return { type: 'danger', text: '需要关注' }
      }
    })
    
    // 获取数据方法
    const fetchEmailStats = async () => {
      try {
        const response = await api.get('/email/stats')
        emailStats.value = response.data
      } catch (error) {
        console.error('获取邮件统计失败:', error)
      }
    }
    
    const fetchEmailAliases = async () => {
      try {
        const response = await api.get('/email/aliases')
        emailAliases.value = response.data
      } catch (error) {
        console.error('获取邮箱别名失败:', error)
      }
    }
    
    const fetchSecurityData = async () => {
      try {
        const [statsResponse, loginsResponse, alertsResponse] = await Promise.all([
          api.get('/security/dashboard'),
          api.get('/auth/recent-logins'),
          api.get('/security/alerts/active')
        ])
        
        securityStats.value = statsResponse.data
        recentLogins.value = loginsResponse.data.content || []
        activeAlerts.value = alertsResponse.data.content || []
      } catch (error) {
        console.error('获取安全数据失败:', error)
      }
    }
    
    // 工具方法
    const formatTime = (time) => {
      return new Date(time).toLocaleString('zh-CN')
    }
    
    const getLoginStatusType = (status, riskScore) => {
      if (status === 'FAILED') return 'danger'
      if (riskScore > 70) return 'warning'
      return 'success'
    }
    
    const getLoginStatusText = (status, riskScore) => {
      if (status === 'FAILED') return '失败'
      if (riskScore > 70) return '高风险'
      return '正常'
    }
    
    const getSeverityType = (severity) => {
      const typeMap = {
        'CRITICAL': 'danger',
        'HIGH': 'warning',
        'MEDIUM': 'warning',
        'LOW': 'info'
      }
      return typeMap[severity] || 'info'
    }
    
    // 导航方法
    const goToEmailManagement = () => {
      router.push('/email')
    }
    
    const goToSecurityMonitor = () => {
      router.push('/security')
    }
    
    const createNewAlias = () => {
      router.push('/email/aliases/new')
    }
    
    const viewMessages = () => {
      router.push('/email/messages')
    }
    
    const manageSettings = () => {
      router.push('/settings')
    }
    
    // 初始化数据
    const initializeDashboard = async () => {
      try {
        loading.value = true
        await Promise.all([
          fetchEmailStats(),
          fetchEmailAliases(),
          fetchSecurityData()
        ])
      } catch (error) {
        ElMessage.error('加载数据失败')
      } finally {
        loading.value = false
      }
    }
    
    onMounted(() => {
      initializeDashboard()
    })
    
    return {
      loading,
      emailStats,
      emailAliases,
      securityStats,
      recentLogins,
      activeAlerts,
      systemStatus,
      formatTime,
      getLoginStatusType,
      getLoginStatusText,
      getSeverityType,
      goToEmailManagement,
      goToSecurityMonitor,
      createNewAlias,
      viewMessages,
      manageSettings
    }
  }
}
</script>

<style scoped>
.unified-dashboard {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.dashboard-header {
  margin-bottom: 24px;
  text-align: center;
}

.page-title {
  color: #303133;
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.page-subtitle {
  color: #909399;
  font-size: 16px;
  margin: 0;
}

.overview-section {
  margin-bottom: 24px;
}

.overview-card {
  height: 120px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 16px;
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.email-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.alias-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.security-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.alerts-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.card-info {
  flex: 1;
}

.card-number {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 2px;
}

.card-meta {
  font-size: 12px;
  color: #909399;
}

.main-content {
  margin-bottom: 24px;
}

.function-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-header .el-icon {
  margin-right: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
}

.aliases-list, .login-records, .active-alerts {
  margin-bottom: 16px;
}

.alias-item, .login-record, .alert-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.alias-email, .login-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.login-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  color: #303133;
}

.login-time {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

.empty-state, .no-alerts {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.no-alerts {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.safe-icon {
  color: #67c23a;
  font-size: 18px;
}

.quick-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.system-status {
  margin-top: 24px;
}

.status-indicator {
  margin-left: auto;
}

.status-item {
  text-align: center;
  padding: 12px;
}

.status-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.alerts-badge {
  margin-left: 8px;
}

@media (max-width: 768px) {
  .unified-dashboard {
    padding: 12px;
  }
  
  .page-title {
    font-size: 24px;
    flex-direction: column;
    gap: 8px;
  }
  
  .card-content {
    padding: 12px;
  }
  
  .card-number {
    font-size: 28px;
  }
  
  .function-card {
    min-height: auto;
    margin-bottom: 16px;
  }
}
</style>