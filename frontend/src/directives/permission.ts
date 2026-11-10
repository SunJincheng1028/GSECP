import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限：v-permission="['ADMIN']"，无权限时移除元素。
 * 迭代 1 接入后端权限点后可改为按 permission code 判断。
 */
export const permission: Directive<HTMLElement, string[]> = {
  mounted(el, binding) {
    const store = useUserStore()
    if (!store.hasRole(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}
