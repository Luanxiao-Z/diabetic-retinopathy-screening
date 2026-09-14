import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
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
app.use(ElementPlus)
setupPermissionDirective(app)

// 刷新后仅凭 localStorage 中的 token 无法恢复权限，需在挂载前向后端拉取档案/权限
const userStore = useUserStore()
const token = localStorage.getItem('token')
const bootstrap = token
  ? fetchProfile()
      .then((p) => {
        userStore.setProfile({ role: p.role, username: p.username, permissions: p.permissions })
      })
      .catch(() => {
        localStorage.removeItem('token')
      })
  : Promise.resolve()

bootstrap.finally(() => {
  app.mount('#app')
})
