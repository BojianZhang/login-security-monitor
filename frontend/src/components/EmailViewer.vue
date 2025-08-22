<template>
  <div class="email-viewer">
    <div class="email-header">
      <h3 class="email-subject">{{ email.subject || '(无主题)' }}</h3>
      
      <div class="email-meta">
        <div class="meta-row">
          <span class="meta-label">发件人：</span>
          <span class="meta-value">{{ email.fromAddress }}</span>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">收件人：</span>
          <span class="meta-value">{{ email.toAddresses }}</span>
        </div>
        
        <div v-if="email.ccAddresses" class="meta-row">
          <span class="meta-label">抄送：</span>
          <span class="meta-value">{{ email.ccAddresses }}</span>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">时间：</span>
          <span class="meta-value">{{ formatDate(email.receivedAt) }}</span>
        </div>
        
        <div class="meta-row">
          <span class="meta-label">大小：</span>
          <span class="meta-value">{{ formatFileSize(email.messageSize) }}</span>
        </div>
        
        <div v-if="email.attachments && email.attachments.length > 0" class="meta-row">
          <span class="meta-label">附件：</span>
          <span class="meta-value">{{ email.attachments.length }} 个文件</span>
        </div>
      </div>
    </div>

    <!-- 附件列表 -->
    <div v-if="email.attachments && email.attachments.length > 0" class="attachments-section">
      <h4>附件</h4>
      <div class="attachments-list">
        <div 
          v-for="attachment in email.attachments" 
          :key="attachment.id"
          class="attachment-item">
          <el-icon class="attachment-icon">
            <Paperclip />
          </el-icon>
          <span class="attachment-name">{{ attachment.filename }}</span>
          <span class="attachment-size">({{ formatFileSize(attachment.fileSize) }})</span>
          <el-button type="primary" size="small" @click="downloadAttachment(attachment)">
            下载
          </el-button>
        </div>
      </div>
    </div>

    <!-- 邮件内容 -->
    <div class="email-content">
      <div v-if="email.bodyHtml" class="email-html-content">
        <iframe 
          ref="htmlFrame"
          :srcdoc="sanitizedHtmlContent"
          frameborder="0"
          sandbox="allow-same-origin"
          class="html-iframe">
        </iframe>
      </div>
      
      <div v-else-if="email.bodyText" class="email-text-content">
        <pre>{{ email.bodyText }}</pre>
      </div>
      
      <div v-else class="empty-content">
        <el-empty description="邮件内容为空" />
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="email-actions">
      <el-button @click="markAsRead" :disabled="email.isRead">
        {{ email.isRead ? '已读' : '标记为已读' }}
      </el-button>
      <el-button @click="toggleStar" :type="email.isStarred ? 'warning' : ''">
        {{ email.isStarred ? '取消星标' : '添加星标' }}
      </el-button>
      <el-button @click="replyEmail">回复</el-button>
      <el-button @click="forwardEmail">转发</el-button>
      <el-button type="danger" @click="deleteEmail">删除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Paperclip } from '@element-plus/icons-vue'
import type { EmailMessage, EmailAttachment } from '@/api/email-search'

// Props
interface Props {
  email: EmailMessage
}

const props = defineProps<Props>()

// 响应式数据
const htmlFrame = ref<HTMLIFrameElement>()

// 计算属性
const sanitizedHtmlContent = computed(() => {
  if (!props.email.bodyHtml) return ''
  
  // 简单的HTML净化，移除潜在的危险标签和属性
  let content = props.email.bodyHtml
  
  // 移除script标签
  content = content.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
  
  // 移除危险的事件属性
  content = content.replace(/\s*on\w+\s*=\s*"[^"]*"/gi, '')
  content = content.replace(/\s*on\w+\s*=\s*'[^']*'/gi, '')
  
  // 移除javascript:协议
  content = content.replace(/javascript:/gi, '')
  
  // 为iframe添加基本样式
  return `
    <html>
      <head>
        <meta charset="utf-8">
        <style>
          body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 16px;
            word-wrap: break-word;
          }
          img { max-width: 100%; height: auto; }
          table { border-collapse: collapse; width: 100%; }
          th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
          a { color: #409eff; }
        </style>
      </head>
      <body>${content}</body>
    </html>
  `
})

// 组件方法
onMounted(() => {
  adjustIframeHeight()
})

// 调整iframe高度
const adjustIframeHeight = async () => {
  if (!htmlFrame.value) return
  
  await nextTick()
  
  const iframe = htmlFrame.value
  const iframeDocument = iframe.contentDocument || iframe.contentWindow?.document
  
  if (iframeDocument) {
    // 等待内容加载完成
    iframe.onload = () => {
      const body = iframeDocument.body
      const html = iframeDocument.documentElement
      const height = Math.max(
        body?.scrollHeight || 0,
        body?.offsetHeight || 0,
        html?.clientHeight || 0,
        html?.scrollHeight || 0,
        html?.offsetHeight || 0
      )
      iframe.style.height = Math.max(height + 20, 200) + 'px'
    }
  }
}

// 标记为已读
const markAsRead = async () => {
  try {
    // 这里应该调用实际的API
    // await emailApi.markAsRead(props.email.id)
    ElMessage.success('已标记为已读')
    // 更新本地状态
    props.email.isRead = true
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 切换星标
const toggleStar = async () => {
  try {
    // 这里应该调用实际的API
    // await emailApi.toggleStar(props.email.id)
    const action = props.email.isStarred ? '取消星标' : '添加星标'
    ElMessage.success(action + '成功')
    // 更新本地状态
    props.email.isStarred = !props.email.isStarred
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 回复邮件
const replyEmail = () => {
  ElMessage.info('回复功能开发中...')
  // 这里可以跳转到撰写邮件页面，并预填回复内容
}

// 转发邮件
const forwardEmail = () => {
  ElMessage.info('转发功能开发中...')
  // 这里可以跳转到撰写邮件页面，并预填转发内容
}

// 删除邮件
const deleteEmail = async () => {
  try {
    // 这里应该调用实际的API
    // await emailApi.deleteEmail(props.email.id)
    ElMessage.success('邮件已删除')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 下载附件
const downloadAttachment = async (attachment: EmailAttachment) => {
  try {
    ElMessage.info('正在下载附件...')
    // 这里应该调用实际的API下载附件
    // const blob = await emailApi.downloadAttachment(attachment.id)
    // const url = window.URL.createObjectURL(blob)
    // const link = document.createElement('a')
    // link.href = url
    // link.download = attachment.filename
    // link.click()
    // window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 工具方法
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
.email-viewer {
  max-width: 100%;
  background: white;
}

.email-header {
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.email-subject {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  word-wrap: break-word;
}

.email-meta {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
}

.meta-row {
  display: flex;
  margin-bottom: 8px;
}

.meta-row:last-child {
  margin-bottom: 0;
}

.meta-label {
  font-weight: 600;
  color: #666;
  min-width: 60px;
  margin-right: 8px;
}

.meta-value {
  color: #333;
  word-wrap: break-word;
  flex: 1;
}

.attachments-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.attachments-section h4 {
  margin: 0 0 12px 0;
  color: #333;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.attachment-icon {
  color: #666;
}

.attachment-name {
  flex: 1;
  font-weight: 500;
}

.attachment-size {
  color: #999;
  font-size: 12px;
}

.email-content {
  margin-bottom: 20px;
}

.email-html-content {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}

.html-iframe {
  width: 100%;
  min-height: 200px;
  border: none;
}

.email-text-content {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.email-text-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.empty-content {
  padding: 40px 0;
  text-align: center;
}

.email-actions {
  padding-top: 20px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .meta-row {
    flex-direction: column;
  }
  
  .meta-label {
    min-width: auto;
    margin-bottom: 4px;
  }
  
  .attachment-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .email-actions {
    flex-direction: column;
  }
}
</style>