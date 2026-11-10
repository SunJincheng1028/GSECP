import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useUserStore } from '@/stores/user'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    requiresAuth?: boolean
    roles?: string[]
    hidden?: boolean
    description?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '学生注册', hidden: true },
  },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', requiresAuth: true },
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/UsersView.vue'),
        meta: {
          title: '用户管理',
          icon: 'User',
          requiresAuth: true,
          roles: ['ADMIN'],
          description: '创建教师账号、维护用户信息',
        },
      },
      {
        path: 'system/config',
        name: 'system-config',
        component: () => import('@/views/ConfigView.vue'),
        meta: {
          title: '系统配置',
          icon: 'Setting',
          requiresAuth: true,
          roles: ['ADMIN'],
          description: '附件上限与交付物类型字典',
        },
      },
      {
        path: 'teaching/classes',
        name: 'teaching-classes',
        component: () => import('@/views/ClassesView.vue'),
        meta: {
          title: '班级与邀请码',
          icon: 'Collection',
          requiresAuth: true,
          roles: ['ADMIN', 'TEACHER'],
          description: '班级、成员、邀请码与课程',
        },
      },
      {
        path: 'teaching/steps',
        name: 'teaching-steps',
        component: () => import('@/views/StepsView.vue'),
        meta: {
          title: '步骤与交付项',
          icon: 'Document',
          requiresAuth: true,
          roles: ['ADMIN', 'TEACHER'],
          description: '按步骤配置多类型交付要求',
        },
      },
      {
        path: 'teaching/grading',
        name: 'teaching-grading',
        component: () => import('@/views/GradingView.vue'),
        meta: {
          title: '进度与批改',
          icon: 'EditPen',
          requiresAuth: true,
          roles: ['ADMIN', 'TEACHER'],
          description: '查看提交进度并批改评分',
        },
      },
      {
        path: 'teaching/grades',
        name: 'teaching-grades',
        component: () => import('@/views/GradesView.vue'),
        meta: {
          title: '总评汇总',
          icon: 'DataAnalysis',
          requiresAuth: true,
          roles: ['ADMIN', 'TEACHER'],
          description: '按步骤权重汇总总评分',
        },
      },
      {
        path: 'learning/tasks',
        name: 'learning-tasks',
        component: () => import('@/views/TasksView.vue'),
        meta: {
          title: '我的任务',
          icon: 'Tickets',
          requiresAuth: true,
          roles: ['STUDENT'],
          description: '按交付项逐项提交成果',
        },
      },
      {
        path: 'learning/grades',
        name: 'learning-grades',
        component: () => import('@/views/GradesView.vue'),
        meta: {
          title: '我的成绩',
          icon: 'TrendCharts',
          requiresAuth: true,
          roles: ['STUDENT'],
          description: '查看各步骤得分与总评',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', hidden: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const store = useUserStore()

  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles && !store.hasRole(to.meta.roles)) {
    ElMessage.warning('当前角色无权访问该页面')
    return { path: '/dashboard' }
  }
  if ((to.path === '/login' || to.path === '/register') && store.isLoggedIn) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
