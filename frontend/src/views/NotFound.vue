<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <div class="error-title">页面未找到</div>
      <div class="error-description">
        抱歉，您访问的页面不存在或已被删除
      </div>
      <div class="error-actions">
        <el-button type="primary" @click="goHome">
          返回首页
        </el-button>
        <el-button @click="goBack">
          返回上一页
        </el-button>
      </div>
    </div>
    
    <div class="not-found-illustration">
      <el-icon class="illustration-icon">
        <QuestionFilled />
      </el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.not-found-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.not-found-content {
  text-align: center;
  max-width: 600px;
  
  .error-code {
    font-size: 120px;
    font-weight: bold;
    color: #1890ff;
    line-height: 1;
    margin-bottom: 20px;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  }
  
  .error-title {
    font-size: 32px;
    color: #333;
    margin-bottom: 16px;
    font-weight: 600;
  }
  
  .error-description {
    font-size: 16px;
    color: #666;
    margin-bottom: 40px;
    line-height: 1.6;
  }
  
  .error-actions {
    display: flex;
    justify-content: center;
    gap: 16px;
    
    .el-button {
      padding: 12px 32px;
      font-size: 16px;
    }
  }
}

.not-found-illustration {
  margin-left: 60px;
  
  .illustration-icon {
    font-size: 200px;
    color: #d9d9d9;
    opacity: 0.5;
  }
}

@media (max-width: 768px) {
  .not-found-container {
    flex-direction: column;
  }
  
  .not-found-content {
    .error-code {
      font-size: 80px;
    }
    
    .error-title {
      font-size: 24px;
    }
    
    .error-actions {
      flex-direction: column;
      align-items: center;
      
      .el-button {
        width: 200px;
      }
    }
  }
  
  .not-found-illustration {
    margin-left: 0;
    margin-top: 40px;
    
    .illustration-icon {
      font-size: 120px;
    }
  }
}
</style>