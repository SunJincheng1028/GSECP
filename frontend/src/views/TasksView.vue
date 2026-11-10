<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { stepApi } from '@/api/step'
import { submissionApi } from '@/api/submission'
import { useCourseSelection } from '@/composables/useCourseSelection'
import { emptyInput } from '@/components/deliverables/constraints'
import DeliverableInput from '@/components/DeliverableInput.vue'
import SubmissionDetail from '@/components/SubmissionDetail.vue'
import type { StepView, SubmissionItemInput, SubmissionView } from '@/types'

const { state, loadClasses, loadCourses } = useCourseSelection()

const steps = ref<StepView[]>([])
const loading = ref(false)
const submitting = ref(false)
const activeStepId = ref<number | undefined>(undefined)
const inputs = ref<SubmissionItemInput[]>([])
const fileNames = reactive<Record<number, string>>({})
const latest = ref<SubmissionView | null>(null)
const versions = ref<SubmissionView[]>([])
const versionVisible = ref(false)
const versionDetail = ref<SubmissionView | null>(null)

const activeStep = computed(
  () => steps.value.find((item) => item.id === activeStepId.value) ?? null,
)
const canSubmit = computed(() => {
  const step = activeStep.value
  if (!step || step.status !== 'PUBLISHED') return false
  if (latest.value?.status === 'GRADED' && !step.allowResubmit) return false
  return true
})

const statusLabels: Record<string, string> = {
  SUBMITTED: '已提交',
  LATE: '迟交',
  GRADED: '已批改',
  RETURNED: '已退回',
  DRAFT: '未开放',
  PUBLISHED: '进行中',
  WITHDRAWN: '已撤回',
}

async function loadSteps(): Promise<void> {
  steps.value = []
  latest.value = null
  versions.value = []
  inputs.value = []
  if (state.courseId == null) return
  loading.value = true
  try {
    steps.value = await stepApi.list(state.courseId)
    activeStepId.value = steps.value[0]?.id
    if (activeStepId.value != null) {
      await selectStep(activeStepId.value)
    }
  } finally {
    loading.value = false
  }
}

async function handleClassChange(): Promise<void> {
  await loadCourses()
  await loadSteps()
}

async function loadMySubmission(): Promise<void> {
  if (activeStepId.value == null) return
  const [latestValue, history] = await Promise.all([
    submissionApi.mySubmission(activeStepId.value),
    submissionApi.myVersions(activeStepId.value),
  ])
  latest.value = latestValue
  versions.value = history
}

async function selectStep(stepId: number): Promise<void> {
  activeStepId.value = stepId
  const step = steps.value.find((item) => item.id === stepId)
  inputs.value = (step?.items ?? []).map((item) => emptyInput(item.id))
  for (const key of Object.keys(fileNames)) {
    delete fileNames[Number(key)]
  }
  await loadMySubmission()
}

function setInput(index: number, value: SubmissionItemInput): void {
  inputs.value[index] = value
}

function hasContent(input: SubmissionItemInput): boolean {
  return Boolean(
    (input.textValue && input.textValue.trim()) ||
      input.fileId != null ||
      (input.urlValue && input.urlValue.trim()),
  )
}

function loadLastVersion(): void {
  if (!latest.value || !activeStep.value) return
  const previous = new Map(
    latest.value.items.map((item) => [item.deliverableItemId, item]),
  )
  inputs.value = activeStep.value.items.map((item) => {
    const old = previous.get(item.id)
    if (old?.fileId != null) {
      fileNames[item.id] = old.originalName ?? ''
    }
    return {
      deliverableItemId: item.id,
      textValue: old?.textValue ?? null,
      fileId: old?.fileId ?? null,
      urlValue: old?.urlValue ?? null,
    }
  })
  ElMessage.success('已载入上一版内容')
}

async function submit(): Promise<void> {
  const step = activeStep.value
  if (!step || !canSubmit.value || activeStepId.value == null) return

  for (let index = 0; index < step.items.length; index += 1) {
    const item = step.items[index]
    const input = inputs.value[index]
    if (item.required && (!input || !hasContent(input))) {
      ElMessage.warning(`请完成必填交付项「${item.name}」`)
      return
    }
  }
  const payload = inputs.value.filter(hasContent)
  if (!payload.length) {
    ElMessage.warning('请至少填写一个交付项')
    return
  }

  submitting.value = true
  try {
    await submissionApi.submit(activeStepId.value, payload)
    ElMessage.success('提交成功，已生成新版本')
    await loadSteps()
  } finally {
    submitting.value = false
  }
}

function openVersion(submission: SubmissionView): void {
  versionDetail.value = submission
  versionVisible.value = true
}

onMounted(async () => {
  await loadClasses()
  await loadCourses()
  await loadSteps()
})
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">我的任务</h1>
    <p class="page-subtitle">按步骤查看交付要求，逐项上传文档、代码包或填写仓库地址与说明</p>

    <div class="filters">
      <el-select
        v-model="state.classId"
        placeholder="选择班级"
        style="width: 220px"
        @change="handleClassChange"
      >
        <el-option v-for="item in state.classes" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select
        v-model="state.courseId"
        placeholder="选择课程"
        style="width: 240px"
        @change="loadSteps"
      >
        <el-option v-for="item in state.courses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :md="8">
        <el-card v-loading="loading" shadow="never" class="card">
          <template #header><strong>课程步骤（{{ steps.length }}）</strong></template>
          <div
            v-for="step in steps"
            :key="step.id"
            class="step-item"
            :class="{ active: step.id === activeStepId }"
            @click="selectStep(step.id)"
          >
            <div class="step-title">
              <span>{{ step.sortOrder }}. {{ step.title }}</span>
              <el-tag size="small" effect="plain">{{ step.weight }}%</el-tag>
            </div>
            <div class="step-meta">
              <span>{{ step.items.length }} 个交付项</span>
              <span v-if="step.dueAt">截止 {{ step.dueAt }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && !steps.length" description="暂无已发布步骤" :image-size="80" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <template v-if="activeStep">
          <el-card shadow="never" class="card">
            <template #header>
              <div class="detail-head">
                <strong>{{ activeStep.title }}</strong>
                <el-tag size="small" :type="activeStep.status === 'PUBLISHED' ? 'success' : 'info'">
                  {{ statusLabels[activeStep.status] ?? activeStep.status }}
                </el-tag>
              </div>
            </template>
            <el-alert
              v-if="activeStep.requirement"
              class="requirement"
              type="info"
              :closable="false"
              title="提交要求"
              :description="activeStep.requirement"
              show-icon
            />
            <p v-if="activeStep.description" class="description">{{ activeStep.description }}</p>

            <div class="items-title">
              <strong>交付项</strong>
              <div>
                <el-button
                  v-if="latest"
                  size="small"
                  plain
                  @click="loadLastVersion"
                >
                  载入上一版
                </el-button>
              </div>
            </div>

            <div class="inputs">
              <DeliverableInput
                v-for="(item, index) in activeStep.items"
                :key="item.id"
                :item="item"
                :model-value="inputs[index]"
                :initial-name="fileNames[item.id]"
                :disabled="!canSubmit"
                @update:model-value="setInput(index, $event)"
              />
            </div>

            <div class="submit-bar">
              <span v-if="!canSubmit" class="tip">
                {{ activeStep.status !== 'PUBLISHED'
                  ? '该步骤未发布，暂不可提交'
                  : '该步骤已批改且未开启补交' }}
              </span>
              <span v-else class="tip">提交后将生成新版本，旧版本仍会保留</span>
              <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">
                提交成果
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="card section-card">
            <template #header><strong>我的提交记录</strong></template>
            <div v-if="latest" class="latest">
              <div class="latest-head">
                <span>最新版本：第 {{ latest.version }} 版</span>
                <el-tag size="small" :type="latest.status === 'LATE' ? 'warning' : 'success'">
                  {{ statusLabels[latest.status] ?? latest.status }}
                </el-tag>
                <el-tag v-if="latest.score != null" size="small" type="primary">
                  {{ latest.score }} 分
                </el-tag>
              </div>
              <SubmissionDetail :submission="latest" />
            </div>
            <el-empty v-else description="尚未提交成果" :image-size="80" />

            <div v-if="versions.length" class="versions-timeline">
              <strong>提交进度时间线</strong>
              <el-timeline>
                <el-timeline-item
                  v-for="version in versions"
                  :key="version.id"
                  :timestamp="version.submitAt"
                  placement="top"
                  :type="version.latest ? 'primary' : 'info'"
                >
                  <div class="timeline-row">
                    <span>
                      第 {{ version.version }} 版 · {{ statusLabels[version.status] ?? version.status }}
                    </span>
                    <el-tag v-if="version.score != null" size="small" type="primary">
                      {{ version.score }} 分
                    </el-tag>
                    <el-button link type="primary" @click="openVersion(version)">查看详情</el-button>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>
        </template>

        <el-card v-else shadow="never" class="card">
          <el-empty description="请选择左侧步骤查看交付要求" :image-size="110" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="versionVisible" title="历史版本详情" width="720" top="8vh">
      <SubmissionDetail v-if="versionDetail" :submission="versionDetail" />
      <template #footer><el-button @click="versionVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.card {
  border: 1px solid var(--gsecp-border);
  border-radius: 14px;
}

.step-item {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--gsecp-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.step-item:hover {
  border-color: var(--gsecp-primary-light);
}

.step-item.active {
  border-color: var(--gsecp-primary);
  background: #f5f7ff;
}

.step-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.step-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: var(--gsecp-muted);
  font-size: 12px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.requirement {
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.description {
  margin: 0 0 16px;
  color: #374151;
  font-size: 13px;
  line-height: 1.7;
}

.items-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.inputs {
  display: grid;
  gap: 12px;
}

.submit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--gsecp-border);
}

.tip {
  color: var(--gsecp-muted);
  font-size: 12px;
}

.section-card {
  margin-top: 18px;
}

.latest {
  display: grid;
  gap: 12px;
}

.latest-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.versions-timeline {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed var(--gsecp-border);
  font-size: 13px;
}

.versions-timeline :deep(.el-timeline) {
  margin-top: 14px;
  padding-left: 4px;
}

.timeline-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
</style>
