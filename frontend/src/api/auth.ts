import { http } from './request'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types'

export const authApi = {
  login: (params: LoginParams) => http.post<LoginResult>('/auth/login', params),
  register: (params: RegisterParams) => http.post<LoginResult>('/auth/register', params),
  me: () => http.get<UserInfo>('/auth/me'),
  logout: () => http.post<void>('/auth/logout'),
}
