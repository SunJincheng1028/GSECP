import { http } from './request'
import type { UserView } from '@/types'

export const adminApi = {
  users: () => http.get<UserView[]>('/admin/users'),
  createUser: (data: { username: string; password: string; realName: string; userNo: string; role: string }) =>
    http.post<void>('/admin/users', data),
  updateUserStatus: (id: number, status: string) =>
    http.patch<void>(`/admin/users/${id}/status`, { status }),
  resetPassword: (id: number, password: string) =>
    http.post<void>(`/admin/users/${id}/reset-password`, { password }),
}
