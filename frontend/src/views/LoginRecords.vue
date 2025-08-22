<template>
  <div class="login-records-container">
    <el-card>
      <template #header>
        <div class="header-content">
          <h3>登录记录</h3>
          <div class="header-actions">
            <el-input
              v-model="searchQuery"
              placeholder="搜索用户名或IP地址"
              clearable
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
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
      >
        <el-table-column prop="user.username" label="用户名" width="120" />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column label="登录时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="地理位置" width="150">
          <template #default="{ row }">
            {{ formatLocation(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="browser" label="浏览器" width="120" />
        <el-table-column prop="os" label="操作系统" width="120" />
        <el-table-column label="登录状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.loginStatus === 'SUCCESS' ? 'success' : 'danger'"
              size="small"
            >
              {{ row.loginStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getRiskTagType(row.riskScore)"
              size="small"
            >
              {{ getRiskLevel(row.riskScore) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="viewDetails(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
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
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

// 模拟数据
const loading = ref(false)
const searchQuery = ref('')
const tableData = ref([])
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const handleRefresh = () => {
  ElMessage.success('数据刷新成功')
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  ElMessage.info(`每页显示 ${size} 条记录`)
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
}

const viewDetails = (row: any) => {
  ElMessage.info(`查看用户 ${row.user?.username} 的登录详情`)
}

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatLocation = (record: any) => {
  const parts = []
  if (record.country) parts.push(record.country)
  if (record.city) parts.push(record.city)
  return parts.join(', ') || '未知'
}

const getRiskLevel = (score: number) => {
  if (score >= 70) return '高'
  if (score >= 40) return '中'
  return '低'
}

const getRiskTagType = (score: number) => {
  if (score >= 70) return 'danger'
  if (score >= 40) return 'warning'
  return 'success'
}

onMounted(() => {
  // 实际项目中这里会调用API获取数据
})
</script>

<style scoped lang="scss">
.login-records-container {
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
      
      .search-input {
        width: 300px;
      }
    }
  }
  
  .pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: center;
  }
}
</style>