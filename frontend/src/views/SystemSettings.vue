<template>
  <div class="system-settings-container">
    <el-row :gutter="24">
      <!-- 安全设置 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <h4>安全设置</h4>
          </template>
          
          <el-form :model="securitySettings" label-width="140px">
            <el-form-item label="风险评分阈值">
              <el-input-number 
                v-model="securitySettings.riskThreshold" 
                :min="0" 
                :max="100" 
                :step="5"
                controls-position="right"
              />
              <div class="form-item-tip">超过此分数的登录将被标记为高风险</div>
            </el-form-item>
            
            <el-form-item label="异地登录距离阈值">
              <el-input-number 
                v-model="securitySettings.distanceThreshold" 
                :min="100" 
                :max="10000" 
                :step="100"
                controls-position="right"
              />
              <span class="unit">公里</span>
              <div class="form-item-tip">超过此距离的登录将被标记为异地登录</div>
            </el-form-item>
            
            <el-form-item label="时间窗口">
              <el-input-number 
                v-model="securitySettings.timeWindow" 
                :min="5" 
                :max="180" 
                :step="5"
                controls-position="right"
              />
              <span class="unit">分钟</span>
              <div class="form-item-tip">用于检测多地登录的时间窗口</div>
            </el-form-item>
            
            <el-form-item label="自动封禁">
              <el-switch 
                v-model="securitySettings.autoBlock" 
                active-text="启用"
                inactive-text="禁用"
              />
              <div class="form-item-tip">自动封禁高风险登录用户</div>
            </el-form-item>
            
            <el-form-item label="封禁时长">
              <el-input-number 
                v-model="securitySettings.blockDuration" 
                :min="5" 
                :max="1440" 
                :step="5"
                :disabled="!securitySettings.autoBlock"
                controls-position="right"
              />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-form>
          
          <div class="card-actions">
            <el-button type="primary" @click="saveSecuritySettings" :loading="saving">
              保存设置
            </el-button>
            <el-button @click="resetSecuritySettings">重置</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 通知设置 -->
      <el-col :span="12">
        <el-card class="settings-card">
          <template #header>
            <h4>通知设置</h4>
          </template>
          
          <el-form :model="notificationSettings" label-width="140px">
            <el-form-item label="邮件通知">
              <el-switch 
                v-model="notificationSettings.emailEnabled" 
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
            
            <el-form-item label="管理员邮箱">
              <el-select
                v-model="notificationSettings.adminEmails"
                multiple
                filterable
                allow-create
                placeholder="输入邮箱地址"
                style="width: 100%"
              >
                <el-option
                  v-for="email in predefinedEmails"
                  :key="email"
                  :label="email"
                  :value="email"
                />
              </el-select>
              <div class="form-item-tip">接收安全警报的管理员邮箱列表</div>
            </el-form-item>
            
            <el-form-item label="通知级别">
              <el-checkbox-group v-model="notificationSettings.severityLevels">
                <el-checkbox label="CRITICAL">严重</el-checkbox>
                <el-checkbox label="HIGH">高</el-checkbox>
                <el-checkbox label="MEDIUM">中</el-checkbox>
                <el-checkbox label="LOW">低</el-checkbox>
              </el-checkbox-group>
              <div class="form-item-tip">选择需要发送通知的警报级别</div>
            </el-form-item>
            
            <el-form-item label="通知频率限制">
              <el-input-number 
                v-model="notificationSettings.rateLimit" 
                :min="1" 
                :max="60" 
                controls-position="right"
              />
              <span class="unit">分钟/次</span>
              <div class="form-item-tip">相同类型警报的最小通知间隔</div>
            </el-form-item>
            
            <el-form-item label="邮件模板">
              <el-input
                v-model="notificationSettings.emailTemplate"
                type="textarea"
                :rows="4"
                placeholder="邮件通知模板..."
              />
            </el-form-item>
          </el-form>
          
          <div class="card-actions">
            <el-button type="primary" @click="saveNotificationSettings" :loading="saving">
              保存设置
            </el-button>
            <el-button @click="testNotification">测试通知</el-button>
            <el-button @click="resetNotificationSettings">重置</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统信息 -->
    <el-row :gutter="24" class="mt-20">
      <el-col :span="24">
        <el-card class="settings-card">
          <template #header>
            <h4>系统信息</h4>
          </template>
          
          <el-descriptions :column="3" border>
            <el-descriptions-item label="系统版本">
              Login Security Monitor v1.0.0
            </el-descriptions-item>
            <el-descriptions-item label="Java版本">
              OpenJDK 17.0.2
            </el-descriptions-item>
            <el-descriptions-item label="Spring Boot版本">
              3.1.0
            </el-descriptions-item>
            <el-descriptions-item label="数据库">
              MySQL 8.0.33
            </el-descriptions-item>
            <el-descriptions-item label="运行时间">
              {{ systemInfo.uptime }}
            </el-descriptions-item>
            <el-descriptions-item label="内存使用">
              {{ systemInfo.memoryUsage }}
            </el-descriptions-item>
            <el-descriptions-item label="CPU使用率">
              {{ systemInfo.cpuUsage }}
            </el-descriptions-item>
            <el-descriptions-item label="磁盘使用">
              {{ systemInfo.diskUsage }}
            </el-descriptions-item>
            <el-descriptions-item label="活跃连接数">
              {{ systemInfo.activeConnections }}
            </el-descriptions-item>
          </el-descriptions>
          
          <div class="card-actions">
            <el-button type="info" @click="refreshSystemInfo" :loading="refreshing">
              刷新信息
            </el-button>
            <el-button type="warning" @click="clearCache">清除缓存</el-button>
            <el-button type="danger" @click="restartService">重启服务</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const saving = ref(false)
const refreshing = ref(false)

// 安全设置
const securitySettings = reactive({
  riskThreshold: 70,
  distanceThreshold: 500,
  timeWindow: 60,
  autoBlock: false,
  blockDuration: 30
})

// 通知设置
const notificationSettings = reactive({
  emailEnabled: true,
  adminEmails: ['admin@example.com'],
  severityLevels: ['CRITICAL', 'HIGH'],
  rateLimit: 5,
  emailTemplate: '检测到安全警报，请及时处理...'
})

const predefinedEmails = ref([
  'admin@example.com',
  'security@example.com',
  'ops@example.com'
])

// 系统信息
const systemInfo = reactive({
  uptime: '7天 12小时 30分钟',
  memoryUsage: '2.1GB / 4GB (52%)',
  cpuUsage: '15.6%',
  diskUsage: '45.2GB / 100GB (45%)',
  activeConnections: '127'
})

const saveSecuritySettings = async () => {
  try {
    saving.value = true
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('安全设置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const resetSecuritySettings = () => {
  ElMessageBox.confirm('确认要重置安全设置吗？', '提示', {
    type: 'warning'
  }).then(() => {
    Object.assign(securitySettings, {
      riskThreshold: 70,
      distanceThreshold: 500,
      timeWindow: 60,
      autoBlock: false,
      blockDuration: 30
    })
    ElMessage.success('安全设置已重置')
  }).catch(() => {
    // 用户取消
  })
}

const saveNotificationSettings = async () => {
  try {
    saving.value = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('通知设置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const testNotification = async () => {
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('测试通知已发送')
  } catch (error) {
    ElMessage.error('发送失败')
  }
}

const resetNotificationSettings = () => {
  ElMessageBox.confirm('确认要重置通知设置吗？', '提示', {
    type: 'warning'
  }).then(() => {
    Object.assign(notificationSettings, {
      emailEnabled: true,
      adminEmails: ['admin@example.com'],
      severityLevels: ['CRITICAL', 'HIGH'],
      rateLimit: 5,
      emailTemplate: '检测到安全警报，请及时处理...'
    })
    ElMessage.success('通知设置已重置')
  }).catch(() => {
    // 用户取消
  })
}

const refreshSystemInfo = async () => {
  try {
    refreshing.value = true
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('系统信息已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

const clearCache = async () => {
  try {
    await ElMessageBox.confirm('确认要清除系统缓存吗？', '清除缓存', {
      type: 'warning'
    })
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('缓存清除成功')
  } catch (error) {
    // 用户取消或操作失败
  }
}

const restartService = async () => {
  try {
    await ElMessageBox.confirm(
      '重启服务将导致系统暂时不可用，确认要继续吗？', 
      '重启服务', 
      {
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    ElMessage.success('服务重启命令已发送')
  } catch (error) {
    // 用户取消
  }
}

onMounted(() => {
  // 获取系统配置
})
</script>

<style scoped lang="scss">
.system-settings-container {
  .settings-card {
    margin-bottom: 24px;
    
    :deep(.el-card__header) {
      padding: 16px 20px;
      background-color: #fafafa;
      
      h4 {
        margin: 0;
        color: #333;
        font-size: 16px;
        font-weight: 600;
      }
    }
    
    .form-item-tip {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
      line-height: 1.4;
    }
    
    .unit {
      margin-left: 8px;
      color: #666;
      font-size: 14px;
    }
    
    .card-actions {
      margin-top: 24px;
      padding-top: 20px;
      border-top: 1px solid #f0f0f0;
      display: flex;
      gap: 12px;
    }
  }
}

.mt-20 {
  margin-top: 20px;
}
</style>