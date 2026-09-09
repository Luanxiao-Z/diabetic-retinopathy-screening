import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { permission: 'biz:screening:view', title: '筛查看板' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    return true
  }
  if (!token) {
    return { path: '/login' }
  }
  const userStore = useUserStore()
  const required = to.meta.permission as string | undefined
  if (required && userStore.permissions.length && !userStore.permissions.includes(required)) {
    return { path: '/403' }
  }
  return true
})

export default router
