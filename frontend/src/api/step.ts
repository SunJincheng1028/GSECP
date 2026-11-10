import { http } from './request'
import type { StepSaveRequest, StepView } from '@/types'

export const stepApi = {
  list: (courseId: number) => http.get<StepView[]>(`/courses/${courseId}/steps`),
  detail: (id: number) => http.get<StepView>(`/steps/${id}`),
  create: (courseId: number, data: StepSaveRequest) =>
    http.post<number>(`/courses/${courseId}/steps`, data),
  update: (id: number, data: StepSaveRequest) => http.put<void>(`/steps/${id}`, data),
  setStatus: (id: number, status: string) =>
    http.patch<void>(`/steps/${id}/status`, { status }),
}
