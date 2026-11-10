import { http, request } from './request'
import type {
  ProgressView,
  StoredFile,
  SubmissionItemInput,
  SubmissionView,
} from '@/types'

export const submissionApi = {
  submit: (stepId: number, items: SubmissionItemInput[]) =>
    http.post<number>(`/steps/${stepId}/submissions`, { items }),
  mySubmission: (stepId: number) =>
    http.get<SubmissionView | null>(`/steps/${stepId}/my-submission`),
  myVersions: (stepId: number) =>
    http.get<SubmissionView[]>(`/steps/${stepId}/my-submissions`),
  progress: (stepId: number) => http.get<ProgressView[]>(`/steps/${stepId}/submissions`),
  detail: (id: number) => http.get<SubmissionView>(`/submissions/${id}`),
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<StoredFile>('/files/upload', form)
  },
}

export async function downloadFile(fileId: number, filename: string): Promise<void> {
  const response = await request.get(`/files/${fileId}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
