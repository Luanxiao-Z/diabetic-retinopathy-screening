<template>
  <div class="layout" :class="`mode-${effectiveMode}`">
    <!-- ===================== 侧栏 ===================== -->
    <aside class="sider" :aria-hidden="effectiveMode === 'hide'">
      <div class="sider-brand">
        <span class="brand-mark" aria-hidden="true">
          <AppIcon name="scan" :size="20" />
        </span>
        <span class="brand-text">
          <span class="brand-title">DR 智能筛查</span>
          <span class="brand-sub">糖尿病视网膜病变</span>
        </span>
      </div>

      <nav class="sider-nav" aria-label="主导航">
        <div v-for="g in visibleGroups" :key="g.key" class="nav-group">
          <button
            v-if="effectiveMode === 'full'"
            type="button"
            class="nav-group-head"
            :aria-expanded="!collapsedGroups.includes(g.key)"
            @click="toggleGroup(g.key)"
          >
            <span>{{ g.title }}</span>
            <AppIcon
              name="chevronDown"
              :size="14"
              class="ng-caret"
              :class="{ 'is-collapsed': collapsedGroups.includes(g.key) }"
            />
          </button>
          <div v-else class="nav-group-divider" aria-hidden="true"></div>

          <div v-show="!collapsedGroups.includes(g.key)" class="nav-group-body">
            <RouterLink
              v-for="item in g.items"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              :title="item.title"
            >
              <span class="nav-ico" aria-hidden="true"><AppIcon :name="item.icon" :size="18" /></span>
              <span class="nav-txt">{{ item.title }}</span>
            </RouterLink>
          </div>
        </div>

        <p v-if="!visibleGroups.length" class="nav-empty">
          当前账号未分配任何业务权限，请联系系统管理员。
        </p>
      </nav>

      <div class="sider-foot">
        <span class="sider-foot-text">v1.0 · 本地部署</span>
      </div>
    </aside>

    <!-- ===================== 主区 ===================== -->
    <div class="body">
      <header class="topbar">
        <button
          type="button"
          class="icon-btn"
          :title="siderToggleTip"
          :aria-label="siderToggleTip"
          @click="cycleSider"
        >
          <AppIcon :name="effectiveMode === 'hide' ? 'menu' : 'chevronLeft'" :size="18" />
        </button>

        <div class="topbar-title">{{ currentTitle }}</div>

        <div class="topbar-spacer"></div>

        <button
          type="button"
          class="guide-btn"
          title="使用指南"
          aria-label="打开使用指南"
          @click="router.push('/guide')"
        >
          <AppIcon name="book" :size="15" />
          <span class="guide-txt">使用指南</span>
        </button>

        <span class="drs-scope-bar scope-badge" :title="scopeTip">
          <AppIcon name="database" :size="13" />
          <span class="scope-label">数据范围</span>
          <b>{{ scopeText }}</b>
        </span>

        <el-dropdown trigger="click" @command="onCommand">
          <button type="button" class="user-btn" aria-label="账号菜单">
            <span class="user-avatar" aria-hidden="true">{{ avatarText }}</span>
            <span class="user-meta">
              <span class="user-name">{{ userStore.username || '未登录' }}</span>
              <span class="user-role">{{ roleText }}</span>
            </span>
            <AppIcon name="chevronDown" :size="14" class="user-caret" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="content">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@/stores/user'

interface NavItem {
  path: string
  title: string
  icon: string
  permission?: string
}
interface NavGroup {
  key: string
  title: string
  items: NavItem[]
}

const NAV_GROUPS: NavGroup[] = [
  {
    key: 'overview',
    title: '综合看板',
    items: [{ path: '/dashboard', title: '筛查看板', icon: 'dashboard', permission: 'biz:screening:view' }]
  },
  {
    key: 'screening',
    title: '筛查业务',
    items: [
      { path: '/screening/upload', title: '筛查上传', icon: 'upload', permission: 'biz:screening:create' },
      { path: '/screening/records', title: '筛查记录', icon: 'list', permission: 'biz:screening:view' },
      { path: '/screening/todos', title: '随访待办', icon: 'inbox', permission: 'biz:screening:view' },
      { path: '/screening/patients', title: '患者随访', icon: 'activity', permission: 'biz:screening:view' },
      { path: '/screening/statistics', title: '统计分析', icon: 'chart', permission: 'biz:screening:view' }
    ]
  },
  {
    key: 'account',
    title: '账户',
    items: [
      { path: '/profile', title: '个人中心', icon: 'user' },
      { path: '/guide', title: '使用指南', icon: 'book' }
    ]
  },
  {
    key: 'system',
    title: '系统管理',
    items: [
      { path: '/admin/users', title: '用户管理', icon: 'users', permission: 'admin:user:view' },
      { path: '/admin/dicts', title: '字典管理', icon: 'layers', permission: 'admin:dict:view' },
      { path: '/admin/logs', title: '操作日志', icon: 'clock', permission: 'admin:log:view' }
    ]
  }
]

const SIDER_KEY = 'drs_sider_mode'
type SiderMode = 'full' | 'icon' | 'hide'
const SIDER_ORDER: SiderMode[] = ['full', 'icon', 'hide']
const SIDER_LABEL: Record<SiderMode, string> = { full: '展开', icon: '图标', hide: '隐藏' }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/* ---------- 侧栏模式：用户偏好 + 视口宽度取更保守者 ---------- */
const userMode = ref<SiderMode>((() => {
  const v = localStorage.getItem(SIDER_KEY)
  return v === 'icon' || v === 'hide' || v === 'full' ? v : 'full'
})())

const viewportWidth = ref(window.innerWidth)
function onResize() {
  viewportWidth.value = window.innerWidth
}
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))

const effectiveMode = computed<SiderMode>(() => {
  // 窄屏自动降级，避免内容被挤压
  if (viewportWidth.value < 768) return 'hide'
  if (viewportWidth.value < 1024) return 'icon'
  return userMode.value
})

const siderToggleTip = computed(() => {
  const next = SIDER_ORDER[(SIDER_ORDER.indexOf(userMode.value) + 1) % SIDER_ORDER.length]
  return `导航栏：当前${SIDER_LABEL[userMode.value]}，点击切换为${SIDER_LABEL[next]}`
})

function cycleSider() {
  userMode.value = SIDER_ORDER[(SIDER_ORDER.indexOf(userMode.value) + 1) % SIDER_ORDER.length]
  localStorage.setItem(SIDER_KEY, userMode.value)
}

/* ---------- 分组折叠 ---------- */
const collapsedGroups = ref<string[]>([])
function toggleGroup(key: string) {
  collapsedGroups.value = collapsedGroups.value.includes(key)
    ? collapsedGroups.value.filter((k) => k !== key)
    : [...collapsedGroups.value, key]
}

/* ---------- 菜单权限过滤 ---------- */
const visibleGroups = computed(() =>
  NAV_GROUPS.map((g) => ({
    ...g,
    items: g.items.filter((i) => !i.permission || userStore.permissions.includes(i.permission))
  })).filter((g) => g.items.length > 0)
)

/* ---------- 顶栏信息 ---------- */
const currentTitle = computed(() => (route.meta.title as string) || 'DR 智能筛查系统')
const avatarText = computed(() =>
  userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U'
)
const roleText = computed(() => {
  if (userStore.role === 'ADMIN') return '管理员'
  if (userStore.role === 'DOCTOR') return '医生'
  return '未分配角色'
})
const scopeText = computed(() => {
  if (userStore.dataScope === 'ALL') return '全部数据'
  if (userStore.dataScope === 'SELF') return '仅本人数据'
  return userStore.role === 'ADMIN' ? '全部数据' : '仅本人数据'
})
const scopeTip = computed(() =>
  userStore.dataScope === 'ALL'
    ? '当前账号可查看全部筛查记录'
    : '当前账号仅可查看本人创建的筛查记录'
)

function onCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--drs-bg);
}

/* ===================== 侧栏 ===================== */
.sider {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: var(--drs-sider-w);
  background: var(--drs-surface);
  border-right: 1px solid var(--drs-border);
  transition: width 0.2s ease;
  overflow: hidden;
}

.mode-icon .sider {
  width: var(--drs-sider-w-icon);
}

.mode-hide .sider {
  width: 0;
  border-right: none;
}

.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--drs-header-h);
  padding: 0 16px;
  border-bottom: 1px solid var(--drs-border);
  flex-shrink: 0;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--drs-primary) 0%, var(--drs-primary-800) 100%);
  color: #fff;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  white-space: nowrap;
}

.brand-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--drs-ink-900);
}

.brand-sub {
  font-size: 11px;
  color: var(--drs-ink-500);
}

.sider-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 10px 16px;
}

.nav-group + .nav-group {
  margin-top: 6px;
}

.nav-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 8px 6px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: var(--drs-ink-400);
  white-space: nowrap;
}

.nav-group-head:hover {
  color: var(--drs-ink-600);
}

.ng-caret {
  transition: transform 0.18s ease;
}

.ng-caret.is-collapsed {
  transform: rotate(-90deg);
}

.nav-group-divider {
  height: 1px;
  margin: 8px 10px;
  background: var(--drs-border);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 10px;
  margin-bottom: 2px;
  border-radius: var(--drs-radius-sm);
  color: var(--drs-ink-600);
  font-size: 13.5px;
  white-space: nowrap;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.nav-item:hover {
  background: var(--drs-ink-50);
  color: var(--drs-ink-900);
}

.nav-item.router-link-active {
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-weight: 600;
}

.nav-item.router-link-active .nav-ico {
  color: var(--drs-primary);
}

.nav-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  flex-shrink: 0;
  color: var(--drs-ink-500);
}

.mode-icon .nav-txt,
.mode-icon .brand-text,
.mode-icon .sider-foot-text {
  display: none;
}

.mode-icon .nav-item {
  justify-content: center;
  padding: 0;
}

.mode-icon .sider-brand {
  justify-content: center;
  padding: 0;
}

.nav-empty {
  padding: 20px 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--drs-ink-500);
}

.sider-foot {
  flex-shrink: 0;
  padding: 12px 16px;
  border-top: 1px solid var(--drs-border);
  font-size: 11px;
  color: var(--drs-ink-400);
  white-space: nowrap;
}

/* ===================== 主区 ===================== */
.body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  height: var(--drs-header-h);
  padding: 0 20px;
  flex-shrink: 0;
  background: var(--drs-surface);
  border-bottom: 1px solid var(--drs-border);
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: 1px solid var(--drs-border);
  border-radius: var(--drs-radius-sm);
  background: var(--drs-surface);
  color: var(--drs-ink-600);
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.icon-btn:hover {
  background: var(--drs-ink-50);
  border-color: var(--drs-border-strong);
  color: var(--drs-ink-900);
}

.topbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--drs-ink-800);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-spacer {
  flex: 1;
}

.scope-badge {
  flex-shrink: 0;
}

/* 使用指南入口 */
.guide-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  flex-shrink: 0;
  border: 1px solid var(--drs-border);
  border-radius: 999px;
  background: var(--drs-surface);
  color: var(--drs-ink-600);
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.guide-btn:hover {
  background: var(--drs-primary-50);
  border-color: var(--drs-primary-200);
  color: var(--drs-primary-800);
}

.user-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: var(--drs-radius-sm);
  background: none;
  cursor: pointer;
  font: inherit;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.user-btn:hover {
  background: var(--drs-ink-50);
  border-color: var(--drs-border);
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 13px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.user-role {
  font-size: 11px;
  color: var(--drs-ink-500);
}

.user-caret {
  color: var(--drs-ink-400);
}

.content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 页面切换动画：轻微位移 + 淡入，遵循 prefers-reduced-motion */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-leave-to {
  opacity: 0;
}

/* ===================== 响应式 ===================== */
@media (max-width: 900px) {
  .topbar {
    padding: 0 12px;
    gap: 8px;
  }

  .scope-label,
  .user-meta,
  .guide-txt {
    display: none;
  }
}
</style>
