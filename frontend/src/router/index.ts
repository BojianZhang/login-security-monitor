import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: {
        requiresAuth: false,
        title: '用户登录'
      }
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: {
        requiresAuth: true
      },
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/UnifiedDashboard.vue'),
          meta: {
            title: '统一仪表板',
            icon: 'Monitor'
          }
        },
        {
          path: 'email',
          name: 'EmailManagement',
          component: () => import('@/views/EmailManagement.vue'),
          meta: {
            title: '邮件管理',
            icon: 'Message'
          }
        },
        {
          path: 'email/messages',
          name: 'EmailMessages',
          component: () => import('@/views/EmailMessages.vue'),
          meta: {
            title: '邮件列表',
            icon: 'ChatDotSquare'
          }
        },
        {
          path: 'security',
          name: 'SecurityMonitor',
          component: () => import('@/views/SecurityMonitor.vue'),
          meta: {
            title: '安全监控',
            icon: 'Shield'
          }
        },
        {
          path: 'login-records',
          name: 'LoginRecords',
          component: () => import('@/views/LoginRecords.vue'),
          meta: {
            title: '登录记录',
            icon: 'Document'
          }
        },
        {
          path: 'security-alerts',
          name: 'SecurityAlerts',
          component: () => import('@/views/SecurityAlerts.vue'),
          meta: {
            title: '安全警报',
            icon: 'Warning'
          }
        },
        {
          path: 'user-management',
          name: 'UserManagement',
          component: () => import('@/views/UserManagement.vue'),
          meta: {
            title: '用户管理',
            icon: 'User',
            requiresAdmin: true
          }
        },
        {
          path: 'system-settings',
          name: 'SystemSettings',
          component: () => import('@/views/SystemSettings.vue'),
          meta: {
            title: '系统设置',
            icon: 'Setting',
            requiresAdmin: true
          }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue'),
      meta: {
        title: '页面未找到'
      }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 企业级安全邮件管理系统`
  }

  // 如果路由不需要认证，直接通过
  if (!to.meta.requiresAuth) {
    // 如果已经登录且访问登录页，重定向到首页
    if (to.name === 'Login' && authStore.isAuthenticated) {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
    return
  }

  // 检查是否已登录
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    next({ name: 'Login' })
    return
  }

  // 验证token有效性
  const isTokenValid = await authStore.validateToken()
  if (!isTokenValid) {
    ElMessage.error('会话已过期，请重新登录')
    next({ name: 'Login' })
    return
  }

  // 检查管理员权限
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    ElMessage.error('权限不足，需要管理员权限')
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router