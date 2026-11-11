import { inject, onBeforeUnmount, type InjectionKey } from 'vue'
import { useRoute } from 'vue-router'

/**
 * 页面刷新注册表。
 *
 * 刷新入口统一收敛到多标签栏，页面不再各自摆放「刷新」按钮；
 * 页面只需把自身的 load 通过 usePageRefresh 注册一次，布局侧点击刷新时
 * 按当前路由取用对应处理器，避免整页 reload 丢掉 KeepAlive 状态。
 */
export type RefreshHandler = () => void | Promise<void>

export interface RefreshRegistry {
  register(path: string, handler: RefreshHandler): void
  unregister(path: string): void
}

export const REFRESH_KEY: InjectionKey<RefreshRegistry> = Symbol('drs:page-refresh')

/**
 * 在页面组件内调用，把 load 注册给布局的刷新入口。
 * 未注册的页面（例如纯静态页）刷新按钮会自动禁用。
 */
export function usePageRefresh(handler: RefreshHandler): void {
  const route = useRoute()
  const registry = inject(REFRESH_KEY, null)
  if (!registry) return

  const path = route.path
  registry.register(path, handler)
  onBeforeUnmount(() => registry.unregister(path))
}
