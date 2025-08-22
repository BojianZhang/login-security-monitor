<template>
  <div class="dashboard-container">
    <!-- 概览卡片 -->
    <div class="overview-cards">
      <el-row :gutter="24">
        <el-col :xs="12" :sm="6" :lg="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon total-users">
                <el-icon><User /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ overview?.totalUsers || 0 }}</div>
                <div class="card-label">总用户数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :lg="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon active-users">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ overview?.activeUsers || 0 }}</div>
                <div class="card-label">活跃用户</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :lg="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon logins-24h">
                <el-icon><Odometer /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ overview?.loginsLast24h || 0 }}</div>
                <div class="card-label">24小时登录</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="12" :sm="6" :lg="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon security-alerts">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-number">{{ overview?.openAlerts || 0 }}</div>
                <div class="card-label">待处理警报</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="24" class="charts-section">
      <!-- 登录趋势图 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>登录趋势</span>
              <el-select v-model="trendDays" @change="fetchTrendData" size="small">
                <el-option label="最近7天" :value="7" />
                <el-option label="最近14天" :value="14" />
                <el-option label="最近30天" :value="30" />
              </el-select>
            </div>
          </template>
          <div class="chart-container">
            <v-chart
              v-if="trendChartOption"
              :option="trendChartOption"
              :loading="isLoadingTrends"
              autoresize
            />
            <el-empty v-else description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 风险分布图 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <span>风险等级分布</span>
          </template>
          <div class="chart-container">
            <v-chart
              v-if="riskChartOption"
              :option="riskChartOption"
              :loading="isLoadingRiskData"
              autoresize
            />
            <el-empty v-else description="暂无数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格区域 -->
    <el-row :gutter="24" class="tables-section">
      <!-- 最近登录记录 -->
      <el-col :xs="24" :lg="12">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <span>最近登录记录</span>
              <el-button 
                type="text" 
                @click="$router.push('/login-records')"
                size="small"
              >
                查看更多
              </el-button>
            </div>
          </template>
          
          <el-table
            :data="recentLogins?.content || []"
            :loading="isLoadingLogins"
            size="small"
            height="300"
          >
            <el-table-column prop="user.username" label="用户" width="100" />
            <el-table-column label="登录时间" width="140">
              <template #default="{ row }">
                {{ formatTime(row.loginTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="ipAddress" label="IP地址" width="120" />
            <el-table-column label="位置" width="120">
              <template #default="{ row }">
                {{ formatLocation(row) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag
                  :type="row.loginStatus === 'SUCCESS' ? 'success' : 'danger'"
                  size="small"
                >
                  {{ row.loginStatus === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险" width="80">
              <template #default="{ row }">
                <el-tag
                  :type="getRiskTagType(row.riskScore)"
                  size="small"
                >
                  {{ getRiskLevel(row.riskScore) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 最新安全警报 -->
      <el-col :xs="24" :lg="12">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <span>最新安全警报</span>
              <el-button 
                type="text" 
                @click="$router.push('/security-alerts')"
                size="small"
              >
                查看更多
              </el-button>
            </div>
          </template>
          
          <el-table
            :data="securityAlerts?.content || []"
            :loading="isLoadingAlerts"
            size="small"
            height="300"
          >
            <el-table-column prop="user.username" label="用户" width="100" />
            <el-table-column label="警报类型" width="120">
              <template #default="{ row }">
                {{ getAlertTypeName(row.alertType) }}
              </template>
            </el-table-column>
            <el-table-column label="严重程度" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="getSeverityTagType(row.severity)"
                  size="small"
                >
                  {{ getSeverityName(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="140">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag
                  :type="getStatusTagType(row.status)"
                  size="small"
                >
                  {{ getStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import type { LoginRecord, SecurityAlert } from '@/types/api'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const dashboardStore = useDashboardStore()

// 响应式数据
const trendDays = ref(7)

// 计算属性
const overview = computed(() => dashboardStore.overview)
const recentLogins = computed(() => dashboardStore.recentLogins)
const securityAlerts = computed(() => dashboardStore.securityAlerts)
const loginTrends = computed(() => dashboardStore.loginTrends)
const riskDistribution = computed(() => dashboardStore.riskDistribution)

const isLoadingOverview = computed(() => dashboardStore.isLoadingOverview)
const isLoadingLogins = computed(() => dashboardStore.isLoadingLogins)
const isLoadingAlerts = computed(() => dashboardStore.isLoadingAlerts)
const isLoadingTrends = computed(() => dashboardStore.isLoadingTrends)
const isLoadingRiskData = computed(() => dashboardStore.isLoadingRiskData)

// 图表配置
const trendChartOption = computed(() => {
  const trends = loginTrends.value
  if (!trends || trends.length === 0) return null

  const dates = trends.map(t => dayjs(t.date).format('MM-DD'))
  const successData = trends.map(t => t.successfulLogins)
  const failedData = trends.map(t => t.failedLogins)
  const suspiciousData = trends.map(t => t.suspiciousLogins)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['成功登录', '失败登录', '可疑登录']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '成功登录',
        type: 'line',
        data: successData,
        smooth: true,
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '失败登录',
        type: 'line',
        data: failedData,
        smooth: true,
        itemStyle: { color: '#ff4d4f' }
      },
      {
        name: '可疑登录',
        type: 'line',
        data: suspiciousData,
        smooth: true,
        itemStyle: { color: '#faad14' }
      }
    ]
  }
})

const riskChartOption = computed(() => {
  const risk = riskDistribution.value
  if (!risk || Object.keys(risk).length === 0) return null

  const data = [
    { value: risk.high || 0, name: '高风险', itemStyle: { color: '#ff4d4f' } },
    { value: risk.medium || 0, name: '中风险', itemStyle: { color: '#faad14' } },
    { value: risk.low || 0, name: '低风险', itemStyle: { color: '#52c41a' } }
  ]

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '风险分布',
        type: 'pie',
        radius: '60%',
        center: ['50%', '60%'],
        data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
})

// 方法
const fetchTrendData = () => {
  dashboardStore.fetchLoginTrends(trendDays.value)
}

const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

const formatLocation = (record: LoginRecord) => {
  const parts = []
  if (record.country) parts.push(record.country)
  if (record.city) parts.push(record.city)
  return parts.join(', ') || '未知'
}

const getRiskLevel = (score: number) => {
  if (score >= 70) return '高'
  if (score >= 40) return '中'
  return '低'
}

const getRiskTagType = (score: number) => {
  if (score >= 70) return 'danger'
  if (score >= 40) return 'warning'
  return 'success'
}

const getAlertTypeName = (type: string) => {
  const typeMap: { [key: string]: string } = {
    LOCATION_ANOMALY: '异地登录',
    MULTIPLE_LOCATIONS: '多地登录',
    HIGH_RISK_SCORE: '高风险登录',
    SUSPICIOUS_DEVICE: '可疑设备',
    BRUTE_FORCE: '暴力破解'
  }
  return typeMap[type] || type
}

const getSeverityName = (severity: string) => {
  const severityMap: { [key: string]: string } = {
    CRITICAL: '严重',
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  }
  return severityMap[severity] || severity
}

const getSeverityTagType = (severity: string) => {
  const typeMap: { [key: string]: string } = {
    CRITICAL: 'danger',
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'info'
  }
  return typeMap[severity] || 'info'
}

const getStatusName = (status: string) => {
  const statusMap: { [key: string]: string } = {
    OPEN: '待处理',
    ACKNOWLEDGED: '已确认',
    RESOLVED: '已解决',
    FALSE_POSITIVE: '误报'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status: string) => {
  const typeMap: { [key: string]: string } = {
    OPEN: 'danger',
    ACKNOWLEDGED: 'warning',
    RESOLVED: 'success',
    FALSE_POSITIVE: 'info'
  }
  return typeMap[status] || 'info'
}

// 生命周期
onMounted(() => {
  dashboardStore.refreshAllData()
})

// 监听趋势天数变化
watch(trendDays, () => {
  fetchTrendData()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  .overview-cards {
    margin-bottom: 24px;
    
    .overview-card {
      .card-content {
        display: flex;
        align-items: center;
        
        .card-icon {
          width: 64px;
          height: 64px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 16px;
          
          .el-icon {
            font-size: 28px;
            color: #fff;
          }
          
          &.total-users {
            background: linear-gradient(135deg, #667eea, #764ba2);
          }
          
          &.active-users {
            background: linear-gradient(135deg, #f093fb, #f5576c);
          }
          
          &.logins-24h {
            background: linear-gradient(135deg, #4facfe, #00f2fe);
          }
          
          &.security-alerts {
            background: linear-gradient(135deg, #ffecd2, #fcb69f);
          }
        }
        
        .card-info {
          flex: 1;
          
          .card-number {
            font-size: 32px;
            font-weight: 600;
            color: #333;
            line-height: 1;
            margin-bottom: 4px;
          }
          
          .card-label {
            font-size: 14px;
            color: #999;
          }
        }
      }
    }
  }
  
  .charts-section {
    margin-bottom: 24px;
    
    .chart-card {
      .chart-container {
        height: 350px;
      }
    }
  }
  
  .tables-section {
    .table-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }
  }
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 500;
  }
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>