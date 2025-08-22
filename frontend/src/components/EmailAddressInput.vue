<template>
  <div class="email-address-input">
    <el-input
      v-model="displayValue"
      :placeholder="placeholder"
      @input="handleInput"
      @blur="handleBlur"
      @focus="handleFocus">
      <template #suffix>
        <el-dropdown 
          v-if="suggestions.length > 0 && showSuggestions"
          trigger="click"
          placement="bottom-end">
          <el-icon class="suggestion-icon"><ArrowDown /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="suggestion in suggestions"
                :key="suggestion.email"
                @click="selectSuggestion(suggestion)">
                <div class="suggestion-item">
                  <div class="suggestion-email">{{ suggestion.email }}</div>
                  <div class="suggestion-name">{{ suggestion.name }}</div>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </el-input>
    
    <!-- 地址标签 -->
    <div v-if="addresses.length > 0" class="address-tags">
      <el-tag
        v-for="(address, index) in addresses"
        :key="index"
        :type="validateEmail(address) ? '' : 'danger'"
        closable
        @close="removeAddress(index)">
        {{ address }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { contactApi } from '@/api/contact'

// Props
interface Props {
  modelValue: string
  placeholder?: string
  multiple?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '输入邮箱地址',
  multiple: true
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 响应式数据
const displayValue = ref('')
const showSuggestions = ref(false)
const suggestions = ref<Array<{ email: string; name: string }>>([])

// 计算属性
const addresses = computed(() => {
  if (!props.modelValue) return []
  return props.modelValue.split(',').map(addr => addr.trim()).filter(addr => addr)
})

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  if (addresses.value.length === 0) {
    displayValue.value = newValue || ''
  } else {
    displayValue.value = ''
  }
})

// 处理输入
const handleInput = async (value: string) => {
  displayValue.value = value
  
  // 如果输入包含逗号或分号，自动分割地址
  if (value.includes(',') || value.includes(';')) {
    const newAddresses = value.split(/[,;]/).map(addr => addr.trim()).filter(addr => addr)
    if (newAddresses.length > 0) {
      addAddresses(newAddresses)
      displayValue.value = ''
    }
  } else if (value.length > 2) {
    // 获取建议
    await loadSuggestions(value)
  }
}

// 处理失焦
const handleBlur = () => {
  setTimeout(() => {
    showSuggestions.value = false
    
    // 如果有输入值，添加为地址
    if (displayValue.value.trim()) {
      addAddress(displayValue.value.trim())
      displayValue.value = ''
    }
  }, 200)
}

// 处理获焦
const handleFocus = () => {
  if (suggestions.value.length > 0) {
    showSuggestions.value = true
  }
}

// 添加地址
const addAddress = (address: string) => {
  if (!address) return
  
  const currentAddresses = addresses.value
  if (!currentAddresses.includes(address)) {
    currentAddresses.push(address)
    updateValue(currentAddresses)
  }
}

// 批量添加地址
const addAddresses = (newAddresses: string[]) => {
  const currentAddresses = addresses.value
  const uniqueAddresses = [...new Set([...currentAddresses, ...newAddresses])]
  updateValue(uniqueAddresses)
}

// 移除地址
const removeAddress = (index: number) => {
  const currentAddresses = [...addresses.value]
  currentAddresses.splice(index, 1)
  updateValue(currentAddresses)
}

// 选择建议
const selectSuggestion = (suggestion: { email: string; name: string }) => {
  addAddress(suggestion.email)
  displayValue.value = ''
  showSuggestions.value = false
}

// 更新值
const updateValue = (addressList: string[]) => {
  emit('update:modelValue', addressList.join(', '))
}

// 加载建议
const loadSuggestions = async (query: string) => {
  try {
    const response = await contactApi.searchContacts(query)
    suggestions.value = response.data.contacts.map((contact: any) => ({
      email: contact.email,
      name: contact.name || contact.email.split('@')[0]
    }))
    
    showSuggestions.value = suggestions.value.length > 0
  } catch (error) {
    suggestions.value = []
    showSuggestions.value = false
  }
}

// 验证邮箱格式
const validateEmail = (email: string) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}
</script>

<style scoped>
.email-address-input {
  .suggestion-icon {
    cursor: pointer;
    color: #909399;
  }
}

.address-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.suggestion-item {
  .suggestion-email {
    font-weight: 600;
  }
  
  .suggestion-name {
    font-size: 12px;
    color: #999;
  }
}
</style>