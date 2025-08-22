<template>
  <div class="email-search">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="search-input-container">
        <el-autocomplete
          v-model="searchQuery"
          :fetch-suggestions="fetchSuggestions"
          :trigger-on-focus="false"
          :debounce="300"
          placeholder="搜索邮件..."
          class="search-input"
          @select="handleSuggestionSelect"
          @keyup.enter="performSearch">
          <template #prepend>
            <el-button icon="Search" @click="performSearch" />
          </template>
          <template #append>
            <el-button 
              icon="Operation" 
              @click="showAdvancedSearch = !showAdvancedSearch"
              :type="showAdvancedSearch ? 'primary' : ''" />
          </template>
          <template #default="{ item }">
            <div class="suggestion-item">
              <el-icon class="suggestion-icon">
                <User v-if="item.type === 'sender'" />
                <Document v-if="item.type === 'subject'" />
                <Search v-if="item.type === 'keyword'" />
              </el-icon>
              <span class="suggestion-text">{{ item.displayText }}</span>
            </div>
          </template>
        </el-autocomplete>
      </div>
      
      <div class="search-actions">
        <el-button 
          icon="Clock" 
          @click="showSearchHistory = true">
          搜索历史
        </el-button>
        <el-button 
          icon="Download" 
          @click="exportResults">
          导出结果
        </el-button>
      </div>
    </div>

    <!-- 高级搜索面板 -->
    <el-collapse-transition>
      <div v-show="showAdvancedSearch" class="advanced-search-panel">
        <el-card>
          <template #header>
            <span>高级搜索</span>
          </template>
          
          <el-form :model="advancedForm" label-width="80px" class="advanced-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="发件人">
                  <el-input v-model="advancedForm.fromAddress" placeholder="发件人邮箱地址" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="收件人">
                  <el-input v-model="advancedForm.toAddress" placeholder="收件人邮箱地址" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="主题">
                  <el-input v-model="advancedForm.subject" placeholder="邮件主题" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="文件夹">
                  <el-select v-model="advancedForm.folderId" placeholder="选择文件夹" clearable>
                    <el-option
                      v-for="folder in folders"
                      :key="folder.id"
                      :label="folder.folderName"
                      :value="folder.id" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="正文">
                  <el-input 
                    v-model="advancedForm.bodyText" 
                    type="textarea" 
                    :rows="2"
                    placeholder="邮件正文内容" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="时间范围">
                  <el-date-picker
                    v-model="dateRange"
                    type="datetimerange"
                    range-separator="至"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleDateRangeChange" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="筛选条件">
                  <el-space>
                    <el-select v-model="advancedForm.isRead" placeholder="已读状态" clearable>
                      <el-option label="全部" :value="null" />
                      <el-option label="已读" :value="true" />
                      <el-option label="未读" :value="false" />
                    </el-select>
                    <el-select v-model="advancedForm.hasAttachments" placeholder="附件" clearable>
                      <el-option label="全部" :value="null" />
                      <el-option label="有附件" :value="true" />
                      <el-option label="无附件" :value="false" />
                    </el-select>
                    <el-select v-model="advancedForm.priorityLevel" placeholder="优先级" clearable>
                      <el-option label="全部" :value="null" />
                      <el-option label="高" :value="1" />
                      <el-option label="普通" :value="3" />
                      <el-option label="低" :value="5" />
                    </el-select>
                  </el-space>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row>
              <el-col :span="24">
                <div class="advanced-actions">
                  <el-button @click="resetAdvancedForm">重置</el-button>
                  <el-button type="primary" @click="performAdvancedSearch">搜索</el-button>
                </div>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </div>
    </el-collapse-transition>

    <!-- 搜索结果 -->
    <div class="search-results">
      <div v-if="searchPerformed" class="search-info">
        <span class="search-stats">
          找到 {{ searchResult.totalElements }} 封邮件
          <span v-if="searchResult.searchTime">(耗时 {{ searchResult.searchTime }}ms)</span>
        </span>
        <div class="search-query">
          搜索: "{{ searchResult.searchQuery }}"
        </div>
      </div>

      <el-table
        v-loading="searching"
        :data="searchResult.messages"
        stripe
        style="width: 100%"
        @row-click="viewEmail">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="isRead" label="" width="20">
          <template #default="{ row }">
            <div class="read-indicator" :class="{ unread: !row.isRead }"></div>
          </template>
        </el-table-column>
        <el-table-column prop="fromAddress" label="发件人" min-width="180">
          <template #default="{ row }">
            <div class="sender-info">
              <div class="sender-name">{{ getSenderName(row.fromAddress) }}</div>
              <div class="sender-email">{{ row.fromAddress }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="主题" min-width="300">
          <template #default="{ row }">
            <div class="subject-container">
              <span class="subject" v-html="row.subject"></span>
              <el-icon v-if="hasAttachments(row)" class="attachment-icon">
                <Paperclip />
              </el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="folder.folderName" label="文件夹" width="100" />
        <el-table-column prop="receivedAt" label="时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.receivedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="messageSize" label="大小" width="80">
          <template #default="{ row }">
            {{ formatFileSize(row.messageSize) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click.stop="viewEmail(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div v-if="searchResult.totalElements > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="searchResult.totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange" />
      </div>
    </div>

    <!-- 搜索历史对话框 -->
    <el-dialog
      v-model="showSearchHistory"
      title="搜索历史"
      width="600px">
      <div class="search-history-list">
        <div v-if="searchHistory.length === 0" class="empty-history">
          <el-empty description="暂无搜索历史" />
        </div>
        <div v-else>
          <div 
            v-for="item in searchHistory" 
            :key="item.id"
            class="history-item"
            @click="useHistorySearch(item)">
            <div class="history-query">{{ item.query }}</div>
            <div class="history-meta">
              <span class="history-type">{{ item.searchType === 'FULLTEXT' ? '全文搜索' : '高级搜索' }}</span>
              <span class="history-count">{{ item.resultCount }} 个结果</span>
              <span class="history-time">{{ formatDate(item.createdAt) }}</span>
              <el-button 
                type="danger" 
                size="small" 
                icon="Delete"
                @click.stop="deleteHistory(item.id)">
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showSearchHistory = false">关闭</el-button>
        <el-button 
          type="danger" 
          @click="clearAllHistory"
          :disabled="searchHistory.length === 0">
          清空历史
        </el-button>
      </template>
    </el-dialog>

    <!-- 邮件详情对话框 -->
    <el-dialog
      v-model="showEmailDetail"
      title="邮件详情"
      width="80%"
      :close-on-click-modal="false">
      <EmailViewer v-if="currentEmail" :email="currentEmail" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, Operation, Clock, Download, User, Document, 
  Paperclip, Delete 
} from '@element-plus/icons-vue'
import { emailSearchApi, type SearchResult, type EmailMessage, type SearchHistory } from '@/api/email-search'
import { emailFolderApi } from '@/api/email-folder'
import EmailViewer from '@/components/EmailViewer.vue'

// 响应式数据
const searching = ref(false)
const searchPerformed = ref(false)
const showAdvancedSearch = ref(false)
const showSearchHistory = ref(false)
const showEmailDetail = ref(false)

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const dateRange = ref<[string, string] | null>(null)

const searchResult = ref<SearchResult>({
  messages: [],
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
  pageSize: 20,
  searchQuery: '',
  searchTime: 0
})

const searchHistory = ref<SearchHistory[]>([])
const folders = ref([])
const currentEmail = ref<EmailMessage | null>(null)

// 高级搜索表单
const advancedForm = ref({
  fromAddress: '',
  toAddress: '',
  subject: '',
  bodyText: '',
  folderId: null as number | null,
  dateFrom: '',
  dateTo: '',
  isRead: null as boolean | null,
  hasAttachments: null as boolean | null,
  priorityLevel: null as number | null
})

// 组件方法
onMounted(() => {
  loadFolders()
  loadSearchHistory()
})

// 加载文件夹列表
const loadFolders = async () => {
  try {
    const response = await emailFolderApi.getFolders()
    folders.value = response.data.folders
  } catch (error) {
    console.error('加载文件夹失败:', error)
  }
}

// 加载搜索历史
const loadSearchHistory = async () => {
  try {
    const response = await emailSearchApi.getSearchHistory(50)
    searchHistory.value = response.data.history
  } catch (error) {
    console.error('加载搜索历史失败:', error)
  }
}

// 获取搜索建议
const fetchSuggestions = async (query: string, callback: Function) => {
  if (query.length < 2) {
    callback([])
    return
  }
  
  try {
    const response = await emailSearchApi.getSearchSuggestions(query, 10)
    callback(response.data.suggestions)
  } catch (error) {
    callback([])
  }
}

// 处理建议选择
const handleSuggestionSelect = (item: any) => {
  searchQuery.value = item.query
  performSearch()
}

// 执行搜索
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  searching.value = true
  searchPerformed.value = true
  
  try {
    const searchRequest = {
      query: searchQuery.value,
      page: currentPage.value - 1,
      size: pageSize.value,
      fullTextSearch: true,
      highlightEnabled: true
    }
    
    const response = await emailSearchApi.searchMessages(searchRequest)
    searchResult.value = response.data
    
    // 刷新搜索历史
    loadSearchHistory()
    
  } catch (error) {
    ElMessage.error('搜索失败')
    console.error('搜索失败:', error)
  } finally {
    searching.value = false
  }
}

// 执行高级搜索
const performAdvancedSearch = async () => {
  searching.value = true
  searchPerformed.value = true
  
  try {
    const searchParams = {
      ...advancedForm.value,
      page: currentPage.value - 1,
      size: pageSize.value
    }
    
    const response = await emailSearchApi.advancedSearch(searchParams)
    searchResult.value = response.data
    
    // 更新搜索查询显示
    const queryParts = []
    if (advancedForm.value.fromAddress) queryParts.push(`发件人:${advancedForm.value.fromAddress}`)
    if (advancedForm.value.toAddress) queryParts.push(`收件人:${advancedForm.value.toAddress}`)
    if (advancedForm.value.subject) queryParts.push(`主题:${advancedForm.value.subject}`)
    if (advancedForm.value.bodyText) queryParts.push(`正文:${advancedForm.value.bodyText}`)
    
    searchResult.value.searchQuery = queryParts.join(' ')
    
    // 刷新搜索历史
    loadSearchHistory()
    
  } catch (error) {
    ElMessage.error('高级搜索失败')
    console.error('高级搜索失败:', error)
  } finally {
    searching.value = false
  }
}

// 重置高级搜索表单
const resetAdvancedForm = () => {
  advancedForm.value = {
    fromAddress: '',
    toAddress: '',
    subject: '',
    bodyText: '',
    folderId: null,
    dateFrom: '',
    dateTo: '',
    isRead: null,
    hasAttachments: null,
    priorityLevel: null
  }
  dateRange.value = null
}

// 处理日期范围变化
const handleDateRangeChange = (dates: [string, string] | null) => {
  if (dates) {
    advancedForm.value.dateFrom = dates[0]
    advancedForm.value.dateTo = dates[1]
  } else {
    advancedForm.value.dateFrom = ''
    advancedForm.value.dateTo = ''
  }
}

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  if (showAdvancedSearch.value) {
    performAdvancedSearch()
  } else {
    performSearch()
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  if (showAdvancedSearch.value) {
    performAdvancedSearch()
  } else {
    performSearch()
  }
}

// 使用历史搜索
const useHistorySearch = (item: SearchHistory) => {
  searchQuery.value = item.query
  showSearchHistory.value = false
  
  if (item.searchType === 'FULLTEXT') {
    showAdvancedSearch.value = false
    performSearch()
  } else {
    // 解析高级搜索参数（如果有存储的话）
    showAdvancedSearch.value = true
    performAdvancedSearch()
  }
}

// 删除搜索历史
const deleteHistory = async (historyId: number) => {
  try {
    await emailSearchApi.deleteSearchHistory(historyId)
    ElMessage.success('删除成功')
    loadSearchHistory()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 清空所有历史
const clearAllHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有搜索历史吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await emailSearchApi.clearSearchHistory()
    ElMessage.success('清空成功')
    loadSearchHistory()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

// 查看邮件详情
const viewEmail = (email: EmailMessage) => {
  currentEmail.value = email
  showEmailDetail.value = true
}

// 导出搜索结果
const exportResults = async () => {
  if (!searchPerformed.value || searchResult.value.totalElements === 0) {
    ElMessage.warning('没有可导出的搜索结果')
    return
  }
  
  try {
    const searchRequest = {
      query: searchQuery.value,
      ...advancedForm.value
    }
    
    const blob = await emailSearchApi.exportSearchResults(searchRequest, 'csv')
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `email_search_results_${new Date().getTime()}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

// 工具方法
const getSenderName = (email: string) => {
  const match = email.match(/^(.+?)\s*</)
  return match ? match[1].trim() : email.split('@')[0]
}

const hasAttachments = (email: EmailMessage) => {
  return email.attachments && email.attachments.length > 0
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
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
.email-search {
  padding: 20px;
}

.search-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.search-input-container {
  flex: 1;
}

.search-input {
  width: 100%;
}

.search-actions {
  display: flex;
  gap: 10px;
}

.advanced-search-panel {
  margin-bottom: 20px;
}

.advanced-form {
  padding: 20px 0;
}

.advanced-actions {
  text-align: right;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.suggestion-icon {
  color: #909399;
}

.suggestion-text {
  flex: 1;
}

.search-results {
  background: white;
  border-radius: 8px;
}

.search-info {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #f8f9fa;
}

.search-stats {
  font-weight: 600;
  color: #409eff;
}

.search-query {
  margin-top: 4px;
  color: #666;
  font-size: 14px;
}

.read-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: transparent;
}

.read-indicator.unread {
  background: #409eff;
}

.sender-info {
  line-height: 1.4;
}

.sender-name {
  font-weight: 600;
}

.sender-email {
  font-size: 12px;
  color: #999;
}

.subject-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subject {
  flex: 1;
}

.attachment-icon {
  color: #909399;
  font-size: 14px;
}

.pagination-container {
  padding: 20px;
  text-align: center;
}

.search-history-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-history {
  padding: 40px 0;
}

.history-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.3s;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-query {
  font-weight: 600;
  margin-bottom: 4px;
}

.history-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.history-type {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 6px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .search-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-actions {
    justify-content: center;
  }
  
  .advanced-form .el-row {
    margin-bottom: 10px;
  }
}
</style>