import type { SubmissionItemInput } from '@/types'

export interface ItemConstraints {
  maxSizeMb: number
  formats: string[]
  minWords: number
  maxWords: number
  urlHosts: string[]
}

export const DEFAULT_CONSTRAINTS: ItemConstraints = {
  maxSizeMb: 50,
  formats: [],
  minWords: 1,
  maxWords: 20000,
  urlHosts: [],
}

export const DEFAULT_FILE_FORMATS: Record<string, string[]> = {
  DOCUMENT: ['pdf', 'docx', 'md'],
  CODE_ARCHIVE: ['zip', 'rar', 'gz'],
  OTHER_FILE: [],
  IMAGE: ['png', 'jpg', 'jpeg', 'gif', 'webp'],
}

export const DEFAULT_URL_HOSTS: Record<string, string[]> = {
  REPO_URL: ['github.com', 'gitee.com'],
}

function toInt(value: unknown, fallback: number): number {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? Math.trunc(number) : fallback
}

function toStrings(value: unknown): string[] {
  return Array.isArray(value) ? value.map((item) => String(item).toLowerCase()) : []
}

/** 与后端 Constraints.from 保持同一套默认值与字段，解析失败时回退默认值 */
export function parseConstraints(json: string | null | undefined): ItemConstraints {
  if (!json || !json.trim() || json.trim().toLowerCase() === 'null') {
    return { ...DEFAULT_CONSTRAINTS, formats: [], urlHosts: [] }
  }
  try {
    const raw = JSON.parse(json) as Record<string, unknown>
    return {
      maxSizeMb: toInt(raw.maxSizeMb, 50),
      formats: toStrings(raw.formats),
      minWords: toInt(raw.minWords, 1),
      maxWords: toInt(raw.maxWords, 20000),
      urlHosts: toStrings(raw.urlHosts),
    }
  } catch {
    return { ...DEFAULT_CONSTRAINTS, formats: [], urlHosts: [] }
  }
}

export function effectiveFormats(c: ItemConstraints, typeCode: string): string[] {
  return c.formats.length ? c.formats : (DEFAULT_FILE_FORMATS[typeCode] ?? [])
}

export function effectiveHosts(c: ItemConstraints, typeCode: string): string[] {
  return c.urlHosts.length ? c.urlHosts : (DEFAULT_URL_HOSTS[typeCode] ?? [])
}

export function acceptOf(c: ItemConstraints, typeCode: string): string {
  const formats = effectiveFormats(c, typeCode)
  return formats.length ? formats.map((item) => `.${item}`).join(',') : ''
}

export function constraintHint(
  c: ItemConstraints,
  component: string,
  typeCode: string,
): string {
  if (component === 'TextAreaItem') {
    return `需填写 ${c.minWords} ~ ${c.maxWords} 字`
  }
  if (component === 'UrlInputItem') {
    const hosts = effectiveHosts(c, typeCode)
    return hosts.length
      ? `需为 http(s) 链接，域名限于 ${hosts.join(' / ')}`
      : '需为 http(s) 开头的链接'
  }
  const formats = effectiveFormats(c, typeCode)
  const formatText = formats.length ? `支持 ${formats.join(' / ')} 格式，` : ''
  return `${formatText}单个文件不超过 ${c.maxSizeMb} MB`
}

export function emptyInput(deliverableItemId: number): SubmissionItemInput {
  return { deliverableItemId, textValue: null, fileId: null, urlValue: null }
}

export const constraintTemplates: { label: string; value: string }[] = [
  {
    label: '文档',
    value: JSON.stringify({ maxSizeMb: 20, formats: ['pdf', 'docx', 'md'] }, null, 2),
  },
  {
    label: '代码压缩包',
    value: JSON.stringify({ maxSizeMb: 50, formats: ['zip', 'rar', 'gz'] }, null, 2),
  },
  {
    label: '仓库地址',
    value: JSON.stringify({ urlHosts: ['github.com', 'gitee.com'] }, null, 2),
  },
  {
    label: '文本说明',
    value: JSON.stringify({ minWords: 20, maxWords: 2000 }, null, 2),
  },
]
