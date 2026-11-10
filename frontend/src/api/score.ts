import { http } from './request'
import type { GradeReport } from '@/types'

export const scoreApi = {
  grade: (submissionId: number, totalScore: number, comment: string) =>
    http.post<void>(`/submissions/${submissionId}/score`, { totalScore, comment }),
  report: (courseId: number) => http.get<GradeReport>(`/courses/${courseId}/grades`),
  myReport: (courseId: number) => http.get<GradeReport>(`/courses/${courseId}/my-grades`),
}
