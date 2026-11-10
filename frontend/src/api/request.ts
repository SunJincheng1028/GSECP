import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

export const TOKEN_KEY = 'gsecp_token'
export const USER_KEY = 'gsecp_user'

export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

function redirectToLogin(): void {
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as Partial<ApiResponse<unknown>> | undefined
    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (body.code === 200) {
        return response
      }
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) {
        clearAuth()
        redirectToLogin()
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error: unknown) => {
    const err = error as {
      response?: { status?: number; data?: { message?: string } }
      message?: string
    }
    const status = err.response?.status
    const message = err.response?.data?.message || err.message || '网络异常，请稍后重试'

    if (status === 401) {
      clearAuth()
      redirectToLogin()
    } else if (status === 403) {
      ElMessage.error('没有权限执行此操作')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

function extract<T>(response: AxiosResponse<ApiResponse<T>>): T {
  return response.data.data
}

/** 直接返回业务 data 的轻量 HTTP 客户端 */
export const http = {
  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return extract<T>(await request.get<ApiResponse<T>>(url, config))
  },
  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return extract<T>(await request.post<ApiResponse<T>>(url, data, config))
  },
  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return extract<T>(await request.put<ApiResponse<T>>(url, data, config))
  },
  async patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return extract<T>(await request.patch<ApiResponse<T>>(url, data, config))
  },
  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return extract<T>(await request.delete<ApiResponse<T>>(url, config))
  },
}

export default request
