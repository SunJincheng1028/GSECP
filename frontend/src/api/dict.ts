import { http } from './request'
import type { ConfigItem, DeliverableType } from '@/types'

export const dictApi = {
  types: () => http.get<DeliverableType[]>('/dict/deliverable-types'),
  setEnabled: (code: string, enabled: boolean) =>
    http.patch<void>(`/dict/deliverable-types/${code}`, { enabled }),
  config: () => http.get<ConfigItem[]>('/admin/config'),
  updateConfig: (key: string, value: string) =>
    http.put<void>(`/admin/config/${key}`, { value }),
}
