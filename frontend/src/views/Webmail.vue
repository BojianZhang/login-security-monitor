<template>
  <div class="webmail-container">
    <!-- 顶部工具栏 -->
    <div class="webmail-toolbar">
      <div class="toolbar-left">
        <el-button 
          type="primary" 
          icon="Edit"
          @click="showCompose = true">
          写邮件
        </el-button>
        <el-button 
          icon="Refresh"
          @click="refreshMessages">
          刷新
        </el-button>
        <el-button 
          icon="Delete"
          :disabled="selectedMessages.length === 0"
          @click="deleteSelectedMessages">
          删除
        </el-button>
        <el-button 
          icon="Flag"
          :disabled="selectedMessages.length === 0"
          @click="toggleStarSelected">
          星标
        </el-button>
      </div>
      
      <div class="toolbar-right">
        <el-select v-model="currentFolder" @change="switchFolder" style="width: 150px;">
          <el-option
            v-for="folder in folders"
            :key="folder.id"
            :label="folder.folderName"
            :value="folder.id">
            <el-icon style="margin-right: 8px;">
              <Inbox v-if="folder.folderType === 'INBOX'" />
              <Message v-else-if="folder.folderType === 'SENT'" />
              <Document v-else-if="folder.folderType === 'DRAFTS'" />
              <Delete v-else-if="folder.folderType === 'TRASH'" />
              <Warning v-else-if="folder.folderType === 'SPAM'" />
              <Folder v-else />
            </el-icon>
            {{ folder.folderName }}
            <span v-if="folder.unreadCount > 0" class="unread-badge">
              ({{ folder.unreadCount }})
            </span>
          </el-option>
        </el-select>
        
        <el-input
          v-model="searchQuery"
          placeholder="搜索邮件..."
          style="width: 200px; margin-left: 10px;"
          @keyup.enter="searchMessages">
          <template #append>
            <el-button icon="Search" @click="searchMessages" />
          </template>
        </el-input>
      </div>
    </div>

    <div class="webmail-layout">
      <!-- 左侧文件夹列表 -->
      <div class="folder-panel">
        <div class="folder-header">
          <h3>文件夹</h3>
          <el-button 
            size="small" 
            icon="Plus"
            @click="showCreateFolder = true">
            新建
          </el-button>
        </div>
        
        <div class="folder-list">
          <div 
            v-for="folder in folders" 
            :key="folder.id"
            class="folder-item"
            :class="{ active: currentFolder === folder.id }"
            @click="switchFolder(folder.id)">
            <el-icon class="folder-icon">
              <Inbox v-if="folder.folderType === 'INBOX'" />
              <Message v-else-if="folder.folderType === 'SENT'" />
              <Document v-else-if="folder.folderType === 'DRAFTS'" />
              <Delete v-else-if="folder.folderType === 'TRASH'" />
              <Warning v-else-if="folder.folderType === 'SPAM'" />
              <Folder v-else />
            </el-icon>
            <span class="folder-name">{{ folder.folderName }}</span>
            <span v-if="folder.unreadCount > 0" class="unread-count">
              {{ folder.unreadCount }}
            </span>
          </div>
        </div>

        <!-- 存储使用情况 -->
        <div class="storage-info">
          <div class="storage-label">存储使用</div>
          <el-progress 
            :percentage="storagePercentage" 
            :status="storagePercentage > 90 ? 'exception' : 'success'"
            :stroke-width="6" />
          <div class="storage-text">
            {{ formatFileSize(storageUsed) }} / {{ formatFileSize(storageQuota) }}
          </div>
        </div>
      </div>

      <!-- 中间邮件列表 -->
      <div class="message-panel">
        <div class="message-header">
          <div class="message-actions">
            <el-checkbox 
              v-model="selectAll"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll">
              全选
            </el-checkbox>
            
            <el-dropdown @command="handleBatchAction">
              <el-button size="small">
                批量操作 <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="markRead">标记为已读</el-dropdown-item>
                  <el-dropdown-item command="markUnread">标记为未读</el-dropdown-item>
                  <el-dropdown-item command="addStar">添加星标</el-dropdown-item>
                  <el-dropdown-item command="removeStar">移除星标</el-dropdown-item>
                  <el-dropdown-item command="moveToSpam">移至垃圾箱</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          
          <div class="message-count">
            {{ messages.length }} 封邮件
          </div>
        </div>

        <div class="message-list" v-loading="loading">
          <div 
            v-for="message in messages" 
            :key="message.id"
            class="message-item"
            :class="{ 
              selected: selectedMessages.includes(message.id),
              unread: !message.isRead,
              starred: message.isStarred
            }"
            @click="selectMessage(message)">
            
            <div class="message-checkbox">
              <el-checkbox 
                :model-value="selectedMessages.includes(message.id)"
                @change="toggleMessageSelection(message.id)"
                @click.stop />
            </div>
            
            <div class="message-star" @click.stop="toggleStar(message)">
              <el-icon v-if="message.isStarred" class="starred">
                <StarFilled />
              </el-icon>
              <el-icon v-else class="unstarred">
                <Star />
              </el-icon>
            </div>
            
            <div class="message-content">
              <div class="message-header-row">
                <div class="message-from">{{ getSenderName(message.fromAddress) }}</div>
                <div class="message-time">{{ formatTime(message.receivedAt) }}</div>
              </div>
              
              <div class="message-subject">
                {{ message.subject || '(无主题)' }}
                <el-icon v-if="hasAttachments(message)" class="attachment-icon">
                  <Paperclip />
                </el-icon>
              </div>
              
              <div class="message-preview">
                {{ getMessagePreview(message) }}
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="messages.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无邮件" />
          </div>
        </div>

        <!-- 分页 -->
        <div class="message-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[20, 50, 100]"
            :total="totalMessages"
            layout="total, sizes, prev, pager, next"
            @current-change="loadMessages"
            @size-change="loadMessages" />
        </div>
      </div>

      <!-- 右侧邮件详情 -->
      <div class="detail-panel" v-if="currentMessage">
        <div class="detail-header">
          <div class="detail-actions">
            <el-button size="small" @click="replyMessage">回复</el-button>
            <el-button size="small" @click="replyAllMessage">回复全部</el-button>
            <el-button size="small" @click="forwardMessage">转发</el-button>
            <el-button size="small" type="danger" @click="deleteMessage">删除</el-button>
          </div>
          <el-button size="small" icon="Close" @click="currentMessage = null" />
        </div>
        
        <EmailViewer :email="currentMessage" />
      </div>
    </div>

    <!-- 写邮件对话框 -->
    <EmailComposer 
      v-model="showCompose"
      :reply-to="replyToMessage"
      :forward-message="forwardToMessage"
      @sent="handleEmailSent" />

    <!-- 创建文件夹对话框 -->
    <el-dialog
      v-model="showCreateFolder"
      title="创建文件夹"
      width="400px">
      <el-form :model="newFolderForm" label-width="80px">
        <el-form-item label="文件夹名">
          <el-input v-model="newFolderForm.name" placeholder="请输入文件夹名称" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateFolder = false">取消</el-button>
        <el-button type="primary" @click="createFolder">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Edit, Refresh, Delete, Flag, Search, Plus, Close,
  Inbox, Message, Document, Warning, Folder,
  Star, StarFilled, Paperclip, ArrowDown
} from '@element-plus/icons-vue'
import { emailApi } from '@/api/email'
import { emailFolderApi } from '@/api/email-folder'
import EmailViewer from '@/components/EmailViewer.vue'
import EmailComposer from '@/components/EmailComposer.vue'
import type { EmailMessage, EmailFolder } from '@/api/email'

// 响应式数据
const loading = ref(false)
const messages = ref<EmailMessage[]>([])
const folders = ref<EmailFolder[]>([])
const currentFolder = ref<number | null>(null)
const currentMessage = ref<EmailMessage | null>(null)
const selectedMessages = ref<number[]>([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalMessages = ref(0)

// 对话框控制
const showCompose = ref(false)
const showCreateFolder = ref(false)
const replyToMessage = ref<EmailMessage | null>(null)
const forwardToMessage = ref<EmailMessage | null>(null)

// 表单数据
const newFolderForm = ref({
  name: ''
})

// 存储信息
const storageUsed = ref(0)
const storageQuota = ref(1073741824) // 1GB

// 计算属性
const selectAll = computed({
  get: () => selectedMessages.value.length === messages.value.length && messages.value.length > 0,
  set: (value: boolean) => {
    selectedMessages.value = value ? messages.value.map(m => m.id) : []
  }
})

const isIndeterminate = computed(() => 
  selectedMessages.value.length > 0 && selectedMessages.value.length < messages.value.length
)

const storagePercentage = computed(() => 
  Math.round((storageUsed.value / storageQuota.value) * 100)
)

// 组件方法
onMounted(() => {
  loadFolders()
  loadUserInfo()
})

// 监听文件夹变化
watch(currentFolder, (newFolder) => {
  if (newFolder) {
    currentPage.value = 1
    loadMessages()
  }
})

// 加载文件夹列表
const loadFolders = async () => {
  try {
    const response = await emailFolderApi.getFolders()
    folders.value = response.data.folders
    
    // 默认选择收件箱
    const inbox = folders.value.find(f => f.folderType === 'INBOX')
    if (inbox) {
      currentFolder.value = inbox.id
    }
  } catch (error) {
    ElMessage.error('加载文件夹失败')
  }
}

// 加载邮件列表
const loadMessages = async () => {
  if (!currentFolder.value) return
  
  loading.value = true
  try {
    const response = await emailApi.getMessages(currentFolder.value, {
      page: currentPage.value - 1,
      size: pageSize.value,
      query: searchQuery.value
    })
    
    messages.value = response.data.messages
    totalMessages.value = response.data.totalElements
    selectedMessages.value = []
  } catch (error) {
    ElMessage.error('加载邮件失败')
  } finally {
    loading.value = false
  }
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    // 这里应该调用实际的API获取用户存储信息
    // const response = await userApi.getStorageInfo()
    // storageUsed.value = response.data.used
    // storageQuota.value = response.data.quota
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 切换文件夹
const switchFolder = (folderId: number) => {
  currentFolder.value = folderId
  currentMessage.value = null
}

// 刷新邮件
const refreshMessages = () => {
  loadMessages()
  ElMessage.success('刷新完成')
}

// 搜索邮件
const searchMessages = () => {
  currentPage.value = 1
  loadMessages()
}

// 选择邮件
const selectMessage = (message: EmailMessage) => {
  currentMessage.value = message
  
  // 标记为已读
  if (!message.isRead) {
    markAsRead(message)
  }
}

// 切换邮件选择
const toggleMessageSelection = (messageId: number) => {
  const index = selectedMessages.value.indexOf(messageId)
  if (index > -1) {
    selectedMessages.value.splice(index, 1)
  } else {
    selectedMessages.value.push(messageId)
  }
}

// 全选处理
const handleSelectAll = (value: boolean) => {
  selectedMessages.value = value ? messages.value.map(m => m.id) : []
}

// 切换星标
const toggleStar = async (message: EmailMessage) => {
  try {
    await emailApi.toggleStar(message.id)
    message.isStarred = !message.isStarred
    ElMessage.success(message.isStarred ? '已添加星标' : '已移除星标')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 切换选中邮件星标
const toggleStarSelected = async () => {
  if (selectedMessages.value.length === 0) return
  
  try {
    await emailApi.batchToggleStar(selectedMessages.value)
    selectedMessages.value.forEach(id => {
      const message = messages.value.find(m => m.id === id)
      if (message) {
        message.isStarred = !message.isStarred
      }
    })
    ElMessage.success('批量操作完成')
  } catch (error) {
    ElMessage.error('批量操作失败')
  }
}

// 标记为已读
const markAsRead = async (message: EmailMessage) => {
  try {
    await emailApi.markAsRead(message.id)
    message.isRead = true
    
    // 更新文件夹未读数
    const folder = folders.value.find(f => f.id === currentFolder.value)
    if (folder && folder.unreadCount > 0) {
      folder.unreadCount--
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 批量操作
const handleBatchAction = async (command: string) => {
  if (selectedMessages.value.length === 0) {
    ElMessage.warning('请先选择邮件')
    return
  }
  
  try {
    switch (command) {
      case 'markRead':
        await emailApi.batchMarkAsRead(selectedMessages.value)
        break
      case 'markUnread':
        await emailApi.batchMarkAsUnread(selectedMessages.value)
        break
      case 'addStar':
        await emailApi.batchAddStar(selectedMessages.value)
        break
      case 'removeStar':
        await emailApi.batchRemoveStar(selectedMessages.value)
        break
      case 'moveToSpam':
        await emailApi.batchMoveToSpam(selectedMessages.value)
        break
      case 'delete':
        await confirmDelete(() => emailApi.batchDelete(selectedMessages.value))
        break
    }
    
    ElMessage.success('批量操作完成')
    loadMessages()
  } catch (error) {
    ElMessage.error('批量操作失败')
  }
}

// 删除选中邮件
const deleteSelectedMessages = async () => {
  if (selectedMessages.value.length === 0) return
  
  await confirmDelete(() => emailApi.batchDelete(selectedMessages.value))
  loadMessages()
}

// 删除确认
const confirmDelete = async (deleteAction: () => Promise<any>) => {
  try {
    await ElMessageBox.confirm('确定要删除选中的邮件吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteAction()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 回复邮件
const replyMessage = () => {
  if (!currentMessage.value) return
  replyToMessage.value = currentMessage.value
  showCompose.value = true
}

// 回复全部
const replyAllMessage = () => {
  if (!currentMessage.value) return
  replyToMessage.value = { ...currentMessage.value, replyAll: true } as any
  showCompose.value = true
}

// 转发邮件
const forwardMessage = () => {
  if (!currentMessage.value) return
  forwardToMessage.value = currentMessage.value
  showCompose.value = true
}

// 删除当前邮件
const deleteMessage = async () => {
  if (!currentMessage.value) return
  
  await confirmDelete(() => emailApi.deleteMessage(currentMessage.value!.id))
  currentMessage.value = null
  loadMessages()
}

// 创建文件夹
const createFolder = async () => {
  if (!newFolderForm.value.name.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  
  try {
    await emailFolderApi.createFolder({
      folderName: newFolderForm.value.name,
      folderType: 'CUSTOM'
    })
    
    ElMessage.success('文件夹创建成功')
    showCreateFolder.value = false
    newFolderForm.value.name = ''
    loadFolders()
  } catch (error) {
    ElMessage.error('创建文件夹失败')
  }
}

// 邮件发送成功处理
const handleEmailSent = () => {
  showCompose.value = false
  replyToMessage.value = null
  forwardToMessage.value = null
  loadMessages()
}

// 工具方法
const getSenderName = (email: string) => {
  const match = email.match(/^(.+?)\s*</)
  return match ? match[1].trim() : email.split('@')[0]
}

const hasAttachments = (message: EmailMessage) => {
  return message.attachments && message.attachments.length > 0
}

const getMessagePreview = (message: EmailMessage) => {
  const text = message.bodyText || message.bodyHtml || ''
  return text.replace(/<[^>]*>/g, '').substring(0, 100) + (text.length > 100 ? '...' : '')
}

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffTime = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
</script>

<style scoped>
.webmail-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.webmail-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.toolbar-left {
  display: flex;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.webmail-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.folder-panel {
  width: 250px;
  background: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.folder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.folder-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.folder-list {
  flex: 1;
  overflow-y: auto;
}

.folder-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.folder-item:hover {
  background: #f0f0f0;
}

.folder-item.active {
  background: #e6f7ff;
  border-right: 3px solid #1890ff;
}

.folder-icon {
  margin-right: 8px;
  color: #666;
}

.folder-name {
  flex: 1;
  font-size: 14px;
}

.unread-count {
  background: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  min-width: 18px;
  text-align: center;
}

.storage-info {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
}

.storage-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.storage-text {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-top: 4px;
}

.message-panel {
  flex: 1;
  background: white;
  display: flex;
  flex-direction: column;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.message-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.message-count {
  color: #666;
  font-size: 14px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.3s;
}

.message-item:hover {
  background: #f8f9fa;
}

.message-item.selected {
  background: #e6f7ff;
}

.message-item.unread {
  background: #fafafa;
  border-left: 3px solid #1890ff;
}

.message-checkbox {
  margin-right: 12px;
}

.message-star {
  margin-right: 12px;
  cursor: pointer;
}

.message-star .starred {
  color: #faad14;
}

.message-star .unstarred {
  color: #d9d9d9;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.message-from {
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.message-time {
  color: #666;
  font-size: 12px;
  white-space: nowrap;
}

.message-subject {
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
}

.attachment-icon {
  margin-left: 8px;
  color: #666;
  font-size: 14px;
}

.message-preview {
  color: #999;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-pagination {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  text-align: center;
}

.detail-panel {
  width: 400px;
  background: white;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.unread-badge {
  color: #ff4d4f;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .detail-panel {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 1000;
    box-shadow: -2px 0 8px rgba(0,0,0,0.15);
  }
}

@media (max-width: 768px) {
  .webmail-layout {
    flex-direction: column;
  }
  
  .folder-panel {
    width: 100%;
    height: 200px;
  }
  
  .message-panel {
    height: calc(100vh - 300px);
  }
  
  .detail-panel {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100%;
  }
}
</style>