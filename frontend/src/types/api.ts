// 用户相关类型
export interface User {
  id: number
  username: string
  email: string
  fullName: string
  isAdmin: boolean
  lastLogin: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// 登录请求和响应
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  success: boolean
  message: string
  token?: string
  user?: {
    id: number
    username: string
    email: string
    fullName: string
    isAdmin: boolean
    lastLogin: string
  }
  loginInfo?: {
    loginRecordId: number
    loginTime: string
  }
}

// 登录记录
export interface LoginRecord {
  id: number
  user: User
  ipAddress: string
  userAgent: string
  loginTime: string
  loginStatus: 'SUCCESS' | 'FAILED'
  browser?: string
  os?: string
  deviceType?: string
  country?: string
  region?: string
  city?: string
  latitude?: number
  longitude?: number
  timezone?: string
  isp?: string
  riskScore: number
  isSuspicious: boolean
  sessionId: string
  createdAt: string
  updatedAt: string
}

// 安全警报
export interface SecurityAlert {
  id: number
  user: User
  loginRecord: LoginRecord
  alertType: 'LOCATION_ANOMALY' | 'MULTIPLE_LOCATIONS' | 'HIGH_RISK_SCORE' | 'SUSPICIOUS_DEVICE' | 'BRUTE_FORCE'
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED' | 'FALSE_POSITIVE'
  message: string
  details?: string
  createdAt: string
  updatedAt: string
  resolvedAt?: string
  resolvedBy?: User
}

// 仪表板概览数据
export interface DashboardOverview {
  totalUsers: number
  activeUsers: number
  loginsLast24h: number
  totalAlerts: number
  openAlerts: number
  criticalAlerts: number
  suspiciousLogins: number
}

// 登录趋势
export interface LoginTrend {
  date: string
  successfulLogins: number
  failedLogins: number
  suspiciousLogins: number
}

// 地理分布
export interface GeoDistribution {
  country: string
  region: string
  city: string
  loginCount: number
  latitude: number
  longitude: number
}

// 风险分布
export interface RiskDistribution {
  [key: string]: number
}

// API响应类型
export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
}

// 查询参数
export interface QueryParams {
  page?: number
  size?: number
  sort?: string
  direction?: 'ASC' | 'DESC'
  [key: string]: any
}