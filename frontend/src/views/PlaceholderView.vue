<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta.title ?? '功能占位')
const description = computed(
  () => route.meta.description ?? '该功能将在后续迭代中实现。',
)
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">{{ title }}</h1>
    <p class="page-subtitle">{{ description }}</p>
    <el-card class="placeholder" shadow="never">
      <el-empty description="当前为迭代 0 工程基座，页面内容待实现" :image-size="120" />
    </el-card>
  </div>
</template>

<style scoped>
.placeholder {
  border: 1px solid var(--gsecp-border);
  border-radius: 14px;
}
</style>
