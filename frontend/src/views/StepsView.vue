<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dictApi } from '@/api/dict'
import { stepApi } from '@/api/step'
import { useCourseSelection } from '@/composables/useCourseSelection'
import { constraintTemplates } from '@/components/deliverables/constraints'
import type { DeliverableType, StepSaveRequest, StepView } from '@/types'

interface ItemEdit {
  name: string
  typeCode: string
  required: boolean
  constraintJson: string
}

const route = useRoute()
const router = useRouter()
const { state, loadClasses, loadCourses, selectCourseId } = useCourseSelection()

const steps = ref<StepView[]>([])
const types = ref<DeliverableType[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  title: '',
  description: '',
  requirement: '',
  weight: 0,
  publishAt: '',
  dueAt: '',
  status: 'DRAFT',
  allowResubmit: false,
  sortOrder: 0,
  items: [] as ItemEdit[],
})

const enabledTypes = computed(() => types.value.filter((item) => item.enabled))
const totalWeight = computed(() =>
  steps.value.reduce((sum, item) => sum + Number(item.weight ?? 0), 0),
)
const stepStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  WITHDRAWN: '已撤回',
}

function toLocalInput(value: string | null): string {
  return value ? value.replace(' ', 'T').slice(0, 16) : ''
}

async function loadSteps(): Promise<void> {
  steps.value = []
  if (state.courseId == null) return
  loading.value = true
  try {
    steps.value = await stepApi.list(state.courseId)
  } finally {
    loading.value = false
  }
}

async function handleClassChange(): Promise<void> {
  await loadCourses()
  await loadSteps()
}

function newItem(): ItemEdit {
  return { name: '', typeCode: enabledTypes.value[0]?.code ?? 'DOCUMENT', required: true, constraintJson: '' }
}

function resetForm(): void {
  Object.assign(form, {
    title: '',
    description: '',
    requirement: '',
    weight: 0,
    publishAt: '',
    dueAt: '',
    status: 'DRAFT',
    allowResubmit: false,
    sortOrder: steps.value.length + 1,
  })
  form.items = [newItem()]
}

function openCreate(): void {
  if (state.courseId == null) {
    ElMessage.warning('请先选择课程')
    return
  }
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: StepView): void {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    description: row.description ?? '',
    requirement: row.requirement ?? '',
    weight: Number(row.weight ?? 0),
    publishAt: toLocalInput(row.publishAt),
    dueAt: toLocalInput(row.dueAt),
    status: row.status,
    allowResubmit: row.allowResubmit,
    sortOrder: row.sortOrder,
  })
  form.items = row.items.map((item) => ({
    name: item.name,
    typeCode: item.typeCode,
    required: item.required,
    constraintJson: item.constraintJson ?? '',
  }))
  dialogVisible.value = true
}

function addItem(): void {
  form.items.push(newItem())
}

function removeItem(index: number): void {
  form.items.splice(index, 1)
}

function moveItem(index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= form.items.length) return
  const [item] = form.items.splice(index, 1)
  form.items.splice(target, 0, item)
}

function applyTemplate(index: number, value: unknown): void {
  form.items[index].constraintJson = String(value ?? '')
}

async function save(): Promise<void> {
  if (!form.title.trim()) {
    ElMessage.warning('请填写步骤标题')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('至少配置一个交付项')
    return
  }
  for (const item of form.items) {
    if (!item.name.trim() || !item.typeCode) {
      ElMessage.warning('请填写每个交付项的名称与类型')
      return
    }
    if (item.constraintJson.trim()) {
      try {
        JSON.parse(item.constraintJson)
      } catch {
        ElMessage.error(`「${item.name}」的约束配置不是合法 JSON`)
        return
      }
    }
  }

  const payload: StepSaveRequest = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    requirement: form.requirement.trim() || null,
    weight: Number(form.weight) || 0,
    publishAt: form.publishAt || null,
    dueAt: form.dueAt || null,
    status: form.status,
    allowResubmit: form.allowResubmit,
    sortOrder: Number(form.sortOrder) || 0,
    items: form.items.map((item, index) => ({
      name: item.name.trim(),
      typeCode: item.typeCode,
      required: item.required,
      constraintJson: item.constraintJson.trim() || null,
      sortOrder: index,
    })),
  }

  saving.value = true
  try {
    if (editingId.value == null) {
      await stepApi.create(state.courseId!, payload)
    } else {
      await stepApi.update(editingId.value, payload)
    }
    ElMessage.success('步骤已保存')
    dialogVisible.value = false
    await loadSteps()
  } finally {
    saving.value = false
  }
}

async function setStatus(row: StepView, status: string): Promise<void> {
  await stepApi.setStatus(row.id, status)
  ElMessage.success(status === 'PUBLISHED' ? '步骤已发布，学生可以提交' : '步骤已撤回')
  await loadSteps()
}

function goProgress(row: StepView): void {
  void router.push({ path: '/teaching/grading', query: { stepId: String(row.id) } })
}

function goGrades(): void {
  if (state.courseId == null) return
  void router.push({ path: '/teaching/grades', query: { courseId: String(state.courseId) } })
}

onMounted(async () => {
  await loadClasses()
  types.value = await dictApi.types()
  const queryCourseId = route.query.courseId
  if (typeof queryCourseId === 'string' && queryCourseId) {
    await selectCourseId(Number(queryCourseId))
  } else {
    await loadCourses()
  }
  await loadSteps()
})
</script>

<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">步骤与交付项</h1>
        <p class="page-subtitle">按步骤配置文档、代码包、仓库地址、文本等差异化交付要求及约束</p>
      </div>
      <div class="head-actions">
        <el-button @click="goGrades">查看总评</el-button>
        <el-button type="primary" @click="openCreate">新建步骤</el-button>
      </div>
    </div>

    <el-card shadow="never" class="card">
      <div class="filters">
        <el-select
          v-model="state.classId"
          placeholder="选择班级"
          style="width: 220px"
          :loading="state.classesLoading"
          @change="handleClassChange"
        >
          <el-option v-for="item in state.classes" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select
          v-model="state.courseId"
          placeholder="选择课程"
          style="width: 240px"
          :loading="state.coursesLoading"
          @change="loadSteps"
        >
          <el-option v-for="item in state.courses" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <span class="weight-tip">
          已配置权重合计：<strong :class="{ warn: totalWeight !== 100 }">{{ totalWeight }}%</strong>
          <span v-if="totalWeight !== 100">（建议合计 100%）</span>
        </span>
      </div>

      <el-table v-loading="loading" :data="steps" border stripe>
        <el-table-column prop="sortOrder" label="顺序" width="66" />
        <el-table-column prop="title" label="步骤" min-width="160" />
        <el-table-column label="交付项" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="item in row.items" :key="item.id" size="small" effect="plain" class="item-tag">
              {{ item.name }}
            </el-tag>
            <span v-if="!row.items.length" class="muted">未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="权重" width="80">
          <template #default="{ row }">{{ row.weight }}%</template>
        </el-table-column>
        <el-table-column prop="dueAt" label="截止时间" min-width="160">
          <template #default="{ row }">{{ row.dueAt || '不限' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ stepStatusLabels[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submissionCount" label="提交数" width="80" />
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="goProgress(row)">进度/批改</el-button>
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              link
              type="success"
              @click="setStatus(row, 'PUBLISHED')"
            >
              发布
            </el-button>
            <el-button v-else link type="warning" @click="setStatus(row, 'WITHDRAWN')">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !steps.length" description="该课程暂无步骤，点击「新建步骤」开始配置" :image-size="100" />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId == null ? '新建步骤' : '编辑步骤'"
      width="900"
      top="5vh"
    >
      <el-form label-position="top">
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="步骤标题" required>
              <el-input v-model="form.title" placeholder="如：需求分析与选题" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="6">
            <el-form-item label="总评权重 (%)">
              <el-input-number v-model="form.weight" :min="0" :max="100" :step="5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="6">
            <el-form-item label="顺序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :xs="24" :sm="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿（学生不可见）" value="DRAFT" />
                <el-option label="发布（可提交）" value="PUBLISHED" />
                <el-option label="撤回" value="WITHDRAWN" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="发布时间">
              <el-date-picker
                v-model="form.publishAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm"
                placeholder="留空自动使用当前时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="截止时间">
              <el-date-picker
                v-model="form.dueAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm"
                placeholder="留空表示不限时"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="步骤说明">
              <el-input v-model="form.description" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="提交要求">
              <el-input v-model="form.requirement" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="批改后允许补交">
          <el-switch v-model="form.allowResubmit" />
          <span class="tip">已有提交的步骤，需开启此选项才能修改交付项</span>
        </el-form-item>
      </el-form>

      <div class="items-head">
        <strong>交付项配置（{{ form.items.length }}）</strong>
        <el-button size="small" type="primary" plain @click="addItem">添加交付项</el-button>
      </div>

      <div class="items-list">
        <div v-for="(item, index) in form.items" :key="index" class="item-row">
          <div class="item-line">
            <span class="index">{{ index + 1 }}</span>
            <el-input v-model="item.name" placeholder="交付项名称" style="width: 190px" />
            <el-select v-model="item.typeCode" placeholder="类型" style="width: 160px">
              <el-option
                v-for="type in enabledTypes"
                :key="type.code"
                :label="type.name"
                :value="type.code"
              />
            </el-select>
            <el-select
              placeholder="套用约束模板"
              style="width: 150px"
              @change="applyTemplate(index, $event)"
            >
              <el-option
                v-for="tpl in constraintTemplates"
                :key="tpl.label"
                :label="tpl.label"
                :value="tpl.value"
              />
            </el-select>
            <el-checkbox v-model="item.required">必填</el-checkbox>
            <div class="item-ops">
              <el-button link :disabled="index === 0" @click="moveItem(index, -1)">上移</el-button>
              <el-button link :disabled="index === form.items.length - 1" @click="moveItem(index, 1)">
                下移
              </el-button>
              <el-button link type="danger" @click="removeItem(index)">删除</el-button>
            </div>
          </div>
          <el-input
            v-model="item.constraintJson"
            type="textarea"
            :rows="2"
            placeholder='约束 JSON，如 {"maxSizeMb":20,"formats":["pdf","docx"]}；留空使用默认约束'
          />
        </div>
        <el-empty v-if="!form.items.length" description="请至少添加一个交付项" :image-size="60" />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存步骤</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.card {
  border: 1px solid var(--gsecp-border);
  border-radius: 14px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.weight-tip {
  color: var(--gsecp-muted);
  font-size: 13px;
}

.weight-tip strong {
  color: #059669;
}

.weight-tip strong.warn {
  color: #d97706;
}

.item-tag {
  margin: 2px 4px 2px 0;
}

.muted {
  color: var(--gsecp-muted);
}

.tip {
  margin-left: 12px;
  color: var(--gsecp-muted);
  font-size: 12px;
}

.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}

.items-list {
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.item-row {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--gsecp-border);
  border-radius: 10px;
  background: #fafbff;
}

.item-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.index {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: #e0e7ff;
  color: #4338ca;
  font-size: 12px;
  font-weight: 700;
}

.item-ops {
  display: flex;
  gap: 2px;
  margin-left: auto;
}
</style>
