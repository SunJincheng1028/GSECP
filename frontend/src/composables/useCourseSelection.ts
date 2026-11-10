import { reactive } from 'vue'
import { classApi } from '@/api/class'
import type { ClassView, CourseView } from '@/types'

/** 班级 / 课程两级联动选择，供步骤配置、批改、成绩、学生任务页复用 */
export function useCourseSelection() {
  const state = reactive({
    classes: [] as ClassView[],
    courses: [] as CourseView[],
    classId: undefined as number | undefined,
    courseId: undefined as number | undefined,
    classesLoading: false,
    coursesLoading: false,
  })

  async function loadClasses(): Promise<void> {
    state.classesLoading = true
    try {
      state.classes = await classApi.list()
      if (!state.classes.some((item) => item.id === state.classId)) {
        state.classId = state.classes[0]?.id
      }
    } finally {
      state.classesLoading = false
    }
  }

  async function loadCourses(): Promise<void> {
    state.courseId = undefined
    state.courses = []
    if (state.classId == null) {
      return
    }
    state.coursesLoading = true
    try {
      state.courses = await classApi.courses(state.classId)
      state.courseId = state.courses[0]?.id
    } finally {
      state.coursesLoading = false
    }
  }

  /** 从其他页面带 courseId 跳转时，反查所属班级并选中该课程 */
  async function selectCourseId(target: number): Promise<void> {
    const course = await classApi.course(target)
    state.classId = course.classId
    await loadCourses()
    state.courseId = state.courses.some((item) => item.id === target) ? target : undefined
  }

  /** 从其他页面带 stepId 跳转时，反查所属课程并选中该步骤对应的课程 */
  async function selectStepId(target: number, lookup: (stepId: number) => Promise<number>): Promise<void> {
    const courseId = await lookup(target)
    await selectCourseId(courseId)
  }

  return { state, loadClasses, loadCourses, selectCourseId, selectStepId }
}
