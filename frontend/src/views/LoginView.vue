<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'Admin@123456',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await store.login({ username: form.username, password: form.password })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-top">
          <div class="logo">生</div>
          <span class="brand-top-name">生成式软件工程课程设计平台</span>
        </div>

        <div class="brand-body">
          <p class="eyebrow">按步骤管理进度与成果</p>
          <h1>让每一次课程设计<br />都有迹可循</h1>
          <p class="brand-desc">
            按步骤组织课程、配置差异化交付物、检查学生进度与成果，并给出评分。
          </p>

          <div class="feature-list">
            <div class="feature">
              <span class="feature-index">01</span>
              <div>
                <strong>步骤化教学</strong>
                <small>每个步骤独立配置交付要求</small>
              </div>
            </div>
            <div class="feature">
              <span class="feature-index">02</span>
              <div>
                <strong>多类型成果</strong>
                <small>文档、代码包、仓库地址按项提交</small>
              </div>
            </div>
            <div class="feature">
              <span class="feature-index">03</span>
              <div>
                <strong>过程与评分</strong>
                <small>进度总览、批改评分、总评汇总</small>
              </div>
            </div>
          </div>
        </div>

        <div class="brand-bottom">迭代 1 · MVP v0.2</div>
      </section>

      <section class="form-panel">
        <div class="form-card">
          <div class="form-head">
            <h2>欢迎回来</h2>
            <p>登录后进入课程设计管理工作台</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            @keyup.enter="handleSubmit"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="'User'" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="请输入密码"
                :prefix-icon="'Lock'"
              />
            </el-form-item>

            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleSubmit"
            >
              登录
            </el-button>
          </el-form>

          <div class="dev-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>开发环境初始管理员：<code>admin</code> / <code>Admin@123456</code></span>
          </div>

          <div class="register-hint">
            学生还没有账号？
            <router-link to="/register" class="link">凭班级邀请码注册</router-link>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 32px;
  background: radial-gradient(circle at 15% 20%, #e0e7ff 0, transparent 42%),
    radial-gradient(circle at 85% 85%, #ede9fe 0, transparent 45%), #f8fafc;
}

.login-shell {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: min(1040px, 100%);
  min-height: 620px;
  overflow: hidden;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(30, 41, 59, 0.14);
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  color: #fff;
  background: linear-gradient(155deg, #312e81 0%, #4f46e5 55%, #7c3aed 100%);
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.brand-top-name {
  white-space: nowrap;
}

.logo {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 10px;
  font-size: 18px;
}

.eyebrow {
  margin: 0 0 14px;
  font-size: 12px;
  letter-spacing: 0.12em;
  opacity: 0.65;
}

.brand-body h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.4;
  font-weight: 650;
}

.brand-desc {
  margin: 18px 0 34px;
  max-width: 360px;
  font-size: 14px;
  line-height: 1.8;
  opacity: 0.82;
}

.feature-list {
  display: grid;
  gap: 18px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 16px;
}

.feature-index {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 10px;
  font-size: 12px;
  opacity: 0.85;
}

.feature strong,
.feature small {
  display: block;
}

.feature strong {
  font-size: 14px;
  font-weight: 600;
}

.feature small {
  margin-top: 3px;
  font-size: 12px;
  opacity: 0.7;
}

.brand-bottom {
  font-size: 12px;
  opacity: 0.6;
}

.form-panel {
  display: grid;
  place-items: center;
  padding: 40px;
}

.form-card {
  width: min(360px, 100%);
}

.form-head {
  margin-bottom: 32px;
}

.form-head h2 {
  margin: 0 0 8px;
  font-size: 27px;
  font-weight: 650;
}

.form-head p {
  margin: 0;
  color: var(--gsecp-muted);
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border-radius: 10px;
  font-size: 15px;
}

.dev-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 26px;
  padding: 12px 14px;
  border: 1px dashed #c7d2fe;
  border-radius: 10px;
  background: #f5f7ff;
  color: #4b5563;
  font-size: 12px;
}

.dev-hint code {
  color: #4338ca;
  font-weight: 600;
}

.register-hint {
  margin-top: 18px;
  text-align: center;
  color: var(--gsecp-muted);
  font-size: 13px;
}

.link {
  color: var(--gsecp-primary);
  font-weight: 600;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }
}
</style>
