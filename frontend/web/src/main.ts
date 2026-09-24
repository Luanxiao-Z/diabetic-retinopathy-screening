import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'
import { setupPermissionDirective } from './directives/permission'
import { useUserStore } from './stores/user'
import { fetchProfile } from './api/user'

const app = createApp(App)
app.use(createPinia())
app.use(router)
// 全局中文语言包：分页（共 N 条 / 条/页 / 前往）、表格空态（暂无数据）、
// 日期面板、图片加载失败提示、对话框默认按钮等均由组件内部文案渲染，
// 不配置 locale 会以英文显示。
app.use(ElementPlus, { locale: zhCn })
setupPermissionDirective(app)

// 刷新后仅凭 localStorage 中的 token 无法恢复权限，需在挂载前向后端拉取档案/权限
const userStore = useUserStore()
const token = localStorage.getItem('token')
const bootstrap = token
  ? fetchProfile()
      .then((p) => {
        userStore.setProfile({
          role: p.role,
          username: p.username,
          permissions: p.permissions,
          dataScope: p.dataScope
        })
      })
      .catch(() => {
        localStorage.removeItem('token')
      })
  : Promise.resolve()

bootstrap.finally(() => {
  app.mount('#app')
})
