<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { stepApi } from '@/api/step'
import { submissionApi } from '@/api/submission'
import { scoreApi } from '@/api/score'
import { useCourseSelection } from '@/composables/useCourseSelection'
import SubmissionDetail from '@/components/SubmissionDetail.vue'
import type { ProgressView, StepView, SubmissionView } from '@/types'

const route = useRoute()
const { state, loadClasses, loadCourses, selectStepId } = useCourseSelection()

const steps = ref<StepView[]>([])
const stepId = ref<number | undefined>(undefined)
const progress = ref<ProgressView[]>([])
const loading = ref(false)
const grading = ref(false)
const gradeVisible = ref(false)
const detail = ref<SubmissionView | null>(null)
const gradeForm = reactive({ totalScore: 90, comment: '' })

const statusLabels: Record<string, string> = {
  SUBMITTED: '已提交',
  LATE: '迟交',
  GRADED: '已批改',
  RETURNED: '已退回',
}

async function loadProgress(): Promise<void> {
  progress.value = []
  if (stepId.value == null) return
  loading.value = true
  try {
    progress.value = await submissionApi.progress(stepId.value)
  } finally {
    loading.value = false
  }
}

async function loadStepsForCourse(preferred?: number): Promise<void> {
  steps.value = state.courseId == null ? [] : await stepApi.list(state.courseId)
  stepId.value = steps.value.some((item) => item.id === preferred)
    ? preferred
    : steps.value[0]?.id
  await loadProgress()
}

async function handleClassChange(): Promise<void> {
  await loadCourses()
  await loadStepsForCourse()
}

async function openGrade(row: ProgressView): Promise<void> {
  if (row.submissionId == null) {
    ElMessage.info('该学生尚未提交')
    return
  }
  detail.value = await submissionApi.detail(row.submissionId)
  gradeForm.totalScore = row.score == null ? 90 : Number(row.score)
  gradeForm.comment = row.comment ?? ''
  gradeVisible.value = true
}

async function submitGrade(): Promise<void> {
  if (!detail.value) return
  if (gradeForm.totalScore < 0 || gradeForm.totalScore > 100) {
    ElMessage.warning('分数需介于 0 ~ 100 之间')
    return
  }
  grading.value = true
  try {
    await scoreApi.grade(detail.value.id, Number(gradeForm.totalScore), gradeForm.comment.trim())
    ElMessage.success('批改已保存，总评将按步骤权重汇总')
    gradeVisible.value = false
    await loadProgress()
  } finally {
    grading.value = false
  }
}

onMounted(async () => {
  await loadClasses()
  const queryStepId = route.query.stepId
  if (typeof queryStepId === 'string' && queryStepId) {
    const target = Number(queryStepId)
    await selectStepId(target, async (id) => (await stepApi.detail(id)).courseId)
    await loadStepsForCourse(target)
  } else {
    await loadCourses()
    await loadStepsForCourse()
  }
})
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">进度与批改</h1>
    <p class="page-subtitle">查看班级学生在该步骤的最新提交，逐份批改并给出分数与评语</p>

    <div class="filters">
      <el-select
        v-model="state.classId"
        placeholder="选择班级"
        style="width: 210px"
        @change="handleClassChange"
      >
        <el-option v-for="item in state.classes" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select
        v-model="state.courseId"
        placeholder="选择课程"
        style="width: 230px"
        @change="loadStepsForCourse()"
      >
        <el-option v-for="item in state.courses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="stepId" placeholder="选择步骤" style="width: 230px" @change="loadProgress">
        <el-option v-for="item in steps" :key="item.id" :label="item.title" :value="item.id" />
      </el-select>
    </div>

    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-head">
          <strong>提交进度（{{ progress.length }} 人）</strong>
          <span class="tip">仅展示学生最新版本，批改后自动计入总评</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="progress" border stripe>
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="userNo" label="学号" min-width="110">
          <template #default="{ row }">{{ row.userNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status" size="small" :type="row.status === 'LATE' ? 'warning' : 'success'">
              {{ statusLabels[row.status] ?? row.status }}
            </el-tag>
            <el-tag v-else size="small" type="info">未提交</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="80">
          <template #default="{ row }">{{ row.version ? `第 ${row.version} 版` : '—' }}</template>
        </el-table-column>
        <el-table-column prop="submitAt" label="提交时间" min-width="160">
          <template #default="{ row }">{{ row.submitAt || '—' }}</template>
        </el-table-column>
        <el-table-column label="分数" width="90">
          <template #default="{ row }">
            <strong v-if="row.score != null" class="score">{{ row.score }}</strong>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.submissionId == null" @click="openGrade(row)">
              {{ row.score != null ? '重新批改' : '查看/批改' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !progress.length" description="该班级暂无学生或尚未选择步骤" :image-size="100" />
    </el-card>

    <el-dialog v-model="gradeVisible" title="批改提交" width="760" top="6vh">
      <template v-if="detail">
        <SubmissionDetail :submission="detail" />
        <el-divider />
        <el-form label-width="90px">
          <el-form-item label="分数">
            <el-input-number v-model="gradeForm.totalScore" :min="0" :max="100" :precision="1" />
            <span class="tip">0 ~ 100 分，按步骤权重折算进总评</span>
          </el-form-item>
          <el-form-item label="评语">
            <el-input v-model="gradeForm.comment" type="textarea" :rows="3" placeholder="给出改进建议" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="submitGrade">保存批改</el-button>
      </template>
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

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.tip {
  margin-left: 12px;
  color: var(--gsecp-muted);
  font-size: 12px;
}

.score {
  color: #4338ca;
  font-size: 15px;
}
</style>
