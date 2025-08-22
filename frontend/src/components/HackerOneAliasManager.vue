<!-- HackerOne 别名显示组件 -->
<template>
  <div class="hackerone-alias-list">
    <h3>HackerOne 邮箱别名</h3>
    
    <!-- 统计信息 -->
    <div class="stats-card" v-if="stats">
      <el-card class="box-card">
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-label">总数:</span>
            <span class="stat-value">{{ stats.totalCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">活跃:</span>
            <span class="stat-value">{{ stats.activeCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已同步:</span>
            <span class="stat-value">{{ stats.syncedCount }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 操作按钮 -->
    <div class="actions">
      <el-button type="primary" @click="syncAllAliases" :loading="syncing">
        <el-icon><Refresh /></el-icon>
        同步所有HackerOne别名
      </el-button>
      <el-button @click="loadAliases">
        <el-icon><Refresh /></el-icon>
        刷新列表
      </el-button>
    </div>

    <!-- 别名列表 -->
    <div class="alias-list">
      <el-card v-for="alias in aliases" :key="alias.aliasId" class="alias-card">
        <div class="alias-content">
          <!-- 完整邮箱地址 - 可选择和复制 -->
          <div class="email-display">
            <div class="email-text" 
                 :class="{ 'hackerone-email': alias.isHackerOne }"
                 @click="selectText($event)">
              {{ alias.displayFormat }}
            </div>
            <el-button 
              type="text" 
              size="small" 
              @click="copyToClipboard(alias.copyableText)"
              class="copy-button">
              <el-icon><DocumentCopy /></el-icon>
              复制
            </el-button>
          </div>

          <!-- 别名信息 -->
          <div class="alias-info">
            <el-tag v-if="alias.isHackerOne" type="success" size="small">
              HackerOne ({{ alias.username }})
            </el-tag>
            <el-tag v-if="alias.isActive" type="primary" size="small">
              活跃
            </el-tag>
            <el-tag v-else type="info" size="small">
              已禁用
            </el-tag>
          </div>

          <!-- 描述信息 -->
          <div class="description" v-if="alias.description">
            {{ alias.description }}
          </div>

          <!-- 操作按钮 -->
          <div class="alias-actions">
            <el-button 
              type="text" 
              size="small" 
              @click="viewEmails(alias.aliasId)">
              查看邮件
            </el-button>
            <el-button 
              type="text" 
              size="small" 
              @click="showCopyOptions(alias)">
              复制选项
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty v-if="!loading && aliases.length === 0" description="没有找到HackerOne别名">
        <el-button type="primary" @click="syncAllAliases">
          开始同步HackerOne别名
        </el-button>
      </el-empty>
    </div>

    <!-- 复制选项对话框 -->
    <el-dialog v-model="copyDialogVisible" title="复制邮箱地址" width="500px">
      <div class="copy-options">
        <div class="copy-item">
          <label>完整邮箱地址:</label>
          <div class="copy-text" @click="selectText($event)">
            {{ selectedAlias?.displayFormat }}
          </div>
          <el-button type="primary" @click="copyToClipboard(selectedAlias?.copyableText)">
            复制完整地址
          </el-button>
        </div>
        
        <div class="copy-item" v-if="selectedAlias?.username">
          <label>用户名部分:</label>
          <div class="copy-text" @click="selectText($event)">
            {{ selectedAlias.username }}
          </div>
          <el-button @click="copyToClipboard(selectedAlias.username)">
            复制用户名
          </el-button>
        </div>

        <div class="copy-item">
          <label>纯文本格式:</label>
          <textarea 
            class="copy-textarea" 
            :value="selectedAlias?.copyableText" 
            readonly 
            @click="selectText($event)">
          </textarea>
          <el-button @click="copyToClipboard(selectedAlias?.copyableText)">
            复制纯文本
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, DocumentCopy } from '@element-plus/icons-vue'
import api from '@/api/index'

// 响应式数据
const aliases = ref([])
const stats = ref(null)
const loading = ref(false)
const syncing = ref(false)
const copyDialogVisible = ref(false)
const selectedAlias = ref(null)

// 加载HackerOne别名列表
const loadAliases = async () => {
  loading.value = true
  try {
    const response = await api.get('/integrations/hackerone/aliases/copyable-formats')
    aliases.value = response.data
    
    // 加载统计信息
    const statsResponse = await api.get('/integrations/hackerone/stats')
    stats.value = statsResponse.data
    
  } catch (error) {
    ElMessage.error('加载HackerOne别名失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 同步所有HackerOne别名
const syncAllAliases = async () => {
  syncing.value = true
  try {
    const response = await api.post('/integrations/hackerone/sync-aliases')
    
    if (response.data.success) {
      ElMessage.success(`同步完成！已同步 ${response.data.syncedCount} 个别名`)
      await loadAliases() // 重新加载列表
    } else {
      ElMessage.error('同步失败: ' + response.data.message)
    }
  } catch (error) {
    ElMessage.error('同步失败: ' + error.message)
  } finally {
    syncing.value = false
  }
}

// 复制到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板: ' + text)
  } catch (error) {
    // 降级方案：使用传统方法
    const textArea = document.createElement('textarea')
    textArea.value = text
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('已复制到剪贴板: ' + text)
  }
}

// 选择文本内容
const selectText = (event: Event) => {
  const target = event.target as HTMLElement
  if (window.getSelection) {
    const selection = window.getSelection()
    const range = document.createRange()
    range.selectNodeContents(target)
    selection?.removeAllRanges()
    selection?.addRange(range)
  }
}

// 显示复制选项对话框
const showCopyOptions = (alias: any) => {
  selectedAlias.value = alias
  copyDialogVisible.value = true
}

// 查看邮件
const viewEmails = (aliasId: number) => {
  // 导航到邮件查看页面
  console.log('查看别名邮件:', aliasId)
  // 这里可以调用路由跳转或其他操作
}

// 组件挂载时加载数据
onMounted(() => {
  loadAliases()
})
</script>

<style scoped>
.hackerone-alias-list {
  padding: 20px;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.actions {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.alias-list {
  display: grid;
  gap: 15px;
}

.alias-card {
  border-left: 4px solid #409eff;
}

.alias-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.email-display {
  display: flex;
  align-items: center;
  gap: 10px;
}

.email-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 16px;
  font-weight: 500;
  padding: 8px 12px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  user-select: all; /* 允许用户选择文本 */
  flex: 1;
  transition: all 0.3s;
}

.email-text:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

.email-text.hackerone-email {
  background: #f0f9ff;
  border-color: #67c23a;
  color: #529b2e;
}

.copy-button {
  color: #409eff;
}

.alias-info {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.description {
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
}

.alias-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.copy-options {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.copy-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.copy-item label {
  font-weight: bold;
  color: #303133;
}

.copy-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  padding: 8px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  user-select: all;
}

.copy-text:hover {
  background: #ecf5ff;
  border-color: #409eff;
}

.copy-textarea {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  padding: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  resize: vertical;
  min-height: 60px;
  user-select: all;
}

.copy-textarea:focus {
  outline: none;
  border-color: #409eff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .email-display {
    flex-direction: column;
    align-items: stretch;
  }
  
  .actions {
    flex-direction: column;
  }
}
</style>