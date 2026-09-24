import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import BasicLayout from '@/layouts/BasicLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { permission: 'biz:screening:view', title: '筛查看板', group: '综合看板', crumb: '筛查看板' }
      },
      {
        path: 'screening/upload',
        name: 'ScreeningUpload',
        component: () => import('@/views/screening/upload.vue'),
        meta: { permission: 'biz:screening:create', title: '筛查上传', group: '筛查业务', crumb: '筛查上传' }
      },
      {
        path: 'screening/records',
        name: 'ScreeningRecords',
        component: () => import('@/views/screening/record-list.vue'),
        meta: { permission: 'biz:screening:view', title: '筛查记录', group: '筛查业务', crumb: '筛查记录' }
      },
      {
        path: 'screening/statistics',
        name: 'ScreeningStatistics',
        component: () => import('@/views/screening/statistics.vue'),
        meta: { permission: 'biz:screening:view', title: '统计分析', group: '筛查业务', crumb: '统计分析' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', group: '账户', crumb: '个人中心' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { public: true, title: '无访问权限' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
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
  // 权限未就绪（刷新后尚未拉取）时不拦截，避免误跳转；就绪后由 v-permission 控制按钮
  if (required && userStore.permissions.length && !userStore.permissions.includes(required)) {
    return { path: '/403' }
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} · DR 智能筛查系统`
  }
  return true
})

export default router
