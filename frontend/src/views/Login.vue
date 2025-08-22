<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-form-container">
        <!-- Logo和标题 -->
        <div class="login-header">
          <div class="logo">
            <el-icon class="logo-icon">
              <Monitor />
            </el-icon>
          </div>
          <h1 class="title">登录安全监控系统</h1>
          <p class="subtitle">Login Security Monitor</p>
        </div>

        <!-- 登录表单 -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              class="login-button"
              :loading="authStore.isLoading"
              @click="handleLogin"
            >
              {{ authStore.isLoading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 登录说明 -->
        <div class="login-tips">
          <el-alert
            title="系统说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <ul class="tips-list">
                <li>本系统用于监控用户登录行为和安全状态</li>
                <li>系统会记录您的登录地理位置和设备信息</li>
                <li>如发现异常登录行为，系统会自动发送安全警报</li>
                <li>请妥善保管您的账号密码，定期修改密码</li>
              </ul>
            </template>
          </el-alert>
        </div>
      </div>

      <!-- 右侧背景区域 -->
      <div class="login-background">
        <div class="background-content">
          <div class="feature-item">
            <el-icon class="feature-icon">
              <Location />
            </el-icon>
            <h3>地理位置监控</h3>
            <p>实时监控登录地理位置，识别异地登录风险</p>
          </div>
          
          <div class="feature-item">
            <el-icon class="feature-icon">
              <Shield />
            </el-icon>
            <h3>安全风险评估</h3>
            <p>基于多维度数据进行风险评分和异常检测</p>
          </div>
          
          <div class="feature-item">
            <el-icon class="feature-icon">
              <Bell />
            </el-icon>
            <h3>实时安全警报</h3>
            <p>发现安全威胁时立即通知管理员进行处理</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6个字符', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    const isValid = await loginFormRef.value.validate()
    if (!isValid) return

    const success = await authStore.login(loginForm)
    if (success) {
      ElMessage.success('登录成功，正在跳转...')
      setTimeout(() => {
        router.push('/')
      }, 1000)
    }
  } catch (error) {
    console.error('登录失败:', error)
  }
}

// 组件挂载时检查是否已登录
onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/')
  }
})
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-wrapper {
  display: flex;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  max-width: 1000px;
  width: 100%;
  min-height: 600px;
}

.login-form-container {
  flex: 1;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 420px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  
  .logo {
    margin-bottom: 20px;
    
    .logo-icon {
      font-size: 48px;
      color: #1890ff;
    }
  }
  
  .title {
    font-size: 28px;
    color: #333;
    margin-bottom: 8px;
    font-weight: 600;
  }
  
  .subtitle {
    color: #999;
    font-size: 14px;
    margin: 0;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
  }
  
  .login-button {
    width: 100%;
    height: 48px;
    font-size: 16px;
    border-radius: 6px;
  }
}

.login-tips {
  margin-top: 30px;
  
  .tips-list {
    margin: 0;
    padding-left: 20px;
    
    li {
      margin-bottom: 8px;
      color: #666;
      font-size: 14px;
      line-height: 1.5;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

.login-background {
  flex: 1;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  padding: 60px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="1" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
    opacity: 0.3;
  }
}

.background-content {
  position: relative;
  z-index: 1;
  color: #fff;
  
  .feature-item {
    margin-bottom: 40px;
    text-align: center;
    
    .feature-icon {
      font-size: 40px;
      margin-bottom: 16px;
      color: rgba(255, 255, 255, 0.9);
    }
    
    h3 {
      font-size: 20px;
      margin-bottom: 12px;
      font-weight: 600;
    }
    
    p {
      font-size: 14px;
      line-height: 1.6;
      opacity: 0.9;
      margin: 0;
    }
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    max-width: 400px;
    
    .login-background {
      display: none;
    }
    
    .login-form-container {
      max-width: none;
      padding: 40px 20px;
    }
  }
}
</style>