import { http } from './request'
import type { ClassView, CourseView, InviteCodeView, MemberView } from '@/types'

export const classApi = {
  list: () => http.get<ClassView[]>('/classes'),
  detail: (id: number) => http.get<ClassView>(`/classes/${id}`),
  create: (data: { name: string; description: string }) => http.post<void>('/classes', data),
  members: (id: number) => http.get<MemberView[]>(`/classes/${id}/members`),
  removeMember: (id: number, studentId: number) =>
    http.delete<void>(`/classes/${id}/members/${studentId}`),
  inviteCodes: (id: number) => http.get<InviteCodeView[]>(`/classes/${id}/invite-codes`),
  createInviteCode: (id: number, data: { expireDays: number; maxUses: number }) =>
    http.post<string>(`/classes/${id}/invite-codes`, data),
  setInviteCodeEnabled: (id: number, codeId: number, enabled: boolean) =>
    http.patch<void>(`/classes/${id}/invite-codes/${codeId}`, { enabled }),
  courses: (classId: number) => http.get<CourseView[]>(`/classes/${classId}/courses`),
  createCourse: (classId: number, data: { name: string; description: string }) =>
    http.post<void>(`/classes/${classId}/courses`, data),
  setCourseStatus: (courseId: number, status: string) =>
    http.patch<void>(`/courses/${courseId}/status`, { status }),
  course: (courseId: number) => http.get<CourseView>(`/courses/${courseId}`),
}
