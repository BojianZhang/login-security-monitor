<template>
  <div class="rich-text-editor">
    <div class="editor-toolbar">
      <el-button-group>
        <el-button 
          size="small"
          :type="isActive('bold') ? 'primary' : ''"
          @click="execCommand('bold')">
          <strong>B</strong>
        </el-button>
        <el-button 
          size="small"
          :type="isActive('italic') ? 'primary' : ''"
          @click="execCommand('italic')">
          <em>I</em>
        </el-button>
        <el-button 
          size="small"
          :type="isActive('underline') ? 'primary' : ''"
          @click="execCommand('underline')">
          <u>U</u>
        </el-button>
      </el-button-group>
      
      <el-button-group style="margin-left: 8px;">
        <el-button 
          size="small"
          @click="execCommand('justifyLeft')">
          左对齐
        </el-button>
        <el-button 
          size="small"
          @click="execCommand('justifyCenter')">
          居中
        </el-button>
        <el-button 
          size="small"
          @click="execCommand('justifyRight')">
          右对齐
        </el-button>
      </el-button-group>
      
      <el-button-group style="margin-left: 8px;">
        <el-button 
          size="small"
          @click="execCommand('insertUnorderedList')">
          无序列表
        </el-button>
        <el-button 
          size="small"
          @click="execCommand('insertOrderedList')">
          有序列表
        </el-button>
      </el-button-group>
      
      <el-select 
        v-model="fontSize"
        size="small"
        style="width: 80px; margin-left: 8px;"
        @change="changeFontSize">
        <el-option label="12px" value="1" />
        <el-option label="14px" value="2" />
        <el-option label="16px" value="3" />
        <el-option label="18px" value="4" />
        <el-option label="24px" value="5" />
        <el-option label="32px" value="6" />
        <el-option label="48px" value="7" />
      </el-select>
      
      <input 
        type="color" 
        v-model="textColor"
        @change="changeTextColor"
        style="margin-left: 8px; width: 30px; height: 24px; border: none; cursor: pointer;" />
    </div>
    
    <div 
      ref="editorRef"
      class="editor-content"
      contenteditable="true"
      @input="handleInput"
      @paste="handlePaste"
      @keydown="handleKeydown"
      v-html="content">
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

// Props
interface Props {
  modelValue: string
  placeholder?: string
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入内容...',
  height: '300px'
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 响应式数据
const editorRef = ref<HTMLElement>()
const content = ref('')
const fontSize = ref('3')
const textColor = ref('#000000')

// 组件方法
onMounted(() => {
  content.value = props.modelValue
  if (editorRef.value) {
    editorRef.value.style.height = props.height
  }
})

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  if (newValue !== content.value) {
    content.value = newValue
    if (editorRef.value) {
      editorRef.value.innerHTML = newValue
    }
  }
})

// 处理输入
const handleInput = () => {
  if (editorRef.value) {
    content.value = editorRef.value.innerHTML
    emit('update:modelValue', content.value)
  }
}

// 处理粘贴
const handlePaste = (event: ClipboardEvent) => {
  event.preventDefault()
  
  const clipboardData = event.clipboardData
  if (clipboardData) {
    let pastedText = clipboardData.getData('text/plain')
    
    // 清理粘贴的文本，移除危险的标签和属性
    pastedText = pastedText.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    pastedText = pastedText.replace(/javascript:/gi, '')
    pastedText = pastedText.replace(/on\w+\s*=/gi, '')
    
    // 插入清理后的文本
    document.execCommand('insertHTML', false, pastedText.replace(/\n/g, '<br>'))
  }
}

// 处理键盘事件
const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl+B: 粗体
  if (event.ctrlKey && event.key === 'b') {
    event.preventDefault()
    execCommand('bold')
  }
  // Ctrl+I: 斜体
  else if (event.ctrlKey && event.key === 'i') {
    event.preventDefault()
    execCommand('italic')
  }
  // Ctrl+U: 下划线
  else if (event.ctrlKey && event.key === 'u') {
    event.preventDefault()
    execCommand('underline')
  }
}

// 执行编辑命令
const execCommand = (command: string, value?: string) => {
  document.execCommand(command, false, value)
  editorRef.value?.focus()
  handleInput()
}

// 检查样式是否激活
const isActive = (command: string) => {
  return document.queryCommandState(command)
}

// 改变字体大小
const changeFontSize = () => {
  execCommand('fontSize', fontSize.value)
}

// 改变文字颜色
const changeTextColor = () => {
  execCommand('foreColor', textColor.value)
}

// 插入链接
const insertLink = () => {
  const url = prompt('请输入链接地址:')
  if (url) {
    execCommand('createLink', url)
  }
}

// 插入图片
const insertImage = () => {
  const url = prompt('请输入图片地址:')
  if (url) {
    execCommand('insertImage', url)
  }
}
</script>

<style scoped>
.rich-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  flex-wrap: wrap;
  gap: 4px;
}

.editor-content {
  padding: 12px;
  min-height: 200px;
  max-height: 500px;
  overflow-y: auto;
  outline: none;
  line-height: 1.6;
  font-size: 14px;
  color: #333;
}

.editor-content:empty:before {
  content: attr(data-placeholder);
  color: #c0c4cc;
  font-style: italic;
}

.editor-content img {
  max-width: 100%;
  height: auto;
}

.editor-content table {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}

.editor-content table th,
.editor-content table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.editor-content table th {
  background-color: #f2f2f2;
  font-weight: bold;
}

.editor-content blockquote {
  margin: 10px 0;
  padding: 10px 20px;
  background: #f8f9fa;
  border-left: 4px solid #409eff;
  color: #666;
}

.editor-content ul,
.editor-content ol {
  padding-left: 20px;
  margin: 10px 0;
}

.editor-content li {
  margin: 5px 0;
}

.editor-content a {
  color: #409eff;
  text-decoration: none;
}

.editor-content a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .editor-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>