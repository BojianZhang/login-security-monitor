<template>
  <div class="security-alerts-container">
    <el-card>
      <template #header>
        <div class="header-content">
          <h3>安全警报</h3>
          <div class="header-actions">
            <el-select v-model="statusFilter" placeholder="筛选状态" clearable>
              <el-option label="待处理" value="OPEN" />
              <el-option label="已确认" value="ACKNOWLEDGED" />
              <el-option label="已解决" value="RESOLVED" />
              <el-option label="误报" value="FALSE_POSITIVE" />
            </el-select>
            <el-button type="primary" @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        :loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="user.username" label="用户" width="120" />
        
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
        
        <el-table-column prop="message" label="警报消息" min-width="200" />
        
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getStatusTagType(row.status)"
              size="small"
            >
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'OPEN'"
              type="warning" 
              size="small" 
              @click="acknowledgeAlert(row)"
            >
              确认
            </el-button>
            <el-button 
              v-if="['OPEN', 'ACKNOWLEDGED'].includes(row.status)"
              type="success" 
              size="small" 
              @click="resolveAlert(row)"
            >
              解决
            </el-button>
            <el-button 
              v-if="row.status === 'OPEN'"
              type="info" 
              size="small" 
              @click="markFalsePositive(row)"
            >
              误报
            </el-button>
            <el-button type="text" size="small" @click="viewDetails(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="batch-actions" v-if="selectedItems.length > 0">
          <span class="selected-info">已选择 {{ selectedItems.length }} 项</span>
          <el-button type="warning" size="small" @click="batchAcknowledge">
            批量确认
          </el-button>
          <el-button type="success" size="small" @click="batchResolve">
            批量解决
          </el-button>
        </div>
        
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

// 模拟数据
const loading = ref(false)
const statusFilter = ref('')
const tableData = ref([])
const selectedItems = ref([])
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const handleRefresh = () => {
  ElMessage.success('数据刷新成功')
}

const handleSelectionChange = (selection: any[]) => {
  selectedItems.value = selection
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
}

const acknowledgeAlert = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认要确认此警报吗？', '提示', {
      type: 'warning'
    })
    ElMessage.success('警报已确认')
  } catch (error) {
    // 用户取消
  }
}

const resolveAlert = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认要解决此警报吗？', '提示', {
      type: 'info'
    })
    ElMessage.success('警报已解决')
  } catch (error) {
    // 用户取消
  }
}

const markFalsePositive = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认要标记为误报吗？', '提示', {
      type: 'info'
    })
    ElMessage.success('警报已标记为误报')
  } catch (error) {
    // 用户取消
  }
}

const viewDetails = (row: any) => {
  ElMessage.info(`查看警报 ${row.id} 的详细信息`)
}

const batchAcknowledge = async () => {
  try {
    await ElMessageBox.confirm(`确认要批量确认 ${selectedItems.value.length} 个警报吗？`, '批量确认', {
      type: 'warning'
    })
    ElMessage.success(`已批量确认 ${selectedItems.value.length} 个警报`)
    selectedItems.value = []
  } catch (error) {
    // 用户取消
  }
}

const batchResolve = async () => {
  try {
    await ElMessageBox.confirm(`确认要批量解决 ${selectedItems.value.length} 个警报吗？`, '批量解决', {
      type: 'info'
    })
    ElMessage.success(`已批量解决 ${selectedItems.value.length} 个警报`)
    selectedItems.value = []
  } catch (error) {
    // 用户取消
  }
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

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  // 实际项目中这里会调用API获取数据
})
</script>

<style scoped lang="scss">
.security-alerts-container {
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h3 {
      margin: 0;
      color: #333;
    }
    
    .header-actions {
      display: flex;
      gap: 16px;
      align-items: center;
    }
  }
  
  .table-footer {
    margin-top: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .batch-actions {
      display: flex;
      align-items: center;
      gap: 16px;
      
      .selected-info {
        color: #666;
        font-size: 14px;
      }
    }
  }
}
</style>