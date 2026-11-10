import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/api/request'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types'

function readCachedUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<UserInfo | null>(readCachedUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const roles = computed(() => userInfo.value?.roles ?? [])

  function applyLogin(result: LoginResult): void {
    token.value = result.token
    userInfo.value = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  async function login(params: LoginParams): Promise<void> {
    applyLogin(await authApi.login(params))
  }

  async function register(params: RegisterParams): Promise<void> {
    applyLogin(await authApi.register(params))
  }

  async function fetchProfile(): Promise<void> {
    if (!token.value) {
      return
    }
    try {
      userInfo.value = await authApi.me()
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
    } catch {
      logout()
    }
  }

  function logout(): void {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function hasRole(required: string[] = []): boolean {
    if (required.length === 0) {
      return true
    }
    return required.some((role) => roles.value.includes(role))
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    roles,
    login,
    register,
    applyLogin,
    fetchProfile,
    logout,
    hasRole,
  }
})
