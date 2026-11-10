<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'
import { useUserStore } from '@/stores/user'
import type { HealthInfo } from '@/types'

const store = useUserStore()
const health = ref<HealthInfo | null>(null)
const pingResult = ref('尚未执行')

const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN || window.location.origin

const roleLabels: Record<string, string> = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生',
}

const roleTags = computed(() =>
  store.roles.map((role) => roleLabels[role] ?? role),
)

onMounted(async () => {
  try {
    health.value = await http.get<HealthInfo>('/health')
  } catch {
    health.value = null
  }
})

async function checkRole(path: string): Promise<void> {
  try {
    pingResult.value = await http.get<string>(path)
    ElMessage.success('权限校验通过')
  } catch {
    pingResult.value = '权限校验未通过'
  }
}

function openApiDoc(): void {
  window.open(`${backendOrigin}/swagger-ui.html`, '_blank')
}
</script>

<template>
  <div class="page-container">
    <h1 class="page-title">工作台</h1>
    <p class="page-subtitle">
      迭代 1 MVP 已就绪：邀请码进班 → 步骤与交付项配置 → 按类型提交 → 批改评分 → 总评汇总。
    </p>

    <el-row :gutter="18">
      <el-col :xs="24" :md="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">当前用户</div>
          <div class="stat-value">{{ store.userInfo?.realName || '—' }}</div>
          <div class="stat-extra">
            <el-tag v-for="role in roleTags" :key="role" size="small" effect="light">
              {{ role }}
            </el-tag>
            <span v-if="!roleTags.length" class="muted">未分配角色</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">后端服务</div>
          <div class="stat-value" :class="health ? 'ok' : 'bad'">
            {{ health ? health.status : 'OFFLINE' }}
          </div>
          <div class="stat-extra">
            {{ health ? `${health.version} · 迭代 ${health.iteration}` : '请检查后端是否已启动' }}
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">数据库</div>
          <div class="stat-value">MySQL 8</div>
          <div class="stat-extra">Flyway V1__init.sql · 30 张表</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="section-row">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">RBAC 接口连通性自检</div>
          </template>
          <p class="hint">点击按钮调用后端示例接口，验证 JWT 与角色注解是否生效。</p>
          <div class="button-row">
            <el-button v-permission="['ADMIN']" @click="checkRole('/admin/ping')">
              管理员接口
            </el-button>
            <el-button v-permission="['ADMIN', 'TEACHER']" @click="checkRole('/teacher/ping')">
              教师接口
            </el-button>
            <el-button
              v-permission="['ADMIN', 'TEACHER', 'STUDENT']"
              @click="checkRole('/student/ping')"
            >
              学生接口
            </el-button>
            <el-button type="primary" plain @click="openApiDoc">打开接口文档</el-button>
          </div>
          <el-alert class="result" :title="pingResult" type="info" :closable="false" show-icon />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">迭代 1 快捷入口</div>
          </template>
          <div class="quick-links">
            <template v-if="store.hasRole(['ADMIN'])">
              <router-link to="/system/users">用户管理</router-link>
              <router-link to="/system/config">系统配置</router-link>
            </template>
            <template v-if="store.hasRole(['ADMIN', 'TEACHER'])">
              <router-link to="/teaching/classes">班级与邀请码</router-link>
              <router-link to="/teaching/steps">步骤与交付项</router-link>
              <router-link to="/teaching/grading">进度与批改</router-link>
              <router-link to="/teaching/grades">总评汇总</router-link>
            </template>
            <template v-if="store.hasRole(['STUDENT'])">
              <router-link to="/learning/tasks">我的任务</router-link>
              <router-link to="/learning/grades">我的成绩</router-link>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card {
  border: 1px solid var(--gsecp-border);
  border-radius: 14px;
}

.stat-label {
  color: var(--gsecp-muted);
  font-size: 13px;
}

.stat-value {
  margin: 12px 0 10px;
  font-size: 26px;
  font-weight: 650;
}

.stat-value.ok {
  color: #059669;
}

.stat-value.bad {
  color: #dc2626;
}

.stat-extra {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  color: var(--gsecp-muted);
  font-size: 12px;
}

.muted {
  color: var(--gsecp-muted);
}

.section-row {
  margin-top: 18px;
}

.card-header {
  font-size: 15px;
  font-weight: 600;
}

.hint {
  margin: 0 0 16px;
  color: var(--gsecp-muted);
  font-size: 13px;
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.result {
  margin-top: 16px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-links a {
  padding: 12px 14px;
  border: 1px solid var(--gsecp-border);
  border-radius: 10px;
  background: #fafbff;
  color: #374151;
  font-size: 13px;
  text-align: center;
  transition: all 0.15s ease;
}

.quick-links a:hover {
  border-color: var(--gsecp-primary-light);
  color: var(--gsecp-primary);
  background: #f5f7ff;
}
</style>
