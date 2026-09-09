import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 权限 store：当前为轻量化角色权限，前端仅做路由级守卫；
 * 后端 @RequirePermission 为最终鉴权权威。
 */
export const usePermissionStore = defineStore('permission', () => {
  const dynamicRoutes = ref<unknown[]>([])

  function hasPermission(code: string, userPermissions: string[]) {
    return userPermissions.includes(code)
  }

  function setDynamicRoutes(routes: unknown[]) {
    dynamicRoutes.value = routes
  }

  return { dynamicRoutes, hasPermission, setDynamicRoutes }
})
