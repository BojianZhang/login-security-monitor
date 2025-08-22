<template>
  <div class="ssl-certificate-manager">
    <el-card class="page-header">
      <template #header>
        <div class="card-header">
          <span class="title">SSL/TLS 证书管理</span>
          <div class="header-actions">
            <el-button 
              type="primary" 
              icon="Plus"
              @click="showRequestDialog = true">
              申请证书
            </el-button>
            <el-button 
              type="success" 
              icon="Upload"
              @click="showUploadDialog = true">
              上传证书
            </el-button>
            <el-button 
              icon="Refresh"
              @click="loadCertificates">
              刷新
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 统计信息 -->
      <div class="statistics-grid">
        <div class="stat-card">
          <div class="stat-value">{{ statistics.totalCertificates }}</div>
          <div class="stat-label">总证书数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ statistics.activeCertificates }}</div>
          <div class="stat-label">活跃证书</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ statistics.expiredCertificates }}</div>
          <div class="stat-label">已过期</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ statistics.expiringSoonCertificates }}</div>
          <div class="stat-label">即将过期</div>
        </div>
      </div>
    </el-card>

    <!-- 证书列表 -->
    <el-card class="certificate-list">
      <template #header>
        <div class="list-header">
          <span>证书列表</span>
          <div class="list-filters">
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable>
              <el-option label="全部" value="" />
              <el-option label="活跃" value="ACTIVE" />
              <el-option label="待处理" value="PENDING" />
              <el-option label="已过期" value="EXPIRED" />
              <el-option label="错误" value="ERROR" />
            </el-select>
            <el-select v-model="filterType" placeholder="类型筛选" clearable>
              <el-option label="全部" value="" />
              <el-option label="Let's Encrypt" value="FREE_LETSENCRYPT" />
              <el-option label="ZeroSSL" value="FREE_ZEROSSL" />
              <el-option label="用户上传" value="USER_UPLOADED" />
              <el-option label="自签名" value="SELF_SIGNED" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredCertificates"
        v-loading="loading"
        stripe
        style="width: 100%">
        <el-table-column prop="domainName" label="域名" min-width="150" />
        <el-table-column prop="certificateName" label="证书名称" min-width="120" />
        <el-table-column prop="certificateType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getCertificateTypeColor(row.certificateType)">
              {{ getCertificateTypeText(row.certificateType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issuer" label="颁发者" min-width="150" show-overflow-tooltip />
        <el-table-column prop="expiresAt" label="过期时间" width="160">
          <template #default="{ row }">
            <div v-if="row.expiresAt">
              <div>{{ formatDate(row.expiresAt) }}</div>
              <div class="expiry-days" :class="getExpiryClass(row.expiresAt)">
                {{ getExpiryText(row.expiresAt) }}
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="autoRenew" label="自动续期" width="100">
          <template #default="{ row }">
            <el-switch 
              v-model="row.autoRenew" 
              @change="updateAutoRenew(row)"
              :disabled="!row.isFreeType" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small"
              @click="viewCertificate(row)"
              icon="View">
              查看
            </el-button>
            <el-button 
              v-if="row.isFreeType && row.status === 'ACTIVE'"
              type="warning" 
              size="small"
              @click="renewCertificate(row)"
              icon="Refresh">
              续期
            </el-button>
            <el-button 
              type="danger" 
              size="small"
              @click="deleteCertificate(row)"
              icon="Delete">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 申请证书对话框 -->
    <el-dialog
      v-model="showRequestDialog"
      title="申请免费SSL证书"
      width="600px"
      :close-on-click-modal="false">
      <el-form
        ref="requestFormRef"
        :model="requestForm"
        :rules="requestRules"
        label-width="120px">
        <el-form-item label="域名" prop="domainName">
          <el-input v-model="requestForm.domainName" placeholder="example.com" />
        </el-form-item>
        <el-form-item label="邮箱地址" prop="email">
          <el-input v-model="requestForm.email" placeholder="admin@example.com" />
        </el-form-item>
        <el-form-item label="验证方式" prop="challengeType">
          <el-radio-group v-model="requestForm.challengeType">
            <el-radio label="http-01">HTTP验证</el-radio>
            <el-radio label="dns-01">DNS验证</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="证书提供商">
          <el-radio-group v-model="requestForm.provider">
            <el-radio label="letsencrypt">Let's Encrypt</el-radio>
            <el-radio label="zerossl">ZeroSSL</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showRequestDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitCertificateRequest"
          :loading="requesting">
          申请证书
        </el-button>
      </template>
    </el-dialog>

    <!-- 上传证书对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传SSL证书"
      width="600px"
      :close-on-click-modal="false">
      <el-form
        ref="uploadFormRef"
        :model="uploadForm"
        :rules="uploadRules"
        label-width="120px">
        <el-form-item label="域名" prop="domainName">
          <el-input v-model="uploadForm.domainName" placeholder="example.com" />
        </el-form-item>
        <el-form-item label="证书名称" prop="certificateName">
          <el-input v-model="uploadForm.certificateName" placeholder="My SSL Certificate" />
        </el-form-item>
        <el-form-item label="证书文件" prop="certificateFile">
          <el-upload
            ref="certUploadRef"
            :auto-upload="false"
            :file-list="uploadForm.certificateFileList"
            accept=".pem,.crt,.cert"
            :limit="1"
            :on-change="handleCertFileChange">
            <el-button type="primary">选择证书文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 .pem, .crt, .cert 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="私钥文件" prop="privateKeyFile">
          <el-upload
            ref="keyUploadRef"
            :auto-upload="false"
            :file-list="uploadForm.privateKeyFileList"
            accept=".pem,.key"
            :limit="1"
            :on-change="handleKeyFileChange">
            <el-button type="primary">选择私钥文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 .pem, .key 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="证书链文件">
          <el-upload
            ref="chainUploadRef"
            :auto-upload="false"
            :file-list="uploadForm.chainFileList"
            accept=".pem,.crt"
            :limit="1"
            :on-change="handleChainFileChange">
            <el-button>选择证书链文件（可选）</el-button>
            <template #tip>
              <div class="el-upload__tip">可选，支持 .pem, .crt 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitCertificateUpload"
          :loading="uploading">
          上传证书
        </el-button>
      </template>
    </el-dialog>

    <!-- 证书详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="证书详情"
      width="800px">
      <div v-if="currentCertificate" class="certificate-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="域名">
            {{ currentCertificate.domainName }}
          </el-descriptions-item>
          <el-descriptions-item label="证书名称">
            {{ currentCertificate.certificateName }}
          </el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getCertificateTypeColor(currentCertificate.certificateType)">
              {{ getCertificateTypeText(currentCertificate.certificateType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusColor(currentCertificate.status)">
              {{ getStatusText(currentCertificate.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="颁发者">
            {{ currentCertificate.issuer || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="序列号">
            {{ currentCertificate.serialNumber || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="颁发时间">
            {{ formatDate(currentCertificate.issuedAt) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="过期时间">
            {{ formatDate(currentCertificate.expiresAt) || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="自动续期">
            {{ currentCertificate.autoRenew ? '是' : '否' }}
          </el-descriptions-item>
          <el-descriptions-item label="续期提前天数">
            {{ currentCertificate.renewalDaysBefore || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="ACME邮箱" v-if="currentCertificate.acmeAccountEmail">
            {{ currentCertificate.acmeAccountEmail }}
          </el-descriptions-item>
          <el-descriptions-item label="挑战类型" v-if="currentCertificate.challengeType">
            {{ currentCertificate.challengeType }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentCertificate.lastError" class="error-info">
          <h4>错误信息：</h4>
          <el-alert type="error" :closable="false">
            {{ currentCertificate.lastError }}
          </el-alert>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Refresh, View, Delete } from '@element-plus/icons-vue'
import { sslCertificateApi } from '@/api/ssl-certificate'

// 响应式数据
const loading = ref(false)
const requesting = ref(false)
const uploading = ref(false)
const certificates = ref([])
const statistics = ref({
  totalCertificates: 0,
  activeCertificates: 0,
  expiredCertificates: 0,
  expiringSoonCertificates: 0
})

// 筛选条件
const filterStatus = ref('')
const filterType = ref('')

// 对话框控制
const showRequestDialog = ref(false)
const showUploadDialog = ref(false)
const showDetailDialog = ref(false)
const currentCertificate = ref(null)

// 申请表单
const requestFormRef = ref()
const requestForm = ref({
  domainName: '',
  email: '',
  challengeType: 'http-01',
  provider: 'letsencrypt'
})

const requestRules = {
  domainName: [
    { required: true, message: '请输入域名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*$/, 
      message: '请输入有效的域名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

// 上传表单
const uploadFormRef = ref()
const uploadForm = ref({
  domainName: '',
  certificateName: '',
  certificateFile: null,
  privateKeyFile: null,
  chainFile: null,
  certificateFileList: [],
  privateKeyFileList: [],
  chainFileList: []
})

const uploadRules = {
  domainName: [
    { required: true, message: '请输入域名', trigger: 'blur' }
  ],
  certificateName: [
    { required: true, message: '请输入证书名称', trigger: 'blur' }
  ]
}

// 计算属性
const filteredCertificates = computed(() => {
  return certificates.value.filter(cert => {
    const statusMatch = !filterStatus.value || cert.status === filterStatus.value
    const typeMatch = !filterType.value || cert.certificateType === filterType.value
    return statusMatch && typeMatch
  })
})

// 组件方法
onMounted(() => {
  loadCertificates()
  loadStatistics()
})

// 加载证书列表
const loadCertificates = async () => {
  loading.value = true
  try {
    // 这里应该调用实际的API
    // const response = await sslCertificateApi.getAllCertificates()
    // certificates.value = response.data.certificates
    
    // 模拟数据
    certificates.value = [
      {
        id: 1,
        domainName: 'example.com',
        certificateName: 'Example SSL',
        certificateType: 'FREE_LETSENCRYPT',
        status: 'ACTIVE',
        issuer: 'Let\'s Encrypt Authority X3',
        expiresAt: '2024-06-15T10:30:00Z',
        autoRenew: true,
        isFreeType: true
      },
      {
        id: 2,
        domainName: 'test.wearehackerone.com',
        certificateName: 'HackerOne Test SSL',
        certificateType: 'USER_UPLOADED',
        status: 'ACTIVE',
        issuer: 'DigiCert Inc',
        expiresAt: '2024-12-31T23:59:59Z',
        autoRenew: false,
        isFreeType: false
      }
    ]
  } catch (error) {
    ElMessage.error('加载证书列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    // const response = await sslCertificateApi.getStatistics()
    // statistics.value = response.data.statistics
    
    // 模拟数据
    statistics.value = {
      totalCertificates: 15,
      activeCertificates: 12,
      expiredCertificates: 1,
      expiringSoonCertificates: 2
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 申请证书
const submitCertificateRequest = async () => {
  if (!requestFormRef.value) return
  
  const valid = await requestFormRef.value.validate()
  if (!valid) return
  
  requesting.value = true
  try {
    // const response = await sslCertificateApi.requestLetsEncryptCertificate(requestForm.value)
    ElMessage.success('证书申请成功，正在处理中...')
    showRequestDialog.value = false
    resetRequestForm()
    loadCertificates()
  } catch (error) {
    ElMessage.error('证书申请失败')
  } finally {
    requesting.value = false
  }
}

// 上传证书
const submitCertificateUpload = async () => {
  if (!uploadFormRef.value) return
  
  const valid = await uploadFormRef.value.validate()
  if (!valid) return
  
  if (!uploadForm.value.certificateFile || !uploadForm.value.privateKeyFile) {
    ElMessage.warning('请选择证书文件和私钥文件')
    return
  }
  
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('domainName', uploadForm.value.domainName)
    formData.append('certificateName', uploadForm.value.certificateName)
    formData.append('certificateFile', uploadForm.value.certificateFile)
    formData.append('privateKeyFile', uploadForm.value.privateKeyFile)
    if (uploadForm.value.chainFile) {
      formData.append('chainFile', uploadForm.value.chainFile)
    }
    
    // const response = await sslCertificateApi.uploadCertificate(formData)
    ElMessage.success('证书上传成功')
    showUploadDialog.value = false
    resetUploadForm()
    loadCertificates()
  } catch (error) {
    ElMessage.error('证书上传失败')
  } finally {
    uploading.value = false
  }
}

// 文件选择处理
const handleCertFileChange = (file) => {
  uploadForm.value.certificateFile = file.raw
}

const handleKeyFileChange = (file) => {
  uploadForm.value.privateKeyFile = file.raw
}

const handleChainFileChange = (file) => {
  uploadForm.value.chainFile = file.raw
}

// 查看证书详情
const viewCertificate = (certificate) => {
  currentCertificate.value = certificate
  showDetailDialog.value = true
}

// 续期证书
const renewCertificate = async (certificate) => {
  try {
    await ElMessageBox.confirm(
      `确定要续期证书 "${certificate.domainName}" 吗？`,
      '确认续期',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // const response = await sslCertificateApi.renewCertificate(certificate.id)
    ElMessage.success('证书续期成功')
    loadCertificates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('证书续期失败')
    }
  }
}

// 删除证书
const deleteCertificate = async (certificate) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除证书 "${certificate.domainName}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // const response = await sslCertificateApi.deleteCertificate(certificate.id)
    ElMessage.success('证书删除成功')
    loadCertificates()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('证书删除失败')
    }
  }
}

// 更新自动续期设置
const updateAutoRenew = async (certificate) => {
  try {
    // const response = await sslCertificateApi.updateAutoRenew(certificate.id, certificate.autoRenew)
    ElMessage.success('自动续期设置已更新')
  } catch (error) {
    ElMessage.error('更新失败')
    certificate.autoRenew = !certificate.autoRenew // 回滚
  }
}

// 重置表单
const resetRequestForm = () => {
  requestForm.value = {
    domainName: '',
    email: '',
    challengeType: 'http-01',
    provider: 'letsencrypt'
  }
}

const resetUploadForm = () => {
  uploadForm.value = {
    domainName: '',
    certificateName: '',
    certificateFile: null,
    privateKeyFile: null,
    chainFile: null,
    certificateFileList: [],
    privateKeyFileList: [],
    chainFileList: []
  }
}

// 工具方法
const getCertificateTypeText = (type: string) => {
  const types = {
    'FREE_LETSENCRYPT': 'Let\'s Encrypt',
    'FREE_ZEROSSL': 'ZeroSSL',
    'USER_UPLOADED': '用户上传',
    'SELF_SIGNED': '自签名'
  }
  return types[type] || type
}

const getCertificateTypeColor = (type: string) => {
  const colors = {
    'FREE_LETSENCRYPT': '',
    'FREE_ZEROSSL': 'success',
    'USER_UPLOADED': 'info',
    'SELF_SIGNED': 'warning'
  }
  return colors[type] || ''
}

const getStatusText = (status: string) => {
  const statuses = {
    'PENDING': '待处理',
    'ACTIVE': '活跃',
    'EXPIRED': '已过期',
    'REVOKED': '已吊销',
    'ERROR': '错误',
    'RENEWAL_NEEDED': '需要续期'
  }
  return statuses[status] || status
}

const getStatusColor = (status: string) => {
  const colors = {
    'PENDING': 'info',
    'ACTIVE': 'success',
    'EXPIRED': 'danger',
    'REVOKED': 'warning',
    'ERROR': 'danger',
    'RENEWAL_NEEDED': 'warning'
  }
  return colors[status] || ''
}

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN')
}

const getExpiryText = (expiryDate: string) => {
  const now = new Date()
  const expiry = new Date(expiryDate)
  const diffTime = expiry.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays < 0) {
    return `已过期 ${Math.abs(diffDays)} 天`
  } else if (diffDays === 0) {
    return '今天过期'
  } else if (diffDays === 1) {
    return '明天过期'
  } else {
    return `${diffDays} 天后过期`
  }
}

const getExpiryClass = (expiryDate: string) => {
  const now = new Date()
  const expiry = new Date(expiryDate)
  const diffTime = expiry.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays < 0) {
    return 'expired'
  } else if (diffDays <= 7) {
    return 'critical'
  } else if (diffDays <= 30) {
    return 'warning'
  } else {
    return 'normal'
  }
}
</script>

<style scoped>
.ssl-certificate-manager {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.certificate-list {
  margin-top: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-filters {
  display: flex;
  gap: 10px;
}

.expiry-days {
  font-size: 12px;
  margin-top: 4px;
}

.expiry-days.normal {
  color: #67c23a;
}

.expiry-days.warning {
  color: #e6a23c;
}

.expiry-days.critical {
  color: #f56c6c;
}

.expiry-days.expired {
  color: #f56c6c;
  font-weight: bold;
}

.certificate-detail {
  padding: 20px 0;
}

.error-info {
  margin-top: 20px;
}

.error-info h4 {
  margin-bottom: 10px;
  color: #f56c6c;
}

@media (max-width: 768px) {
  .statistics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .header-actions {
    flex-direction: column;
    gap: 5px;
  }
  
  .list-filters {
    flex-direction: column;
    gap: 5px;
  }
}
</style>