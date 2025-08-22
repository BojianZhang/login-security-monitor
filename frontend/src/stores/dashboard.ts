import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi } from '@/services/api'
import type { 
  DashboardOverview,
  LoginRecord,
  SecurityAlert,
  LoginTrend,
  GeoDistribution,
  RiskDistribution,
  PageResponse,
  QueryParams
} from '@/types/api'
import { ElMessage } from 'element-plus'

export const useDashboardStore = defineStore('dashboard', () => {
  // 状态
  const overview = ref<DashboardOverview | null>(null)
  const recentLogins = ref<PageResponse<LoginRecord> | null>(null)
  const securityAlerts = ref<PageResponse<SecurityAlert> | null>(null)
  const loginTrends = ref<LoginTrend[]>([])
  const geoDistribution = ref<GeoDistribution[]>([])
  const riskDistribution = ref<RiskDistribution>({})
  
  const isLoadingOverview = ref(false)
  const isLoadingLogins = ref(false)
  const isLoadingAlerts = ref(false)
  const isLoadingTrends = ref(false)
  const isLoadingGeoData = ref(false)
  const isLoadingRiskData = ref(false)

  // 获取概览数据
  const fetchOverview = async () => {
    try {
      isLoadingOverview.value = true
      const response = await dashboardApi.getOverview()
      overview.value = response.data
    } catch (error: any) {
      console.error('获取概览数据失败:', error)
      ElMessage.error('获取概览数据失败')
    } finally {
      isLoadingOverview.value = false
    }
  }

  // 获取最近登录记录
  const fetchRecentLogins = async (params?: QueryParams) => {
    try {
      isLoadingLogins.value = true
      const response = await dashboardApi.getRecentLogins(params)
      recentLogins.value = response.data
    } catch (error: any) {
      console.error('获取登录记录失败:', error)
      ElMessage.error('获取登录记录失败')
    } finally {
      isLoadingLogins.value = false
    }
  }

  // 获取安全警报
  const fetchSecurityAlerts = async (params?: QueryParams) => {
    try {
      isLoadingAlerts.value = true
      const response = await dashboardApi.getSecurityAlerts(params)
      securityAlerts.value = response.data
    } catch (error: any) {
      console.error('获取安全警报失败:', error)
      ElMessage.error('获取安全警报失败')
    } finally {
      isLoadingAlerts.value = false
    }
  }

  // 获取登录趋势
  const fetchLoginTrends = async (days = 7) => {
    try {
      isLoadingTrends.value = true
      const response = await dashboardApi.getLoginTrends(days)
      loginTrends.value = response.data
    } catch (error: any) {
      console.error('获取登录趋势失败:', error)
      ElMessage.error('获取登录趋势失败')
    } finally {
      isLoadingTrends.value = false
    }
  }

  // 获取地理分布数据
  const fetchGeoDistribution = async () => {
    try {
      isLoadingGeoData.value = true
      const response = await dashboardApi.getGeoDistribution()
      geoDistribution.value = response.data
    } catch (error: any) {
      console.error('获取地理分布数据失败:', error)
      ElMessage.error('获取地理分布数据失败')
    } finally {
      isLoadingGeoData.value = false
    }
  }

  // 获取风险分布数据
  const fetchRiskDistribution = async () => {
    try {
      isLoadingRiskData.value = true
      const response = await dashboardApi.getRiskDistribution()
      riskDistribution.value = response.data
    } catch (error: any) {
      console.error('获取风险分布数据失败:', error)
      ElMessage.error('获取风险分布数据失败')
    } finally {
      isLoadingRiskData.value = false
    }
  }

  // 刷新所有数据
  const refreshAllData = async () => {
    await Promise.all([
      fetchOverview(),
      fetchRecentLogins({ page: 0, size: 10 }),
      fetchSecurityAlerts({ page: 0, size: 10 }),
      fetchLoginTrends(),
      fetchGeoDistribution(),
      fetchRiskDistribution()
    ])
  }

  // 清除数据
  const clearData = () => {
    overview.value = null
    recentLogins.value = null
    securityAlerts.value = null
    loginTrends.value = []
    geoDistribution.value = []
    riskDistribution.value = {}
  }

  return {
    // 状态
    overview,
    recentLogins,
    securityAlerts,
    loginTrends,
    geoDistribution,
    riskDistribution,
    
    // 加载状态
    isLoadingOverview,
    isLoadingLogins,
    isLoadingAlerts,
    isLoadingTrends,
    isLoadingGeoData,
    isLoadingRiskData,
    
    // 方法
    fetchOverview,
    fetchRecentLogins,
    fetchSecurityAlerts,
    fetchLoginTrends,
    fetchGeoDistribution,
    fetchRiskDistribution,
    refreshAllData,
    clearData
  }
})