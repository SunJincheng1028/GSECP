<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="code">404</div>
    <p>页面不存在或已被移动</p>
    <el-button type="primary" @click="router.push('/dashboard')">返回工作台</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  min-height: 100vh;
  background: var(--gsecp-bg);
}

.code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

p {
  margin: 0 0 12px;
  color: var(--gsecp-muted);
}
</style>
