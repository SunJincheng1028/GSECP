<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { classApi } from '@/api/class'
import type {
  ClassView,
  CourseView,
  InviteCodeView,
  MemberView,
} from '@/types'

const router = useRouter()

const classes = ref<ClassView[]>([])
const members = ref<MemberView[]>([])
const inviteCodes = ref<InviteCodeView[]>([])
const courses = ref<CourseView[]>([])
const selectedId = ref<number | undefined>(undefined)
const loading = ref(false)
const detailLoading = ref(false)
const activeTab = ref('members')

const classVisible = ref(false)
const inviteVisible = ref(false)
const courseVisible = ref(false)
const classFormRef = ref<FormInstance>()
const courseFormRef = ref<FormInstance>()

const classForm = reactive({ name: '', description: '' })
const courseForm = reactive({ name: '', description: '' })
const inviteForm = reactive({ expireDays: 30, maxUses: 0 })
const lastCode = ref('')

const classRules: FormRules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
}
const courseRules: FormRules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
}

const current = computed(() => classes.value.find((item) => item.id === selectedId.value) ?? null)
const courseStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
}

function selectClass(row: ClassView): void {
  selectedId.value = row.id
}

async function loadClasses(): Promise<void> {
  loading.value = true
  try {
    classes.value = await classApi.list()
    if (!classes.value.some((item) => item.id === selectedId.value)) {
      selectedId.value = classes.value[0]?.id
    }
    await loadDetail()
  } finally {
    loading.value = false
  }
}

async function loadDetail(): Promise<void> {
  if (selectedId.value == null) {
    members.value = []
    inviteCodes.value = []
    courses.value = []
    return
  }
  detailLoading.value = true
  try {
    const id = selectedId.value
    const [memberList, codeList, courseList] = await Promise.all([
      classApi.members(id),
      classApi.inviteCodes(id),
      classApi.courses(id),
    ])
    members.value = memberList
    inviteCodes.value = codeList
    courses.value = courseList
  } finally {
    detailLoading.value = false
  }
}

watch(selectedId, () => {
  void loadDetail()
})

async function submitClass(): Promise<void> {
  if (!classFormRef.value) return
  if (!(await classFormRef.value.validate().catch(() => false))) return
  await classApi.create({ ...classForm })
  ElMessage.success('班级已创建')
  classVisible.value = false
  Object.assign(classForm, { name: '', description: '' })
  selectedId.value = undefined
  await loadClasses()
}

async function submitCourse(): Promise<void> {
  if (!courseFormRef.value || selectedId.value == null) return
  if (!(await courseFormRef.value.validate().catch(() => false))) return
  await classApi.createCourse(selectedId.value, { ...courseForm })
  ElMessage.success('课程已创建，默认为草稿状态')
  courseVisible.value = false
  Object.assign(courseForm, { name: '', description: '' })
  await loadDetail()
}

async function setCourseStatus(row: CourseView, status: string): Promise<void> {
  await classApi.setCourseStatus(row.id, status)
  ElMessage.success(status === 'PUBLISHED' ? '课程已发布，学生可见' : '课程状态已更新')
  await loadDetail()
}

async function removeMember(row: MemberView): Promise<void> {
  if (selectedId.value == null) return
  const confirmed = await ElMessageBox.confirm(
    `确定将「${row.realName}」移出班级吗？`,
    '移除成员',
    { type: 'warning' },
  ).catch(() => false)
  if (!confirmed) return
  await classApi.removeMember(selectedId.value, row.studentId)
  ElMessage.success('成员已移除')
  await loadDetail()
  await loadClasses()
}

async function generateCode(): Promise<void> {
  if (selectedId.value == null) return
  lastCode.value = await classApi.createInviteCode(selectedId.value, { ...inviteForm })
  ElMessage.success('邀请码已生成，请发给学生')
  await loadDetail()
}

async function toggleCode(row: InviteCodeView): Promise<void> {
  if (selectedId.value == null) return
  await classApi.setInviteCodeEnabled(selectedId.value, row.id, row.enabled)
  ElMessage.success(row.enabled ? '邀请码已启用' : '邀请码已停用')
}

async function copyCode(code: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('邀请码已复制')
  } catch {
    ElMessage.info(`请手动复制：${code}`)
  }
}

function openSteps(row: CourseView): void {
  void router.push({ path: '/teaching/steps', query: { courseId: String(row.id) } })
}

onMounted(loadClasses)
</script>

<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">班级与邀请码</h1>
        <p class="page-subtitle">创建班级、生成邀请码让学生进班，并在班级下建立课程</p>
      </div>
      <el-button type="primary" @click="classVisible = true">新建班级</el-button>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="card">
          <template #header><strong>我的班级（{{ classes.length }}）</strong></template>
          <el-table
            v-loading="loading"
            :data="classes"
            highlight-current-row
            size="small"
            @row-click="selectClass"
          >
            <el-table-column prop="name" label="班级" min-width="130" />
            <el-table-column prop="studentCount" label="学生" width="64" />
            <el-table-column prop="courseCount" label="课程" width="64" />
          </el-table>
          <el-empty v-if="!loading && !classes.length" description="暂无班级" :image-size="80" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card v-if="current" v-loading="detailLoading" shadow="never" class="card">
          <template #header>
            <div class="detail-head">
              <strong>{{ current.name }}</strong>
              <div class="detail-meta">
                <span>负责教师：{{ current.teacherName }}</span>
                <span>学生 {{ current.studentCount }} 人</span>
                <el-tag size="small" :type="current.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ current.status === 'ACTIVE' ? '进行中' : '已归档' }}
                </el-tag>
              </div>
            </div>
          </template>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="班级成员" name="members">
              <el-table :data="members" size="small" border stripe>
                <el-table-column prop="username" label="用户名" min-width="110" />
                <el-table-column prop="realName" label="姓名" min-width="100" />
                <el-table-column prop="userNo" label="学号" min-width="110">
                  <template #default="{ row }">{{ row.userNo || '—' }}</template>
                </el-table-column>
                <el-table-column prop="joinedAt" label="加入时间" min-width="160" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button link type="danger" :disabled="row.status !== 'ACTIVE'" @click="removeMember(row)">
                      移除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="邀请码" name="codes">
              <div class="tab-actions">
                <el-button type="primary" plain size="small" @click="inviteVisible = true; lastCode = ''">
                  生成邀请码
                </el-button>
                <span class="tip">学生可在注册页凭邀请码加入本班</span>
              </div>
              <el-table :data="inviteCodes" size="small" border stripe>
                <el-table-column prop="code" label="邀请码" min-width="120">
                  <template #default="{ row }">
                    <code class="code">{{ row.code }}</code>
                    <el-button link type="primary" @click="copyCode(row.code)">复制</el-button>
                  </template>
                </el-table-column>
                <el-table-column prop="expireAt" label="过期时间" min-width="160">
                  <template #default="{ row }">{{ row.expireAt || '永不过期' }}</template>
                </el-table-column>
                <el-table-column label="使用次数" min-width="110">
                  <template #default="{ row }">
                    {{ row.usedCount }} / {{ row.maxUses || '不限' }}
                  </template>
                </el-table-column>
                <el-table-column label="启用" width="90">
                  <template #default="{ row }">
                    <el-switch v-model="row.enabled" size="small" @change="toggleCode(row)" />
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="课程" name="courses">
              <div class="tab-actions">
                <el-button type="primary" plain size="small" @click="courseVisible = true">
                  新建课程
                </el-button>
                <span class="tip">发布课程后学生才能看到其中的步骤</span>
              </div>
              <el-table :data="courses" size="small" border stripe>
                <el-table-column prop="name" label="课程" min-width="140" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
                      {{ courseStatusLabels[row.status] ?? row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="stepCount" label="步骤" width="70" />
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openSteps(row)">配置步骤</el-button>
                    <el-button
                      v-if="row.status === 'DRAFT'"
                      link
                      type="success"
                      @click="setCourseStatus(row, 'PUBLISHED')"
                    >
                      发布
                    </el-button>
                    <el-button
                      v-else-if="row.status === 'PUBLISHED'"
                      link
                      type="warning"
                      @click="setCourseStatus(row, 'ARCHIVED')"
                    >
                      归档
                    </el-button>
                    <el-button v-else link type="primary" @click="setCourseStatus(row, 'PUBLISHED')">
                      重新发布
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-card v-else shadow="never" class="card">
          <el-empty description="请先创建或选择一个班级" :image-size="110" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="classVisible" title="新建班级" width="460">
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="90px">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="classForm.name" placeholder="如：2026 春 软件工程 1 班" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="classForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classVisible = false">取消</el-button>
        <el-button type="primary" @click="submitClass">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="courseVisible" title="新建课程" width="460">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="90px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="courseForm.name" placeholder="如：生成式软件工程课程设计" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="courseForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCourse">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inviteVisible" title="生成班级邀请码" width="440">
      <el-form label-width="110px">
        <el-form-item label="有效天数">
          <el-input-number v-model="inviteForm.expireDays" :min="0" :max="365" />
          <span class="tip inline">0 表示永不过期</span>
        </el-form-item>
        <el-form-item label="最大使用次数">
          <el-input-number v-model="inviteForm.maxUses" :min="0" :max="1000" />
          <span class="tip inline">0 表示不限次数</span>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="generateCode">生成新邀请码</el-button>
      <div v-if="lastCode" class="code-result">
        <span>新邀请码</span>
        <code>{{ lastCode }}</code>
        <el-button link type="primary" @click="copyCode(lastCode)">复制</el-button>
      </div>
      <template #footer>
        <el-button @click="inviteVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.card {
  border: 1px solid var(--gsecp-border);
  border-radius: 14px;
}

.detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--gsecp-muted);
  font-size: 12px;
  font-weight: 400;
}

.tab-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.tip {
  color: var(--gsecp-muted);
  font-size: 12px;
}

.tip.inline {
  margin-left: 10px;
}

.code {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #4338ca;
}

.code-result {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px 14px;
  border: 1px dashed #c7d2fe;
  border-radius: 10px;
  background: #f5f7ff;
  font-size: 13px;
}

.code-result code {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #4338ca;
}
</style>
