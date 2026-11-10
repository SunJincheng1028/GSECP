<script setup lang="ts">
import { computed } from 'vue'
import type { ItemConstraints } from './constraints'
import type { SubmissionItemInput } from '@/types'

const props = defineProps<{
  modelValue: SubmissionItemInput
  constraints: ItemConstraints
  disabled?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: SubmissionItemInput] }>()

const value = computed({
  get: () => props.modelValue.textValue ?? '',
  set: (text: string) =>
    emit('update:modelValue', {
      ...props.modelValue,
      textValue: text,
      fileId: null,
      urlValue: null,
    }),
})

const length = computed(() => value.value.trim().length)
const tooShort = computed(() => length.value > 0 && length.value < props.constraints.minWords)
const tooLong = computed(() => length.value > props.constraints.maxWords)
</script>

<template>
  <div class="text-item">
    <el-input
      v-model="value"
      type="textarea"
      :rows="5"
      :disabled="disabled"
      resize="vertical"
      placeholder="请输入说明内容"
    />
    <div class="counter" :class="{ warn: tooShort || tooLong }">
      {{ length }} 字（要求 {{ constraints.minWords }} ~ {{ constraints.maxWords }} 字）
    </div>
  </div>
</template>

<style scoped>
.counter {
  margin-top: 6px;
  color: var(--gsecp-muted);
  font-size: 12px;
  text-align: right;
}

.counter.warn {
  color: #dc2626;
}
</style>
