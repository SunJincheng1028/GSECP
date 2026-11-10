<script setup lang="ts">
import { computed, type Component } from 'vue'
import FileUploadItem from './deliverables/FileUploadItem.vue'
import UrlInputItem from './deliverables/UrlInputItem.vue'
import TextAreaItem from './deliverables/TextAreaItem.vue'
import { constraintHint, parseConstraints } from './deliverables/constraints'
import type { DeliverableItemView, SubmissionItemInput } from '@/types'

const props = defineProps<{
  item: DeliverableItemView
  modelValue: SubmissionItemInput
  initialName?: string
  disabled?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: SubmissionItemInput] }>()

/** 字典 frontend_component -> 前端组件；新增类型只需在此注册，未注册回退通用附件 */
const registry: Record<string, Component> = {
  FileUploadItem,
  UrlInputItem,
  TextAreaItem,
  ImageUploadItem: FileUploadItem,
}

const current = computed<Component>(
  () => registry[props.item.frontendComponent] ?? FileUploadItem,
)
const constraints = computed(() => parseConstraints(props.item.constraintJson))
const hint = computed(() =>
  constraintHint(constraints.value, props.item.frontendComponent, props.item.typeCode),
)

const extraProps = computed<Record<string, unknown>>(() => {
  if (current.value === FileUploadItem) {
    return {
      constraints: constraints.value,
      typeCode: props.item.typeCode,
      initialName: props.initialName,
    }
  }
  if (current.value === TextAreaItem) {
    return { constraints: constraints.value }
  }
  return {}
})
</script>

<template>
  <div class="deliverable">
    <div class="head">
      <span class="name">{{ item.name }}</span>
      <el-tag size="small" effect="plain">{{ item.typeName }}</el-tag>
      <el-tag v-if="item.required" size="small" type="danger" effect="light">必填</el-tag>
      <el-tag v-else size="small" type="info" effect="light">选填</el-tag>
    </div>
    <p class="hint">{{ hint }}</p>
    <component
      :is="current"
      v-bind="extraProps"
      :model-value="modelValue"
      :disabled="disabled"
      @update:model-value="emit('update:modelValue', $event)"
    />
  </div>
</template>

<style scoped>
.deliverable {
  padding: 16px 18px;
  border: 1px solid var(--gsecp-border);
  border-radius: 12px;
  background: #fff;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.name {
  font-size: 14px;
  font-weight: 600;
}

.hint {
  margin: 8px 0 14px;
  color: var(--gsecp-muted);
  font-size: 12px;
}
</style>
