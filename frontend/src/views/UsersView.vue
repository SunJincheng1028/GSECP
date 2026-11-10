<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { UserView } from '@/types'

const users = ref<UserView[]>([])
const loading = ref(false)
const createVisible = ref(false)
const resetVisible = ref(false)
const createFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()
const currentUser = ref<UserView | null>(null)

const createForm = reactive({ username: '', password: '', realName: '', userNo: '', role: 'TEACHER' })
const resetForm = reactive({ password: '' })

const createRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}
const resetRules: FormRules = {
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }],
}

async function load(): Promise<void> {
  loading.value = true
  try {
    users.value = await adminApi.users()
  } finally {
    loading.value = false
  }
}

async function submitCreate(): Promise<void> {
  if (!createFormRef.value) return
  if (!(await createFormRef.value.validate().catch(() => false))) return
  await adminApi.createUser({ ...createForm })
  ElMessage.success('账号已创建')
  createVisible.value = false
  Object.assign(createForm, { username: '', password: '', realName: '', userNo: '', role: 'TEACHER' })
  await load()
}

async function toggleStatus(row: UserView): Promise<void> {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await adminApi.updateUserStatus(row.id, next)
  ElMessage.success(next === 'ACTIVE' ? '账号已启用' : '账号已禁用')
  await load()
}

function openReset(row: UserView): void {
  currentUser.value = row
  resetForm.password = ''
  resetVisible.value = true
}

async function submitReset(): Promise<void> {
  if (!resetFormRef.value || !currentUser.value) return
  if (!(await resetFormRef.value.validate().catch(() => false))) return
  await adminApi.resetPassword(currentUser.value.id, resetForm.password)
  ElMessage.success('密码已重置')
  resetVisible.value = false
}

async function confirmRemove(row: UserView): Promise<void> {
  await ElMessageBox.confirm(`确定要禁用账号「${row.username}」吗？`, '提示', { type: 'warning' })
  await adminApi.updateUserStatus(row.id, 'DISABLED')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">创建教师账号、启用/禁用账号与重置密码；学生通过邀请码自行注册</p>
      </div>
      <el-button type="primary" @click="createVisible = true">新建账号</el-button>
    </div>

    <el-card shadow="never" class="card">
      <el-table v-loading="loading" :data="users" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="userNo" label="学号/工号" min-width="120">
          <template #default="{ row }">{{ row.userNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ row.role || '未分配' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggleStatus(row)">
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="primary" @click="openReset(row)">重置密码</el-button>
            <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="confirmRemove(row)">
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createVisible" title="新建账号" width="480">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="createForm.userNo" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420">
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReset">确定</el-button>
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
</style>
