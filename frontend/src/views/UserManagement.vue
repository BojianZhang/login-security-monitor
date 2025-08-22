<template>
  <div class="user-management-container">
    <el-card>
      <template #header>
        <div class="header-content">
          <h3>用户管理</h3>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              添加用户
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        :loading="loading"
        row-key="id"
      >
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="fullName" label="姓名" width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isAdmin ? 'danger' : 'primary'" size="small">
              {{ row.isAdmin ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
              {{ row.isActive ? '活跃' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">
            {{ row.lastLogin ? formatTime(row.lastLogin) : '从未登录' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="editUser(row)">
              编辑
            </el-button>
            <el-button 
              type="text" 
              size="small" 
              @click="toggleUserStatus(row)"
            >
              {{ row.isActive ? '禁用' : '启用' }}
            </el-button>
            <el-button type="text" size="small" @click="resetPassword(row)">
              重置密码
            </el-button>
            <el-button 
              type="text" 
              size="small" 
              class="danger-text"
              @click="deleteUser(row)"
            >
              删除
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

    <!-- 创建/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '创建用户'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="userForm"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" type="email" />
        </el-form-item>
        <el-form-item label="姓名" prop="fullName">
          <el-input v-model="userForm.fullName" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox v-model="userForm.isAdmin">管理员权限</el-checkbox>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="userForm.isActive" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref([])
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const userForm = reactive({
  username: '',
  email: '',
  fullName: '',
  password: '',
  isAdmin: false,
  isActive: true
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const showCreateDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

const editUser = (user: any) => {
  isEdit.value = true
  dialogVisible.value = true
  Object.assign(userForm, user)
}

const resetForm = () => {
  Object.assign(userForm, {
    username: '',
    email: '',
    fullName: '',
    password: '',
    isAdmin: false,
    isActive: true
  })
}

const handleSubmit = async () => {
  try {
    submitLoading.value = true
    // 这里调用API
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success(isEdit.value ? '用户更新成功' : '用户创建成功')
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const toggleUserStatus = async (user: any) => {
  try {
    await ElMessageBox.confirm(
      `确认要${user.isActive ? '禁用' : '启用'}用户 ${user.username} 吗？`,
      '提示',
      { type: 'warning' }
    )
    ElMessage.success(`用户已${user.isActive ? '禁用' : '启用'}`)
  } catch (error) {
    // 用户取消
  }
}

const resetPassword = async (user: any) => {
  try {
    await ElMessageBox.confirm(`确认要重置用户 ${user.username} 的密码吗？`, '重置密码', {
      type: 'warning'
    })
    ElMessage.success('密码重置成功，新密码已发送到用户邮箱')
  } catch (error) {
    // 用户取消
  }
}

const deleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(`确认要删除用户 ${user.username} 吗？此操作不可撤销！`, '删除用户', {
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    })
    ElMessage.success('用户删除成功')
  } catch (error) {
    // 用户取消
  }
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
}

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  // 实际项目中这里会调用API获取用户列表
})
</script>

<style scoped lang="scss">
.user-management-container {
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    h3 {
      margin: 0;
      color: #333;
    }
  }
  
  .pagination-container {
    margin-top: 24px;
    display: flex;
    justify-content: center;
  }
  
  .danger-text {
    color: #f56c6c !important;
    
    &:hover {
      color: #f56c6c !important;
    }
  }
}
</style>