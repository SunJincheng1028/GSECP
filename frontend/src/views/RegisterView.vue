<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { RegisterParams } from '@/types'

const router = useRouter()
const store = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive<RegisterParams>({
  username: '',
  password: '',
  realName: '',
  userNo: '',
  inviteCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  inviteCode: [{ required: true, message: '请输入班级邀请码', trigger: 'blur' }],
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
    await store.register({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim(),
      userNo: form.userNo.trim(),
      inviteCode: form.inviteCode.trim().toUpperCase(),
    })
    await router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <div class="head">
        <div class="mark">生</div>
        <h1>学生注册</h1>
        <p>凭教师提供的班级邀请码加入班级</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="用于登录" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="学号">
              <el-input v-model="form.userNo" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="班级邀请码" prop="inviteCode">
          <el-input v-model="form.inviteCode" placeholder="请输入 8 位邀请码" />
        </el-form-item>

        <el-button type="primary" class="submit" :loading="loading" @click="handleSubmit">
          注册并加入班级
        </el-button>
      </el-form>

      <div class="footer">
        已有账号？
        <router-link to="/login" class="link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  background: radial-gradient(circle at 20% 15%, #e0e7ff, transparent 45%),
    radial-gradient(circle at 85% 85%, #ede9fe, transparent 45%), #f8fafc;
}

.register-card {
  width: min(640px, 100%);
  padding: 36px 40px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 20px 60px rgba(30, 41, 59, 0.12);
}

.head {
  margin-bottom: 26px;
}

.mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 650;
}

.head p {
  margin: 0;
  color: var(--gsecp-muted);
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border-radius: 10px;
}

.footer {
  margin-top: 20px;
  text-align: center;
  color: var(--gsecp-muted);
  font-size: 13px;
}

.link {
  color: var(--gsecp-primary);
  font-weight: 600;
}
</style>
