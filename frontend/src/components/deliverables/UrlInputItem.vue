<script setup lang="ts">
import { computed } from 'vue'
import type { SubmissionItemInput } from '@/types'

const props = defineProps<{
  modelValue: SubmissionItemInput
  disabled?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: SubmissionItemInput] }>()

const value = computed({
  get: () => props.modelValue.urlValue ?? '',
  set: (text: string) =>
    emit('update:modelValue', {
      ...props.modelValue,
      urlValue: text,
      textValue: null,
      fileId: null,
    }),
})
</script>

<template>
  <el-input v-model="value" :disabled="disabled" placeholder="https://github.com/your-name/your-repo">
    <template #prefix><el-icon><Link /></el-icon></template>
  </el-input>
</template>
