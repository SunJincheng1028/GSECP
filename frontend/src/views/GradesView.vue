<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { scoreApi } from '@/api/score'
import { useCourseSelection } from '@/composables/useCourseSelection'
import { useUserStore } from '@/stores/user'
import type { GradeReport, StepGrade } from '@/types'

const route = useRoute()
const store = useUserStore()
const { state, loadClasses, loadCourses, selectCourseId } = useCourseSelection()

const report = ref<GradeReport | null>(null)
const loading = ref(false)

const isStudent = computed(
  () =>
    store.roles.includes('STUDENT') &&
    !store.roles.some((role) => role === 'ADMIN' || role === 'TEACHER'),
)
const studentSteps = computed<StepGrade[]>(() =>
  isStudent.value ? (report.value?.rows[0]?.steps ?? []) : [],
)

async function loadReport(): Promise<void> {
  report.value = null
  if (state.courseId == null) return
  loading.value = true
  try {
    report.value = isStudent.value
      ? await scoreApi.myReport(state.courseId)
      : await scoreApi.report(state.courseId)
  } finally {
    loading.value = false
  }
}

async function handleClassChange(): Promise<void> {
  await loadCourses()
  await loadReport()
}

function cellText(cell: StepGrade | undefined): string {
  return cell?.score == null ? '—' : String(cell.score)
}

onMounted(async () => {
  await loadClasses()
  const queryCourseId = route.query.courseId
  if (typeof queryCourseId === 'string' && queryCourseId) {
    await selectCourseId(Number(queryCourseId))
  } else {
    await loadCourses()
  }
  await loadReport()
})
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">{{ isStudent ? '我的成绩' : '总评汇总' }}</h1>
    <p class="page-subtitle">
      各步骤得分按权重折算后累加为总评分；「已批改权重」表示目前参与汇总的步骤占比
    </p>

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
        @change="loadReport"
      >
        <el-option v-for="item in state.courses" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </div>

    <el-card v-if="report" v-loading="loading" shadow="never" class="card">
      <template #header>
        <div class="card-head">
          <strong>{{ report.courseName }}</strong>
          <span class="tip">
            共 {{ report.columns.length }} 个计分步骤 · {{ report.rows.length }} 名学生
          </span>
        </div>
      </template>

      <el-table :data="report.rows" border stripe>
        <el-table-column prop="realName" label="姓名" min-width="100" fixed="left" />
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="userNo" label="学号" min-width="110">
          <template #default="{ row }">{{ row.userNo || '—' }}</template>
        </el-table-column>
        <el-table-column
          v-for="(column, index) in report.columns"
          :key="column.stepId"
          :min-width="130"
        >
          <template #header>
            <div class="column-head">
              <span>{{ column.title }}</span>
              <small>{{ column.weight }}%</small>
            </div>
          </template>
          <template #default="{ row }">
            <el-tooltip
              v-if="row.steps[index]?.comment"
              :content="row.steps[index].comment"
              placement="top"
            >
              <span class="score">{{ cellText(row.steps[index]) }}</span>
            </el-tooltip>
            <span v-else :class="{ score: row.steps[index]?.score != null }">
              {{ cellText(row.steps[index]) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="总评分" width="110" fixed="right">
          <template #default="{ row }">
            <strong class="total">{{ row.totalScore }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="已批改权重" width="120" fixed="right">
          <template #default="{ row }">{{ row.gradedWeight }}%</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !report.rows.length" description="暂无学生成绩数据" :image-size="100" />
    </el-card>

    <el-card v-if="isStudent && studentSteps.length" shadow="never" class="card feedback-card">
      <template #header><strong>各步骤反馈与评语</strong></template>
      <div v-for="cell in studentSteps" :key="cell.stepId" class="feedback-item">
        <div class="feedback-head">
          <strong>{{ cell.title }}</strong>
          <span>{{ cell.weight }}%</span>
          <el-tag size="small" :type="cell.score != null ? 'success' : 'info'">
            {{ cell.score != null ? `${cell.score} 分` : '尚未批改' }}
          </el-tag>
        </div>
        <p>{{ cell.comment || '暂无评语' }}</p>
      </div>
    </el-card>

    <el-card v-if="!report" v-loading="loading" shadow="never" class="card">
      <el-empty description="请选择班级与课程查看总评" :image-size="110" />
    </el-card>
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
  color: var(--gsecp-muted);
  font-size: 12px;
  font-weight: 400;
}

.column-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.column-head small {
  color: var(--gsecp-muted);
  font-weight: 400;
}

.score {
  color: #4338ca;
  font-weight: 600;
}

.total {
  color: #059669;
  font-size: 16px;
}

.feedback-card {
  margin-top: 18px;
}

.feedback-item {
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid var(--gsecp-border);
  border-radius: 10px;
  background: #fafbff;
}

.feedback-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  color: var(--gsecp-muted);
  font-size: 13px;
}

.feedback-item p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
