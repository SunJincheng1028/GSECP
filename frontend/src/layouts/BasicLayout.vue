<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const roleLabels: Record<string, string> = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生',
}

const menuRoutes = computed(() => {
  const root = router.options.routes.find((item) => item.path === '/')
  return (root?.children ?? []).filter(
    (child) => !child.meta?.hidden && store.hasRole(child.meta?.roles ?? []),
  )
})

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title ?? '工作台')
const currentRole = computed(() => {
  const first = store.roles[0]
  return first ? roleLabels[first] ?? first : '未分配'
})

async function handleCommand(command: string): Promise<void> {
  if (command === 'refresh') {
    await store.fetchProfile()
    ElMessage.success('用户信息已刷新')
    return
  }
  if (command === 'logout') {
    try {
      await authApi.logout()
    } catch {
      // 无状态登出，接口失败不影响本地清理
    }
    store.logout()
    ElMessage.success('已退出登录')
    await router.push('/login')
  }
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="236px" class="aside">
      <div class="brand">
        <div class="brand-mark">生</div>
        <div class="brand-text">
          <div class="brand-name">生成式软件工程<br />课程设计平台</div>
        </div>
      </div>

      <el-menu class="menu" :default-active="activeMenu" router background-color="transparent">
        <el-menu-item
          v-for="item in menuRoutes"
          :key="item.path"
          :index="`/${item.path}`"
        >
          <el-icon><component :is="item.meta?.icon || 'Menu'" /></el-icon>
          <span>{{ item.meta?.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <span class="dot" />
        v0.2 · 迭代 1 MVP
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="header-crumb">首页</span>
          <span class="header-sep">/</span>
          <span class="header-current">{{ pageTitle }}</span>
        </div>

        <div class="header-right">
          <el-tag type="primary" effect="light" round>{{ currentRole }}</el-tag>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <el-icon><UserFilled /></el-icon>
              {{ store.userInfo?.realName || '未登录' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="refresh">刷新信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  display: flex;
  flex-direction: column;
  background: #111827;
  color: #e5e7eb;
  border-right: 1px solid #1f2937;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.brand-text {
  min-width: 0;
}

.brand-name {
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.55;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.menu {
  flex: 1;
  padding: 12px 10px;
  border-right: none;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  margin-bottom: 4px;
  border-radius: 9px;
  color: #cbd5e1;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(99, 102, 241, 0.16);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.9), rgba(139, 92, 246, 0.75));
  color: #fff;
}

.aside-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  font-size: 12px;
  color: #6b7280;
}

.dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 6px;
  border-radius: 50%;
  background: #34d399;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid var(--gsecp-border);
}

.header-crumb,
.header-sep {
  color: var(--gsecp-muted);
  font-size: 14px;
}

.header-sep {
  margin: 0 10px;
}

.header-current {
  font-size: 14px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  font-size: 14px;
  outline: none;
}

.main {
  padding: 0;
  overflow: auto;
  background: var(--gsecp-bg);
}
</style>
