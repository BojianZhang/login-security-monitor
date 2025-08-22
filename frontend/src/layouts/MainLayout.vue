<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '250px'" class="sidebar">
      <div class="logo-container">
        <el-icon v-if="isCollapsed" class="logo-icon">
          <Monitor />
        </el-icon>
        <div v-else class="logo-text">
          <el-icon class="logo-icon">
            <Monitor />
          </el-icon>
          <span>Security Monitor</span>
        </div>
      </div>
      
      <el-menu
        :default-active="$route.name"
        class="sidebar-menu"
        :collapse="isCollapsed"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
      >
        <el-menu-item index="Dashboard" route="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表板</template>
        </el-menu-item>
        
        <el-menu-item index="LoginRecords" route="/login-records">
          <el-icon><Document /></el-icon>
          <template #title>登录记录</template>
        </el-menu-item>
        
        <el-menu-item index="SecurityAlerts" route="/security-alerts">
          <el-icon><Warning /></el-icon>
          <template #title>安全警报</template>
        </el-menu-item>
        
        <el-menu-item 
          v-if="authStore.isAdmin" 
          index="UserManagement" 
          route="/user-management"
        >
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        
        <el-menu-item 
          v-if="authStore.isAdmin" 
          index="SystemSettings" 
          route="/system-settings"
        >
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 头部 -->
      <el-header class="header">
        <div class="header-left">
          <el-button 
            type="text" 
            @click="toggleCollapse"
            class="collapse-btn"
          >
            <el-icon>
              <Expand v-if="isCollapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta?.title">
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <!-- 通知图标 -->
          <el-badge :value="alertCount" :max="99" class="notification-badge">
            <el-button type="text" @click="showNotifications">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          
          <!-- 用户菜单 -->
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userInitials }}
              </el-avatar>
              <span v-if="!isCollapsed" class="username">
                {{ authStore.user?.fullName || authStore.user?.username }}
              </span>
            </div>
            
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主体内容 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
    
    <!-- 通知抽屉 -->
    <el-drawer
      v-model="notificationDrawer"
      title="最新通知"
      direction="rtl"
      size="400px"
    >
      <div class="notification-content">
        <el-empty 
          v-if="notifications.length === 0" 
          description="暂无新通知" 
        />
        
        <div v-else class="notification-list">
          <div 
            v-for="notification in notifications" 
            :key="notification.id"
            class="notification-item"
          >
            <div class="notification-header">
              <el-tag 
                :type="getAlertTypeColor(notification.severity)"
                size="small"
              >
                {{ notification.severity }}
              </el-tag>
              <span class="notification-time">
                {{ formatTime(notification.createdAt) }}
              </span>
            </div>
            <div class="notification-content">
              {{ notification.message }}
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDashboardStore } from '@/stores/dashboard'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { SecurityAlert } from '@/types/api'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const dashboardStore = useDashboardStore()

// 响应式数据
const isCollapsed = ref(false)
const notificationDrawer = ref(false)
const notifications = ref<SecurityAlert[]>([])

// 计算属性
const currentRoute = computed(() => route)
const userInitials = computed(() => {
  const user = authStore.user
  if (user?.fullName) {
    return user.fullName.substring(0, 1).toUpperCase()
  }
  return user?.username?.substring(0, 1).toUpperCase() || 'U'
})

const alertCount = computed(() => {
  return dashboardStore.overview?.openAlerts || 0
})

// 方法
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const showNotifications = () => {
  notificationDrawer.value = true
  loadNotifications()
}

const loadNotifications = async () => {
  try {
    await dashboardStore.fetchSecurityAlerts({ page: 0, size: 10, status: 'OPEN' })
    if (dashboardStore.securityAlerts) {
      notifications.value = dashboardStore.securityAlerts.content
    }
  } catch (error) {
    console.error('加载通知失败:', error)
  }
}

const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人资料功能开发中')
      break
    case 'settings':
      ElMessage.info('设置功能开发中')
      break
    case 'logout':
      await handleLogout()
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    authStore.logout()
    dashboardStore.clearData()
    await router.push('/login')
  } catch (error) {
    // 用户取消了操作
  }
}

const getAlertTypeColor = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return 'danger'
    case 'HIGH': return 'warning'
    case 'MEDIUM': return 'info'
    case 'LOW': return 'success'
    default: return 'info'
  }
}

const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

// 生命周期
onMounted(() => {
  dashboardStore.fetchOverview()
})
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  transition: all 0.3s;
  
  .logo-container {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(255, 255, 255, 0.05);
    
    .logo-icon {
      color: #1890ff;
      font-size: 24px;
    }
    
    .logo-text {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #fff;
      font-weight: 600;
      font-size: 16px;
    }
  }
  
  .sidebar-menu {
    border: none;
    
    .el-menu-item {
      height: 50px;
      line-height: 50px;
      
      &:hover {
        background-color: rgba(24, 144, 255, 0.1);
      }
    }
  }
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .collapse-btn {
      font-size: 16px;
      color: #666;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .notification-badge {
      .el-button {
        font-size: 16px;
        color: #666;
        
        &:hover {
          color: #1890ff;
        }
      }
    }
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background-color 0.3s;
      
      &:hover {
        background-color: #f5f5f5;
      }
      
      .username {
        font-weight: 500;
        color: #333;
      }
    }
  }
}

.main-content {
  background-color: #f0f2f5;
  padding: 24px;
  overflow: auto;
}

.notification-content {
  .notification-list {
    .notification-item {
      padding: 16px;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .notification-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        .notification-time {
          color: #999;
          font-size: 12px;
        }
      }
      
      .notification-content {
        color: #666;
        line-height: 1.5;
      }
    }
  }
}
</style>