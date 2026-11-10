<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type UploadRawFile } from 'element-plus'
import { submissionApi } from '@/api/submission'
import { acceptOf, effectiveFormats, type ItemConstraints } from './constraints'
import type { SubmissionItemInput } from '@/types'

const props = defineProps<{
  modelValue: SubmissionItemInput
  constraints: ItemConstraints
  typeCode?: string
  initialName?: string
  disabled?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: SubmissionItemInput] }>()

const fileName = ref(props.initialName ?? '')
const uploading = ref(false)

const accept = computed(() => acceptOf(props.constraints, props.typeCode ?? ''))

watch(
  () => props.initialName,
  (value) => {
    fileName.value = value ?? ''
  },
)
watch(
  () => props.modelValue.fileId,
  (value) => {
    if (value == null) {
      fileName.value = ''
    }
  },
)

function emitChange(patch: Partial<SubmissionItemInput>): void {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

function beforeUpload(file: UploadRawFile): boolean {
  void doUpload(file)
  return false
}

async function doUpload(file: File): Promise<void> {
  const formats = effectiveFormats(props.constraints, props.typeCode ?? '')
  const extension = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : ''
  if (formats.length && !formats.includes(extension)) {
    ElMessage.error(`仅支持 ${formats.join(' / ')} 格式`)
    return
  }
  const limit = props.constraints.maxSizeMb * 1024 * 1024
  if (file.size > limit) {
    ElMessage.error(`文件不能超过 ${props.constraints.maxSizeMb} MB`)
    return
  }

  uploading.value = true
  try {
    const stored = await submissionApi.upload(file)
    fileName.value = stored.originalName
    emitChange({ fileId: stored.id, textValue: null, urlValue: null })
    ElMessage.success('附件上传成功，提交后生效')
  } catch {
    // 请求拦截器已统一提示错误，此处避免产生未处理的 Promise 异常
  } finally {
    uploading.value = false
  }
}

function clear(): void {
  fileName.value = ''
  emitChange({ fileId: null })
}
</script>

<template>
  <div class="file-item">
    <el-upload
      :show-file-list="false"
      :accept="accept"
      :disabled="disabled || uploading"
      :before-upload="beforeUpload"
    >
      <el-button :loading="uploading" :disabled="disabled" type="primary" plain>
        <el-icon style="margin-right: 6px"><UploadFilled /></el-icon>
        {{ modelValue.fileId ? '重新上传' : '选择文件并上传' }}
      </el-button>
    </el-upload>
    <div v-if="modelValue.fileId" class="file-chip">
      <el-icon><Document /></el-icon>
      <span class="file-name">{{ fileName || `附件 #${modelValue.fileId}` }}</span>
      <el-button v-if="!disabled" link type="danger" @click="clear">移除</el-button>
    </div>
  </div>
</template>

<style scoped>
.file-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.file-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 4px 8px;
  border: 1px solid var(--gsecp-border);
  border-radius: 8px;
  background: #f9fafb;
  font-size: 13px;
}

.file-name {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
