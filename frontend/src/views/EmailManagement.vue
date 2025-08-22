<template>
  <div class="email-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon><Message /></el-icon>
        邮件管理
      </h2>
      <div class="header-actions">
        <el-button type="primary" @click="createAliasVisible = true">
          <el-icon><Plus /></el-icon>
          新建别名
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 邮件统计概览 -->
    <div class="stats-overview">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon messages">
                <el-icon><Message /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ emailStats.totalMessages || 0 }}</div>
                <div class="stat-label">总邮件数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon unread">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ emailStats.unreadMessages || 0 }}</div>
                <div class="stat-label">未读邮件</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon aliases">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ emailStats.totalAliases || 0 }}</div>
                <div class="stat-label">邮箱别名</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon storage">
                <el-icon><FolderOpened /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ formatStorage(emailStats.totalMessageSize) }}</div>
                <div class="stat-label">存储使用</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <el-row :gutter="20">
        <!-- 左侧：邮箱别名管理 -->
        <el-col :xs="24" :lg="12">
          <el-card class="content-card">
            <template #header>
              <div class="card-header">
                <span>
                  <el-icon><UserFilled /></el-icon>
                  邮箱别名管理
                </span>
                <el-button size="small" text type="primary" @click="createAliasVisible = true">
                  添加别名
                </el-button>
              </div>
            </template>

            <div class="aliases-section">
              <div v-if="loading.aliases" class="loading-container">
                <el-skeleton :rows="3" animated />
              </div>
              
              <div v-else-if="aliases.length === 0" class="empty-state">
                <el-empty description="暂无邮箱别名">
                  <el-button type="primary" @click="createAliasVisible = true">
                    创建第一个别名
                  </el-button>
                </el-empty>
              </div>
              
              <div v-else class="aliases-list">
                <div 
                  v-for="alias in aliases" 
                  :key="alias.id"
                  class="alias-item"
                >
                  <div class="alias-main">
                    <div class="alias-email">
                      <el-icon><User /></el-icon>
                      <strong>{{ alias.aliasEmail }}@{{ alias.domain?.domainName }}</strong>
                    </div>
                    <div class="alias-meta">
                      <el-tag 
                        :type="alias.isActive ? 'success' : 'info'" 
                        size="small"
                      >
                        {{ alias.isActive ? '已激活' : '已禁用' }}
                      </el-tag>
                      <span class="alias-date">
                        创建于: {{ formatDate(alias.createdAt) }}
                      </span>
                    </div>
                    <div v-if="alias.forwardTo" class="alias-forward">
                      <el-icon><Right /></el-icon>
                      转发到: {{ alias.forwardTo }}
                    </div>
                  </div>
                  
                  <div class="alias-actions">
                    <el-button-group>
                      <el-button 
                        size="small" 
                        :type="alias.isActive ? 'warning' : 'success'"
                        @click="toggleAliasStatus(alias)"
                        :loading="alias.updating"
                      >
                        {{ alias.isActive ? '禁用' : '启用' }}
                      </el-button>
                      <el-button 
                        size="small" 
                        type="primary"
                        @click="editAlias(alias)"
                      >
                        编辑
                      </el-button>
                      <el-button 
                        size="small" 
                        type="danger"
                        @click="deleteAlias(alias)"
                        :loading="alias.deleting"
                      >
                        删除
                      </el-button>
                    </el-button-group>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：域名和文件夹管理 -->
        <el-col :xs="24" :lg="12">
          <el-card class="content-card">
            <template #header>
              <div class="card-header">
                <span>
                  <el-icon><Globe /></el-icon>
                  域名和文件夹
                </span>
              </div>
            </template>

            <!-- 可用域名 -->
            <div class="domains-section">
              <h4 class="section-title">可用域名</h4>
              <div class="domains-list">
                <div 
                  v-for="domain in domains" 
                  :key="domain.id"
                  class="domain-item"
                >
                  <div class="domain-info">
                    <el-icon><Globe /></el-icon>
                    <strong>{{ domain.domainName }}</strong>
                    <el-tag v-if="domain.isPrimary" type="success" size="small">
                      主域名
                    </el-tag>
                  </div>
                  <div class="domain-stats">
                    {{ getUserAliasCount(domain.id) }} 个别名
                  </div>
                </div>
              </div>
            </div>

            <!-- 邮件文件夹 -->
            <div class="folders-section">
              <h4 class="section-title">邮件文件夹</h4>
              <div class="folders-list">
                <div 
                  v-for="folder in folders" 
                  :key="folder.id"
                  class="folder-item"
                  @click="openFolder(folder)"
                >
                  <div class="folder-info">
                    <el-icon>
                      <component :is="getFolderIcon(folder.folderType)" />
                    </el-icon>
                    <span class="folder-name">{{ folder.folderName }}</span>
                    <el-badge 
                      v-if="folder.unreadCount > 0"
                      :value="folder.unreadCount" 
                      class="folder-badge"
                    />
                  </div>
                  <div class="folder-count">
                    {{ folder.messageCount || 0 }} 条
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 创建别名对话框 -->
    <el-dialog 
      v-model="createAliasVisible" 
      title="创建邮箱别名" 
      width="500px"
    >
      <el-form 
        :model="aliasForm" 
        :rules="aliasFormRules" 
        ref="aliasFormRef"
        label-width="100px"
      >
        <el-form-item label="别名前缀" prop="aliasEmail">
          <el-input 
            v-model="aliasForm.aliasEmail" 
            placeholder="输入别名前缀"
            @input="validateAliasEmail"
          />
          <div class="form-hint">只能包含字母、数字、点号和短横线</div>
        </el-form-item>
        
        <el-form-item label="选择域名" prop="domainId">
          <el-select 
            v-model="aliasForm.domainId" 
            placeholder="选择域名"
            style="width: 100%"
          >
            <el-option
              v-for="domain in domains"
              :key="domain.id"
              :label="domain.domainName"
              :value="domain.id"
            />
          </el-select>
        </el-form-item>
        
        <div v-if="aliasForm.aliasEmail && aliasForm.domainId" class="preview-email">
          <strong>完整邮箱地址：</strong>
          {{ aliasForm.aliasEmail }}@{{ getSelectedDomainName() }}
        </div>
      </el-form>
      
      <template #footer>
        <el-button @click="createAliasVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitCreateAlias"
          :loading="creating"
        >
          创建别名
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑别名对话框 -->
    <el-dialog 
      v-model="editAliasVisible" 
      title="编辑邮箱别名" 
      width="500px"
    >
      <el-form 
        :model="editForm" 
        ref="editFormRef"
        label-width="100px"
      >
        <el-form-item label="邮箱地址">
          <div class="readonly-email">
            {{ currentAlias?.aliasEmail }}@{{ currentAlias?.domain?.domainName }}
          </div>
        </el-form-item>
        
        <el-form-item label="转发地址">
          <el-input 
            v-model="editForm.forwardTo" 
            placeholder="选填：设置转发地址"
            clearable
          />
          <div class="form-hint">留空表示不转发，直接接收到此别名</div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editAliasVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitEditAlias"
          :loading="updating"
        >
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Message, Plus, Refresh, Bell, UserFilled, FolderOpened,
  User, Right, Globe, Folder, Delete, EditPen, Setting,
  Inbox, Promotion, ChatDotSquare, DocumentDelete
} from '@element-plus/icons-vue'
import api from '@/utils/api'

export default {
  name: 'EmailManagement',
  components: {
    Message, Plus, Refresh, Bell, UserFilled, FolderOpened,
    User, Right, Globe, Folder, Delete, EditPen, Setting,
    Inbox, Promotion, ChatDotSquare, DocumentDelete
  },
  setup() {
    // 响应式数据
    const loading = reactive({
      aliases: false,
      domains: false,
      folders: false
    })
    
    const emailStats = ref({})
    const aliases = ref([])
    const domains = ref([])
    const folders = ref([])
    
    // 对话框状态
    const createAliasVisible = ref(false)
    const editAliasVisible = ref(false)
    const creating = ref(false)
    const updating = ref(false)
    
    // 表单数据
    const aliasForm = reactive({
      aliasEmail: '',
      domainId: null
    })
    
    const editForm = reactive({
      forwardTo: ''
    })
    
    const currentAlias = ref(null)
    const aliasFormRef = ref(null)
    const editFormRef = ref(null)
    
    // 表单验证规则
    const aliasFormRules = {
      aliasEmail: [
        { required: true, message: '请输入别名前缀', trigger: 'blur' },
        { 
          pattern: /^[a-zA-Z0-9.-]+$/, 
          message: '只能包含字母、数字、点号和短横线', 
          trigger: 'blur' 
        }
      ],
      domainId: [
        { required: true, message: '请选择域名', trigger: 'change' }
      ]
    }
    
    // 计算属性
    const getSelectedDomainName = computed(() => {
      const selectedDomain = domains.value.find(d => d.id === aliasForm.domainId)
      return selectedDomain ? selectedDomain.domainName : ''
    })
    
    // 数据获取方法
    const fetchEmailStats = async () => {
      try {
        const response = await api.get('/email/stats')
        emailStats.value = response.data
      } catch (error) {
        console.error('获取邮件统计失败:', error)
      }
    }
    
    const fetchAliases = async () => {
      try {
        loading.aliases = true
        const response = await api.get('/email/aliases')
        aliases.value = response.data
      } catch (error) {
        ElMessage.error('获取邮箱别名失败')
      } finally {
        loading.aliases = false
      }
    }
    
    const fetchDomains = async () => {
      try {
        loading.domains = true
        const response = await api.get('/email/domains')
        domains.value = response.data
      } catch (error) {
        ElMessage.error('获取域名列表失败')
      } finally {
        loading.domains = false
      }
    }
    
    const fetchFolders = async () => {
      try {
        loading.folders = true
        const response = await api.get('/email/folders')
        folders.value = response.data
      } catch (error) {
        ElMessage.error('获取文件夹列表失败')
      } finally {
        loading.folders = false
      }
    }
    
    // 操作方法
    const refreshData = async () => {
      await Promise.all([
        fetchEmailStats(),
        fetchAliases(),
        fetchDomains(),
        fetchFolders()
      ])
      ElMessage.success('数据已刷新')
    }
    
    const validateAliasEmail = () => {
      aliasForm.aliasEmail = aliasForm.aliasEmail.toLowerCase()
    }
    
    const submitCreateAlias = async () => {
      if (!aliasFormRef.value) return
      
      try {
        await aliasFormRef.value.validate()
        creating.value = true
        
        const response = await api.post('/email/aliases', aliasForm)
        
        if (response.data.success) {
          ElMessage.success('邮箱别名创建成功')
          createAliasVisible.value = false
          
          // 重置表单
          Object.assign(aliasForm, {
            aliasEmail: '',
            domainId: null
          })
          
          // 刷新别名列表
          await fetchAliases()
          await fetchEmailStats()
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '创建别名失败')
      } finally {
        creating.value = false
      }
    }
    
    const toggleAliasStatus = async (alias) => {
      try {
        alias.updating = true
        
        const response = await api.put(`/email/aliases/${alias.id}/toggle`)
        
        if (response.data.success) {
          alias.isActive = response.data.alias.isActive
          ElMessage.success(response.data.message)
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      } finally {
        alias.updating = false
      }
    }
    
    const editAlias = (alias) => {
      currentAlias.value = alias
      editForm.forwardTo = alias.forwardTo || ''
      editAliasVisible.value = true
    }
    
    const submitEditAlias = async () => {
      try {
        updating.value = true
        
        const response = await api.put(
          `/email/aliases/${currentAlias.value.id}/forward`,
          editForm
        )
        
        if (response.data.success) {
          ElMessage.success('别名设置已更新')
          editAliasVisible.value = false
          
          // 更新本地数据
          const index = aliases.value.findIndex(a => a.id === currentAlias.value.id)
          if (index !== -1) {
            aliases.value[index].forwardTo = editForm.forwardTo
          }
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '更新失败')
      } finally {
        updating.value = false
      }
    }
    
    const deleteAlias = async (alias) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除邮箱别名 "${alias.aliasEmail}@${alias.domain.domainName}" 吗？`,
          '确认删除',
          {
            type: 'warning'
          }
        )
        
        alias.deleting = true
        
        const response = await api.delete(`/email/aliases/${alias.id}`)
        
        if (response.data.success) {
          ElMessage.success('邮箱别名已删除')
          await fetchAliases()
          await fetchEmailStats()
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(error.response?.data?.message || '删除失败')
        }
      } finally {
        alias.deleting = false
      }
    }
    
    // 工具方法
    const formatStorage = (bytes) => {
      if (!bytes) return '0 B'
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(bytes) / Math.log(1024))
      return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${sizes[i]}`
    }
    
    const formatDate = (dateString) => {
      return new Date(dateString).toLocaleDateString('zh-CN')
    }
    
    const getUserAliasCount = (domainId) => {
      return aliases.value.filter(alias => alias.domain?.id === domainId).length
    }
    
    const getFolderIcon = (folderType) => {
      const iconMap = {
        'INBOX': 'Inbox',
        'SENT': 'Promotion',
        'DRAFT': 'EditPen',
        'TRASH': 'Delete',
        'SPAM': 'ChatDotSquare',
        'CUSTOM': 'Folder'
      }
      return iconMap[folderType] || 'Folder'
    }
    
    const openFolder = (folder) => {
      // TODO: 导航到邮件列表页面
      console.log('打开文件夹:', folder)
    }
    
    // 初始化
    onMounted(() => {
      refreshData()
    })
    
    return {
      loading,
      emailStats,
      aliases,
      domains,
      folders,
      createAliasVisible,
      editAliasVisible,
      creating,
      updating,
      aliasForm,
      editForm,
      currentAlias,
      aliasFormRef,
      editFormRef,
      aliasFormRules,
      getSelectedDomainName,
      refreshData,
      validateAliasEmail,
      submitCreateAlias,
      toggleAliasStatus,
      editAlias,
      submitEditAlias,
      deleteAlias,
      formatStorage,
      formatDate,
      getUserAliasCount,
      getFolderIcon,
      openFolder
    }
  }
}
</script>

<style scoped>
.email-management {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stats-overview {
  margin-bottom: 24px;
}

.stat-card {
  height: 100px;
  cursor: default;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 16px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 20px;
  color: white;
}

.stat-icon.messages {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.unread {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.aliases {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.storage {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.content-card {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-header .el-icon {
  margin-right: 8px;
}

.loading-container {
  padding: 20px;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

.aliases-list {
  max-height: 400px;
  overflow-y: auto;
}

.alias-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.alias-main {
  flex: 1;
  margin-right: 16px;
}

.alias-email {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  margin-bottom: 8px;
}

.alias-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.alias-date {
  font-size: 12px;
  color: #909399;
}

.alias-forward {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
}

.domains-section, .folders-section {
  margin-bottom: 24px;
}

.domain-item, .folder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.domain-item:hover, .folder-item:hover {
  background: #f9f9f9;
}

.folder-item {
  cursor: pointer;
}

.domain-info, .folder-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.folder-badge {
  margin-left: 8px;
}

.domain-stats, .folder-count {
  font-size: 12px;
  color: #909399;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.preview-email {
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  color: #1e40af;
  font-size: 14px;
  margin-top: 16px;
}

.readonly-email {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

@media (max-width: 768px) {
  .email-management {
    padding: 12px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .alias-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .alias-actions {
    width: 100%;
  }
  
  .alias-actions .el-button-group {
    width: 100%;
  }
  
  .alias-actions .el-button {
    flex: 1;
  }
}
</style>