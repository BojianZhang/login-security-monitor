<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="80%"
    :close-on-click-modal="false"
    :before-close="handleClose"
    class="email-composer-dialog">
    
    <div class="composer-form">
      <el-form ref="formRef" :model="form" label-width="60px">
        <!-- 发件人 -->
        <el-form-item label="发件人">
          <el-select v-model="form.fromAddress" style="width: 100%;">
            <el-option
              v-for="alias in userAliases"
              :key="alias.id"
              :label="alias.fullEmail"
              :value="alias.fullEmail">
              <div class="alias-option">
                <div class="alias-email">{{ alias.fullEmail }}</div>
                <div class="alias-name">{{ alias.displayName || alias.aliasEmail }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 收件人 -->
        <el-form-item label="收件人" required>
          <EmailAddressInput 
            v-model="form.toAddresses"
            placeholder="输入收件人邮箱地址，多个地址用逗号分隔" />
        </el-form-item>

        <!-- 抄送 -->
        <el-form-item v-if="showCC" label="抄送">
          <EmailAddressInput 
            v-model="form.ccAddresses"
            placeholder="输入抄送邮箱地址" />
        </el-form-item>

        <!-- 密送 -->
        <el-form-item v-if="showBCC" label="密送">
          <EmailAddressInput 
            v-model="form.bccAddresses"
            placeholder="输入密送邮箱地址" />
        </el-form-item>

        <!-- 更多选项 -->
        <div class="composer-options">
          <el-button 
            v-if="!showCC" 
            size="small" 
            text 
            @click="showCC = true">
            +抄送
          </el-button>
          <el-button 
            v-if="!showBCC" 
            size="small" 
            text 
            @click="showBCC = true">
            +密送
          </el-button>
          <el-button 
            size="small" 
            text 
            @click="showAdvancedOptions = !showAdvancedOptions">
            {{ showAdvancedOptions ? '隐藏高级选项' : '高级选项' }}
          </el-button>
        </div>

        <!-- 高级选项 -->
        <div v-if="showAdvancedOptions" class="advanced-options">
          <el-form-item label="回复">
            <el-input v-model="form.replyTo" placeholder="回复地址" />
          </el-form-item>
          
          <el-form-item label="优先级">
            <el-select v-model="form.priorityLevel">
              <el-option label="高" :value="1" />
              <el-option label="普通" :value="3" />
              <el-option label="低" :value="5" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="发送时间">
            <el-date-picker
              v-model="form.scheduledAt"
              type="datetime"
              placeholder="选择发送时间（留空立即发送）"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </div>

        <!-- 主题 -->
        <el-form-item label="主题" required>
          <el-input v-model="form.subject" placeholder="请输入邮件主题" />
        </el-form-item>

        <!-- 附件 -->
        <el-form-item label="附件">
          <el-upload
            ref="uploadRef"
            :file-list="attachments"
            :auto-upload="false"
            :on-change="handleAttachmentChange"
            :on-remove="handleAttachmentRemove"
            multiple
            drag
            class="attachment-upload">
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持多个文件，单个文件不超过50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 邮件内容 -->
        <el-form-item label="内容">
          <div class="editor-toolbar">
            <el-button-group>
              <el-button 
                :type="editorMode === 'html' ? 'primary' : ''"
                size="small"
                @click="switchEditorMode('html')">
                富文本
              </el-button>
              <el-button 
                :type="editorMode === 'text' ? 'primary' : ''"
                size="small"
                @click="switchEditorMode('text')">
                纯文本
              </el-button>
            </el-button-group>
          </div>

          <!-- HTML编辑器 -->
          <div v-if="editorMode === 'html'" class="html-editor">
            <RichTextEditor v-model="form.bodyHtml" />
          </div>

          <!-- 纯文本编辑器 -->
          <el-input 
            v-else
            v-model="form.bodyText"
            type="textarea"
            :rows="12"
            placeholder="请输入邮件内容..." />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="composer-footer">
        <div class="footer-left">
          <el-button @click="saveDraft">保存草稿</el-button>
          <el-dropdown @command="handleSendCommand">
            <el-button type="primary" :loading="sending">
              {{ form.scheduledAt ? '定时发送' : '发送' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="send">立即发送</el-dropdown-item>
                <el-dropdown-item command="schedule">定时发送</el-dropdown-item>
                <el-dropdown-item command="draft">保存为草稿</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        
        <div class="footer-right">
          <el-button @click="handleClose">取消</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, ArrowDown } from '@element-plus/icons-vue'
import { emailApi } from '@/api/email'
import { emailAliasApi } from '@/api/email-alias'
import EmailAddressInput from '@/components/EmailAddressInput.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import type { EmailMessage } from '@/api/email'

// Props
interface Props {
  modelValue: boolean
  replyTo?: EmailMessage | null
  forwardMessage?: EmailMessage | null
}

const props = withDefaults(defineProps<Props>(), {
  replyTo: null,
  forwardMessage: null
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'sent': []
}>()

// 响应式数据
const formRef = ref()
const uploadRef = ref()
const sending = ref(false)
const editorMode = ref<'html' | 'text'>('html')
const showCC = ref(false)
const showBCC = ref(false)
const showAdvancedOptions = ref(false)
const userAliases = ref([])
const attachments = ref([])

const form = ref({
  fromAddress: '',
  toAddresses: '',
  ccAddresses: '',
  bccAddresses: '',
  replyTo: '',
  subject: '',
  bodyText: '',
  bodyHtml: '',
  priorityLevel: 3,
  scheduledAt: ''
})

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const dialogTitle = computed(() => {
  if (props.replyTo) {
    return `回复: ${props.replyTo.subject}`
  } else if (props.forwardMessage) {
    return `转发: ${props.forwardMessage.subject}`
  } else {
    return '写邮件'
  }
})

// 监听对话框打开
watch(visible, (newValue) => {
  if (newValue) {
    initializeForm()
    loadUserAliases()
  }
})

// 监听回复和转发消息
watch([() => props.replyTo, () => props.forwardMessage], () => {
  if (visible.value) {
    initializeForm()
  }
})

// 初始化表单
const initializeForm = () => {
  if (props.replyTo) {
    // 回复邮件
    form.value.toAddresses = props.replyTo.fromAddress
    form.value.subject = props.replyTo.subject.startsWith('Re:') 
      ? props.replyTo.subject 
      : `Re: ${props.replyTo.subject}`
    
    // 如果是回复全部，添加原始收件人到抄送
    if ((props.replyTo as any).replyAll) {
      const originalTo = props.replyTo.toAddresses
      const originalCC = props.replyTo.ccAddresses
      
      if (originalCC) {
        form.value.ccAddresses = originalCC
        showCC.value = true
      }
    }
    
    // 添加引用内容
    const replyContent = generateReplyContent(props.replyTo)
    form.value.bodyHtml = replyContent
    form.value.bodyText = replyContent.replace(/<[^>]*>/g, '')
    
  } else if (props.forwardMessage) {
    // 转发邮件
    form.value.subject = props.forwardMessage.subject.startsWith('Fwd:') 
      ? props.forwardMessage.subject 
      : `Fwd: ${props.forwardMessage.subject}`
    
    // 添加转发内容
    const forwardContent = generateForwardContent(props.forwardMessage)
    form.value.bodyHtml = forwardContent
    form.value.bodyText = forwardContent.replace(/<[^>]*>/g, '')
    
  } else {
    // 新邮件
    resetForm()
  }
}

// 生成回复内容
const generateReplyContent = (originalMessage: EmailMessage) => {
  const senderName = getSenderName(originalMessage.fromAddress)
  const date = new Date(originalMessage.receivedAt).toLocaleString('zh-CN')
  
  return `
    <br><br>
    <div style="border-left: 3px solid #ccc; padding-left: 10px; margin-left: 10px;">
      <p><strong>发件人:</strong> ${originalMessage.fromAddress}</p>
      <p><strong>时间:</strong> ${date}</p>
      <p><strong>主题:</strong> ${originalMessage.subject}</p>
      <br>
      ${originalMessage.bodyHtml || originalMessage.bodyText || ''}
    </div>
  `
}

// 生成转发内容
const generateForwardContent = (originalMessage: EmailMessage) => {
  const date = new Date(originalMessage.receivedAt).toLocaleString('zh-CN')
  
  return `
    <br><br>
    <div style="border: 1px solid #ccc; padding: 10px;">
      <p><strong>---------- 转发邮件 ----------</strong></p>
      <p><strong>发件人:</strong> ${originalMessage.fromAddress}</p>
      <p><strong>收件人:</strong> ${originalMessage.toAddresses}</p>
      <p><strong>时间:</strong> ${date}</p>
      <p><strong>主题:</strong> ${originalMessage.subject}</p>
      <br>
      ${originalMessage.bodyHtml || originalMessage.bodyText || ''}
    </div>
  `
}

// 重置表单
const resetForm = () => {
  form.value = {
    fromAddress: '',
    toAddresses: '',
    ccAddresses: '',
    bccAddresses: '',
    replyTo: '',
    subject: '',
    bodyText: '',
    bodyHtml: '',
    priorityLevel: 3,
    scheduledAt: ''
  }
  
  attachments.value = []
  showCC.value = false
  showBCC.value = false
  showAdvancedOptions.value = false
}

// 加载用户别名
const loadUserAliases = async () => {
  try {
    const response = await emailAliasApi.getUserAliases()
    userAliases.value = response.data.aliases
    
    // 设置默认发件人
    if (userAliases.value.length > 0 && !form.value.fromAddress) {
      form.value.fromAddress = userAliases.value[0].fullEmail
    }
  } catch (error) {
    console.error('加载用户别名失败:', error)
  }
}

// 切换编辑器模式
const switchEditorMode = (mode: 'html' | 'text') => {
  editorMode.value = mode
  
  if (mode === 'text' && form.value.bodyHtml) {
    // 从HTML转换为纯文本
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = form.value.bodyHtml
    form.value.bodyText = tempDiv.textContent || tempDiv.innerText || ''
  } else if (mode === 'html' && form.value.bodyText) {
    // 从纯文本转换为HTML
    form.value.bodyHtml = form.value.bodyText.replace(/\n/g, '<br>')
  }
}

// 处理附件变化
const handleAttachmentChange = (file: any) => {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  
  attachments.value.push(file)
}

// 移除附件
const handleAttachmentRemove = (file: any) => {
  const index = attachments.value.findIndex(item => item.uid === file.uid)
  if (index > -1) {
    attachments.value.splice(index, 1)
  }
}

// 发送命令处理
const handleSendCommand = (command: string) => {
  switch (command) {
    case 'send':
      form.value.scheduledAt = ''
      sendEmail()
      break
    case 'schedule':
      showAdvancedOptions.value = true
      nextTick(() => {
        ElMessage.info('请设置发送时间后点击发送')
      })
      break
    case 'draft':
      saveDraft()
      break
  }
}

// 发送邮件
const sendEmail = async () => {
  // 表单验证
  if (!form.value.toAddresses.trim()) {
    ElMessage.error('请输入收件人')
    return
  }
  
  if (!form.value.subject.trim()) {
    ElMessage.error('请输入邮件主题')
    return
  }
  
  sending.value = true
  try {
    const formData = new FormData()
    
    // 添加表单数据
    Object.keys(form.value).forEach(key => {
      if (form.value[key]) {
        formData.append(key, form.value[key])
      }
    })
    
    // 添加附件
    attachments.value.forEach(file => {
      formData.append('attachments', file.raw)
    })
    
    await emailApi.sendEmail(formData)
    
    ElMessage.success('邮件发送成功')
    emit('sent')
    visible.value = false
    
  } catch (error) {
    ElMessage.error('邮件发送失败')
  } finally {
    sending.value = false
  }
}

// 保存草稿
const saveDraft = async () => {
  try {
    await emailApi.saveDraft(form.value)
    ElMessage.success('草稿保存成功')
  } catch (error) {
    ElMessage.error('保存草稿失败')
  }
}

// 关闭对话框
const handleClose = async () => {
  // 检查是否有未保存的内容
  const hasContent = form.value.subject.trim() || 
                    form.value.bodyText.trim() || 
                    form.value.bodyHtml.trim() ||
                    attachments.value.length > 0
  
  if (hasContent) {
    try {
      await ElMessageBox.confirm(
        '邮件内容尚未保存，是否保存为草稿？',
        '提示',
        {
          confirmButtonText: '保存草稿',
          cancelButtonText: '直接关闭',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
      
      await saveDraft()
      visible.value = false
    } catch (action) {
      if (action === 'cancel') {
        visible.value = false
      }
    }
  } else {
    visible.value = false
  }
}

// 工具方法
const getSenderName = (email: string) => {
  const match = email.match(/^(.+?)\s*</)
  return match ? match[1].trim() : email.split('@')[0]
}
</script>

<style scoped>
.email-composer-dialog {
  .el-dialog__body {
    padding: 20px;
    max-height: 70vh;
    overflow-y: auto;
  }
}

.composer-form {
  .el-form-item {
    margin-bottom: 16px;
  }
}

.alias-option {
  .alias-email {
    font-weight: 600;
  }
  
  .alias-name {
    font-size: 12px;
    color: #999;
  }
}

.composer-options {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}

.advanced-options {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.editor-toolbar {
  margin-bottom: 8px;
}

.html-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 300px;
}

.attachment-upload {
  width: 100%;
}

.composer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-left {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .email-composer-dialog {
    .el-dialog {
      width: 95% !important;
      margin-top: 5vh !important;
    }
  }
  
  .composer-footer {
    flex-direction: column;
    gap: 12px;
  }
  
  .footer-left,
  .footer-right {
    width: 100%;
    display: flex;
    justify-content: center;
  }
}
</style>