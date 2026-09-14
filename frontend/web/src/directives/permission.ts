import type { App, Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 指令：依据当前用户权限集控制元素显隐。
 * 用法：v-permission="'biz:screening:export'" 或 v-permission="['a','b']"（满足其一即可）。
 * 后端 @RequirePermission 为最终鉴权权威，此处仅为前端体验优化。
 */
const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const value = binding.value
    const codes = Array.isArray(value) ? value : [value]
    const has = codes.some((c) => userStore.permissions.includes(c))
    if (!has) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function setupPermissionDirective(app: App) {
  app.directive('permission', permission)
}
