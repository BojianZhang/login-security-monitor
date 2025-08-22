import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import Cookies from 'js-cookie'
import { authApi } from '@/services/api'
import type { LoginRequest, User } from '@/types/api'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isLoading = ref(false)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.isAdmin || false)

  // 初始化认证状态
  const initializeAuth = () => {
    const savedToken = Cookies.get('token')
    const savedUser = localStorage.getItem('user')
    
    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        user.value = JSON.parse(savedUser)
      } catch (error) {
        console.error('解析用户信息失败:', error)
        logout()
      }
    }
  }

  // 登录
  const login = async (loginData: LoginRequest) => {
    try {
      isLoading.value = true
      const response = await authApi.login(loginData)
      
      if (response.data.success && response.data.token && response.data.user) {
        token.value = response.data.token
        user.value = {
          ...response.data.user,
          isActive: true,
          createdAt: '',
          updatedAt: ''
        } as User
        
        // 保存到本地存储
        Cookies.set('token', token.value, { expires: 1 }) // 1天过期
        localStorage.setItem('user', JSON.stringify(user.value))
        
        ElMessage.success('登录成功')
        return true
      } else {
        ElMessage.error(response.data.message || '登录失败')
        return false
      }
    } catch (error: any) {
      console.error('登录错误:', error)
      const message = error.response?.data?.message || '登录失败，请重试'
      ElMessage.error(message)
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 登出
  const logout = () => {
    user.value = null
    token.value = null
    
    // 清除本地存储
    Cookies.remove('token')
    localStorage.removeItem('user')
    
    ElMessage.success('已成功登出')
  }

  // 验证token有效性
  const validateToken = async () => {
    if (!token.value) {
      return false
    }

    try {
      const response = await authApi.validateToken()
      return response.data.success
    } catch (error) {
      console.error('Token验证失败:', error)
      logout()
      return false
    }
  }

  // 更新用户信息
  const updateUser = (newUserData: Partial<User>) => {
    if (user.value) {
      user.value = { ...user.value, ...newUserData }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  return {
    // 状态
    user,
    token,
    isLoading,
    
    // 计算属性
    isAuthenticated,
    isAdmin,
    
    // 方法
    initializeAuth,
    login,
    logout,
    validateToken,
    updateUser
  }
})