import request from '@/utils/request'
import type { 
  LoginRequest, 
  LoginResponse, 
  User,
  LoginRecord,
  SecurityAlert,
  DashboardOverview,
  LoginTrend,
  GeoDistribution,
  RiskDistribution,
  PageResponse,
  QueryParams,
  ApiResponse
} from '@/types/api'

// 认证相关API
export const authApi = {
  // 用户登录
  login: (data: LoginRequest) => 
    request.post<LoginResponse>('/auth/login', data),
  
  // 验证token
  validateToken: () => 
    request.get<ApiResponse<string>>('/auth/validate')
}

// 仪表板相关API
export const dashboardApi = {
  // 获取概览数据
  getOverview: () => 
    request.get<DashboardOverview>('/dashboard/overview'),
  
  // 获取最近登录记录
  getRecentLogins: (params?: QueryParams) => 
    request.get<PageResponse<LoginRecord>>('/dashboard/recent-logins', { params }),
  
  // 获取安全警报
  getSecurityAlerts: (params?: QueryParams) => 
    request.get<PageResponse<SecurityAlert>>('/dashboard/security-alerts', { params }),
  
  // 获取登录趋势
  getLoginTrends: (days?: number) => 
    request.get<LoginTrend[]>('/dashboard/login-trends', { 
      params: { days: days || 7 } 
    }),
  
  // 获取地理分布
  getGeoDistribution: () => 
    request.get<GeoDistribution[]>('/dashboard/geo-distribution'),
  
  // 获取风险分布
  getRiskDistribution: () => 
    request.get<RiskDistribution>('/dashboard/risk-distribution')
}

// 用户相关API
export const userApi = {
  // 获取用户列表
  getUsers: (params?: QueryParams) => 
    request.get<PageResponse<User>>('/admin/users', { params }),
  
  // 获取用户详情
  getUserById: (id: number) => 
    request.get<User>(`/admin/users/${id}`),
  
  // 创建用户
  createUser: (data: Partial<User>) => 
    request.post<User>('/admin/users', data),
  
  // 更新用户
  updateUser: (id: number, data: Partial<User>) => 
    request.put<User>(`/admin/users/${id}`, data),
  
  // 删除用户
  deleteUser: (id: number) => 
    request.delete(`/admin/users/${id}`),
  
  // 激活/停用用户
  toggleUserStatus: (id: number) => 
    request.patch(`/admin/users/${id}/toggle-status`)
}

// 登录记录相关API
export const loginRecordApi = {
  // 获取登录记录
  getLoginRecords: (params?: QueryParams) => 
    request.get<PageResponse<LoginRecord>>('/admin/login-records', { params }),
  
  // 获取用户的登录记录
  getUserLoginRecords: (userId: number, params?: QueryParams) => 
    request.get<PageResponse<LoginRecord>>(`/admin/users/${userId}/login-records`, { params }),
  
  // 获取登录记录详情
  getLoginRecordById: (id: number) => 
    request.get<LoginRecord>(`/admin/login-records/${id}`)
}

// 安全警报相关API
export const alertApi = {
  // 获取安全警报
  getAlerts: (params?: QueryParams) => 
    request.get<PageResponse<SecurityAlert>>('/admin/alerts', { params }),
  
  // 获取警报详情
  getAlertById: (id: number) => 
    request.get<SecurityAlert>(`/admin/alerts/${id}`),
  
  // 确认警报
  acknowledgeAlert: (id: number) => 
    request.patch(`/admin/alerts/${id}/acknowledge`),
  
  // 解决警报
  resolveAlert: (id: number, resolution?: string) => 
    request.patch(`/admin/alerts/${id}/resolve`, { resolution }),
  
  // 标记为误报
  markAsFalsePositive: (id: number, reason?: string) => 
    request.patch(`/admin/alerts/${id}/false-positive`, { reason })
}