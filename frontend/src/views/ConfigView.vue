<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dictApi } from '@/api/dict'
import type { ConfigItem, DeliverableType } from '@/types'

const configs = ref<ConfigItem[]>([])
const types = ref<DeliverableType[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    ;[configs.value, types.value] = await Promise.all([dictApi.config(), dictApi.types()])
  } finally {
    loading.value = false
  }
}

async function saveConfig(row: ConfigItem): Promise<void> {
  await dictApi.updateConfig(row.key, row.value)
  ElMessage.success('配置已保存')
}

async function toggleType(row: DeliverableType): Promise<void> {
  await dictApi.setEnabled(row.code, row.enabled)
  ElMessage.success(row.enabled ? '类型已启用' : '类型已停用')
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">系统配置</h1>
    <p class="page-subtitle">维护系统参数与交付物类型字典；停用类型后教师新建步骤将无法选用</p>

    <el-row :gutter="18">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="card">
          <template #header><strong>系统参数</strong></template>
          <el-table v-loading="loading" :data="configs" size="small">
            <el-table-column prop="key" label="配置键" min-width="180" />
            <el-table-column label="配置值" min-width="130">
              <template #default="{ row }">
                <el-input v-model="row.value" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="saveConfig(row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="card">
          <template #header><strong>交付物类型字典</strong></template>
          <el-table v-loading="loading" :data="types" size="small">
            <el-table-column prop="name" label="类型" min-width="110" />
            <el-table-column prop="code" label="编码" min-width="130" />
            <el-table-column prop="frontendComponent" label="前端组件" min-width="140" />
            <el-table-column label="启用" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" @change="toggleType(row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.card {
  border: 1px solid var(--gsecp-border);
  border-radius: 14px;
}
</style>
