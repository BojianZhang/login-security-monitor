// 全局错误处理和工具函数
import { ElMessage, ElNotification } from 'element-plus'
import { AxiosError } from 'axios'

/**
 * 统一错误处理工具
 */
export class ErrorHandler {
  /**
   * 处理API错误
   */
  static handleApiError(error: AxiosError | Error, defaultMessage = '操作失败') {
    let message = defaultMessage
    let details = ''

    if (error instanceof AxiosError) {
      // Axios错误
      if (error.response) {
        const { status, data } = error.response
        
        switch (status) {
          case 400:
            message = data?.message || '请求参数错误'
            break
          case 401:
            message = '请重新登录'
            // 可以在这里处理重定向到登录页
            break
          case 403:
            message = '没有权限执行此操作'
            break
          case 404:
            message = '请求的资源不存在'
            break
          case 422:
            message = '数据验证失败'
            if (data?.errors) {
              details = Object.values(data.errors).flat().join(', ')
            }
            break
          case 500:
            message = '服务器内部错误'
            break
          case 503:
            message = '服务暂时不可用'
            break
          default:
            message = data?.message || `请求失败 (${status})`
        }
      } else if (error.request) {
        message = '网络连接失败，请检查网络状态'
      }
    }

    // 显示错误消息
    if (details) {
      ElNotification({
        title: message,
        message: details,
        type: 'error',
        duration: 5000
      })
    } else {
      ElMessage.error(message)
    }

    // 记录错误日志
    console.error('[ErrorHandler]', {
      message,
      details,
      originalError: error
    })

    return { message, details }
  }

  /**
   * 处理表单验证错误
   */
  static handleValidationError(errors: Record<string, string[]>) {
    const firstError = Object.values(errors)[0]?.[0]
    if (firstError) {
      ElMessage.error(firstError)
    }
  }

  /**
   * 显示成功消息
   */
  static showSuccess(message: string) {
    ElMessage.success(message)
  }

  /**
   * 显示警告消息
   */
  static showWarning(message: string) {
    ElMessage.warning(message)
  }

  /**
   * 显示信息消息
   */
  static showInfo(message: string) {
    ElMessage.info(message)
  }
}

/**
 * 加载状态管理工具
 */
export class LoadingManager {
  private static loadingStates = new Map<string, boolean>()

  /**
   * 设置加载状态
   */
  static setLoading(key: string, loading: boolean) {
    this.loadingStates.set(key, loading)
  }

  /**
   * 获取加载状态
   */
  static isLoading(key: string): boolean {
    return this.loadingStates.get(key) || false
  }

  /**
   * 清除所有加载状态
   */
  static clearAll() {
    this.loadingStates.clear()
  }

  /**
   * 异步操作包装器
   */
  static async withLoading<T>(
    key: string, 
    operation: () => Promise<T>,
    options: {
      successMessage?: string
      errorMessage?: string
      showError?: boolean
    } = {}
  ): Promise<T | null> {
    try {
      this.setLoading(key, true)
      const result = await operation()
      
      if (options.successMessage) {
        ErrorHandler.showSuccess(options.successMessage)
      }
      
      return result
    } catch (error) {
      if (options.showError !== false) {
        ErrorHandler.handleApiError(
          error as Error, 
          options.errorMessage
        )
      }
      return null
    } finally {
      this.setLoading(key, false)
    }
  }
}

/**
 * 数据验证工具
 */
export class ValidationUtils {
  /**
   * 验证邮箱格式
   */
  static isValidEmail(email: string): boolean {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    return emailRegex.test(email)
  }

  /**
   * 验证别名格式
   */
  static isValidAlias(alias: string): boolean {
    const aliasRegex = /^[a-zA-Z0-9._-]+$/
    return aliasRegex.test(alias) && alias.length >= 1 && alias.length <= 64
  }

  /**
   * 验证文件夹名称
   */
  static isValidFolderName(name: string): boolean {
    const folderRegex = /^[\u4e00-\u9fa5a-zA-Z0-9._\-\s]+$/
    return folderRegex.test(name) && name.length >= 1 && name.length <= 100
  }

  /**
   * 清理输入文本
   */
  static sanitizeInput(input: string): string {
    return input.trim().replace(/\s+/g, ' ')
  }
}

/**
 * 格式化工具
 */
export class FormatUtils {
  /**
   * 格式化文件大小
   */
  static formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B'
    
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
  }

  /**
   * 格式化日期
   */
  static formatDate(date: string | Date, format = 'YYYY-MM-DD HH:mm:ss'): string {
    const d = new Date(date)
    
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const minute = String(d.getMinutes()).padStart(2, '0')
    const second = String(d.getSeconds()).padStart(2, '0')
    
    return format
      .replace('YYYY', String(year))
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hour)
      .replace('mm', minute)
      .replace('ss', second)
  }

  /**
   * 格式化相对时间
   */
  static formatRelativeTime(date: string | Date): string {
    const now = new Date().getTime()
    const past = new Date(date).getTime()
    const diff = now - past

    const minute = 60 * 1000
    const hour = 60 * minute
    const day = 24 * hour
    const week = 7 * day

    if (diff < minute) {
      return '刚刚'
    } else if (diff < hour) {
      return `${Math.floor(diff / minute)} 分钟前`
    } else if (diff < day) {
      return `${Math.floor(diff / hour)} 小时前`
    } else if (diff < week) {
      return `${Math.floor(diff / day)} 天前`
    } else {
      return this.formatDate(date, 'MM-DD HH:mm')
    }
  }
}

export default {
  ErrorHandler,
  LoadingManager,
  ValidationUtils,
  FormatUtils
}