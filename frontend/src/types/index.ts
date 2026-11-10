export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface UserInfo {
  userId: number
  username: string
  realName: string
  userNo: string | null
  roles: string[]
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface RegisterParams {
  username: string
  password: string
  realName: string
  userNo: string
  inviteCode: string
}

export interface HealthInfo {
  status: string
  version: string
  iteration: string
}

export interface UserView {
  id: number
  username: string
  realName: string
  userNo: string | null
  role: string
  status: string
  createdAt: string
}

export interface ConfigItem {
  key: string
  value: string
  description: string
}

export interface DeliverableType {
  code: string
  name: string
  frontendComponent: string
  backendHandler: string
  enabled: boolean
  sortOrder: number
  builtIn: number
}

export interface ClassView {
  id: number
  name: string
  description: string | null
  teacherId: number
  teacherName: string
  status: string
  studentCount: number
  courseCount: number
  createdAt: string
}

export interface MemberView {
  studentId: number
  username: string
  realName: string
  userNo: string | null
  status: string
  joinedAt: string
}

export interface InviteCodeView {
  id: number
  code: string
  expireAt: string | null
  maxUses: number
  usedCount: number
  enabled: boolean
  createdAt: string
}

export interface CourseView {
  id: number
  classId: number
  className: string
  name: string
  description: string | null
  status: string
  startAt: string | null
  endAt: string | null
  stepCount: number
  createdAt: string
}

export interface DeliverableItemView {
  id: number
  name: string
  typeCode: string
  typeName: string
  frontendComponent: string
  required: boolean
  constraintJson: string | null
  sortOrder: number
}

export interface StepView {
  id: number
  courseId: number
  title: string
  description: string | null
  requirement: string | null
  weight: number
  publishAt: string | null
  dueAt: string | null
  status: string
  allowResubmit: boolean
  sortOrder: number
  submissionCount: number
  myStatus: string | null
  items: DeliverableItemView[]
}

export interface DeliverableItemInput {
  name: string
  typeCode: string
  required: boolean
  constraintJson: string | null
  sortOrder: number
}

export interface StepSaveRequest {
  title: string
  description: string | null
  requirement: string | null
  weight: number
  publishAt: string | null
  dueAt: string | null
  status: string
  allowResubmit: boolean
  sortOrder: number
  items: DeliverableItemInput[]
}

export interface SubmissionItemInput {
  deliverableItemId: number
  textValue: string | null
  fileId: number | null
  urlValue: string | null
}

export interface SubmissionItemView {
  id: number
  deliverableItemId: number
  name: string
  typeCode: string
  typeName: string
  frontendComponent: string
  required: boolean
  textValue: string | null
  fileId: number | null
  originalName: string | null
  urlValue: string | null
}

export interface SubmissionView {
  id: number
  stepId: number
  studentId: number
  studentName: string
  version: number
  status: string
  latest: boolean
  submitAt: string
  score: number | null
  comment: string | null
  items: SubmissionItemView[]
}

export interface ProgressView {
  studentId: number
  username: string
  realName: string
  userNo: string | null
  submissionId: number | null
  status: string | null
  version: number | null
  submitAt: string | null
  score: number | null
  comment: string | null
}

export interface StepColumn {
  stepId: number
  title: string
  weight: number
}

export interface StepGrade {
  stepId: number
  title: string
  weight: number
  score: number | null
  comment: string | null
  submissionId: number | null
}

export interface StudentGradeRow {
  studentId: number
  username: string
  realName: string
  userNo: string | null
  steps: StepGrade[]
  totalScore: number
  gradedWeight: number
}

export interface GradeReport {
  courseId: number
  courseName: string
  columns: StepColumn[]
  rows: StudentGradeRow[]
}

export interface StoredFile {
  id: number
  originalName: string
  storagePath: string
  sizeBytes: number
  md5: string
}
