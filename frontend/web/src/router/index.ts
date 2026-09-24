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
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { public: true, title: '注册' }
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
        meta: { permission: 'biz:screening:view', title: '工作台', group: '综合看板', crumb: '工作台' }
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
        path: 'screening/todos',
        name: 'ScreeningTodos',
        component: () => import('@/views/screening/todo-center.vue'),
        meta: { permission: 'biz:screening:view', title: '随访待办', group: '筛查业务', crumb: '随访待办' }
      },
      {
        path: 'screening/statistics',
        name: 'ScreeningStatistics',
        component: () => import('@/views/screening/statistics.vue'),
        meta: { permission: 'biz:screening:view', title: '统计分析', group: '筛查业务', crumb: '统计分析' }
      },
      {
        path: 'screening/patients',
        name: 'PatientFollowUp',
        component: () => import('@/views/screening/patient-followup.vue'),
        meta: { permission: 'biz:screening:view', title: '患者随访', group: '筛查业务', crumb: '患者随访' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', group: '账户', crumb: '个人中心' }
      },
      {
        path: 'screening/records/:id/report',
        name: 'ScreeningReport',
        component: () => import('@/views/screening/report.vue'),
        meta: { permission: 'biz:screening:view', title: '诊断报告', group: '筛查业务', crumb: '诊断报告' }
      },
      {
        path: 'model-info',
        name: 'ModelInfo',
        component: () => import('@/views/model-info/index.vue'),
        meta: { permission: 'biz:screening:view', title: '模型信息', group: '综合看板', crumb: '模型信息' }
      },
      {
        path: 'guide',
        name: 'Guide',
        component: () => import('@/views/guide/index.vue'),
        meta: { title: '使用指南', group: '帮助', crumb: '使用指南' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/about/index.vue'),
        meta: { title: '关于系统', group: '帮助', crumb: '关于系统' }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/user-list.vue'),
        meta: { permission: 'admin:user:view', title: '用户管理', group: '系统管理', crumb: '用户管理' }
      },
      {
        path: 'admin/dicts',
        name: 'AdminDicts',
        component: () => import('@/views/admin/dict-manage.vue'),
        meta: { permission: 'admin:dict:view', title: '字典管理', group: '系统管理', crumb: '字典管理' }
      },
      {
        path: 'admin/logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/operation-log.vue'),
        meta: { permission: 'admin:log:view', title: '操作日志', group: '系统管理', crumb: '操作日志' }
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
